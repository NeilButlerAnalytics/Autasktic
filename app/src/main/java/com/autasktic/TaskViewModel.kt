package com.autasktic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autasktic.data.AppDatabase
import com.autasktic.data.TaskEntity
import com.autasktic.notifications.TaskAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).taskDao()
    private val preferences = application.getSharedPreferences("autasktic_preferences", 0)
    private val colourMutex = Mutex()
    private val _accentColour = MutableStateFlow(
        preferences.getString("accent_colour", "#6D5E8C") ?: "#6D5E8C",
    )
    val accentColour: StateFlow<String> = _accentColour.asStateFlow()
    private val _customAccentColours = MutableStateFlow(
        preferences.getString("custom_accent_colours", "")
            ?.split(',')
            ?.filter(String::isNotBlank)
            ?: emptyList(),
    )
    val customAccentColours: StateFlow<List<String>> = _customAccentColours.asStateFlow()
    private val _taskFontSize = MutableStateFlow(
        preferences.getFloat("task_font_size", 16f),
    )
    val taskFontSize: StateFlow<Float> = _taskFontSize.asStateFlow()
    private val _pendingTaskColour = MutableStateFlow(TaskColours.palette.first())
    val pendingTaskColour: StateFlow<String> = _pendingTaskColour.asStateFlow()

    fun observeActiveTasks(date: LocalDate): Flow<List<TaskEntity>> =
        dao.observeActiveTasks(date.toString())

    fun observeCompletedTasks(date: LocalDate): Flow<List<TaskEntity>> =
        dao.observeCompletedTasks(date.toString())

    fun prepareNewTaskColour() {
        viewModelScope.launch {
            colourMutex.withLock {
                _pendingTaskColour.value = TaskColours.choose(dao.getUsedColours().toSet())
            }
        }
    }

    fun addTask(
        description: String,
        date: LocalDate,
        time: LocalTime?,
        colour: String? = null,
    ) {
        viewModelScope.launch {
            colourMutex.withLock {
                val taskDate = date.coerceAtLeast(LocalDate.now())
                val task = TaskEntity(
                    description = description.trim(),
                    date = taskDate.toString(),
                    time = time?.toString(),
                    colour = colour ?: TaskColours.choose(dao.getUsedColours().toSet()),
                    createdAt = LocalDateTime.now().toString(),
                )
                val taskId = dao.insert(task)
                TaskAlarmScheduler.schedule(
                    getApplication(),
                    task.copy(id = taskId),
                )
            }
        }
    }

    fun completeTask(taskId: Long, steps: String? = null) {
        viewModelScope.launch {
            val task = dao.getById(taskId)
            if (steps != null) {
                dao.setSteps(taskId, steps)
            }
            dao.complete(taskId, LocalDateTime.now().toString())
            TaskAlarmScheduler.cancel(getApplication(), taskId)
            task?.let { createNextRoutineTask(it) }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            dao.delete(taskId)
            TaskAlarmScheduler.cancel(getApplication(), taskId)
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.insert(task)
        }
    }

    fun reopenTask(taskId: Long) {
        viewModelScope.launch {
            dao.reopen(taskId)
            dao.getById(taskId)?.let { TaskAlarmScheduler.schedule(getApplication(), it) }
        }
    }

    fun rescheduleActiveTimedTasks() {
        viewModelScope.launch {
            dao.getActiveTimedTasks().forEach {
                TaskAlarmScheduler.schedule(getApplication(), it)
            }
        }
    }

    fun carryOverOverdueTasks() {
        viewModelScope.launch {
            val today = LocalDate.now()
            dao.getOverdueActiveTasks(today.toString()).forEach { task ->
                TaskAlarmScheduler.cancel(getApplication(), task.id)
                dao.carryOver(task.id, today.toString())
                dao.getById(task.id)?.let {
                    TaskAlarmScheduler.schedule(getApplication(), it)
                }
            }
        }
    }

    fun refreshTimedTasks() {
        viewModelScope.launch {
            val today = LocalDate.now()
            dao.getOverdueActiveTasks(today.toString()).forEach { task ->
                TaskAlarmScheduler.cancel(getApplication(), task.id)
                dao.carryOver(task.id, today.toString())
            }
            dao.getActiveTimedTasks().forEach {
                TaskAlarmScheduler.schedule(getApplication(), it)
            }
        }
    }

    fun updateTask(task: TaskEntity, description: String, date: LocalDate, time: LocalTime?) {
        updateTask(task, description, date, time, task.reminderMinutes, task.steps)
    }

    fun updateTask(
        task: TaskEntity,
        description: String,
        date: LocalDate,
        time: LocalTime?,
        reminderMinutes: Int?,
        steps: String,
    ) {
        viewModelScope.launch {
            TaskAlarmScheduler.cancel(getApplication(), task.id)
            val taskDate = date.coerceAtLeast(LocalDate.now())
            val updatedTask = task.copy(
                description = description.trim(),
                date = taskDate.toString(),
                time = time?.toString(),
                wasCarriedOver = false,
                reminderMinutes = reminderMinutes,
                steps = steps,
            )
            dao.update(updatedTask)
            TaskAlarmScheduler.schedule(getApplication(), updatedTask)
        }
    }

    fun rescheduleTask(task: TaskEntity, date: LocalDate) {
        updateTask(
            task,
            task.description,
            date,
            task.time?.let(LocalTime::parse),
            task.reminderMinutes,
            task.steps,
        )
    }

    fun setStatus(taskId: Long, status: String) {
        viewModelScope.launch {
            dao.setStatus(taskId, status)
        }
    }

    fun setAccentColour(colour: String) {
        preferences.edit().putString("accent_colour", colour).apply()
        _accentColour.value = colour
    }

    fun saveCustomAccentColour(colour: String) {
        val colours = (_customAccentColours.value + colour).distinct()
        preferences.edit().putString("custom_accent_colours", colours.joinToString(",")).apply()
        _customAccentColours.value = colours
        setAccentColour(colour)
    }

    fun setTaskFontSize(size: Float) {
        val boundedSize = size.coerceIn(12f, 28f)
        preferences.edit().putFloat("task_font_size", boundedSize).apply()
        _taskFontSize.value = boundedSize
    }

    fun setFocus(taskId: Long, focused: Boolean) {
        viewModelScope.launch {
            if (focused) dao.clearFocus()
            dao.setFocus(taskId, focused)
        }
    }

    fun setReminder(taskId: Long, minutes: Int?) {
        viewModelScope.launch {
            dao.setReminder(taskId, minutes)
            dao.getById(taskId)?.let {
                TaskAlarmScheduler.cancel(getApplication(), taskId)
                TaskAlarmScheduler.schedule(getApplication(), it)
            }
        }
    }

    fun setSteps(taskId: Long, steps: String) {
        viewModelScope.launch {
            dao.setSteps(taskId, steps)
        }
    }

    fun setRoutine(taskId: Long, recurrence: String?) {
        viewModelScope.launch {
            dao.setRecurrence(taskId, recurrence)
        }
    }

    private suspend fun createNextRoutineTask(task: TaskEntity) {
        val recurrence = task.recurrence ?: return
        val nextDate = nextRoutineDate(LocalDate.parse(task.date), recurrence)
            ?: return
        val nextTask = task.copy(
            id = 0,
            date = nextDate.toString(),
            isCompleted = false,
            status = "NOT_STARTED",
            isFocus = false,
            completedAt = null,
            wasCarriedOver = false,
            createdAt = LocalDateTime.now().toString(),
        )
        val nextId = dao.insert(nextTask)
        TaskAlarmScheduler.schedule(getApplication(), nextTask.copy(id = nextId))
    }

    private fun nextRoutineDate(date: LocalDate, recurrence: String): LocalDate? =
        when {
            recurrence == "DAILY" -> date.plusDays(1)
            recurrence == "WEEKDAYS" -> {
                var next = date.plusDays(1)
                while (next.dayOfWeek.value > 5) next = next.plusDays(1)
                next
            }
            recurrence == "WEEKENDS" -> {
                var next = date.plusDays(1)
                while (next.dayOfWeek.value < 6) next = next.plusDays(1)
                next
            }
            recurrence == "WEEKLY" -> date.plusWeeks(1)
            recurrence.startsWith("DAYS:") -> {
                val days = recurrence.removePrefix("DAYS:")
                    .split(',')
                    .mapNotNull(String::toIntOrNull)
                    .toSet()
                var next = date.plusDays(1)
                repeat(7) {
                    if (next.dayOfWeek.value in days) return next
                    next = next.plusDays(1)
                }
                null
            }
            else -> null
        }

    fun moveForward(taskId: Long, currentDate: LocalDate, days: Long) {
        viewModelScope.launch {
            dao.moveForward(taskId, currentDate.plusDays(days).toString())
        }
    }

}

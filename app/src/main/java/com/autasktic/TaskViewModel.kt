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
    private val colourMutex = Mutex()
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
                val task = TaskEntity(
                    description = description.trim(),
                    date = date.toString(),
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

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            dao.complete(taskId, LocalDateTime.now().toString())
            TaskAlarmScheduler.cancel(getApplication(), taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            dao.delete(taskId)
            TaskAlarmScheduler.cancel(getApplication(), taskId)
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
        viewModelScope.launch {
            TaskAlarmScheduler.cancel(getApplication(), task.id)
            val updatedTask = task.copy(
                description = description.trim(),
                date = date.toString(),
                time = time?.toString(),
                wasCarriedOver = false,
            )
            dao.update(updatedTask)
            TaskAlarmScheduler.schedule(getApplication(), updatedTask)
        }
    }

    fun moveForward(taskId: Long, currentDate: LocalDate, days: Long) {
        viewModelScope.launch {
            dao.moveForward(taskId, currentDate.plusDays(days).toString())
        }
    }

}

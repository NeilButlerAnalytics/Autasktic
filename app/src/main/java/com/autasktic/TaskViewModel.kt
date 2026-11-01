package com.autasktic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autasktic.data.AppDatabase
import com.autasktic.data.TaskEntity
import com.autasktic.notifications.TaskAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).taskDao()

    fun observeActiveTasks(date: LocalDate): Flow<List<TaskEntity>> =
        dao.observeActiveTasks(date.toString())

    fun observeCompletedTasks(date: LocalDate): Flow<List<TaskEntity>> =
        dao.observeCompletedTasks(date.toString())

    fun addTask(description: String, date: LocalDate, time: LocalTime?) {
        viewModelScope.launch {
            val task = TaskEntity(
                description = description.trim(),
                date = date.toString(),
                time = time?.toString(),
                colour = pastelColours.random(),
                createdAt = LocalDateTime.now().toString(),
            )
            val taskId = dao.insert(task)
            TaskAlarmScheduler.schedule(
                getApplication(),
                task.copy(id = taskId),
            )
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            dao.complete(taskId, LocalDateTime.now().toString())
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

    fun updateTask(task: TaskEntity, description: String, date: LocalDate, time: LocalTime?) {
        viewModelScope.launch {
            TaskAlarmScheduler.cancel(getApplication(), task.id)
            val updatedTask = task.copy(
                description = description.trim(),
                date = date.toString(),
                time = time?.toString(),
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

    private companion object {
        val pastelColours = listOf("#D9EAD3", "#D0E0E3", "#FCE5CD", "#EADCF8", "#FFF2CC")
    }
}

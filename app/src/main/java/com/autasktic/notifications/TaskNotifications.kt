package com.autasktic.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.autasktic.MainActivity
import com.autasktic.data.AppDatabase
import com.autasktic.data.TaskEntity
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "task_due"
private const val TASK_ID_EXTRA = "task_id"
private const val KIND_EXTRA = "kind"
private const val DUE_KIND = 0
private const val REMINDER_KIND = 1

object TaskAlarmScheduler {
    fun schedule(context: Context, task: TaskEntity) {
        val time = task.time ?: return
        val triggerAt = LocalDateTime.parse("${task.date}T$time")
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        scheduleAlarm(context, task, triggerAt, DUE_KIND)
        task.reminderMinutes?.let { minutes ->
            val reminderAt = triggerAt - minutes * 60_000L
            if (reminderAt > System.currentTimeMillis()) {
                scheduleAlarm(context, task, reminderAt, REMINDER_KIND)
            }
        }
    }

    private fun scheduleAlarm(context: Context, task: TaskEntity, triggerAt: Long, kind: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = pendingIntent(context, task.id, kind)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, taskId, DUE_KIND))
        alarmManager.cancel(pendingIntent(context, taskId, REMINDER_KIND))
    }

    private fun pendingIntent(context: Context, taskId: Long, kind: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (taskId * 2 + kind).hashCode(),
            Intent(context, TaskAlarmReceiver::class.java)
                .putExtra(TASK_ID_EXTRA, taskId)
                .putExtra(KIND_EXTRA, kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TASK_ID_EXTRA, -1L)
        if (taskId < 0) return
        val kind = intent.getIntExtra(KIND_EXTRA, DUE_KIND)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = AppDatabase.getInstance(context).taskDao().getById(taskId)
                if (task != null && !task.isCompleted) {
                    TaskNotificationHelper.show(context, task, kind == REMINDER_KIND)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class TaskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(context).taskDao().getActiveTimedTasks()
                    .forEach { TaskAlarmScheduler.schedule(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object TaskNotificationHelper {
    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Task due notifications",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for scheduled incomplete tasks"
            },
        )
    }

    fun show(context: Context, task: TaskEntity, isReminder: Boolean = false) {
        createChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            task.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isReminder) "Upcoming task" else "Task due")
            .setContentText(task.description)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
    }
}

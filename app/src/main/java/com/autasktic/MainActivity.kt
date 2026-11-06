package com.autasktic

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autasktic.data.TaskEntity
import com.autasktic.voice.OnDeviceVoiceRecognizer
import com.autasktic.voice.VoiceTaskParser
import com.autasktic.notifications.TaskNotificationHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val taskViewModel by viewModels<TaskViewModel>()

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(
                    android.content.Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName"),
                    ),
                )
            } else {
                taskViewModel.refreshTimedTasks()
            }
        } else {
            taskViewModel.refreshTimedTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val accentColour by taskViewModel.accentColour.collectAsStateWithLifecycle()
            AutaskticTheme(accentColour) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TodayScreen(taskViewModel)
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(viewModel: TaskViewModel) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }
    val tasks by viewModel.observeActiveTasks(selectedDate)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val completedTasks by viewModel.observeCompletedTasks(selectedDate)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingTaskColour by viewModel.pendingTaskColour.collectAsStateWithLifecycle()
    val accentColour by viewModel.accentColour.collectAsStateWithLifecycle()
    val customAccentColours by viewModel.customAccentColours.collectAsStateWithLifecycle()
    val taskFontSize by viewModel.taskFontSize.collectAsStateWithLifecycle()
    val carriedOverTasks = tasks.filter { it.wasCarriedOver && it.time == null }
    val doneForNowTasks = tasks.filter {
        it.status == "DONE_FOR_NOW" && it.time == null
    }
    val regularTasks = tasks.filter { task ->
        task.time != null || (!task.wasCarriedOver && task.status != "DONE_FOR_NOW")
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    val carriedOverExpandedByDate = remember { mutableStateMapOf<LocalDate, Boolean>() }
    val doneForNowExpandedByDate = remember { mutableStateMapOf<LocalDate, Boolean>() }
    val carriedOverExpanded = carriedOverExpandedByDate[selectedDate] ?: false
    val doneForNowExpanded = doneForNowExpandedByDate[selectedDate] ?: false
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var actionTask by remember { mutableStateOf<TaskEntity?>(null) }
    var pendingActionTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showBreakdownDialog by remember { mutableStateOf(false) }
    var showRoutineDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(durationMillis = 140),
        label = "date swipe feedback",
    )
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var recentlyDeletedTask by remember { mutableStateOf<TaskEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isDateTransitioning by remember { mutableStateOf(false) }
    var dateTransitionJob by remember { mutableStateOf<Job?>(null) }
    val dateTransitionScope = rememberCoroutineScope()
    val pageAlpha by animateFloatAsState(
        targetValue = if (isDateTransitioning) 0f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "date page fade",
    )
    val context = LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) showVoiceDialog = true
    }
    val notificationPermissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }
    LaunchedEffect(Unit) {
        TaskNotificationHelper.createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun moveToDate(newDate: LocalDate) {
        if (newDate == selectedDate) return
        dateTransitionJob?.cancel()
        isDateTransitioning = true
        dateTransitionJob = dateTransitionScope.launch {
            delay(180)
            selectedDate = newDate
            delay(24)
            isDateTransitioning = false
        }
    }
    LaunchedEffect(recentlyDeletedTask?.id) {
            val deletedTask = recentlyDeletedTask ?: return@LaunchedEffect
            val result = snackbarHostState.showSnackbar(
                message = "Task deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Long,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.restoreTask(deletedTask)
            }
            recentlyDeletedTask = null
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
                .graphicsLayer { alpha = pageAlpha }
                .blur(
                    if (showAddDialog || selectedTask != null || showVoiceDialog ||
                        actionTask != null || pendingActionTask != null
                    ) 10.dp else 0.dp,
                ),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedSwipeOffset }
                .pointerInput(selectedDate) {
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                            swipeOffset = (swipeOffset + amount).coerceIn(-48f, 48f)
                        },
                        onDragEnd = {
                            if (abs(dragDistance) > 80f) {
                                moveToDate(if (dragDistance > 0) {
                                    selectedDate.minusDays(1)
                                } else {
                                    selectedDate.plusDays(1)
                                })
                            }
                            swipeOffset = 0f
                        },
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { showDatePicker = true },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (selectedDate == today) "Today" else "Tasks",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = selectedDate.format(
                        DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (selectedDate != today) {
            TextButton(
                onClick = { moveToDate(today) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Return to today")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { if (selectedDate >= today) showAddDialog = true },
                enabled = selectedDate >= today,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Add by Text")
            }
            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        showVoiceDialog = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = selectedDate >= today,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Add by Voice")
            }
        }
        if (carriedOverTasks.isNotEmpty() || doneForNowTasks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (carriedOverTasks.isNotEmpty()) {
                        TaskGroupHeader(
                            title = "Carried over",
                            count = carriedOverTasks.size,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                carriedOverExpandedByDate[selectedDate] = !carriedOverExpanded
                            },
                        )
                    }
                    if (doneForNowTasks.isNotEmpty()) {
                        TaskGroupHeader(
                            title = "Done for now",
                            count = doneForNowTasks.size,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                doneForNowExpandedByDate[selectedDate] = !doneForNowExpanded
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(selectedDate) {
                    var dragDistance = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                            swipeOffset = (swipeOffset + amount).coerceIn(-48f, 48f)
                        },
                        onDragEnd = {
                            if (abs(dragDistance) > 80f) {
                                moveToDate(if (dragDistance > 0) {
                                    selectedDate.minusDays(1)
                                } else {
                                    selectedDate.plusDays(1)
                                })
                            }
                            swipeOffset = 0f
                        },
                    )
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tasks.isEmpty()) {
                item { Text("No tasks for this date", style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(regularTasks, key = { "regular-${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = viewModel::completeTask,
                        onOpen = { selectedTask = task },
                        onLongPress = { actionTask = task },
                        descriptionFontSize = taskFontSize,
                        now = now,
                    )
                }
                if (carriedOverTasks.isNotEmpty()) {
                    if (carriedOverExpanded) {
                        items(carriedOverTasks, key = { "carried-${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onComplete = viewModel::completeTask,
                                onOpen = { selectedTask = task },
                                onLongPress = { actionTask = task },
                                descriptionFontSize = taskFontSize,
                                now = now,
                            )
                        }
                    }
                }
                if (doneForNowTasks.isNotEmpty()) {
                    if (doneForNowExpanded) {
                        items(doneForNowTasks, key = { "done-for-now-${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onComplete = viewModel::completeTask,
                                onOpen = { selectedTask = task },
                                onLongPress = { actionTask = task },
                                descriptionFontSize = taskFontSize,
                                now = now,
                            )
                        }
                    }
                }
            }
            if (showCompleted && completedTasks.isNotEmpty()) {
                item {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(completedTasks, key = { "completed-${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = null,
                        onReopen = { viewModel.reopenTask(task.id) },
                        onDelete = {
                            viewModel.deleteTask(task.id)
                            recentlyDeletedTask = task
                        },
                        onOpen = {},
                        onLongPress = {},
                        descriptionFontSize = taskFontSize,
                        now = now,
                    )
                }
            }
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    Button(
        onClick = { showCompleted = !showCompleted },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
    ) {
        Text(if (showCompleted) "Hide completed" else "Show completed")
    }
    IconButton(
        onClick = { showAccentDialog = true },
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp),
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "App accent colour settings",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    }

    if (showDatePicker) {
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    showDatePicker = false
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth,
            ).apply {
                setOnDismissListener { showDatePicker = false }
            }.show()
        }
    }

    if (showAddDialog) {
        LaunchedEffect(Unit) {
            viewModel.prepareNewTaskColour()
        }
        AddTaskDialog(
            initialDate = selectedDate,
            taskColour = pendingTaskColour,
            onDismiss = { showAddDialog = false },
            onSave = { description, date, time, colour ->
                viewModel.addTask(description, date.coerceAtLeast(today), time, colour)
                showAddDialog = false
            },
        )
    }
    selectedTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onComplete = { steps ->
                viewModel.completeTask(task.id, steps)
                selectedTask = null
            },
            onSave = { description, date, time, reminder, steps ->
                viewModel.updateTask(
                    task,
                    description,
                    date.coerceAtLeast(today),
                    time,
                    reminder,
                    steps,
                )
                selectedTask = null
            },
        )
    }
    actionTask?.let { task ->
        TaskActionDialog(
            task = task,
            onDismiss = { actionTask = null },
            onReschedule = {
                pendingActionTask = task
                showRescheduleDialog = true
            },
            onBreakdown = {
                pendingActionTask = task
                showBreakdownDialog = true
            },
            onRoutine = {
                pendingActionTask = task
                showRoutineDialog = true
            },
            onReminder = {
                pendingActionTask = task
                showReminderDialog = true
            },
            onStatus = { status ->
                viewModel.setStatus(task.id, status)
                actionTask = null
            },
            onFocus = {
                viewModel.setFocus(task.id, !task.isFocus)
                actionTask = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                recentlyDeletedTask = task
                actionTask = null
            },
        )
    }
    if (showRescheduleDialog) {
        pendingActionTask?.let { task ->
            RescheduleDialog(
                task = task,
                onDismiss = {
                    showRescheduleDialog = false
                    pendingActionTask = null
                },
                onReschedule = { date ->
                    viewModel.rescheduleTask(task, date)
                    showRescheduleDialog = false
                    pendingActionTask = null
                    actionTask = null
                },
            )
        }
    }
    if (showBreakdownDialog) {
        pendingActionTask?.let { task ->
            BreakdownDialog(
                task = task,
                onDismiss = {
                    showBreakdownDialog = false
                    pendingActionTask = null
                },
                onSave = { steps ->
                    viewModel.setSteps(task.id, steps)
                    showBreakdownDialog = false
                    pendingActionTask = null
                    actionTask = null
                },
            )
        }
    }
    if (showRoutineDialog) {
        pendingActionTask?.let { task ->
            RoutineDialog(
                task = task,
                onDismiss = {
                    showRoutineDialog = false
                    pendingActionTask = null
                },
                onSave = { recurrence ->
                    viewModel.setRoutine(task.id, recurrence)
                    showRoutineDialog = false
                    pendingActionTask = null
                    actionTask = null
                },
            )
        }
    }
    if (showReminderDialog) {
        pendingActionTask?.let { task ->
            ReminderDialog(
                task = task,
                onDismiss = {
                    showReminderDialog = false
                    pendingActionTask = null
                },
                onSave = { minutes ->
                    viewModel.setReminder(task.id, minutes)
                    showReminderDialog = false
                    pendingActionTask = null
                    actionTask = null
                },
            )
        }
    }
    if (showVoiceDialog) {
        VoiceTaskDialog(
            initialDate = selectedDate,
            onDismiss = { showVoiceDialog = false },
            onSave = { description, date, time ->
                viewModel.addTask(description, date.coerceAtLeast(today), time)
                showVoiceDialog = false
            },
        )
    }
    if (showAccentDialog) {
        AccentColourDialog(
            currentColour = accentColour,
            customColours = customAccentColours,
            currentFontSize = taskFontSize,
            onDismiss = { showAccentDialog = false },
            onSelect = {
                viewModel.setAccentColour(it)
                showAccentDialog = false
            },
            onSavePreset = viewModel::saveCustomAccentColour,
            onFontSizeChange = viewModel::setTaskFontSize,
        )
    }
}

@Composable
private fun TaskGroupHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TaskCard(
    task: TaskEntity,
    onComplete: ((Long) -> Unit)?,
    onReopen: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    descriptionFontSize: Float,
    now: LocalDateTime,
) {
    val colour = Color(android.graphics.Color.parseColor(task.colour))
    val timedProgress = task.time?.let { progressFor(task, now) }
    val shape = RoundedCornerShape(16.dp)
    var dragOffset by remember { mutableStateOf(0f) }
    var swipeTarget by remember { mutableStateOf<Float?>(null) }
    val swipeDuration = if (onDelete != null) 500 else 220
    val animatedOffset by animateFloatAsState(
        targetValue = swipeTarget ?: dragOffset,
        animationSpec = tween(durationMillis = swipeDuration),
        label = "task swipe",
    )
    val swipeProgress = (abs(animatedOffset) / 1200f).coerceIn(0f, 1f)
    val swipeThreshold = 180f
    LaunchedEffect(swipeTarget) {
        if (swipeTarget != null && (onComplete != null || onDelete != null)) {
            delay(swipeDuration.toLong())
            onComplete?.invoke(task.id) ?: onDelete?.invoke()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .combinedClickable(
                onClick = { onReopen?.invoke() ?: onOpen() },
                onLongClick = onLongPress,
            )
            .pointerInput(task.id, onComplete, onDelete) {
                if (onComplete != null || onDelete != null) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffset = (dragOffset + amount).coerceIn(-240f, 240f)
                        },
                        onDragEnd = {
                            if (abs(dragOffset) > swipeThreshold) {
                                swipeTarget = if (dragOffset > 0) 1200f else -1200f
                            } else {
                                dragOffset = 0f
                            }
                        },
                    )
                }
            }
            .graphicsLayer {
                alpha = 1f - (abs(animatedOffset) / 1200f).coerceIn(0f, 0.65f)
                rotationZ = (animatedOffset / 1200f) * 4f
            }
            .then(
                if (timedProgress != null && !task.isCompleted) {
                    Modifier.border(3.dp, colour, shape)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (timedProgress != null && !task.isCompleted) {
                MaterialTheme.colorScheme.surface
            } else if (task.isCompleted) {
                lerp(Color.LightGray, Color(0xFFE57373), swipeProgress)
            } else {
                colour
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (timedProgress != null && !task.isCompleted) {
                        Modifier.drawBehind {
                            drawRect(
                                color = colour,
                                size = androidx.compose.ui.geometry.Size(
                                    width = size.width * timedProgress,
                                    height = size.height,
                                ),
                            )
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.description, fontSize = descriptionFontSize.sp)
                task.time?.let {
                    Text(it, fontSize = (descriptionFontSize * 0.8f).sp)
                }
                if (task.wasCarriedOver) {
                    Text(
                        "Carried over",
                        fontSize = (descriptionFontSize * 0.7f).sp,
                    )
                }
                if (task.status == "IN_PROGRESS") {
                    Text("Started", fontSize = (descriptionFontSize * 0.7f).sp)
                } else if (task.status == "DONE_FOR_NOW") {
                    Text("Done for now", fontSize = (descriptionFontSize * 0.7f).sp)
                }
                if (task.isFocus) {
                    Text("Focus", fontSize = (descriptionFontSize * 0.7f).sp)
                }
                if (task.steps.isNotBlank()) {
                    val steps = task.steps.lines().filter(String::isNotBlank)
                    val completedSteps = steps.count { it.startsWith("[x] ") }
                    Text(
                        "$completedSteps of ${steps.size} steps",
                        fontSize = (descriptionFontSize * 0.7f).sp,
                    )
                }
                if (task.isCompleted) {
                    val completedAt = task.completedAt?.let(LocalDateTime::parse)
                    Text(
                        completedAt?.let {
                            "Completed at ${
                                it.format(DateTimeFormatter.ofPattern("HH:mm", Locale.UK))
                            }"
                        } ?: "Completed",
                        fontSize = (descriptionFontSize * 0.75f).sp,
                    )
                }
            }
        }
    }
}

private fun progressFor(task: TaskEntity, now: LocalDateTime): Float {
    val taskTime = task.time?.let(LocalTime::parse) ?: return 0f
    val scheduled = LocalDate.parse(task.date).atTime(taskTime)
    val preparationStart = scheduled.minusHours(2)
    if (now <= preparationStart) return 0f
    if (now >= scheduled) return 1f
    return java.time.Duration.between(preparationStart, now).toMinutes() / 120f
}

@Composable
private fun AddTaskDialog(
    initialDate: LocalDate,
    taskColour: String,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, LocalTime?, String) -> Unit,
) {
    TaskPanelDialog(
        initialDescription = "",
        initialDate = initialDate,
        initialTime = null,
        taskColour = taskColour,
        isAdding = true,
        onDismiss = onDismiss,
        onSave = { description, date, time, _, _ ->
            onSave(description, date, time, taskColour)
        },
    )
}

@Composable
private fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit,
    onSave: (String, LocalDate, LocalTime?, Int?, String) -> Unit,
) {
    TaskPanelDialog(
        initialDescription = task.description,
        initialDate = LocalDate.parse(task.date),
        initialTime = task.time?.let(LocalTime::parse),
        initialReminderMinutes = task.reminderMinutes,
        initialSteps = task.steps,
        taskColour = task.colour,
        isAdding = false,
        onDismiss = onDismiss,
        onSave = onSave,
        onComplete = onComplete,
    )
}

@Composable
private fun TaskPanelDialog(
    initialDescription: String,
    initialDate: LocalDate,
    initialTime: LocalTime?,
    initialReminderMinutes: Int? = null,
    initialSteps: String = "",
    taskColour: String,
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, LocalTime?, Int?, String) -> Unit,
    onComplete: ((String) -> Unit)? = null,
) {
    var description by remember { mutableStateOf(initialDescription) }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    var reminderMinutes by remember { mutableStateOf(initialReminderMinutes) }
    var steps by remember { mutableStateOf(initialSteps) }
    var showCompletePrompt by remember { mutableStateOf(false) }
    var showDateEditor by remember { mutableStateOf(false) }
    var showTimeEditor by remember { mutableStateOf(false) }
    var showDateFeedback by remember { mutableStateOf(false) }
    var dateFeedbackHideJob by remember { mutableStateOf<Job?>(null) }
    val dateFeedbackAlpha by animateFloatAsState(
        targetValue = if (showDateFeedback) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "date feedback alpha",
    )
    val dateFeedbackScope = rememberCoroutineScope()
    val context = LocalContext.current
    val colour = Color(android.graphics.Color.parseColor(taskColour))
    val today = LocalDate.now()
    val focusRequester = remember { FocusRequester() }
    var showEditHint by remember { mutableStateOf(false) }
    val editHintAlpha by rememberInfiniteTransition(label = "edit hint transition")
        .animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "edit hint alpha",
        )
    val closeAndSave = {
        if (description.isNotBlank()) {
            onSave(description, date, time, reminderMinutes, steps)
        } else {
            onDismiss()
        }
    }
    fun moveDateAndShowFeedback(newDate: LocalDate) {
        date = newDate
        showDateFeedback = true
        dateFeedbackHideJob?.cancel()
        dateFeedbackHideJob = dateFeedbackScope.launch {
            delay(1_000)
            showDateFeedback = false
        }
    }

    Dialog(onDismissRequest = if (isAdding) onDismiss else closeAndSave) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.94f),
                shape = RoundedCornerShape(32.dp),
                color = colour,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = 380.dp,
                            max = if (isAdding) 480.dp else 620.dp,
                        )
                        .padding(12.dp)
                        .clickable {
                            focusRequester.requestFocus()
                            showEditHint = true
                        },
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusRequester.requestFocus()
                            showEditHint = true
                        }
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                ) {
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp),
                    )
                    if (isAdding && showEditHint) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit task description",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .graphicsLayer { alpha = editHintAlpha },
                        )
                    }
                }
                if (!isAdding && steps.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            "Steps",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        steps.lines()
                            .filter(String::isNotBlank)
                            .forEachIndexed { index, step ->
                                val completed = step.startsWith("[x] ")
                                val label = step.removePrefix("[x] ").removePrefix("[ ] ")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = completed,
                                        onCheckedChange = { checked ->
                                            val updated = steps.lines()
                                                .filter(String::isNotBlank)
                                                .toMutableList()
                                            updated[index] = if (checked) {
                                                "[x] $label"
                                            } else {
                                                "[ ] $label"
                                            }
                                            steps = updated.joinToString("\n")
                                            if (checked && updated.all { it.startsWith("[x] ") }) {
                                                showCompletePrompt = true
                                            }
                                        },
                                    )
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(darken(colour), RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (date > today) {
                                moveDateAndShowFeedback(date.minusDays(1))
                            } else {
                                Toast.makeText(
                                    context,
                                    "Tasks cannot be scheduled before today",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous day",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    TextButton(
                        onClick = { showDateEditor = true },
                        modifier = Modifier
                            .weight(0.55f)
                            .size(44.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Choose date",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    TextButton(
                        onClick = { showTimeEditor = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = "Choose time",
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    IconButton(
                        onClick = { moveDateAndShowFeedback(date.plusDays(1)) },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next day",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    }
                if (isAdding) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = {
                                if (description.isNotBlank()) {
                                    onSave(description, date, time, reminderMinutes, steps)
                                }
                            },
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save task")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel task")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(42.dp)
                    .graphicsLayer { alpha = dateFeedbackAlpha }
                    .background(darken(colour), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (showDateEditor) {
        LaunchedEffect(Unit) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    date = LocalDate.of(year, month + 1, day).coerceAtLeast(today)
                    showDateEditor = false
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).apply {
                datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                setOnDismissListener { showDateEditor = false }
            }.show()
        }
    }

    if (showTimeEditor) {
        TimeEditorDialog(
            initialTime = time,
            initialReminderMinutes = reminderMinutes,
            onDismiss = { showTimeEditor = false },
            onSave = { selectedTime, reminder ->
                time = selectedTime
                reminderMinutes = reminder
                showTimeEditor = false
            },
            onRemoveTime = {
                time = null
                reminderMinutes = null
                showTimeEditor = false
            },
        )
    }
    if (showCompletePrompt) {
        AlertDialog(
            onDismissRequest = { showCompletePrompt = false },
            title = { Text("Task complete?") },
            text = { Text("All steps are ticked. Would you like to complete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompletePrompt = false
                        onComplete?.invoke(steps)
                    },
                ) {
                    Text("Complete task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompletePrompt = false }) {
                    Text("Not yet")
                }
            },
        )
    }
}
}

@Composable
private fun TimeEditorDialog(
    initialTime: LocalTime?,
    initialReminderMinutes: Int?,
    onDismiss: () -> Unit,
    onSave: (LocalTime, Int?) -> Unit,
    onRemoveTime: () -> Unit,
) {
    val context = LocalContext.current
    val selected = initialTime ?: LocalTime.now().withSecond(0).withNano(0)
    var hour by remember { mutableStateOf(selected.hour) }
    var minute by remember { mutableStateOf(selected.minute) }
    var reminderMinutes by remember { mutableStateOf(initialReminderMinutes) }
    var showReminderOptions by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Target time", style = MaterialTheme.typography.titleLarge)
                if (showReminderOptions) {
                    Text(
                        "Reminder",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    listOf(
                        null to "At target time",
                        5 to "5 minutes before",
                        10 to "10 minutes before",
                        15 to "15 minutes before",
                        30 to "30 minutes before",
                        60 to "1 hour before",
                    ).forEach { (minutes, label) ->
                        TextButton(
                            onClick = {
                                reminderMinutes = minutes
                                showReminderOptions = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(label)
                        }
                    }
                    TextButton(
                        onClick = { showReminderOptions = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Back to target time")
                    }
                } else {
                    AndroidView(
                        factory = {
                            android.widget.TimePicker(context).apply {
                                setIs24HourView(true)
                                hour = this@apply.hour
                                minute = this@apply.minute
                                setOnTimeChangedListener { _, newHour, newMinute ->
                                    hour = newHour
                                    minute = newMinute
                                }
                            }
                        },
                        update = { picker ->
                            picker.hour = hour
                            picker.minute = minute
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = { showReminderOptions = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            reminderMinutes?.let { "Reminder: $it minutes before" }
                                ?: "Reminder: at target time",
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = onRemoveTime) {
                            Text("Remove target time")
                        }
                        TextButton(
                            onClick = {
                                onSave(LocalTime.of(hour, minute), reminderMinutes)
                            },
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskActionDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onReschedule: () -> Unit,
    onBreakdown: () -> Unit,
    onRoutine: () -> Unit,
    onReminder: () -> Unit,
    onStatus: (String) -> Unit,
    onFocus: () -> Unit,
    onDelete: () -> Unit,
) {
    var showStatusOptions by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(task.description, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = Color(0xFFB3261E),
                    )
                }
            }
        },
        text = {
            Column {
                ActionMenuRow(Icons.Default.CalendarToday, "Reschedule", onReschedule)
                ActionMenuRow(Icons.AutoMirrored.Filled.List, "Break down task", onBreakdown)
                ActionMenuRow(
                    Icons.Default.Repeat,
                    if (task.recurrence == null) "Make routine" else "Change routine",
                    onRoutine,
                )
                if (task.time != null) {
                    ActionMenuRow(Icons.Default.Notifications, "Set reminder", onReminder)
                }
                ActionMenuRow(
                    if (showStatusOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Status",
                    onClick = { showStatusOptions = !showStatusOptions },
                )
                if (showStatusOptions) {
                    ActionMenuRow(
                        Icons.Default.PlayArrow,
                        "Started",
                        onClick = { onStatus("IN_PROGRESS") },
                        indent = true,
                    )
                    ActionMenuRow(
                        Icons.Default.CheckCircle,
                        "Done for now",
                        onClick = { onStatus("DONE_FOR_NOW") },
                        indent = true,
                    )
                    ActionMenuRow(
                        Icons.Default.Close,
                        "Clear status",
                        onClick = { onStatus("NOT_STARTED") },
                        indent = true,
                    )
                }
                ActionMenuRow(
                    Icons.Default.Star,
                    if (task.isFocus) "Remove focus" else "Set as focus",
                    onFocus,
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ActionMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    indent: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = if (indent) 48.dp else 0.dp,
                top = 7.dp,
                bottom = 7.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(label, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RescheduleDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onReschedule: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule task") },
        text = {
            Column {
                listOf(
                    today to "Today",
                    today.plusDays(1) to "Tomorrow",
                    today.plusDays(2) to "In two days",
                    today.plusDays(((6 - today.dayOfWeek.value + 7) % 7).toLong())
                        to "This weekend",
                    today.plusDays(7) to "Next week",
                ).forEach { (date, label) ->
                    TextButton(
                        onClick = { onReschedule(date) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(label) }
                }
                Text(
                    "Current date: ${task.date}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun BreakdownDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var stepsText by remember {
        mutableStateOf(
            task.steps.lines()
                .filter(String::isNotBlank)
                .joinToString("\n") { it.removePrefix("[x] ").removePrefix("[ ] ") },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Break down task") },
        text = {
            OutlinedTextField(
                value = stepsText,
                onValueChange = { stepsText = it },
                label = { Text("One step per line") },
                minLines = 4,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val remainingExisting = task.steps.lines()
                        .filter(String::isNotBlank)
                        .toMutableList()
                    val steps = stepsText.lines()
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .joinToString("\n") { label ->
                            val existingIndex = remainingExisting.indexOfFirst {
                                it.removePrefix("[x] ").removePrefix("[ ] ") == label
                            }
                            if (existingIndex >= 0) {
                                val existing = remainingExisting.removeAt(existingIndex)
                                if (existing.startsWith("[x] ")) "[x] $label" else "[ ] $label"
                            } else {
                                "[ ] $label"
                            }
                        }
                    onSave(steps)
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RoutineDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var selectedDays by remember {
        mutableStateOf(
            task.recurrence?.removePrefix("DAYS:")?.split(',')
                ?.mapNotNull(String::toIntOrNull)?.toSet() ?: emptySet(),
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Routine") },
        text = {
            Column {
                TextButton(onClick = { onSave("DAILY") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Every day")
                }
                TextButton(onClick = { onSave("WEEKDAYS") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Weekdays")
                }
                TextButton(onClick = { onSave("WEEKENDS") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Weekends")
                }
                TextButton(onClick = { onSave("WEEKLY") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Every week")
                }
                Text("Choose days", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, label ->
                        TextButton(
                            onClick = {
                                val day = index + 1
                                selectedDays = if (day in selectedDays) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(
                                label,
                                color = if (index + 1 in selectedDays) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
                if (task.recurrence != null) {
                    TextButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Turn off routine")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedDays.isNotEmpty()) {
                        onSave("DAYS:${selectedDays.sorted().joinToString(",")}")
                    } else {
                        onDismiss()
                    }
                },
            ) { Text("Save selected days") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ReminderDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder") },
        text = {
            Column {
                listOf(
                    null to "At deadline",
                    5 to "5 minutes before",
                    10 to "10 minutes before",
                    15 to "15 minutes before",
                    30 to "30 minutes before",
                    60 to "1 hour before",
                ).forEach { (minutes, label) ->
                    TextButton(
                        onClick = { onSave(minutes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(label) }
                }
                if (task.reminderMinutes != null) {
                    TextButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove extra reminder")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun darken(colour: Color): Color =
    Color(colour.red * 0.78f, colour.green * 0.78f, colour.blue * 0.78f, 1f)

@Composable
private fun TaskFieldsDialog(
    title: String,
    initialDescription: String,
    initialDate: LocalDate,
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, LocalTime?) -> Unit,
    extraActions: @Composable (() -> Unit)? = null,
) {
    var description by remember { mutableStateOf(initialDescription) }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    val context = LocalContext.current
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Task description") },
                )
                TextButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                date = LocalDate.of(year, month + 1, day).coerceAtLeast(today)
                            },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth,
                        ).apply {
                            datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        }.show()
                    },
                ) {
                    Text("Date: ${date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK))}")
                }
                TextButton(
                    onClick = {
                        val selected = time ?: LocalTime.now().withSecond(0).withNano(0)
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> time = LocalTime.of(hour, minute) },
                            selected.hour,
                            selected.minute,
                            true,
                        ).show()
                    },
                ) {
                    Text(time?.let { "Time: ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}" } ?: "Time: None")
                }
            }
        },
        confirmButton = {
            Row {
                extraActions?.invoke()
                TextButton(
                    enabled = description.isNotBlank(),
                    onClick = { onSave(description, date, time) },
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MoveForwardDialog(
    onDismiss: () -> Unit,
    onMove: (Long) -> Unit,
) {
    var daysText by remember { mutableStateOf("1") }
    val days = daysText.toLongOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move task forward") },
        text = {
            OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it.filter(Char::isDigit) },
                label = { Text("Number of days") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = days != null && days > 0,
                onClick = { onMove(days!!) },
            ) {
                Text("Move")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun VoiceTaskDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, LocalTime?) -> Unit,
) {
    var status by remember { mutableStateOf("Preparing on-device listening…") }
    var spokenText by remember { mutableStateOf("") }
    var showConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val recognizer = remember {
        OnDeviceVoiceRecognizer(
            context = context,
            onStateChanged = { status = it },
            onText = {
                spokenText = it
                showConfirmation = true
            },
        )
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer.destroy() }
    }
    LaunchedEffect(recognizer) {
        if (recognizer.isAvailable()) {
            recognizer.start()
        } else {
            status = "On-device speech recognition is unavailable on this phone."
        }
    }

    if (!showConfirmation) {
        AlertDialog(
            onDismissRequest = {
                recognizer.stop()
                onDismiss()
            },
            title = { Text("Add by voice") },
            text = { Text(status) },
            confirmButton = {
                TextButton(
                    enabled = recognizer.isAvailable(),
                    onClick = { recognizer.start() },
                ) {
                    Text("Listen again")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        recognizer.stop()
                        onDismiss()
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    } else {
        val parsed = remember(spokenText, initialDate) {
            VoiceTaskParser.parse(spokenText, initialDate)
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirm task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(parsed.description)
                    Text("Date: ${parsed.date}")
                    Text("Time: ${parsed.time ?: "None"}")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsed.description.isNotBlank(),
                    onClick = { onSave(parsed.description, parsed.date, parsed.time) },
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

private val accentColourPresets = listOf(
    "#6D5E8C",
    "#3F638F",
    "#2F7D6D",
    "#8A5A44",
    "#A24B63",
    "#5E6B3F",
)

@Composable
private fun AccentColourDialog(
    currentColour: String,
    customColours: List<String>,
    currentFontSize: Float,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onSavePreset: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
) {
    var selectedColour by remember { mutableStateOf(currentColour) }
    var savedColours by remember { mutableStateOf(customColours) }
    val initialHsv = remember(currentColour) {
        FloatArray(3).also {
            android.graphics.Color.colorToHSV(
                android.graphics.Color.parseColor(currentColour),
                it,
            )
        }
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var brightness by remember { mutableStateOf(initialHsv[2]) }
    fun updateColour() {
        selectedColour = String.format(
            Locale.US,
            "#%06X",
            android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
                and 0xFFFFFF,
        )
    }
    fun selectPreset(colour: String) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(colour), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        selectedColour = colour
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accent colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a preset or create your own colour.")
                (accentColourPresets + savedColours).distinct().chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(preset)),
                                        RoundedCornerShape(21.dp),
                                    )
                                    .clickable { selectPreset(preset) },
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Color(android.graphics.Color.parseColor(selectedColour)),
                            RoundedCornerShape(12.dp),
                        ),
                )
                Text("Hue")
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        updateColour()
                    },
                    valueRange = 0f..360f,
                )
                Text("Saturation")
                Slider(
                    value = saturation,
                    onValueChange = {
                        saturation = it
                        updateColour()
                    },
                    valueRange = 0f..1f,
                )
                Text("Brightness")
                Slider(
                    value = brightness,
                    onValueChange = {
                        brightness = it
                        updateColour()
                    },
                    valueRange = 0f..1f,
                )
                Text("Task text size: ${currentFontSize.toInt()}sp")
                Slider(
                    value = currentFontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 12f..28f,
                    steps = 7,
                )
                TextButton(
                    onClick = {
                        if (selectedColour !in savedColours) {
                            savedColours = savedColours + selectedColour
                            onSavePreset(selectedColour)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save colour as preset")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selectedColour) }) { Text("Use colour") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AutaskticTheme(
    accentColour: String,
    content: @Composable () -> Unit,
) {
    val primary = try {
        Color(android.graphics.Color.parseColor(accentColour))
    } catch (_: IllegalArgumentException) {
        Color(0xFF6D5E8C)
    }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = primary,
            secondary = primary,
            tertiary = primary,
        ),
        content = content,
    )
}

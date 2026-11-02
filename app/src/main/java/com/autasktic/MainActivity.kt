package com.autasktic

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
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
            AutaskticTheme {
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
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(durationMillis = 140),
        label = "date swipe feedback",
    )
    var showVoiceDialog by remember { mutableStateOf(false) }
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
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .blur(if (showAddDialog || selectedTask != null || showVoiceDialog) 10.dp else 0.dp),
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
                                selectedDate = if (dragDistance > 0) {
                                    selectedDate.minusDays(1)
                                } else {
                                    selectedDate.plusDays(1)
                                }
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
                onClick = { selectedDate = today },
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
                onClick = { showAddDialog = true },
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
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Add by Voice")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { showCompleted = !showCompleted }) {
            Text(if (showCompleted) "Hide completed" else "Show completed")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tasks.isEmpty()) {
                item { Text("No tasks for this date", style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = viewModel::completeTask,
                        onOpen = { selectedTask = task },
                        now = now,
                    )
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
                items(completedTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = null,
                        onReopen = { viewModel.reopenTask(task.id) },
                        onOpen = {},
                        now = now,
                    )
                }
            }
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
                viewModel.addTask(description, date, time, colour)
                showAddDialog = false
            },
        )
    }
    selectedTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { selectedTask = null },
            onSave = { description, date, time ->
                viewModel.updateTask(task, description, date, time)
                selectedTask = null
            },
        )
    }
    if (showVoiceDialog) {
        VoiceTaskDialog(
            initialDate = selectedDate,
            onDismiss = { showVoiceDialog = false },
            onSave = { description, date, time ->
                viewModel.addTask(description, date, time)
                showVoiceDialog = false
            },
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    onComplete: ((Long) -> Unit)?,
    onReopen: (() -> Unit)? = null,
    onOpen: () -> Unit,
    now: LocalDateTime,
) {
    val colour = Color(android.graphics.Color.parseColor(task.colour))
    val timedProgress = task.time?.let { progressFor(task, now) }
    val shape = RoundedCornerShape(16.dp)
    var dragOffset by remember { mutableStateOf(0f) }
    var swipeTarget by remember { mutableStateOf<Float?>(null) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeTarget ?: dragOffset,
        animationSpec = tween(durationMillis = 220),
        label = "task swipe",
    )
    LaunchedEffect(swipeTarget) {
        if (swipeTarget != null && onComplete != null) {
            delay(220)
            onComplete(task.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .clickable(onClick = { onReopen?.invoke() ?: onOpen() })
            .pointerInput(task.id, onComplete) {
                if (onComplete != null) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffset = (dragOffset + amount).coerceIn(-240f, 240f)
                        },
                        onDragEnd = {
                            if (abs(dragOffset) > 120f) {
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
                Color.LightGray
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
                Text(task.description, style = MaterialTheme.typography.titleMedium)
                task.time?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (task.wasCarriedOver) {
                    Text(
                        "Carried over",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (task.isCompleted) {
                    Text("Completed", style = MaterialTheme.typography.bodySmall)
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
        onSave = { description, date, time ->
            onSave(description, date, time, taskColour)
        },
    )
}

@Composable
private fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, LocalTime?) -> Unit,
) {
    TaskPanelDialog(
        initialDescription = task.description,
        initialDate = LocalDate.parse(task.date),
        initialTime = task.time?.let(LocalTime::parse),
        taskColour = task.colour,
        isAdding = false,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
private fun TaskPanelDialog(
    initialDescription: String,
    initialDate: LocalDate,
    initialTime: LocalTime?,
    taskColour: String,
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, LocalTime?) -> Unit,
) {
    var description by remember { mutableStateOf(initialDescription) }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
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
            onSave(description, date, time)
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
                        .heightIn(min = 380.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
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
                            if (date > today) moveDateAndShowFeedback(date.minusDays(1))
                        },
                        enabled = date > today,
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
                }
                if (isAdding) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = {
                                if (description.isNotBlank()) onSave(description, date, time)
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
                setOnDismissListener { showDateEditor = false }
            }.show()
        }
    }

    if (showTimeEditor) {
        LaunchedEffect(Unit) {
            val selected = time ?: LocalTime.now().withSecond(0).withNano(0)
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    time = LocalTime.of(hour, minute)
                    showTimeEditor = false
                },
                selected.hour,
                selected.minute,
                true,
            ).apply {
                setOnDismissListener { showTimeEditor = false }
            }.show()
        }
    }
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
                            { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth,
                        ).show()
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

@Composable
private fun AutaskticTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

# Autasktic Progress

Personal Task Organiser for a Nothing CMF Phone 1.

## Device and development setup

- **Target device:** Nothing CMF Phone 1
- **Connection:** USB cable to the development laptop
- **Deployment method:** Android Debug Bridge (ADB), once Android platform tools and a JDK are available
- **App constraints:** Local-first, offline-capable, no account, cloud storage, AI, or subscription

## Overall stages

- [x] Stage 1 - Basic application
- [x] Stage 2 - Task management
- [x] Stage 3 - Voice entry
- [x] Stage 4 - Visual polish

## Stage 1 - Basic application

### Progress

- [x] Define the Stage 1 scope
- [x] Create the progress tracker
- [x] Create the Kotlin and Jetpack Compose Android project
- [x] Create the initial Today home screen
- [x] Display the current date
- [x] Add text task creation
- [x] Default new tasks to today
- [x] Add optional time selection
- [x] Add Room/SQLite local storage
- [x] Display active tasks as pastel task cards
- [x] Support task completion
- [x] Complete tasks by swiping them left or right
- [x] Reopen completed tasks by tapping them
- [x] Hide completed tasks from the active list
- [x] Show completed tasks only for their scheduled date
- [x] Notify for timed incomplete tasks while the app is closed
- [x] Request Android exact-alarm access for exact timed notifications
- [x] Verify exact-time notification delivery on the CMF Phone 1
- [x] Verify persistence after closing and reopening the app
- [x] Build and deploy to the Nothing CMF Phone 1 over USB

### Stage 1 acceptance check

Stage 1 is complete when the app opens on Today, allows a task to be created by text,
stores it locally, displays it as an active task, and allows it to be completed.
The task must still be present after the app is closed and reopened before completion.

### Current status

The Stage 1 task flow is implemented and confirmed on the phone. The app
has a Today screen, text-entry dialog, today/date selection, optional time selection,
Room persistence, pastel active-task cards, and completion handling. A Gradle wrapper
has been generated for repeatable builds. The APK has been installed and launched on
the connected Nothing CMF Phone 1 over USB. The Stage 1 smoke test was confirmed:
creating, reopening, and completing a task all work.

## Stage 2 - Task management

- [x] Edit task descriptions
- [x] Edit task dates
- [x] Edit or remove task times
- [x] Move tasks forward by a selected number of days
- [x] Navigate backward and forward between task dates
- [x] Return to today from another date
- [x] Swipe left to move forward and swipe right to move backward
- [x] Tap the centered date to jump with a calendar picker
- [x] Show completed tasks
- [x] Hide completed tasks
- [x] Add dynamic task-card sizing and scrolling

### Stage 2 current status

The Stage 2 implementation is complete in the debug build. Tasks can be opened by
tapping a card, edited, completed, or moved forward by a selected number of days.
Completed tasks can be shown or hidden, the selected date can move backward and
forward, and the task list uses scrolling for larger task counts. Voice and text
creation now default to the selected date. The build is ready for installation and
manual verification on the phone.

## Stage 3 - Voice entry

- [x] Request microphone permission
- [x] Add on-device speech recognition
- [x] Recognise basic UK English date expressions
- [x] Recognise basic UK English time expressions
- [x] Remove recognised date and time phrases from descriptions
- [x] Add confirmation before saving
- [x] Ensure there is no cloud-recognition fallback

### Stage 3 current status

The Stage 3 implementation builds successfully and is installed on the CMF Phone 1.
Voice entry uses Android's on-device recognizer only, requests microphone permission,
parses the initial UK date/time phrases, and presents the result for confirmation
before saving. Phone verification of microphone permission, speech capture, and the
example phrases remains.

## Stage 4 - Visual polish

- [x] Add blurred backgrounds behind task panels
- [ ] Add expanding and closing animations
- [ ] Add task-card expansion for editing
- [x] Add timed-task borders
- [x] Add two-hour progressive pastel fills
- [x] Add accessibility label for task completion
- [x] Add voice-parser regression tests
- [x] Add build and USB deployment documentation
- [x] Add exact-alarm scheduling and permission flow
- [x] Add long-press task actions
- [x] Add quick rescheduling presets
- [x] Add optional pre-deadline reminders
- [x] Add optional task breakdown steps
- [x] Add simple recurring routines
- [x] Add manual focus and lightweight task statuses
- [x] Add colour-matched auto-saving task panel
- [x] Add icon-based date/time controls and bounded date adjustment
- [x] Match Popup1 and compact bottom Box2 styling to the visual reference
- [ ] Refine colours, typography, spacing, and accessibility

### Stage 4 current status

Timed tasks now use a two-hour fill span calculated from the device's current local
time. The fill begins at the left edge, progresses across the card, and reaches the
full pastel colour at the scheduled time. Timed cards use a thicker pastel border,
task panels blur the Today screen behind them, and task-card size changes animate.
The timed-fill behavior was verified on the CMF Phone 1. Custom expanding panel
transitions and final accessibility/visual refinement remain as optional polish;
the current native panels are stable and usable. The final debug build and unit
tests pass, and the final APK is installed on the CMF Phone 1. Exact alarms use
Android's special Alarms & reminders access when enabled, with an inexact fallback
when that access is unavailable. The exact-time test was completed on the connected
CMF Phone 1 and the notification arrived at the scheduled time.

## Excluded from the MVP

AI interpretation, categories, automatic priorities, calendar integrations, cloud
synchronisation, multiple users, task sharing, and productivity statistics remain
excluded from the MVP. Recurring routines, reminders, and lightweight status
actions are now supported through the task action menu.

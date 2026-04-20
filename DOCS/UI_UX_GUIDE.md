# UI / UX Development Guide

## Purpose

This document defines the next-stage product structure for the Android app.
It is intended to guide implementation, interaction design, copywriting, and follow-up testing.

Current conclusion:

- keep the app Android-only
- keep the current Kotlin + Compose stack
- do not add side navigation
- do not add bottom navigation yet
- keep a single processing-focused main page
- add a dedicated `关于与说明` page from the top app bar
- move file visibility and task visibility into a clear list-based experience

## Product goals

The app is no longer a one-shot converter.
It already supports batch processing, queue persistence, retry, cancellation, and progress tracking.
Because of that, the UI must make each imported file visible and controllable.

The next UI phase should solve these problems:

- users must clearly see which files were imported
- users must clearly see which files are supported or unsupported
- users must clearly see which files are queued, running, successful, failed, or canceled
- users must be able to remove items from the current working list
- users must understand where output files are written
- help text, project links, and legal statements must not crowd the main processing screen

## Information architecture

### Page structure

For the current stage, the app should contain only two first-class pages:

1. `处理`
2. `关于与说明`

### Navigation decision

Current decision:

- no side drawer
- no bottom navigation
- use a top app bar on the main page
- place an entry such as `说明` or `关于` in the top app bar

Reason:

- the primary task is still single-purpose: import files and process them
- adding navigation chrome too early makes the app look more complex than it is
- statements, project links, and help content are low-frequency destinations and should not occupy main navigation slots

### Future navigation upgrade rule

Only consider bottom navigation later if the app gains another real high-frequency destination, for example:

- `处理`
- `记录`
- `关于`

Until that happens, keep the structure flat.

## Main page: `处理`

### Core principle

The processing page must be list-centric.
Summary text may remain, but it must summarize the list instead of replacing it.

### Recommended layout order

1. top app bar
2. import and output actions
3. concise batch summary
4. file/task list
5. batch actions
6. execution status hint

### Top app bar

The top app bar should contain:

- page title: `音乐解锁`
- optional subtitle omitted for now
- action entry: `关于` or `说明`

It may also later contain a lightweight overflow menu, but not in the current phase.

### Import and output section

This area should remain near the top.
Recommended actions:

- `选择文件`
- `选择输出目录`
- `开始执行`

Optional supporting action:

- `加入队列`

Implementation note:

- if the product keeps a two-step `加入队列 -> 开始执行` flow, the list must clearly show when a file is only imported versus actually queued
- if the product later simplifies to `导入后自动进入待处理列表`, that is acceptable, but the state model must stay explicit

### Output directory display

The current default output folder should be clearly explained in the UI:

- default directory is app-created
- current folder name: `UnlockMusicOutput`
- current path form: `Android/data/dev.unlockmusic.android/files/UnlockMusicOutput`

The UI should show:

- current output directory
- whether it is the app default directory
- that the user may manually switch to another folder

### Batch summary

Keep a compact summary block above the list.
Recommended wording pattern:

- `共导入 18 个文件`
- `可处理 15 个`
- `不支持 3 个`

This summary should always reflect the actual list contents.

## File / task list

### Why the list is required

The current summary-only approach is not sufficient once batch size grows.
The user needs one visible source of truth for imported files and their states.

### List positioning

The list should occupy the main body of the page.
It should not be hidden behind a modal dialog by default.
If a secondary full-screen list page is introduced later, the main page should still keep a compact visible list or entry summary.

### Data model principle

The list should act as the primary surface for imported items.
Avoid splitting the experience into three unrelated concepts:

- selected file count
- queued tasks
- warning cards

Instead, each imported file should appear as one visible item with a clear state.

### Item fields

Each row should display:

- file name
- detected type
- current state
- error reason when failed
- output file name when successful
- remove action when removable

Optional secondary text:

- source URI
- imported order index

### Status system

Use one stable set of user-facing states:

- `未入队`
- `不支持`
- `排队中`
- `执行中`
- `成功`
- `失败`
- `已取消`

Do not mix these with inconsistent labels like `不会执行`, `不可执行`, `异常`, or `未处理` unless there is a strict semantic distinction.

### Status meaning

- `未入队`: imported but not yet added into the runnable queue
- `不支持`: detected file type is not currently supported and will not run
- `排队中`: ready to run
- `执行中`: currently being processed
- `成功`: output has been generated
- `失败`: processing ended with an error
- `已取消`: task was canceled or skipped after cancellation

### Sorting

Default sort order:

- import order

Do not auto-reorder by status during execution.
The current item may be highlighted, but the overall order should remain stable so users can still find files they just imported.

### Filtering

Recommended filter tabs or chips:

- `全部`
- `可处理`
- `不支持`
- `处理中`
- `成功`
- `失败`

This is optional for the first implementation pass, but the list structure should be built so filters can be added without redesign.

## Item actions

### Remove behavior

The list needs removal support, but behavior must be state-aware.

Recommended rules:

- `未入队`: can remove directly
- `不支持`: can remove directly
- `排队中`: can remove from queue
- `执行中`: cannot hard-delete immediately
- `成功`: can remove from visible list only
- `失败`: can remove directly
- `已取消`: can remove directly

### Important copy rule

Removing an item from the app list must not imply deleting the original disk file.
The UI should make this clear, either through wording or one-time guidance.

Recommended semantics:

- `从列表移除`
- avoid wording that sounds like deleting the source file

### Batch actions

Recommended batch operations near the list:

- `删除不支持项`
- `清空已成功项`
- `重试失败项`
- `全部清空`

Do not expose all batch actions if they are not supported correctly yet.
Hidden or disabled is better than misleading.

## Execution UX

### During execution

The page should remain stable during processing.
Do not let rows jump around because status changed.

Recommended behavior:

- keep list order fixed
- highlight the currently running item
- update only the necessary row state and overall summary

### Current task visibility

The user should be able to identify the current item without hunting through text.

Recommended cues:

- running row highlight
- progress percentage in the row
- top summary line such as `正在处理 3 / 18`

### Retry behavior

Retry should be targeted.
Recommended actions:

- `重试失败项`
- optionally `重试未完成项`

Successful items should not be re-queued by mistake.

## Unsupported file UX

### Current decision

Unsupported files should remain visible in the list.
They should not silently disappear.

### Required behavior

- the row shows `不支持`
- the row can be removed
- the top summary reflects unsupported count
- queue-related buttons explain why they are disabled

### Copy principle

Warnings should be direct and explicit.
Do not rely on subtle hint text alone.

Recommended wording style:

- `当前选中的文件都不支持，不能加入队列`
- `这次只有 15 个文件会执行，另外 3 个不支持文件不会执行`

## Duplicate import handling

This needs a clear rule before implementation expands.

Recommended default:

- deduplicate by source URI
- if the same file is imported again in the same session, skip it
- show a brief message such as `已跳过重复文件`

Reason:

- repeated imports make the list noisy
- duplicated queue entries create ambiguity about user intent

If later evidence shows users truly want duplicate processing, make it an explicit option rather than the default.

## Empty state

When the list is empty, the page should say so plainly.

Recommended copy:

- `还没有导入文件`
- `选择文件后会在这里显示`

Do not fill the empty state with too much explanation.

## About page: `关于与说明`

### Why this page exists

Project links, legal text, privacy notes, and usage help should be separated from the processing flow.
They matter, but they should not compete with the main task screen.

### Recommended sections

1. `项目地址`
2. `支持格式`
3. `使用说明`
4. `免责声明`
5. `隐私说明`
6. `开源许可`

### Section details

#### 项目地址

Should include:

- GitHub repository link
- Issue / feedback link
- version number

#### 支持格式

Should list currently supported input families:

- QMC family
- NCM
- KGM / KGMA / VPR

#### 使用说明

Should explain:

- how to import files
- where output files are written by default
- that unsupported files will stay visible but will not run
- that removing from the list does not delete the original source file

#### 免责声明

Should include a concise legal statement such as:

- for learning and research use
- users are responsible for their own usage and compliance

#### 隐私说明

Should clearly state:

- files are processed locally on device
- no upload is performed by the app itself

#### 开源许可

Should provide:

- app license
- third-party dependency license entry or redirect

## Copywriting guidelines

### Tone

Use direct Chinese copy.
Avoid technical jargon unless necessary.
Avoid ambiguous wording.

### Good copy characteristics

- short
- explicit
- action-oriented
- matches actual behavior

### Copy examples

Prefer:

- `加入队列`
- `开始执行`
- `从列表移除`
- `当前选中的文件都不支持，不能加入队列`

Avoid:

- vague warnings
- overloaded mixed-state descriptions
- wording that suggests source files are being deleted

## Suggested implementation order

Implementation should proceed in this order:

1. refactor the main page around a first-class list
2. unify imported-item and queued-task presentation into one visible list model
3. add per-item removal support with state-aware rules
4. add clear summary and disabled-state reasons
5. add the `关于与说明` page
6. add batch actions
7. add filtering if the base list is stable

## Acceptance criteria

This UI phase is complete only when:

- users can see every imported file in a dedicated list area
- users can see each file's state without guessing
- unsupported items are obvious and removable
- queue/execution actions explain disabled states
- output directory behavior is understandable
- help and legal content are moved out of the main workflow
- the app still works well on phone-sized screens

## Deferred items

These are intentionally not required for this phase:

- bottom navigation
- side drawer
- queue history page
- metadata write-back UI
- advanced search
- desktop/tablet-specific navigation patterns

## Notes for development

Implementation should prefer evolving the current screen instead of replacing it with a heavy multi-page flow.
The target is a clearer batch-processing experience, not a more complicated app.

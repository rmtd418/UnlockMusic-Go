# Roadmap

## Active phase

`Phase 2` is complete:

- scaffold Android modules
- wire SAF file selection and output-directory selection
- port QMC decrypt coverage into `core`
- port NCM decrypt coverage into `core`
- port KGM/VPR decrypt coverage into `core`
- run queued batches through an Android foreground service
- persist queue snapshots and recover interrupted batches after process restart
- move persisted queue/session state onto DataStore with legacy settings migration
- add cancellation, retry, and richer progress reporting around the foreground service
- fail unsupported selections early and validate SAF access before foreground execution starts
- add instrumentation coverage for SAF-driven UI, real DocumentProvider I/O, and foreground-service execution paths
- validate the provider-backed instrumentation suite on an emulator and fix Android 16/API 36 provider compatibility issues
- validate real-file QMC conversion on device-side storage

## Next steps

1. Rework the main UI around a first-class file/task list so imported files, states, and remove actions are always visible
2. Add a dedicated `关于与说明` page from the top app bar for project links, help copy, privacy notes, and disclaimers
3. Keep DataStore as the MVP persistence layer; only introduce Room if queue history, search, or auditing becomes a real requirement
4. Tighten notification and execution UX further if larger real-world batches expose new bottlenecks
5. Decide whether metadata write-back should stay deferred or return as an opt-in post-MVP phase
6. Re-enable Compose instrumentation on API 36+ once the upstream Espresso compatibility issue is resolved

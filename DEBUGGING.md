# Debugging Sidekick

Quick reference for Android debugging. All scripts are in `./scripts/`.

## Quick Start

```bash
./scripts/1_build.sh debug
./scripts/2_launch.sh
```

## Common Workflows

**Full build-to-debug workflow:**
```bash
./scripts/1_build.sh debug      # Build APK, install, grant permissions
./scripts/2_launch.sh           # Launch app, complete onboarding
./scripts/3_add_fake_data.sh 15 # Add 15 fake runs for testing
./scripts/4_debug.sh 60         # Capture 60 seconds of logs, screenshots, memory
```

**Code change verification (no data):**
```bash
./scripts/1_build.sh debug
./scripts/2_launch.sh
# Verify UI and basic functionality
```

**App crashing - check logs:**
```bash
./scripts/logcat_filter.sh com.fghbuild.sidekick E  # Errors only
./scripts/logcat_filter.sh com.fghbuild.sidekick D  # Debug + up
# Press Ctrl+C to exit
```

**Memory leak suspect:**
```bash
./scripts/memory_profiler.sh com.fghbuild.sidekick 120 2
# Monitors for 2 minutes, samples every 2 seconds, generates report
```

**Visual testing (UI regression):**
```bash
./scripts/screenshot_sequence.sh 10 500  # 10 frames at 500ms intervals
# Creates screenshots_TIMESTAMP/ with PNG sequence
```

## Quick Commands

```bash
adb exec-out screencap -p > screenshot.png    # Single screenshot
adb logcat com.fghbuild.sidekick:D *:S        # Direct log monitor
adb shell am start -n com.fghbuild.sidekick/.MainActivity  # Launch app
adb shell dumpsys meminfo com.fghbuild.sidekick            # Memory snapshot
adb shell sqlite3 /data/data/com.fghbuild.sidekick/databases/run_database.db  # Database
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Device not found | Start emulator or connect device via USB; `adb devices` to verify |
| Build fails | `./gradlew clean build` |
| Script not found | Verify you're in project root; scripts are in `./scripts/` |

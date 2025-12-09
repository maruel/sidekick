#!/bin/bash
# Comprehensive debug session: logs, screenshots, memory, system state
set -e

DURATION=${1:-30} APP_PACKAGE=${2:-"com.fghbuild.sidekick"} APP_ACTIVITY="MainActivity"
SESSION_ID=$(date +%Y%m%d_%H%M%S) SESSION_DIR="debug_sessions/$SESSION_ID"

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }
log_warn() { echo "[WARN] $1"; }

check_device() {
    adb devices | grep -q "device" && { log_success "Device ready"; return 0; }
    log_warn "No device, starting emulator..."
    AVD=$(emulator -list-avds | head -1) || { echo "No AVD"; exit 1; }
    emulator -avd "$AVD" & sleep 8
}

setup_session() {
    mkdir -p "$SESSION_DIR"/{logs,screenshots,memory,system}
    log_success "Session: $SESSION_DIR"
}

start_logcat() {
    log_info "Starting logcat..."
    adb logcat -v threadtime > "$SESSION_DIR/logs/logcat_full.txt" & LOGCAT_PID=$!
    adb logcat "$APP_PACKAGE:D" -- "*:S" > "$SESSION_DIR/logs/logcat_app.txt" & APP_LOGCAT_PID=$!
    adb logcat "*:E" | grep -E "AndroidRuntime|FATAL" > "$SESSION_DIR/logs/crashes.txt" 2>/dev/null & CRASH_PID=$!
}

start_screenshots() {
    log_info "Starting screenshots (1s intervals)..."
    (for i in $(seq 1 "$DURATION"); do
        adb exec-out screencap -p > "$SESSION_DIR/screenshots/screen_$i.png" 2>/dev/null || true
        sleep 1
    done) & SCREENSHOT_PID=$!
}

start_memory_monitoring() {
    log_info "Starting memory monitoring..."
    (for i in $(seq 1 $((DURATION / 2))); do
        { echo "=== Memory $(date +%H:%M:%S) ===" && adb shell dumpsys meminfo "$APP_PACKAGE"; echo ""; } >> "$SESSION_DIR/memory/meminfo.txt" 2>/dev/null || true
        sleep 2
    done) & MEMORY_PID=$!
}

start_system_monitoring() {
    log_info "Starting system monitoring..."
    (for i in $(seq 1 $((DURATION / 5))); do
        { echo "=== System $(date +%H:%M:%S) ==="; echo "-- CPU --"; adb shell top -n 1 2>/dev/null | head -20 || true; echo ""; echo "-- Process --"; adb shell pidof "$APP_PACKAGE" 2>/dev/null || true; echo ""; echo "-- Focus --"; adb shell dumpsys window windows 2>/dev/null | grep "mCurrentFocus" || true; echo ""; } >> "$SESSION_DIR/system/system_state.txt"
        sleep 5
    done) & SYSTEM_PID=$!
}

launch_app() {
    log_info "Launching $APP_PACKAGE"
    adb shell am start -n "$APP_PACKAGE/.$APP_ACTIVITY" 2>/dev/null || true
    sleep 2
}

stop_bg() {
    log_info "Stopping..."
    kill $LOGCAT_PID $APP_LOGCAT_PID $CRASH_PID $MEMORY_PID $SYSTEM_PID $SCREENSHOT_PID 2>/dev/null || true
}

generate_report() {
    log_info "Generating report..."
    cat > "$SESSION_DIR/DEBUG_REPORT.md" <<EOF
# Debug Session Report
**Session:** $SESSION_ID  
**Date:** $(date)  
**Duration:** ${DURATION}s  
**Package:** $APP_PACKAGE  

## Contents
- \`logcat_full.txt\` - Complete system log
- \`logcat_app.txt\` - App-specific logs
- \`crashes.txt\` - Error/crash logs
- \`screenshots/\` - Screenshot sequence (1/sec)
- \`memory/meminfo.txt\` - Memory snapshots
- \`system/system_state.txt\` - Process/window state

## Quick Analysis
**Screenshots:** $(find "$SESSION_DIR/screenshots/" -name "*.png" 2>/dev/null | wc -l)  
**Errors:** $(grep -c "ERROR\|Exception" "$SESSION_DIR/logs/crashes.txt" 2>/dev/null || echo "0")  

## Next Steps
1. Review logcat: \`cat logs/logcat_app.txt\`
2. Check screenshots: \`ls screenshots/\`
3. Examine memory: \`cat memory/meminfo.txt\`

EOF
    log_success "Report: $SESSION_DIR/DEBUG_REPORT.md"
}

trap "log_warn 'Interrupted'; stop_bg; exit 1" INT TERM

echo "================================"
echo "Android Debug Session"
echo "================================"
echo ""

log_info "Starting for $APP_PACKAGE (${DURATION}s)"
check_device
setup_session
launch_app
start_logcat
start_screenshots
start_memory_monitoring
start_system_monitoring
log_success "Recording for $DURATION seconds..."
echo "Interact with app now..."

sleep "$DURATION"
stop_bg
sleep 2
generate_report

echo ""
echo "================================"
echo "Debug Session Complete"
echo "================================"
echo "Results: $SESSION_DIR"

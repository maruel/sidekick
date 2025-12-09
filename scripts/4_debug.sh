#!/bin/bash
# Capture debug session (logs, screenshots, memory)
set -e

APP_PACKAGE="com.fghbuild.sidekick"
DURATION=${1:-30}

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }
log_error() { echo "[ERROR] $1"; }

check_device() {
    log_info "Checking device..."
    adb devices | grep -q "device" || { log_error "No device found"; exit 1; }
    DEVICE=$(adb devices | grep "device" | head -1 | awk '{print $1}')
    log_success "Device: $DEVICE"
}

capture_session() {
    log_info "Capturing debug session for ${DURATION}s..."
    adb logcat "$APP_PACKAGE:D" "*:S" > debug_logcat.txt 2>&1 &
    LOGCAT_PID=$!
    adb exec-out screencap -p > debug_screenshot_start.png
    sleep "$DURATION"
    kill $LOGCAT_PID 2>/dev/null || true
    adb exec-out screencap -p > debug_screenshot_end.png
    log_success "Session captured"
}

capture_memory() {
    log_info "Capturing memory info..."
    adb shell dumpsys meminfo "$APP_PACKAGE" > debug_meminfo.txt
    log_success "Memory info: debug_meminfo.txt"
}

capture_package_info() {
    log_info "Capturing package info..."
    {
        echo "=== Package Info ==="
        adb shell dumpsys package "$APP_PACKAGE" | head -50
        echo ""
        echo "=== Process Info ==="
        adb shell ps -ef | grep "$APP_PACKAGE" || true
    } > debug_package_info.txt
    log_success "Package info: debug_package_info.txt"
}

print_summary() {
    echo ""
    echo "========================================"
    echo "Debug Session Complete"
    echo "========================================"
    echo "Package: $APP_PACKAGE | Device: $DEVICE | Duration: ${DURATION}s"
    echo ""
    echo "Generated files:"
    echo "  - debug_logcat.txt"
    echo "  - debug_meminfo.txt"
    echo "  - debug_package_info.txt"
    echo "  - debug_screenshot_start.png"
    echo "  - debug_screenshot_end.png"
    echo ""
}

trap "log_error 'Failed'; exit 1" ERR

echo "========================================"
echo "Sidekick Debug Session"
echo "========================================"
echo ""

check_device
capture_session
capture_memory
capture_package_info
print_summary

log_success "Debug complete"

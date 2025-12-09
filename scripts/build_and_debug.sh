#!/bin/bash
# Build, install, launch workflow
set -e

VARIANT=${1:-"debug"} RUN_TESTS=${2:-"false"} APP_PACKAGE="com.fghbuild.sidekick" APP_ACTIVITY="MainActivity"

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }
log_error() { echo "[ERROR] $1"; }
log_warn() { echo "[WARN] $1"; }

check_device() {
    log_info "Checking device..."
    adb devices | grep -q "device" || { log_error "No device found"; exit 1; }
    DEVICE=$(adb devices | grep "device" | head -1 | awk '{print $1}')
    log_success "Device: $DEVICE"
}

run_unit_tests() {
    [ "$RUN_TESTS" != "true" ] && return
    log_info "Running tests..."
    if ./gradlew test --no-daemon; then
        log_success "Tests passed"
    else
        log_warn "Tests failed (continuing)"
    fi
}

clean_build() {
    log_info "Cleaning..."
    ./gradlew clean --no-daemon -q
    log_success "Clean complete"
}

assemble_apk() {
    log_info "Building APK ($VARIANT)..."
    UPPER_VARIANT=$(echo "$VARIANT" | tr '[:lower:]' '[:upper:]')
    ./gradlew "assemble${UPPER_VARIANT}" --no-daemon
    APK_PATH="app/build/outputs/apk/$VARIANT/app-${VARIANT}.apk"
    [ -f "$APK_PATH" ] || { log_error "APK not found"; exit 1; }
    log_success "APK: $(du -h "$APK_PATH" | cut -f1)"
}

uninstall_app() {
    log_info "Uninstalling..."
    adb shell pm uninstall "$APP_PACKAGE" 2>/dev/null || true
    sleep 1
    log_success "Uninstall complete"
}

install_app() {
    log_info "Installing..."
    adb install -r "$APK_PATH" || { log_error "Installation failed"; exit 1; }
    log_success "Installed"
}

grant_permissions() {
    log_info "Granting runtime permissions..."
    adb shell pm grant "$APP_PACKAGE" android.permission.ACCESS_FINE_LOCATION
    adb shell pm grant "$APP_PACKAGE" android.permission.ACCESS_COARSE_LOCATION
    adb shell pm grant "$APP_PACKAGE" android.permission.BLUETOOTH_SCAN
    adb shell pm grant "$APP_PACKAGE" android.permission.BLUETOOTH_CONNECT
    adb shell pm grant "$APP_PACKAGE" android.permission.RECORD_AUDIO
    adb shell pm grant "$APP_PACKAGE" android.permission.POST_NOTIFICATIONS
    log_success "Permissions granted"
}

launch_app() {
    log_info "Launching..."
    adb shell am start -n "$APP_PACKAGE/.$APP_ACTIVITY" || { log_error "Launch failed"; exit 1; }
    sleep 2
    log_success "Launched"
}

complete_onboarding() {
    log_info "Completing onboarding..."
    sleep 1
    # Tap Birth Year field
    adb shell input tap 160 373
    sleep 1
    # Clear field and enter birth year
    adb shell input keyevent KEYCODE_CTRL_A
    sleep 0.5
    adb shell input keyevent KEYCODE_DEL
    sleep 0.5
    adb shell input text "1978"
    sleep 1
    # Close keyboard
    adb shell input keyevent KEYCODE_BACK
    sleep 1
    # Tap Get Started button
    adb shell input tap 160 430
    sleep 3
    log_success "Onboarding complete"
}

capture_initial_state() {
    log_info "Capturing state..."
    adb exec-out screencap -p > initial_screenshot.png
    adb logcat -t 100 "$APP_PACKAGE:D" "*:S" > initial_logs.txt 2>/dev/null || true
    { echo "=== App Package Info ==="; adb shell dumpsys package "$APP_PACKAGE" | head -30; echo ""; echo "=== Memory Info ==="; adb shell dumpsys meminfo "$APP_PACKAGE" | head -20; } > app_info.txt
    log_success "Files: app_info.txt, initial_logs.txt, initial_screenshot.png"
}

print_summary() {
    echo ""
    echo "========================================"
    echo "Build & Debug Complete"
    echo "========================================"
    echo "Package: $APP_PACKAGE | Variant: $VARIANT | Device: $DEVICE"
    echo ""
    echo "Generated: initial_screenshot.png, initial_logs.txt, app_info.txt"
    echo ""
}

trap "log_error 'Failed'; exit 1" ERR

echo "========================================"
echo "Android Build & Debug"
echo "========================================"
echo ""

check_device
run_unit_tests
clean_build
assemble_apk
uninstall_app
install_app
grant_permissions
launch_app
complete_onboarding
capture_initial_state
print_summary

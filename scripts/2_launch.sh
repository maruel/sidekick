#!/bin/bash
# Launch app and complete onboarding
set -e

APP_PACKAGE="com.fghbuild.sidekick"
APP_ACTIVITY="MainActivity"

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }
log_error() { echo "[ERROR] $1"; }

check_device() {
    log_info "Checking device..."
    adb devices | grep -q "device" || { log_error "No device found"; exit 1; }
    DEVICE=$(adb devices | grep "device" | head -1 | awk '{print $1}')
    log_success "Device: $DEVICE"
}

is_onboarding_shown() {
    # Check if onboarding has already been completed by checking shared preferences
    # If user_birth_year was saved, onboarding is done
    local prefs_content=$(adb shell "run-as $APP_PACKAGE cat shared_prefs/sidekick_prefs.xml 2>/dev/null" 2>/dev/null)
    if echo "$prefs_content" | grep -q "user_birth_year"; then
        return 1  # Onboarding already done
    fi
    # Default: onboarding needs to be done
    return 0
}

launch_app() {
    log_info "Launching app..."
    adb shell am start -n "$APP_PACKAGE/.$APP_ACTIVITY" || { log_error "Launch failed"; exit 1; }
    sleep 3
    log_success "Launched"
}

complete_onboarding() {
    log_info "Checking if onboarding is needed..."
    # Check if onboarding screen is actually displayed
    if ! is_onboarding_shown; then
        log_info "Onboarding already completed, skipping..."
        return
    fi
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

capture_screenshot() {
    log_info "Capturing screenshot..."
    adb exec-out screencap -p > launch_screenshot.png
    log_success "Screenshot: launch_screenshot.png"
}

print_summary() {
    echo ""
    echo "========================================"
    echo "App Launched"
    echo "========================================"
    echo "Package: $APP_PACKAGE | Device: $DEVICE"
    echo ""
    echo "Screenshot: launch_screenshot.png"
    echo ""
    echo "Next: ./scripts/3_add_fake_data.sh (optional)"
    echo ""
}

trap "log_error 'Failed'; exit 1" ERR

echo "========================================"
echo "Sidekick Launch"
echo "========================================"
echo ""

check_device
launch_app
complete_onboarding
capture_screenshot
print_summary

log_success "Launch complete"

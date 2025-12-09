#!/bin/bash
# Build APK, install, and grant permissions
set -e

VARIANT=${1:-"debug"}
APP_PACKAGE="com.fghbuild.sidekick"

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

print_summary() {
    echo ""
    echo "========================================"
    echo "Build Complete"
    echo "========================================"
    echo "Package: $APP_PACKAGE | Variant: $VARIANT | Device: $DEVICE"
    echo ""
    echo "Next: ./scripts/2_launch.sh"
    echo ""
}

trap "log_error 'Failed'; exit 1" ERR

echo "========================================"
echo "Sidekick Build"
echo "========================================"
echo ""

check_device
clean_build
assemble_apk
uninstall_app
install_app
grant_permissions
print_summary

log_success "Build complete"

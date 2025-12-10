#!/bin/bash
# Generate fake run history for UI design testing
set -e

APP_PACKAGE="com.fghbuild.sidekick"

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }
log_error() { echo "[ERROR] $1"; }

check_device() {
    log_info "Checking device..."
    adb devices | grep -q "device" || { log_error "No device found"; exit 1; }
    DEVICE=$(adb devices | grep "device" | head -1 | awk '{print $1}')
    log_success "Device: $DEVICE"
}

ensure_app_initialized() {
    log_info "Ensuring app database exists..."
    adb shell am start -n "$APP_PACKAGE/.MainActivity" 2>/dev/null || true
    sleep 3
    adb shell am force-stop "$APP_PACKAGE"
    sleep 1
    log_success "App database initialized"
}

generate_runs() {
    local count=${1:-10}
    log_info "Generating $count fake runs..."
    adb shell am force-stop "$APP_PACKAGE"
    sleep 1
    local sql_commands=""
    for i in $(seq 1 "$count"); do
        local days_ago=$((30 - (i % 30)))
        local now_seconds
        now_seconds=$(date +%s)
        local start_time=$((now_seconds * 1000 - days_ago * 86400000))
        local duration_mins=$((15 + RANDOM % 45))
        local duration_millis=$((duration_mins * 60 * 1000))
        local end_time=$((start_time + duration_millis))
        local distance_km=$((2 + RANDOM % 14))
        local distance_m=$((distance_km * 1000))
        local pace=$((duration_mins * 60 / distance_km))
        local avg_hr=$((120 + RANDOM % 60))
        local min_hr=$((avg_hr - 20 - RANDOM % 10))
        local max_hr=$((avg_hr + 20 + RANDOM % 20))
        [ $min_hr -lt 60 ] && min_hr=60
        sql_commands="${sql_commands}INSERT INTO runs (startTime, endTime, distanceMeters, durationMillis, averagePaceMinPerKm, maxHeartRate, minHeartRate, averageHeartRate) VALUES ($start_time, $end_time, $distance_m, $duration_millis, $pace, $max_hr, $min_hr, $avg_hr);"
    done
    if adb shell "run-as $APP_PACKAGE sqlite3 databases/sidekick_database \"$sql_commands\"" 2>/dev/null; then
        log_success "Generated $count fake runs"
    else
        log_error "Failed to insert runs via sqlite3"
        return 1
    fi
}

verify_runs() {
     log_info "Verifying runs..."
     local count
     count=$(adb shell "run-as $APP_PACKAGE sqlite3 databases/sidekick_database \"SELECT COUNT(*) FROM runs;\"" 2>/dev/null || echo "0")
     log_success "Total runs in database: $count"
}

trap "log_error 'Failed'; exit 1" ERR

echo "========================================"
echo "Sidekick Fake Data Generator"
echo "========================================"
echo ""

check_device
ensure_app_initialized
generate_runs "${1:-10}"
verify_runs

log_success "Fake data complete"

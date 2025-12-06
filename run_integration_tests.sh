#!/bin/bash
# Integration Test Runner with xvfb for headless Android emulator
set -eu

export ANDROID_HOME="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
echo "Using ANDROID_HOME=$ANDROID_HOME"
echo "Using ANDROID_AVD_HOME=$ANDROID_AVD_HOME"

# Configuration
RESOLUTION="1024x768x24"
EMULATOR_NAME="test_device"
SYSTEM_IMAGE="system-images;android-36;google_apis;x86_64"
TIMEOUT=900  # 15 minutes timeout for emulator boot

LOG_FILE="/tmp/emulator_boot.log"
EMULATOR_PID=""

# Find an available display number
find_available_display() {
    local display_num=99
    while [ $display_num -lt 200 ]; do
        if ! [ -S "/tmp/.X11-unix/X${display_num}" ] 2>/dev/null; then
            echo $display_num
            return 0
        fi
        display_num=$((display_num + 1))
    done
    echo "Error: Could not find available display" >&2
    return 1
}

DISPLAY_NUM=$(find_available_display)
if [ -z "$DISPLAY_NUM" ]; then
    exit 1
fi

export DISPLAY=:${DISPLAY_NUM}
echo "Using DISPLAY=:${DISPLAY_NUM}"

# Kill any existing emulators before starting
echo "Cleaning up any stale emulator processes..."
pkill -9 -f "emulator.*${EMULATOR_NAME}" >/dev/null 2>&1 || true
pkill -9 -f "qemu" >/dev/null 2>&1 || true
adb kill-server >/dev/null 2>&1 || true
sleep 2

# Diagnostic function
diagnose() {
    echo "=== Diagnostic Information ==="
    echo "Checking required tools..."
    command -v adb >/dev/null 2>&1 && echo "✓ adb found" || echo "✗ adb NOT found"
    command -v emulator >/dev/null 2>&1 && echo "✓ emulator found" || echo "✗ emulator NOT found"
    command -v sdkmanager >/dev/null 2>&1 && echo "✓ sdkmanager found" || echo "✗ sdkmanager NOT found"
    command -v avdmanager >/dev/null 2>&1 && echo "✓ avdmanager found" || echo "✗ avdmanager NOT found"
    echo ""
    echo "Emulator process status:"
    if [ ! -z "$EMULATOR_PID" ] && kill -0 $EMULATOR_PID 2>/dev/null; then
        echo "✓ Emulator process is running (PID: $EMULATOR_PID)"
        echo "Last 20 lines of emulator log:"
        tail -20 "$LOG_FILE" 2>/dev/null || echo "Log file not available yet"
    else
        echo "✗ Emulator process is NOT running"
        echo "Full emulator log:"
        cat "$LOG_FILE" 2>/dev/null || echo "No log file"
    fi
    echo ""
    echo "ADB devices output:"
    adb devices || echo "adb failed"
    echo ""
}

cleanup() {
    echo "Cleaning up..."
    if [ ! -z "$EMULATOR_PID" ]; then
        echo "Killing emulator (PID: $EMULATOR_PID)..."
        kill $EMULATOR_PID 2>/dev/null || true
        sleep 2
    fi
    if pgrep -f "emulator.*${EMULATOR_NAME}" >/dev/null 2>&1; then
        echo "Force killing remaining emulator processes..."
        pkill -9 -f "emulator.*${EMULATOR_NAME}" || true
    fi
    pkill -9 -f "qemu" >/dev/null 2>&1 || true
    if [ ! -z "${XVFB_PID:-}" ]; then
        echo "Killing Xvfb (PID: $XVFB_PID)..."
        kill $XVFB_PID 2>/dev/null || true
    fi
    adb kill-server >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Starting Xvfb on DISPLAY=:${DISPLAY_NUM}..."
Xvfb :${DISPLAY_NUM} -screen 0 ${RESOLUTION} &
XVFB_PID=$!
sleep 2

echo "Checking Xvfb startup..."
if ! kill -0 $XVFB_PID 2>/dev/null; then
    echo "Error: Xvfb failed to start"
    exit 1
fi

# Check if system image is installed, install if missing
echo "Checking system image: $SYSTEM_IMAGE"
if ! sdkmanager --list_installed | grep -q "$SYSTEM_IMAGE"; then
    echo "Installing system image: $SYSTEM_IMAGE"
    echo "no" | sdkmanager --install "$SYSTEM_IMAGE" || { echo "Error: Failed to install system image"; exit 1; }
else
    echo "System image already installed"
fi

# Ensure AVD directory exists for avdmanager
mkdir -p "$ANDROID_AVD_HOME"

# Check if emulator AVD exists, if not create it
echo "Checking for emulator AVD: $EMULATOR_NAME"
if ! emulator -list-avds | grep -q "^${EMULATOR_NAME}$"; then
    echo "AVD not found. Creating $EMULATOR_NAME..."
    if echo "no" | avdmanager create avd -n ${EMULATOR_NAME} -k "$SYSTEM_IMAGE" --force 2>&1; then
        echo "AVD creation command completed"
    else
        echo "Error: avdmanager create avd command failed"
        exit 1
    fi
else
    echo "AVD already exists"
fi

# Validate AVD config
echo "Validating AVD configuration..."
AVD_CONFIG="${ANDROID_AVD_HOME}/${EMULATOR_NAME}.avd/config.ini"
if [ ! -f "$AVD_CONFIG" ]; then
    echo "Error: AVD config not found at $AVD_CONFIG"
    echo "Available AVDs:"
    emulator -list-avds || echo "Failed to list AVDs"
    echo "Contents of $ANDROID_AVD_HOME:"
    ls -la "$ANDROID_AVD_HOME" 2>/dev/null || echo "Directory does not exist"
    exit 1
fi
echo "AVD config found"

rm -f "$LOG_FILE"
echo "Booting emulator: $EMULATOR_NAME (logging to $LOG_FILE)..."

# Check for KVM availability
ACCEL_FLAG="auto"
if [ ! -e /dev/kvm ]; then
    echo "KVM not available, using software acceleration"
    ACCEL_FLAG="off"
else
    echo "KVM available, using hardware acceleration"
    ACCEL_FLAG="on"
fi

# Launch emulator with detailed output to diagnose startup issues
emulator -avd ${EMULATOR_NAME} \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -no-metrics \
    -memory 2048 \
    -accel ${ACCEL_FLAG} \
    -read-only \
    -logcat '*:V' \
    > "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!
echo "Emulator started with PID: $EMULATOR_PID"

# Give it a bit longer to start
sleep 8

# Check immediately what happened
if ! kill -0 $EMULATOR_PID 2>/dev/null; then
    echo "Error: Emulator process died immediately after launch"
    echo ""
    echo "=== Emulator output (first 30 lines) ==="
    head -30 "$LOG_FILE"
    echo ""
    echo "=== Emulator output (last 30 lines) ==="
    tail -30 "$LOG_FILE"
    diagnose
    exit 1
fi

echo "Emulator process is still running (PID: $EMULATOR_PID)"

echo "Waiting for emulator to boot (timeout: ${TIMEOUT}s)..."
COUNTER=0
LAST_DEVICE_COUNT=0

while [ $COUNTER -lt $TIMEOUT ]; do
    # Check if emulator process is still running
    if ! kill -0 $EMULATOR_PID 2>/dev/null; then
        echo "Error: Emulator process crashed"
        diagnose
        exit 1
    fi

    # Check for connected device
    DEVICE_COUNT=$(adb devices 2>/dev/null | grep -c "emulator.*device" 2>/dev/null || echo "0")
    if [ "$DEVICE_COUNT" -gt "$LAST_DEVICE_COUNT" ]; then
        LAST_DEVICE_COUNT=$DEVICE_COUNT
        echo "Device detected! (attempt: $((COUNTER/5)))"
    fi

    if [ "$DEVICE_COUNT" -gt 0 ]; then
        echo "Emulator ready! Running 'adb shell getprop sys.boot_completed'..."
        if adb shell getprop sys.boot_completed | grep -q "1"; then
            echo "Boot completed confirmed!"
            break
        else
            echo "Boot still in progress..."
        fi
    fi

    echo "Waiting... ($COUNTER/$TIMEOUT)"
    sleep 5
    COUNTER=$((COUNTER + 5))
done

if [ $COUNTER -ge $TIMEOUT ]; then
    echo "Error: Emulator failed to boot within ${TIMEOUT} seconds"
    diagnose
    exit 1
fi

sleep 5

echo "Clearing app data and cache from emulator..."
adb shell pm clear com.fghbuild.sidekick || true
adb shell rm -rf /data/user/0/com.fghbuild.sidekick || true
adb shell rm -rf /data/data/com.fghbuild.sidekick || true
adb shell rm -rf /data/app/*/app_dxmaker_cache || true

echo "Running integration tests..."
./gradlew connectedAndroidTest --continue

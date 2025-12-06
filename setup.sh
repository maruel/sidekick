#!/bin/bash
# Installs Java, Android SDK cmdline-tools, emulator, and required system images
# Usage: ./setup.sh [--no-emulator]
#   --no-emulator: Skip installing emulator and system images (for build-only jobs)
set -eu

INSTALL_EMULATOR=true
for arg in "$@"; do
    case $arg in
        --no-emulator)
            INSTALL_EMULATOR=false
            ;;
    esac
done

echo "Installing system dependencies..."
sudo apt-get update -qq
# On CI, we use actions/setup-java because it automatically caches graddle output, which is critical for
# performance.
#  openjdk-21-jdk-headless
if [ "$INSTALL_EMULATOR" = true ]; then
    sudo apt-get install -qq -y curl libpulse0 libvulkan1 xvfb
else
    sudo apt-get install -qq -y curl
fi

ANDROID_HOME="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
mkdir -p "$ANDROID_HOME/cmdline-tools"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# Check if cmdline-tools is already installed
if [ ! -f "$SDKMANAGER" ]; then
    echo "Fetching latest cmdline-tools version from developer.android.com..."
    SDK_URL=$(curl -s "https://developer.android.com/studio" | grep -o 'https://dl\.google\.com/android/repository/commandlinetools-linux-[0-9]*_latest\.zip' | head -n 1)
    if [ -z "$SDK_URL" ]; then
        echo "Error: Could not find cmdline-tools download URL from developer.android.com"
        exit 1
    fi
    echo "Downloading cmdline-tools from: $SDK_URL"
    wget -q "$SDK_URL" -O /tmp/cmdline-tools.zip
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract
    rm /tmp/cmdline-tools.zip
    mv /tmp/cmdline-tools-extract/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
    chmod 755 "$ANDROID_HOME/cmdline-tools/latest/bin"/*
else
    echo "Updating cmdline-tools..."
    "$SDKMANAGER" --update >/dev/null 2>&1 || true
fi

# Accept all licenses
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

# Install required SDK components
echo "Installing Android SDK packages..."
if [ "$INSTALL_EMULATOR" = true ]; then
    "$SDKMANAGER" \
        "build-tools;36.0.0" \
        "emulator" \
        "platform-tools" \
        "platforms;android-36" \
        "system-images;android-36;google_apis;x86_64"
else
    "$SDKMANAGER" \
        "build-tools;36.0.0" \
        "platform-tools" \
        "platforms;android-36"
fi
echo "Android SDK setup complete!"

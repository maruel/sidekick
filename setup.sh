#!/bin/bash
# Installs Java, Android SDK cmdline-tools, emulator, and required system images
set -eu

echo "Installing system dependencies..."
sudo apt-get update -qq
# On CI, we use actions/setup-java because it automatically caches graddle output, which is critical for
# performance.
#  openjdk-21-jdk-headless
sudo apt-get install -qq -y curl libpulse0 libvulkan1 xvfb

mkdir -p "$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"
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
# Move cmdline-tools to proper location
mv /tmp/cmdline-tools-extract/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
chmod 755 "$ANDROID_HOME/cmdline-tools/latest/bin"/*
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
# Accept all licenses
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

# Install required SDK components
echo "Installing Android SDK packages..."
"$SDKMANAGER" \
    "build-tools;36.0.0" \
    "emulator" \
    "platform-tools" \
    "platforms;android-36" \
    "system-images;android-36;google_apis;x86_64"
echo "Android SDK setup complete!"

#!/bin/bash
# Real-time logcat filtering
set -e

APP_PACKAGE=${1:-"com.fghbuild.sidekick"}
LOG_LEVEL=${2:-"D"}

[ "$1" = "-h" ] || [ "$1" = "--help" ] && {
    cat <<EOF
Real-time logcat filter

USAGE: ./logcat_filter.sh [app_package] [log_level]

ARGS:
    app_package  - Package name (default: com.fghbuild.sidekick)
    log_level    - V/D/I/W/E (default: D)

EXAMPLES:
    ./logcat_filter.sh com.fghbuild.sidekick D
    ./logcat_filter.sh com.fghbuild.sidekick E

Press Ctrl+C to exit
EOF
    exit 0
}

echo "========================================="
echo "Logcat Filter - Real-time Monitor"
echo "Package: $APP_PACKAGE | Level: $LOG_LEVEL"
echo "Press Ctrl+C to exit"
echo "========================================="
echo ""

adb logcat -v threadtime "$APP_PACKAGE:$LOG_LEVEL" "*:S" | while read -r line; do
    echo "$line"
done

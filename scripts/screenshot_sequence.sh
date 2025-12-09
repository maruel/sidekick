#!/bin/bash
# Capture sequential screenshots for visual testing
set -e

NUM_FRAMES=${1:-10}
INTERVAL_MS=${2:-500}
OUTPUT_DIR=${3:-"screenshots_$(date +%Y%m%d_%H%M%S)"}
INTERVAL_SEC=$(echo "scale=2; $INTERVAL_MS / 1000" | bc)

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }

[ "$1" = "-h" ] || [ "$1" = "--help" ] && {
    cat <<EOF
Screenshot Sequence Capture

USAGE: ./screenshot_sequence.sh [num_frames] [interval_ms] [output_dir]

ARGS:
    num_frames    - Number of screenshots (default: 10)
    interval_ms   - Milliseconds between captures (default: 500)
    output_dir    - Output directory (default: screenshots_TIMESTAMP)

EXAMPLES:
    ./screenshot_sequence.sh 20 1000 myapp_screens
    ./screenshot_sequence.sh 5 250

OUTPUT: screen_001.png, screen_002.png, ...

ANALYSIS:
    feh --slideshow-delay 500 screenshots_*/
    convert -delay 50 screenshots_*/screen_*.png output.gif

EOF
    exit 0
}

mkdir -p "$OUTPUT_DIR"
log_info "Capturing $NUM_FRAMES frames at ${INTERVAL_MS}ms intervals"
log_info "Output: $OUTPUT_DIR"
echo ""

TOTAL_DURATION=$(echo "scale=1; ($NUM_FRAMES - 1) * $INTERVAL_SEC" | bc)
log_info "Total duration: ${TOTAL_DURATION}s"
echo ""

for i in $(seq 1 "$NUM_FRAMES"); do
    FRAME_NUM=$(printf "%03d" "$i")
    FILENAME="$OUTPUT_DIR/screen_$FRAME_NUM.png"
    
    adb exec-out screencap -p > "$FILENAME"
    SIZE=$(du -h "$FILENAME" | cut -f1)
    ELAPSED=$(echo "scale=1; ($i - 1) * $INTERVAL_SEC" | bc)
    PROGRESS=$((i * 100 / NUM_FRAMES))
    FILLED=$((PROGRESS / 5))
    EMPTY=$((20 - FILLED))
    BAR="["$(printf '#%.0s' $(seq 1 $FILLED))$(printf '-%.0s' $(seq 1 $EMPTY))"]"
    
    printf "\r%s %3d%% [%2d/%2d] %5.1fs %s" "$BAR" "$PROGRESS" "$i" "$NUM_FRAMES" "$ELAPSED" "$SIZE"
    
    [ "$i" -lt "$NUM_FRAMES" ] && sleep "$INTERVAL_SEC"
done

echo "" && echo ""
log_success "Captured $NUM_FRAMES to: $OUTPUT_DIR"
TOTAL_SIZE=$(du -sh "$OUTPUT_DIR" | cut -f1)
log_info "Total size: $TOTAL_SIZE | First: screen_001.png | Last: screen_$(printf '%03d' "$NUM_FRAMES").png"
echo ""
echo "Next steps:"
echo "  feh --slideshow-delay 500 $OUTPUT_DIR/"
echo "  convert -delay 50 $OUTPUT_DIR/screen_*.png output.gif"

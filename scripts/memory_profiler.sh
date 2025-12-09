#!/bin/bash
# Memory usage profiler - monitor and graph memory consumption
set -e

APP_PACKAGE=${1:-"com.fghbuild.sidekick"}
DURATION=${2:-60}
INTERVAL=${3:-2}
OUTPUT_DIR="memory_profile_$(date +%Y%m%d_%H%M%S)"
CSV_FILE="$OUTPUT_DIR/memory_data.csv"
LOG_FILE="$OUTPUT_DIR/memory_log.txt"

log_info() { echo "[INFO] $1"; }
log_success() { echo "[OK] $1"; }
log_warn() { echo "[WARN] $1"; }

[ "$1" = "-h" ] || [ "$1" = "--help" ] && {
    cat <<EOF
Memory Profiler - Monitor app memory usage

USAGE: ./memory_profiler.sh [app_package] [duration] [interval]

ARGS:
    app_package  - Package name (default: com.fghbuild.sidekick)
    duration     - Total duration in seconds (default: 60)
    interval     - Sample interval in seconds (default: 2)

EXAMPLES:
    ./memory_profiler.sh
    ./memory_profiler.sh com.fghbuild.sidekick 300 5
    ./memory_profiler.sh com.fghbuild.sidekick 30 1

OUTPUT: memory_data.csv, memory_log.txt, memory_report.txt, memory_profile.png

FIELDS:
    Native Heap  - C/C++ memory
    Java Heap    - Java/Kotlin objects
    Graphics     - GPU memory
    TOTAL        - Sum of all memory

INTERPRETATION:
    Flat line = Normal
    Continuous increase = Possible leak
    Spikes = Allocation/deallocation
EOF
    exit 0
}

setup() {
    mkdir -p "$OUTPUT_DIR"
    echo "timestamp,elapsed_sec,native_heap_kb,java_heap_kb,graphics_kb,total_kb,gc_count" > "$CSV_FILE"
    log_info "App: $APP_PACKAGE | Duration: ${DURATION}s | Interval: ${INTERVAL}s"
    log_info "Output: $OUTPUT_DIR"
    log_info ""
    log_info "Profiling..."
    log_info ""
}

profile_memory() {
    START_TIME=$(date +%s) SAMPLE=1
    
    while true; do
        CURRENT_TIME=$(date +%s) ELAPSED=$((CURRENT_TIME - START_TIME))
        [ "$ELAPSED" -ge "$DURATION" ] && break
        
        MEMINFO=$(adb shell dumpsys meminfo "$APP_PACKAGE")
        NATIVE_HEAP=$(echo "$MEMINFO" | grep "Native Heap" | awk '{print $3}' | tr -d ',')
        JAVA_HEAP=$(echo "$MEMINFO" | grep "Java Heap" | awk '{print $3}' | tr -d ',')
        GRAPHICS=$(echo "$MEMINFO" | grep "Graphics" | awk '{print $2}' | tr -d ',')
        TOTAL=$(echo "$MEMINFO" | grep "TOTAL " | awk '{print $2}' | tr -d ',')
        GC_COUNT=$(echo "$MEMINFO" | grep "GC_COUNT" | awk '{print $2}')
        
        NATIVE_HEAP=${NATIVE_HEAP:-0} JAVA_HEAP=${JAVA_HEAP:-0} GRAPHICS=${GRAPHICS:-0} TOTAL=${TOTAL:-0} GC_COUNT=${GC_COUNT:-0}
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
        echo "$TIMESTAMP,$ELAPSED,$NATIVE_HEAP,$JAVA_HEAP,$GRAPHICS,$TOTAL,$GC_COUNT" >> "$CSV_FILE"
        
        { echo "=== Sample $SAMPLE (${ELAPSED}s) ===" && echo "$MEMINFO" && echo ""; } >> "$LOG_FILE"
        
        PERCENT=$((ELAPSED * 100 / DURATION)) FILLED=$((PERCENT / 5)) EMPTY=$((20 - FILLED))
        BAR="["$(printf '#%.0s' $(seq 1 $FILLED))$(printf '-%.0s' $(seq 1 $EMPTY))"]"
        TOTAL_MB=$((TOTAL / 1024))
        printf "\r%s %3d%% [%2d samples] %4dMB" "$BAR" "$PERCENT" "$SAMPLE" "$TOTAL_MB"
        
        SAMPLE=$((SAMPLE + 1))
        sleep "$INTERVAL"
    done
    echo "" && echo ""
}

analyze_memory() {
    log_info "Analyzing..."
    
    REPORT="$OUTPUT_DIR/memory_report.txt"
    {
        echo "Memory Profiling Report"
        echo "======================="
        echo ""
        echo "Session: $(date)"
        echo "Package: $APP_PACKAGE"
        echo "Duration: ${DURATION}s"
        echo "Samples: $(tail -n +2 "$CSV_FILE" | wc -l)"
        echo ""
        echo "Total Memory Statistics (in MB)"
        echo "==============================="
        
        awk -F',' 'NR>1 {total=$5/1024; if(NR==2){first=total;min=total;max=total} else {if(total<min)min=total; if(total>max)max=total} sum+=total}
        END {if(NR>1){count=NR-1; avg=sum/count; printf "  First:    %6.1f MB\n", first; printf "  Min:      %6.1f MB\n", min; printf "  Max:      %6.1f MB\n", max; printf "  Average:  %6.1f MB\n", avg; printf "  Last:     %6.1f MB\n", total; printf "  Growth:   %6.1f MB (%+.1f%%)\n", (total-first), ((total-first)/first*100)}}' "$CSV_FILE"
        
        echo ""
        echo "Heap Breakdown (Latest)"
        echo "======================="
        LAST_ROW=$(tail -1 "$CSV_FILE")
        NATIVE_KB=$(echo "$LAST_ROW" | cut -d',' -f3) JAVA_KB=$(echo "$LAST_ROW" | cut -d',' -f4) GRAPHICS_KB=$(echo "$LAST_ROW" | cut -d',' -f5)
        echo "  Native: $((NATIVE_KB / 1024)) MB | Java: $((JAVA_KB / 1024)) MB | Graphics: $((GRAPHICS_KB / 1024)) MB"
        echo ""
        echo "Interpretation"
        echo "=============="
        echo "Flat = Normal | Continuous increase = Leak | Spikes = Alloc/dealloc"
    } > "$REPORT"
    
    log_success "Report: $REPORT"
}

generate_plot() {
    log_info "Generating plot..."
    
    PLOT_SCRIPT="$OUTPUT_DIR/plot.gnuplot"
    cat > "$PLOT_SCRIPT" <<'EOF'
set datafile separator ","
set xlabel "Time (seconds)"
set ylabel "Memory (MB)"
set title "Memory Usage Profile"
set terminal png size 1024,600
set output 'memory_profile.png'
set style data lines
set grid

plot 'memory_data.csv' using (column(2)):(column(5)/1024) title "Total" linewidth 2, \
     'memory_data.csv' using (column(2)):(column(3)/1024) title "Native" linewidth 1.5, \
     'memory_data.csv' using (column(2)):(column(4)/1024) title "Java" linewidth 1.5
EOF
    
    log_success "Plot script: $PLOT_SCRIPT"
    
    if command -v gnuplot &>/dev/null; then
        cd "$OUTPUT_DIR" && gnuplot plot.gnuplot && cd - >/dev/null
        log_success "Generated: memory_profile.png"
    else
        log_warn "gnuplot not found. Run: cd $OUTPUT_DIR && gnuplot plot.gnuplot"
    fi
}

print_summary() {
    echo ""
    echo "========================================"
    echo "Memory Profiling Complete"
    echo "========================================"
    echo ""
    log_success "Output: $OUTPUT_DIR"
    echo ""
    echo "Files: memory_data.csv, memory_log.txt, memory_report.txt"
    [ -f "$OUTPUT_DIR/memory_profile.png" ] && echo ", memory_profile.png"
    echo ""
}

trap "log_warn 'Interrupted'; exit 1" INT TERM

echo "========================================"
echo "Android Memory Profiler"
echo "========================================"
echo ""

setup
profile_memory
analyze_memory
generate_plot
print_summary

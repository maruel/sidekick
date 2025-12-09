#!/bin/bash
# Script to extract file descriptions from Kotlin source files
# Purpose: Help LLM coding agents quickly learn about the codebase
set -eu
missing=()
while IFS= read -r file; do
    rel_path=${file#*app/src/main/java/}
    first_comment=$(head -20 "$file" 2>/dev/null | grep -m 1 '^[[:space:]]*//[^/]' || true)
    if [ -n "$first_comment" ]; then
        desc="${first_comment#*//}"
        desc="${desc#[[:space:]]}"
        echo "$rel_path: $desc"
    else
        missing+=("$rel_path")
    fi
done < <(find app/src/main/java -type f -name "*.kt" | sort)
if [ ${#missing[@]} -gt 0 ]; then
    echo ""
    echo "Missing: ${missing[*]}"
fi

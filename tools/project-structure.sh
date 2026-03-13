#!/bin/bash

# Output File Name
OUTPUT_FILE="docs/project-structure/project-structure$(date +'%Y%m%d_%H%M%S').txt"

# Scanning Directory
TARGET_DIR="."

# Files to be ignored (Regex format)
IGNORE_PATTERN="node_modules\|\.git\|\.github\|\.gradle\|__pycache__\|\.venv"

echo "Project Structure Generating: $TARGET_DIR"
echo "Target File: $OUTPUT_FILE"

# Add Title
echo "Project Root Folder: $(pwd)" > "$OUTPUT_FILE"
echo "Date Created: $(date +'%Y-%m-%d %H:%M:%S')" >> "$OUTPUT_FILE"
echo "----------------------------------------" >> "$OUTPUT_FILE"

find "$TARGET_DIR" -not -path '*/.*' | grep -v "$IGNORE_PATTERN" | sed -e 's/[^-][^\/]*\// |/g' -e 's/|/  /g' -e 's/  \([^ ]\)/|-- \1/' >> "$OUTPUT_FILE"

echo "İşlem tamamlandı!"


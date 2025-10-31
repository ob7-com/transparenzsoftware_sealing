#!/bin/bash

# Script to migrate Transparenzsoftware code to new package structure
# from de.safe_ev.transparenzsoftware.* to com.metabit.custom.safe.transparency.*

SOURCE_DIR="./temp-transparenzsoftware/src/main/java/de/safe_ev/transparenzsoftware"
TARGET_DIR="./src/main/java/com/metabit/custom/safe/transparency"

# Copy all Java files and replace package declarations
find "$SOURCE_DIR" -name "*.java" -type f 2>/dev/null | while read file; do
    # Get relative path from source
    rel_path="${file#$SOURCE_DIR/}"
    
    # Create target directory structure
    target_file="$TARGET_DIR/$rel_path"
    target_dir=$(dirname "$target_file")
    mkdir -p "$target_dir"
    
    # Copy file and replace package declaration
    sed 's/^package de\.safe_ev\.transparenzsoftware\./package com.metabit.custom.safe.transparency./g' "$file" | \
    sed 's/import de\.safe_ev\.transparenzsoftware\./import com.metabit.custom.safe.transparency./g' | \
    sed 's/de\.safe_ev\.transparenzsoftware\./com.metabit.custom.safe.transparency./g' > "$target_file"
    
    echo "Migrated: $rel_path"
done

echo "Migration complete!"


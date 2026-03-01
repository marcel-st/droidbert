#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 /path/to/fdroiddata"
  exit 1
fi

FDROIDDATA_DIR="$1"
SRC_FILE="fdroid/com.droidbert.yml"
SRC_META_DIR="fdroid/com.droidbert"
DST_DIR="$FDROIDDATA_DIR/metadata"
DST_FILE="$DST_DIR/com.droidbert.yml"
DST_META_DIR="$DST_DIR/com.droidbert"

if [ ! -f "$SRC_FILE" ]; then
  echo "Source file not found: $SRC_FILE"
  exit 1
fi

if [ ! -d "$SRC_META_DIR" ]; then
  echo "Source metadata directory not found: $SRC_META_DIR"
  exit 1
fi

if [ ! -d "$FDROIDDATA_DIR" ]; then
  echo "fdroiddata directory not found: $FDROIDDATA_DIR"
  exit 1
fi

mkdir -p "$DST_DIR"
cp "$SRC_FILE" "$DST_FILE"
mkdir -p "$DST_META_DIR"
cp -R "$SRC_META_DIR/"* "$DST_META_DIR/"

echo "Copied: $SRC_FILE -> $DST_FILE"
echo "Copied: $SRC_META_DIR -> $DST_META_DIR"

if [ -d "$FDROIDDATA_DIR/.git" ]; then
  echo
  echo "Next commands:"
  echo "  cd \"$FDROIDDATA_DIR\""
  echo "  git add metadata/com.droidbert.yml metadata/com.droidbert"
  echo "  git commit -m \"Add Droidbert (com.droidbert)\""
  echo "  git push origin add-droidbert"
fi
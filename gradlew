#!/usr/bin/env bash
# Minimal bootstrapper to download a Gradle distribution and run it.
# Usage: ./gradlew [args]
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$ROOT_DIR/.gradle-dist"
GRADLE_VERSION=${GRADLE_VERSION:-8.5.1}
GRADLE_HOME="$DIST_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  echo "Gradle $GRADLE_VERSION no encontrado. Descargando..."
  mkdir -p "$DIST_DIR"
  ZIP_PATH="/tmp/gradle-$GRADLE_VERSION-bin.zip"
  DOWNLOAD_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  curl -fSL --retry 5 --retry-delay 5 -o "$ZIP_PATH" "$DOWNLOAD_URL"
  unzip -q "$ZIP_PATH" -d "$DIST_DIR"
  rm -f "$ZIP_PATH"
fi

exec "$GRADLE_BIN" "$@"

#!/usr/bin/env bash
set -euo pipefail
VERSION=8.5.1
DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$DIR/gradle/gradle-$VERSION"
TMP_ZIP="/tmp/gradle-$VERSION.zip"

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  echo "Gradle $VERSION no encontrado localmente. Descargando..."
  mkdir -p "$DIR/gradle"
  if command -v curl >/dev/null 2>&1; then
    curl -sSL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$TMP_ZIP"
  else
    wget -q -O "$TMP_ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  fi
  unzip -q "$TMP_ZIP" -d "$DIR/gradle"
  rm -f "$TMP_ZIP"
fi

exec "$DIST_DIR/bin/gradle" "$@"

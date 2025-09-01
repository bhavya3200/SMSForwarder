#!/usr/bin/env bash
set -euo pipefail
GRADLE_VERSION=${GRADLE_VERSION:-8.7}

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WRAP_DIR="$HERE/.gradle/wrapper"
BIN_DIR="$HERE/.gradle/gradle-$GRADLE_VERSION/bin"
GRADLE_BIN="$BIN_DIR/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$WRAP_DIR" "$HERE/.gradle"
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl -L -o "$HERE/gradle-$GRADLE_VERSION-bin.zip" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -q "$HERE/gradle-$GRADLE_VERSION-bin.zip" -d "$HERE/.gradle"
  mv "$HERE/.gradle/gradle-$GRADLE_VERSION" "$HERE/.gradle/gradle-$GRADLE_VERSION" >/dev/null 2>&1 || true
  rm -f "$HERE/gradle-$GRADLE_VERSION-bin.zip"
fi

exec "$GRADLE_BIN" "$@"

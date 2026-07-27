#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_DIR="$SCRIPT_DIR/android-sdk"
KOTLIN_DIR="$SCRIPT_DIR/kotlinc"
BUILD_TOOLS="34.0.0"
PLATFORM="android-34"

say() { printf "\033[1;34m[INFO]\033[0m %s\n" "$*"; }
die() { printf "\033[1;31m[ERROR]\033[0m %s\n" "$*"; exit 1; }

# ─── 1. Kotlin Compiler ────────────────────────────────────────────────────────
if [[ ! -d "$KOTLIN_DIR" ]]; then
    say "Downloading Kotlin compiler..."
    mkdir -p "$KOTLIN_DIR"
    curl -L -o /tmp/kotlin.zip \
        "https://github.com/JetBrains/kotlin/releases/download/v2.1.0/kotlin-compiler-2.1.0.zip"
    unzip -q /tmp/kotlin.zip -d "$SCRIPT_DIR/"
    mv "$SCRIPT_DIR/kotlin-compiler-2.1.0"/* "$KOTLIN_DIR/" 2>/dev/null || true
    chmod +x "$KOTLIN_DIR/bin/kotlinc"
    rm -rf /tmp/kotlin.zip "$SCRIPT_DIR/kotlin-compiler-2.1.0"
    say "Kotlin ready: $KOTLIN_DIR"
else
    say "Kotlin already present"
fi

# ─── 2. Android SDK platform (android.jar) ─────────────────────────────────────
mkdir -p "$SDK_DIR/platforms/$PLATFORM"
if [[ ! -f "$SDK_DIR/platforms/$PLATFORM/android.jar" ]]; then
    say "Downloading Android $PLATFORM platform..."
    curl -L -o /tmp/platform-34.zip \
        "https://dl.google.com/android/repository/platform-34_r08.zip"
    unzip -q /tmp/platform-34.zip -d /tmp/platform-extract/
    cp /tmp/platform-extract/android-*/android.jar "$SDK_DIR/platforms/$PLATFORM/"
    rm -rf /tmp/platform-34.zip /tmp/platform-extract
    say "android.jar installed"
else
    say "android.jar already present"
fi

# ─── 3. Build tools (aapt2, d8, zipalign, apksigner) ──────────────────────────
mkdir -p "$SDK_DIR/build-tools/$BUILD_TOOLS"
TOOLS=("aapt2" "d8" "zipalign" "apksigner")

download_build_tools() {
    say "Attempting alternative build-tools download..."
    # Build-tools are bundled inside the SDK zip hosted publicly
    local url="https://github.com/nicegraham/Android-Build-Tools/releases/download/v34.0.0/build-tools-34.0.0-linux.zip"
    curl -L -o /tmp/bt.zip "$url" && unzip -q /tmp/bt.zip -d /tmp/bt-extract/ && cp -r /tmp/bt-extract/build-tools/34.0.0/* "$SDK_DIR/build-tools/$BUILD_TOOLS/" && rm -rf /tmp/bt.zip /tmp/bt-extract && return 0
    return 1
}

for tool in "${TOOLS[@]}"; do
    if [[ ! -f "$SDK_DIR/build-tools/$BUILD_TOOLS/$tool" ]]; then
        if ! download_build_tools; then
            die "Failed to download build-tools. Install Android SDK build-tools 34.0.0\nmanually and set ANDROID_HOME env var."
        fi
        break
    fi
done

# Make binaries executable
chmod +x "$SDK_DIR/build-tools/$BUILD_TOOLS/"* 2>/dev/null || true

say "═══════════════════════════════════════════"
say "  All tools ready!"
say ""
say "  SDK:    $SDK_DIR"
say "  Kotlin: $KOTLIN_DIR"
say ""
say "  Build:  cd $SCRIPT_DIR && bash build.sh"
say "═══════════════════════════════════════════"

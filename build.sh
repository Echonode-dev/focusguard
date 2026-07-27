#!/bin/bash

# ============================================
# COLOR OUTPUT
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[*]${NC} $1"; }
print_success() { echo -e "${GREEN}[✓]${NC} $1"; }
print_error() { echo -e "${RED}[✗]${NC} $1"; }

# ============================================
# PATHS - USING SYSTEM TOOLS (ARM64 NATIVE)
# ============================================
SDK="/tmp/focusguard/android-sdk"
PLATFORM="$SDK/platforms/android-34"

# Use system tools (ARM64 compatible) - these are already installed
AAPT="aapt"                    # /usr/bin/aapt
ZIPALIGN="zipalign"            # /usr/bin/zipalign
APKSIGNER="apksigner"          # /usr/bin/apksigner
D8="$SDK/build-tools/34.0.0/d8"

# ============================================
# CHECK TOOLS
# ============================================
print_status "Checking build tools..."

# Check system tools
if ! command -v aapt &> /dev/null; then
    print_error "aapt not found. Install with: apt install -y aapt"
    exit 1
else
    print_success "aapt found: $(which aapt)"
fi

if ! command -v zipalign &> /dev/null; then
    print_error "zipalign not found. Install with: apt install -y zipalign"
    exit 1
else
    print_success "zipalign found: $(which zipalign)"
fi

if ! command -v apksigner &> /dev/null; then
    print_error "apksigner not found. Install with: apt install -y apksigner"
    exit 1
else
    print_success "apksigner found: $(which apksigner)"
fi

# Check d8 (must be executable)
if [ ! -f "$D8" ]; then
    print_error "d8 not found at $D8"
    exit 1
fi
chmod +x "$D8" 2>/dev/null
print_success "d8 found"

# Check Kotlin
if ! command -v kotlinc &> /dev/null; then
    print_error "kotlinc not found. Install with: pkg install kotlin"
    exit 1
fi
print_success "Kotlin found"

# ============================================
# CHECK SOURCE FILES
# ============================================
print_status "Checking source files..."

if [ ! -f "AndroidManifest.xml" ]; then
    print_error "AndroidManifest.xml not found!"
    exit 1
fi
print_success "AndroidManifest.xml found"

if [ ! -d "res" ]; then
    print_error "res/ folder not found!"
    exit 1
fi
print_success "res/ folder found"

KT_FILES=$(find src -name "*.kt" -type f 2>/dev/null)
if [ -z "$KT_FILES" ]; then
    print_error "No Kotlin files found in src/"
    exit 1
fi
print_success "Found Kotlin files:"
echo "$KT_FILES" | sed 's/^/  /'

# ============================================
# CLEAN
# ============================================
print_status "Cleaning previous build..."
rm -f app.unaligned.apk app.aligned.apk app.apk classes.dex
rm -rf obj/ classes/
mkdir -p obj classes
print_success "Clean complete"

# ============================================
# 1. COMPILE RESOURCES (aapt)
# ============================================
print_status "Compiling resources with aapt..."
$AAPT package -f -M AndroidManifest.xml -S res -I $PLATFORM/android.jar -F app.unaligned.apk

if [ $? -ne 0 ]; then
    print_error "AAPT compilation failed!"
    exit 1
fi
print_success "Resources compiled"

# ============================================
# 2. COMPILE KOTLIN (kotlinc)
# ============================================
print_status "Compiling Kotlin source..."
kotlinc -cp $PLATFORM/android.jar -d classes $KT_FILES

if [ $? -ne 0 ]; then
    print_error "Kotlin compilation failed!"
    exit 1
fi
print_success "Kotlin compiled"

# ============================================
# 3. CONVERT TO DEX (d8)
# ============================================
print_status "Converting to DEX with d8..."
find classes -name "*.class" > classlist.txt 2>/dev/null

if [ ! -s classlist.txt ]; then
    print_error "No .class files generated!"
    exit 1
fi

$D8 --lib $PLATFORM/android.jar --output . @classlist.txt
if [ $? -ne 0 ]; then
    print_error "D8 conversion failed!"
    rm -f classlist.txt
    exit 1
fi
rm -f classlist.txt

if [ ! -f "classes.dex" ]; then
    print_error "classes.dex not generated!"
    exit 1
fi
print_success "DEX created: $(ls -lh classes.dex | awk '{print $5}')"

# ============================================
# 4. ADD DEX TO APK (aapt)
# ============================================
print_status "Adding DEX to APK..."
$AAPT add app.unaligned.apk classes.dex
if [ $? -ne 0 ]; then
    print_error "Failed to add DEX to APK!"
    exit 1
fi
print_success "DEX added to APK"

# ============================================
# 5. ALIGN APK (zipalign)
# ============================================
print_status "Aligning APK with zipalign..."
$ZIPALIGN -v -p 4 app.unaligned.apk app.aligned.apk 2>&1 | grep -v "Unknown"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    print_error "Alignment failed!"
    exit 1
fi
print_success "APK aligned"

# ============================================
# 6. SIGN APK (apksigner)
# ============================================
print_status "Signing APK..."

KEYSTORE="$HOME/.android/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    print_status "Generating debug keystore..."
    keytool -genkey -v -keystore "$KEYSTORE" \
        -alias androiddebugkey \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass android \
        -keypass android \
        -dname "CN=Android Debug, O=Android, C=US" \
        2>&1 | grep -v "Generating"
    
    if [ $? -ne 0 ]; then
        print_error "Keystore generation failed!"
        exit 1
    fi
    print_success "Keystore generated"
fi

$APKSIGNER sign --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out app.apk app.aligned.apk 2>&1 | grep -v "WARNING"

if [ $? -ne 0 ]; then
    print_error "Signing failed!"
    exit 1
fi
print_success "APK signed"

# ============================================
# 7. FINISH
# ============================================
print_success "✅ BUILD COMPLETE!"
print_success "APK: app.apk ($(ls -lh app.apk | awk '{print $5}'))"

rm -f app.unaligned.apk app.aligned.apk

echo ""
print_success "APK is ready: $(pwd)/app.apk"

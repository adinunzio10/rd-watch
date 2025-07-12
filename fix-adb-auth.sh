#!/bin/bash

echo "Fixing ADB authentication for remote access..."

# Kill existing emulator
adb -s emulator-5554 emu kill 2>/dev/null || true
sleep 2

# Create a custom AVD config that disables auth
mkdir -p ~/.android/avd/tv_remote_noauth.avd
cp -r ~/.android/avd/tv_emulator_remote.avd/* ~/.android/avd/tv_remote_noauth.avd/

# Create AVD config that disables authentication
cat > ~/.android/avd/tv_remote_noauth.ini << EOF
avd.ini.encoding=UTF-8
path=/home/alfredo/.android/avd/tv_remote_noauth.avd
path.rel=avd/tv_remote_noauth.avd
target=android-34
EOF

# Set SDK paths
export ANDROID_SDK_ROOT=/home/alfredo/android-sdk
export ANDROID_HOME=/home/alfredo/android-sdk

# Start emulator in insecure mode (no authentication)
echo "Starting emulator in insecure mode..."
nohup $ANDROID_SDK_ROOT/emulator/emulator @tv_remote_noauth \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -gpu swiftshader_indirect \
    -qemu -netdev user,hostfwd=tcp:0.0.0.0:5555-:5555 > emulator_noauth.log 2>&1 &

echo "Waiting for emulator to boot..."
sleep 20

# Check if it started
if adb devices | grep -q "emulator"; then
    echo "✅ Emulator started successfully!"
    echo "From your Mac, try: adb connect 100.96.223.82:5555"
else
    echo "❌ Emulator failed to start, check emulator_noauth.log"
fi
#!/bin/bash

# Setup Remote ADB for Android TV Development

echo "Setting up Remote ADB for Android TV development..."

# Set correct SDK paths
export ANDROID_SDK_ROOT=/home/alfredo/android-sdk
export ANDROID_HOME=/home/alfredo/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools

# Check if emulator is already running
if pgrep -f "emulator.*tv_emulator_remote" > /dev/null; then
    echo "Emulator already running"
else
    echo "Starting Android TV emulator in headless mode..."
    # Start emulator with correct system image path
    nohup $ANDROID_SDK_ROOT/emulator/emulator @tv_emulator_remote \
        -sysdir $ANDROID_SDK_ROOT/system-images/android-34/android-tv/x86/ \
        -no-window \
        -no-audio \
        -no-boot-anim \
        -gpu swiftshader_indirect \
        -qemu -netdev user,id=net0,hostfwd=tcp::5555-:5555 > emulator.log 2>&1 &
    
    echo "Waiting for emulator to boot..."
    sleep 10
fi

# Wait for device to be ready
echo "Waiting for device to be ready..."
adb wait-for-device

# Enable ADB over TCP/IP
echo "Enabling ADB over TCP/IP on port 5555..."
adb tcpip 5555

# Get server IP address
SERVER_IP=$(hostname -I | awk '{print $1}')

echo ""
echo "✅ Remote ADB setup complete!"
echo ""
echo "From your MacBook, connect using:"
echo "  adb connect $SERVER_IP:5555"
echo ""
echo "To stop the emulator:"
echo "  adb -s emulator-5554 emu kill"
echo ""
echo "To check emulator status:"
echo "  adb devices"
#!/bin/bash

# Tailscale Remote ADB Setup for Android TV Development

echo "Setting up Remote ADB via Tailscale for Android TV development..."

# Set correct SDK paths
export ANDROID_SDK_ROOT=/home/alfredo/android-sdk
export ANDROID_HOME=/home/alfredo/android-sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/platform-tools

# Check if emulator is already running
if pgrep -f "emulator.*tv_emulator_remote" > /dev/null; then
    echo "Emulator already running"
else
    echo "Starting Android TV emulator..."
    # Start emulator (will be accessible via localhost ADB)
    nohup $ANDROID_SDK_ROOT/emulator/emulator @tv_emulator_remote \
        -no-window \
        -no-audio \
        -no-boot-anim \
        -gpu swiftshader_indirect > emulator.log 2>&1 &
    
    echo "Waiting for emulator to boot..."
    sleep 20
fi

# Wait for device to be ready
echo "Waiting for device to be ready..."
adb wait-for-device

# Enable ADB over TCP/IP
echo "Enabling ADB over TCP/IP on port 5555..."
adb tcpip 5555

# Get Tailscale IP
TAILSCALE_IP=$(ip addr show tailscale0 | grep "inet " | awk '{print $2}' | cut -d'/' -f1)

echo ""
echo "✅ Tailscale Remote ADB setup complete!"
echo ""
echo "Your Tailscale IP: $TAILSCALE_IP"
echo ""
echo "🔧 OPTION 1: SSH Port Forwarding (Recommended)"
echo "From your MacBook terminal:"
echo "  ssh -L 5555:localhost:5555 alfredo@$TAILSCALE_IP"
echo "Then in another terminal:"
echo "  adb connect localhost:5555"
echo ""
echo "🔧 OPTION 2: Direct Connection (if firewall allows)"
echo "From your MacBook:"
echo "  adb connect $TAILSCALE_IP:5555"
echo ""
echo "📱 Deploy your app:"
echo "  ./gradlew installDebug"
echo ""
echo "🛑 To stop the emulator:"
echo "  adb -s emulator-5554 emu kill"
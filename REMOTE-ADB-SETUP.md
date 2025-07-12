# Remote ADB Setup for Android TV Development

## Quick Start

1. **On this Linux server**, run:
   ```bash
   ./setup-remote-adb.sh
   ```

2. **On your MacBook**, connect to the emulator:
   ```bash
   adb connect <SERVER_IP>:5555
   ```
   Replace `<SERVER_IP>` with the IP shown by the script.

3. **Deploy your Android TV app**:
   ```bash
   # From your MacBook, in the project directory
   ./gradlew installDebug
   ```

## Manual Setup (if script fails)

1. Start the emulator manually:
   ```bash
   export ANDROID_SDK_ROOT=/home/alfredo/android-sdk
   $ANDROID_SDK_ROOT/emulator/emulator @tv_emulator_remote -no-window -no-audio &
   ```

2. Enable TCP/IP mode:
   ```bash
   adb tcpip 5555
   ```

3. Find server IP:
   ```bash
   hostname -I
   ```

## Firewall Configuration

If connection fails, ensure port 5555 is open:
```bash
# Check firewall status
sudo ufw status

# If needed, allow port 5555
sudo ufw allow 5555/tcp
```

## Troubleshooting

- **Connection refused**: Check if emulator is running (`adb devices`)
- **Device offline**: Restart adb (`adb kill-server && adb start-server`)
- **Slow performance**: Use `-gpu swiftshader_indirect` for better headless performance

## Security Note

For development only! In production, use SSH tunneling:
```bash
# From MacBook
ssh -L 5555:localhost:5555 user@server
adb connect localhost:5555
```
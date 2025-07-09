# Authentication Issues Tracking Document

## Overview
This document tracks ongoing authentication issues with the RD Watch Android TV application, specifically problems with OAuth2/Device Code authentication and API Key authentication.

## Current Issues

### Issue 1: QR Code/Device Code Not Showing
**Status**: Partially resolved, but core network issue remains
**Symptoms**: 
- Clicking OAuth2 authentication button shows loading screen indefinitely
- API call to Real-Debrid times out after 15 seconds (previously 30 seconds)
- QR code never appears

**Root Cause**: Network connectivity issue between Android app and Real-Debrid API
- API endpoint works fine via curl: `https://api.real-debrid.com/oauth/v2/device/code`
- Returns proper device code response when tested externally
- App makes the request but never receives a response

### Issue 2: Home Screen Loading After Authentication
**Status**: Resolved
**Symptoms**: 
- After successful API key authentication, home screen shows loading indefinitely
- Sometimes authentication succeeds but navigation gets stuck

**Solution Applied**: 
- Wrapped all protected screens with AuthGuard component
- Added 500ms delay after authentication success for state propagation
- Added 100ms delay after API key save

## Fixes Implemented

### 1. Timeout and Error Handling (AuthRepository.kt)
```kotlin
- Added timeout wrapper with withTimeout(API_CALL_TIMEOUT_MS)
- Reduced timeout from 30s to 15s for better UX
- Enhanced error messages for different failure scenarios
- Added detailed logging for debugging
```

### 2. AuthGuard Implementation (AppNavigation.kt)
```kotlin
- Wrapped TVHomeScreen, BrowseScreen, SearchScreen, ProfileScreen with AuthGuard
- Proper authentication redirect handling
- Shows loading indicator during auth state initialization
```

### 3. Navigation Delay (MainActivity.kt)
```kotlin
- Added 500ms delay after authentication success before navigation
- Ensures auth state is fully propagated before screen transition
```

### 4. Retry Mechanism (AuthViewModel.kt)
```kotlin
- Added retry logic for transient network errors
- Up to 3 retry attempts with 2-second delays
- Only retries on network-related errors
```

### 5. IPv4 DNS Resolution (IPv4DnsResolver.kt)
```kotlin
- Custom DNS resolver that prioritizes IPv4 addresses
- Helps avoid IPv6 connectivity issues on some Android TV devices
- Applied to PublicClient in NetworkModule
```

### 6. Network Connectivity Check (NetworkUtils.kt)
```kotlin
- Checks network availability before API calls
- Provides network type information for debugging
- Added ACCESS_NETWORK_STATE permission
```

## Diagnostic Information

### Working curl command:
```bash
curl -v -X GET "https://api.real-debrid.com/oauth/v2/device/code?client_id=X245A4XAIBGVM&new_credentials=yes" -H "Accept: application/json"
```

### API Response (from curl):
```json
{
    "device_code": "CW54XSUHGRKT42TJVMRWLB6LTZK6U7AJO5GMCFWAIYXB5BFEVIMA",
    "user_code": "RE6WDL3D",
    "interval": 5,
    "expires_in": 900,
    "verification_url": "https://real-debrid.com/device",
    "direct_verification_url": "https://real-debrid.com/authorize?client_id=X245A4XAIBGVM&device_id=CW54XSUHGRKT42TJVMRWLB6LTZK6U7AJO5GMCFWAIYXB5BFEVIMA"
}
```

### Logs showing the issue:
```
2025-07-07 09:48:40.091 AuthRepository    D  Starting device flow...
2025-07-07 09:48:40.091 AuthRepository    D  Calling getDeviceCode API with CLIENT_ID: X245A4XAIBGVM
2025-07-07 09:48:40.102 okhttp.OkHttpClient I  --> GET https://api.real-debrid.com/oauth/v2/device/code?client_id=X245A4XAIBGVM&new_credentials=yes
2025-07-07 09:49:10.093 AuthRepository    E  Device flow API call timed out after 30000ms
```

## Potential Causes

1. **IPv6 Connectivity Issues**
   - API resolves to both IPv4 and IPv6 addresses
   - Android might be trying IPv6 first and failing
   - Implemented IPv4-only DNS resolver as workaround

2. **Android TV/Emulator Network Configuration**
   - Firewall or proxy blocking the connection
   - VPN interference
   - Emulator-specific network issues

3. **SSL/TLS Issues**
   - Certificate validation problems
   - TLS version mismatch

## Next Steps to Try

1. **Test on Physical Device**
   - Rule out emulator-specific issues
   - Check if problem persists on real hardware

2. **Add Network Interceptor for Debugging**
   - Log full request/response details
   - Check SSL handshake completion

3. **Try Different HTTP Client Configuration**
   - Disable HTTP/2
   - Force specific TLS version
   - Add custom SSL socket factory

4. **Alternative Approach**
   - Consider WebView-based authentication as fallback
   - Implement manual code entry option

## Related Files

- `/app/src/main/java/com/rdwatch/androidtv/auth/AuthRepository.kt` - Main authentication logic
- `/app/src/main/java/com/rdwatch/androidtv/auth/ui/AuthViewModel.kt` - ViewModel with retry logic
- `/app/src/main/java/com/rdwatch/androidtv/auth/ui/AuthenticationScreen.kt` - UI components
- `/app/src/main/java/com/rdwatch/androidtv/di/NetworkModule.kt` - Network configuration
- `/app/src/main/java/com/rdwatch/androidtv/network/interceptors/IPv4DnsResolver.kt` - IPv4 DNS resolver
- `/app/src/main/java/com/rdwatch/androidtv/util/NetworkUtils.kt` - Network connectivity checks
- `/app/src/main/java/com/rdwatch/androidtv/presentation/navigation/AppNavigation.kt` - Navigation with AuthGuard

## API Configuration

- Client ID: `X245A4XAIBGVM` (Real Debrid client ID for open source apps)
- OAuth2 Base URL: `https://api.real-debrid.com/`
- Device Code Endpoint: `/oauth/v2/device/code`
- Polling Interval: 5 seconds
- Code Expiry: 900 seconds (15 minutes)

## Current State

The authentication flow is properly implemented with:
- Proper error handling and retry logic
- Network connectivity checks
- IPv4 DNS resolution
- Reduced timeouts for better UX
- AuthGuard protecting all authenticated screens

However, the core issue remains: the Android app cannot reach the Real-Debrid OAuth2 API endpoint, even though the endpoint is accessible via curl from the same machine.

## Testing Checklist

- [ ] Test on physical Android TV device
- [ ] Test with VPN disabled
- [ ] Test on different network (WiFi vs Ethernet)
- [ ] Check Android TV device date/time settings
- [ ] Verify no proxy settings in Android
- [ ] Test with mobile hotspot
- [ ] Check for any custom firewall rules
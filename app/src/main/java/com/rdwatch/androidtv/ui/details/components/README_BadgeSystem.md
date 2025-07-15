# Quality Badge System Implementation

## Overview
This document describes the quality badge system implementation for the Advanced Source Selection UI in the RD Watch Android TV app.

## Created/Modified Files

### 1. **QualityBadge.kt** (Enhanced)
- Enhanced color functions for better TV visibility
- Added `getEnhancedBadgeColor()` function with granular color coding:
  - Resolution badges: 8K (red), 4K (purple), 1080p (blue), 720p (green)
  - HDR badges: Dolby Vision (black), HDR10+ (dark blue), HDR10 (blue)
  - Codec badges: AV1 (emerald), HEVC (green), H.264 (light green)
  - Audio badges: Atmos (near black), DTS:X (dark gray), TrueHD (dark orange)
  - Release badges: REMUX (violet), BluRay (blue), WEB-DL (cyan)
  - Health indicators: Color-coded based on seeder count
- Improved TV-optimized sizing with larger text and padding
- Focus state support for D-pad navigation

### 2. **SourceBadgeContainer.kt** (New)
Comprehensive badge container components:
- `SourceBadgeContainer`: Main container for displaying source metadata badges
- `PrimaryBadgeRow`: Displays main quality indicators (resolution, HDR, codec, audio)
- `ProviderBadge`: Shows source provider with reliability color coding
- `FileSizeBadge`: Displays file size in human-readable format
- `HealthIndicatorBadge`: Shows seeders/leechers for P2P sources
- `SpecialFeatureBadge`: Highlights special features like CACHED or PACK
- `CompactBadgeRow`: Condensed view for list items
- `BadgeGrid`: Grid layout for detailed views

### 3. **BadgeStyles.kt** (New)
Centralized styling configuration:
- Color palette optimized for TV viewing distance
- Sizing configurations (small, medium, large) with TV-appropriate dimensions
- Animation durations for smooth transitions
- Focus state configurations with blue focus ring
- Helper functions for dynamic color selection
- Preset badge combinations for common scenarios

### 4. **SourceListItem.kt** (New)
Source list item components:
- `SourceListItem`: Compact list item with badges and metadata
- `ExpandedSourceListItem`: Detailed view with full metadata
- `ProviderIcon`: Visual representation of source provider
- `HealthStatusText`: P2P health indicator
- `QualityScoreIndicator`: Visual quality score (A+, A, B+, etc.)
- TV-optimized with focus states and D-pad navigation

## Design Decisions

### Color Coding Strategy
- **Resolution**: Gradient from red (8K) to gray (SD) for quick quality recognition
- **HDR**: Blue tones with Dolby Vision in black for premium feel
- **Codec**: Green shades indicating efficiency (darker = more efficient)
- **Audio**: Orange/yellow tones with premium formats (Atmos/DTS:X) in dark colors
- **Release**: Purple/blue gradient based on quality
- **Health**: Green to red based on P2P health status

### TV Optimization
- **Minimum font size**: 12sp for small badges (readable from 10 feet)
- **High contrast**: White text on colored backgrounds
- **Focus states**: 2dp blue border with slight elevation
- **Spacing**: Generous padding between badges (8dp minimum)
- **Corner radius**: Rounded corners (6-10dp) for modern TV UI

### Badge Priority System
Badges are sorted by priority:
1. Resolution (priority: 100)
2. HDR (priority: 93-95)
3. Codec (priority: 80)
4. Audio (priority: 68-70)
5. Release type (priority: 60)
6. Health status (priority: 50)

## Integration with Source Metadata

The badge system integrates seamlessly with the `SourceMetadata` model:
- `getQualityBadges()` method generates appropriate badges
- Automatic badge selection based on source properties
- Dynamic text generation (e.g., "150S" for seeders)
- Support for all metadata types defined in the data model

## Accessibility Features
- Focus navigation support for D-pad control
- High contrast colors for visibility
- Clear text labels (no icon-only badges)
- Consistent sizing for predictable navigation

## Usage Examples

```kotlin
// Display badges for a source
SourceBadgeContainer(
    sourceMetadata = sourceMetadata,
    badgeSize = QualityBadgeSize.MEDIUM,
    showProvider = true,
    showFileSize = true
)

// Compact badge row for list items
CompactBadgeRow(
    sourceMetadata = sourceMetadata,
    maxBadges = 4
)

// Individual badge
AdvancedQualityBadgeComponent(
    badge = QualityBadge("4K", QualityBadge.Type.RESOLUTION, 100),
    size = QualityBadgeSize.LARGE,
    focusable = true
)
```

## Future Enhancements
- Animated badge transitions
- Custom badge icons
- User preference for badge display
- Badge tooltips with detailed information
- Batch badge selection mode
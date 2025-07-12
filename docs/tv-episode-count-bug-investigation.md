# TV Episode Count Bug Investigation

## Issue Description
When selecting a TV show season, the episode count changes from the correct number (e.g., "13 episodes") to "0 episodes" in the UI.

**Reproduction Steps:**
1. Navigate to TV show "Chuck" 
2. Initially shows: Specials + 5 seasons with correct episode counts
3. Click into "Season 1" 
4. Episode count changes from "13 episodes" to "0 episodes"

## Investigation History

### Session 1: Initial Analysis (2025-07-12)

**Initial Hypothesis:** Episode count was being overridden with `episodes.size` during loading.

**Fix Attempted:** Changed `episodeCount = episodes.size` to `episodeCount = seasonResponse.episodeCount` in `mapTMDbSeasonResponseToTVSeason()` (TVDetailsViewModel.kt:854)

**Result:** ❌ Did not work

### Session 2: Deep Dive Analysis (2025-07-12)

**Key Findings from Logs:**
```
Season Selection Debug:
- Selecting season 1: Season 1
- Episodes loaded: 0
- Episode count claimed: 13
- Episodes have valid data: false
- Season 1 needs on-demand loading: no episodes loaded but episodeCount=13

API Response:
- TMDb API returned valid data with 13 episodes
- "API RESULT: Saving season 1 for TV 1404 with 13 episodes"
- "Loaded cached season 1 for TV 1404: episodes=0"
- "Season 1 response was invalid (id=3650, episodes=0)"
```

**Root Cause Identified:** 
The issue is in the **validation logic**, not the episode count assignment. The database entity for TV shows stores seasons with metadata but `episodes = emptyList()` by design (see TMDbEntityMappers.kt:575).

The validation check `seasonResponse.episodes.isNotEmpty()` fails because:
1. TV show entities store season metadata with `episodeCount` but `episodes = emptyList()` by design
2. Episodes are loaded separately via season details API
3. During loading, cached season has valid `episodeCount` but empty `episodes` list
4. Validation fails and season is marked "invalid"

**Fix Attempted:** Updated validation logic in TVDetailsViewModel.kt:
```kotlin
// Before:
if (seasonResponse.id != 0 && seasonResponse.episodes.isNotEmpty()) {

// After: 
if (seasonResponse.id != 0 && (seasonResponse.episodes.isNotEmpty() || seasonResponse.episodeCount > 0)) {
```

**Locations Updated:**
- Line 532 (initial season loading)
- Line 687 (on-demand season loading)

**Result:** ❌ Still did not work

### Session 3: Comprehensive Fix Attempt (2025-07-12)

**New Root Cause Theory:** 
State synchronization issue - the selected season object becomes stale when episode data is loaded asynchronously.

**Comprehensive Fix Applied:**
1. **Enhanced Debug Logging:**
   - Added logging in `TMDbEntityMappers.toTVResponse()` to track season parsing
   - Added logging in `SeasonSelector` component to track UI data
   - Added logging in `selectSeason()` method to track season state
   - Added logging for season updates with episode data

2. **Critical State Synchronization Fix:**
   - Added logic to update `_selectedSeason.value` when episode data is loaded
   - This ensures the UI gets the updated season object with correct episode count

3. **Database Mapping Improvements:**
   - Added warning logs for fallback season mappings
   - Enhanced tracking of episode count preservation

**Key Fix Location:**
`TVDetailsViewModel.kt:719-724` - Updates selected season state when episode data loads asynchronously

**Expected Result:** ✅ Should now work - selected season will be updated with loaded episode data

## Current Status
- **Commits:** 2 fixes attempted and pushed to `feature/scraper-source-selection` branch
- **Status:** Issue persists, need deeper investigation

## Next Investigation Steps

### Areas to Investigate:

1. **UI Layer Issue:** 
   - Check if `getFormattedEpisodeCount()` in TVSeason model is using the right data source
   - Verify SeasonSelector component is displaying the correct field

2. **State Management:**
   - Investigate season state updates during selection
   - Check if there's a race condition between UI updates and data loading

3. **Database Layer:**
   - Verify if episodes are actually being saved to database correctly
   - Check if there's a mismatch between what's saved and what's retrieved

4. **Repository Layer:**
   - Investigate the NetworkBoundResource flow
   - Check if cached data is being properly updated after API fetch

### Debugging Approach:

1. **Add more detailed logging** to track data flow:
   - Log `episodeCount` vs `episodes.size` at each step
   - Log the actual season data being passed to UI
   - Log SeasonSelector data source

2. **Check UI component data binding:**
   - Verify what data `getFormattedEpisodeCount()` actually uses
   - Check if UI is recomposing with updated data

3. **Verify database operations:**
   - Check if season episodes are properly saved after API fetch
   - Verify database query results

## Code Locations

### Key Files:
- `TVDetailsViewModel.kt` - Season loading and state management
- `SeasonSelector.kt` - UI component displaying episode counts
- `TVShowModels.kt` - `getFormattedEpisodeCount()` method (line 127-129)
- `TMDbEntityMappers.kt` - Database entity mapping (line 575: `episodes = emptyList()`)
- `TMDbTVRepositoryImpl.kt` - Repository layer with NetworkBoundResource

### Critical Methods:
- `TVDetailsViewModel.selectSeason()` - Handles season selection
- `TVDetailsViewModel.loadSeasonOnDemand()` - Loads season details
- `TVSeason.getFormattedEpisodeCount()` - Returns formatted episode count string
- `mapTMDbSeasonResponseToTVSeason()` - Maps API response to UI model

## Test Case
**TV Show:** Chuck (TMDb ID: 1404)
**Season:** Season 1
**Expected:** "13 episodes"
**Actual:** "0 episodes" after selection

## Logs Reference
See investigation logs from 2025-07-12 01:54:42 for detailed API response and state changes.

---
*Document created: 2025-07-12*
*Last updated: 2025-07-12*
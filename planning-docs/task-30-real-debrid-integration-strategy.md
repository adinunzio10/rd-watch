# Task #30: Replace Mock Data with Real-Debrid API Integration

## Strategy Overview

**Complexity Score: 7** - Full swarm collaboration with 4+ specialized agents working in parallel.

## Risk Assessment

### Security Risks
- **API Key Exposure**: Ensure auth tokens are properly secured via AuthManager
- **Data Validation**: Validate all API responses before database insertion
- **Rate Limiting**: Implement proper backoff strategies for API calls

### Breaking Changes
- ViewModels will emit different data types (Flow instead of static lists)
- UI components need to handle loading/error states
- Database schema might need updates for RD-specific fields

### Performance Concerns
- Large torrent/download lists need pagination
- Background sync must not block UI thread
- Image caching for content posters/backdrops
- Efficient data transformation from API to entities

## Agent Assignments

### Agent 1: Backend/API Integration Specialist
**Focus**: Create Real-Debrid repository and data transformation layer

**Tasks**:
1. Create `RealDebridContentRepository` interface and implementation
2. Implement API data fetchers:
   - Fetch torrents list with pagination
   - Fetch downloads list
   - Get torrent info and files
3. Create data mappers:
   - RD Torrent → ContentEntity
   - RD Download → ContentEntity
   - Handle file metadata extraction
4. Implement caching strategy with Room

**Files to create/modify**:
- `/repository/RealDebridContentRepository.kt`
- `/repository/RealDebridContentRepositoryImpl.kt`
- `/data/mappers/RealDebridMappers.kt`
- `/data/entities/ContentEntity.kt` (extend for RD fields)

### Agent 2: Database & Persistence Specialist
**Focus**: Update Room database schema and DAOs for RD content

**Tasks**:
1. Extend or create new entities:
   - Add RD-specific fields to ContentEntity
   - Create TorrentEntity for torrent metadata
   - Add DownloadEntity for download tracking
2. Update DAOs:
   - ContentDao with upsert operations
   - Queries for filtering by source (local/RD)
3. Create database migrations if needed
4. Implement sync status tracking

**Files to create/modify**:
- `/data/entities/TorrentEntity.kt`
- `/data/entities/DownloadEntity.kt`
- `/data/dao/ContentDao.kt`
- `/data/AppDatabase.kt` (add new entities/version)
- `/data/DatabaseMigrations.kt`

### Agent 3: ViewModel & UI State Specialist
**Focus**: Update ViewModels to use repositories instead of mock data

**Tasks**:
1. Update BrowseViewModel:
   - Replace MovieList.list with repository flows
   - Add loading/error states
   - Implement refresh mechanism
2. Update MovieDetailsViewModel:
   - Use repository for movie lookup
   - Add RD-specific actions (unrestrict, download)
3. Create HomeViewModel updates:
   - Show mixed content (local + RD)
   - Add content filtering options
4. Implement proper error handling

**Files to modify**:
- `/ui/browse/BrowseViewModel.kt`
- `/ui/details/MovieDetailsViewModel.kt`
- `/ui/home/HomeViewModel.kt`
- `/ui/common/UiState.kt` (create if needed)

### Agent 4: Background Sync & Performance Specialist
**Focus**: Implement background synchronization and performance optimizations

**Tasks**:
1. Create WorkManager sync worker:
   - Periodic sync of RD library
   - Handle network failures gracefully
2. Implement pagination for large datasets:
   - Torrent list pagination
   - Downloads pagination
3. Add data refresh mechanisms:
   - Pull-to-refresh support
   - Manual sync triggers
4. Optimize data loading:
   - Implement proper Flow operators
   - Add memory caching layer

**Files to create/modify**:
- `/workers/RealDebridSyncWorker.kt`
- `/repository/PaginationHelper.kt`
- `/di/WorkManagerModule.kt`
- Update repository implementations for pagination

## Integration Coordinator Tasks
After individual agents complete their work:

1. **Dependency Injection Updates**:
   - Wire new repositories in Hilt modules
   - Update ViewModel providers

2. **Testing Integration**:
   - Create repository tests
   - Add ViewModel tests with mock repositories
   - Integration tests for data flow

3. **UI Polish**:
   - Add loading indicators
   - Error state UI components
   - Empty state handling

## Implementation Order

### Phase 1: Foundation (Agents 1 & 2 in parallel)
- Create data models and mappers
- Set up database schema
- Implement basic repository structure

### Phase 2: Integration (Agent 3)
- Update ViewModels to use repositories
- Handle state management
- Connect to existing UI

### Phase 3: Optimization (Agent 4)
- Add background sync
- Implement pagination
- Performance tuning

### Phase 4: Polish (All agents)
- Error handling refinement
- UI state improvements
- Testing and bug fixes

## Success Criteria

1. All MovieList.MOCK_DATA references removed
2. Real-Debrid content displays in UI
3. Data persists in Room database
4. Background sync works reliably
5. Performance acceptable for large libraries
6. Proper error handling throughout
7. All existing tests updated and passing

## Subtask Mapping

- **30.1**: Repository Updates → Agent 1
- **30.2**: API Data Transformation → Agent 1
- **30.3**: Room Database Population → Agent 2
- **30.4**: ViewModel Modifications → Agent 3
- **30.5**: Refresh Mechanisms → Agent 3
- **30.6**: Error Handling → All agents
- **30.7**: Pagination Implementation → Agent 4
- **30.8**: Background Sync Setup → Agent 4
- **30.9**: Performance Optimization → Agent 4
- **30.10**: Testing Integration → Coordinator
- **30.11**: Documentation and Cleanup → All agents

## Notes

- Maintain backwards compatibility where possible
- Use coroutines and Flow for all async operations
- Follow MVVM architecture patterns strictly
- Ensure TV focus navigation remains functional
- Test on actual Android TV device/emulator
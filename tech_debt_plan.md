# Technical Debt Review Tracker - RD Watch Android TV

## Introduction

This document is designed to systematically track and manage the review of technical debt within the RD Watch Android TV application codebase. The goal is to ensure a consistent review process across different modules and components, focusing on common issues, especially those that can arise from rapid development or AI-assisted coding.

**How to Use This Document:**

1.  **Select a Codebase Area:** Choose an area from the list below that has not yet been thoroughly reviewed.
2.  **Fill in Reviewer Info:** Add your name/handle and the date you are starting the review for that section.
3.  **Understand the Scope:** Read the description and key files for the area.
4.  **Targeted Review:** Consider the "Specific Review Focus Points" if any are listed.
5.  **Checklist Pass:** Go through the "Common Tech Debt Checklist" and mark items generally observed in the area.
6.  **Log Specific Findings:** For each concrete instance of tech debt, add a row to the "Findings & Action Items" table. Be specific about the file, line numbers, and the issue.
7.  **Add General Notes:** Use the "Reviewer General Notes & Observations" section for broader comments.
8.  **Prioritize & Assign (Optional):** As a team, review the findings to prioritize and assign tasks for remediation.

**Tech Debt Categories (for "Findings" table and checklist):**

*   Inconsistent Naming
*   Lack/Meaningless Comments
*   Overly Complex Code
*   Duplicated Code (DRY)
*   Magic Numbers/Strings
*   Dead Code
*   Insufficient Error Handling
*   Tight Coupling/Low Modularity
*   Inconsistent Formatting
*   Suboptimal Algorithms
*   Missing/Inadequate Tests
*   Global State Overuse
*   Cargo Cult Programming
*   Inconsistent APIs
*   Unresolved TODOs/FIXMEs

---

## Codebase Areas for Review

1.  Core Application Setup & Entry Points
2.  Authentication System (`auth/`)
3.  Networking Layer (`network/`)
4.  Data Layer & Persistence (`data/`)
5.  Repositories (General) (`repository/`, `data/repository/`)
6.  Dependency Injection (`di/`)
7.  Account File Browser System (`ui/filebrowser/` and related)
8.  Player System (`player/`)
9.  Scraper System (`scraper/`)
10. General UI & Presentation (presentation/, ui/ - core parts)
11. Core Utilities & Reactive Programming (`core/`)
12. Background Workers (`workers/`)
13. Legacy Code & Transition Areas
14. Build & Configuration

---

## Area 1: Core Application Setup & Entry Points

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Covers the main application entry points (`MainActivity.kt`, `RdWatchApplication.kt`), UI theming (`ui/theme/`), and overall application lifecycle and global configurations.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/MainActivity.kt`
    *   `app/src/main/java/com/rdwatch/androidtv/RdWatchApplication.kt`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/theme/`

### 2. Specific Review Focus Points for This Area

*   Clarity of application initialization sequence.
*   Correctness of theme application and TV-optimized typography.
*   Handling of global states or services initialized at startup.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 2: Authentication System (`auth/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Manages user authentication, token storage, and related UI. Includes `AuthManager.kt`, `AuthRepository.kt`, `TokenStorage.kt`, `auth/models/`, and `auth/ui/`.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/auth/`
    *   `app/src/main/java/com/rdwatch/androidtv/auth/ui/`
    *   `app/src/main/java/com/rdwatch/androidtv/auth/models/`

### 2. Specific Review Focus Points for This Area

*   Security of token handling and storage.
*   Correctness of OAuth flow implementation (if applicable).
*   Clarity of authentication state management.
*   Error handling for authentication failures.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 3: Networking Layer (`network/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope</h3>

*   Handles all external API communications. Includes Retrofit setup, OkHttp client, API service definitions (`RealDebridApiService`, `OAuth2ApiService`), interceptors, and network data models.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/network/`
    *   `app/src/main/java/com/rdwatch/androidtv/network/api/`
    *   `app/src/main/java/com/rdwatch/androidtv/network/interceptors/`
    *   `app/src/main/java/com/rdwatch/androidtv/network/models/`
    *   `app/src/main/java/com/rdwatch/androidtv/network/adapters/`

### 2. Specific Review Focus Points for This Area

*   Clarity and consistency of API service definitions.
*   Effectiveness of interceptors (e.g., auth, logging, error handling).
*   Proper use of data models for requests/responses.
*   Resilience to network errors and retry mechanisms.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 4: Data Layer & Persistence (`data/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Manages local data storage using Room database and SharedPreferences. Includes `AppDatabase.kt`, DAOs, entities, DB mappers, migrations, and preferences handling.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/data/`
    *   `app/src/main/java/com/rdwatch/androidtv/data/dao/`
    *   `app/src/main/java/com/rdwatch/androidtv/data/entities/`
    *   `app/src/main/java/com/rdwatch/androidtv/data/mappers/` (DB specific mappers)
    *   `app/src/main/java/com/rdwatch/androidtv/data/migrations/`
    *   `app/src/main/java/com/rdwatch/androidtv/data/preferences/`

### 2. Specific Review Focus Points for This Area

*   Efficiency and correctness of DAO queries.
*   Normalization and design of database entities.
*   Handling of database migrations.
*   Appropriate use of SharedPreferences vs. Database.
*   Thread safety for database operations.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests** (especially for migrations and DAOs)
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 5: Repositories (General) (`repository/`, `data/repository/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Covers data abstraction layers that coordinate between different data sources (network, database, cache). Includes general repositories like `MovieRepository`, `UserRepository`, and base repository patterns (`BaseRepository`, `Result` wrapper, `NetworkBoundResource`).
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/repository/`
    *   `app/src/main/java/com/rdwatch/androidtv/data/repository/` (distinguish from FileBrowser specific repo if needed)
    *   `app/src/main/java/com/rdwatch/androidtv/repository/base/`

### 2. Specific Review Focus Points for This Area

*   Clear separation of concerns between repositories and ViewModels/UseCases.
*   Consistent use of `Result` wrapper or similar error handling patterns.
*   Effectiveness of data source coordination (e.g., cache-then-network).
*   Correct implementation of reactive data streams (`Flow`).

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 6: Dependency Injection (`di/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Manages Hilt/Dagger dependency injection setup. Includes all DI modules, qualifiers, and scopes.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/di/`
    *   `app/src/main/java/com/rdwatch/androidtv/di/qualifiers/`
    *   `app/src/main/java/com/rdwatch/androidtv/di/scopes/`

### 2. Specific Review Focus Points for This Area

*   Correctness and clarity of module definitions.
*   Appropriate use of scopes (e.g., `@Singleton`, `@ActivityScoped`).
*   Avoidance of common DI pitfalls (e.g., providing concrete classes where interfaces are better).
*   Readability and organization of DI modules.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods** (in provider methods)
*   [ ] **Duplicated Code (DRY Violations)** (in provider methods)
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code** (unused providers)
*   [ ] **Insufficient Error Handling** (less applicable, but check for misconfigurations)
*   [ ] **Lack of Modularity/Tight Coupling** (modules too large or interdependent)
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests** (DI graph validation if possible)
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 7: Account File Browser System (`ui/filebrowser/` and related)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   The complete MVVM system for browsing account files, including UI (Compose for TV), ViewModel, repository, caching, and navigation specific to this feature.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/AccountFileBrowserScreen.kt`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/AccountFileBrowserViewModel.kt`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/components/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/models/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/repository/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/cache/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/mappers/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/filebrowser/navigation/`

### 2. Specific Review Focus Points for This Area

*   Correctness of MVVM implementation and data flow (Screen -> VM -> Repo -> VM -> Screen).
*   Efficiency and usability of TV-optimized Compose components.
*   State management complexity in ViewModel (`FileBrowserState`, `FileBrowserEvents`).
*   Performance of file list rendering, pagination, filtering, and sorting.
*   Robustness of bulk operations and error recovery.
*   Clarity of caching strategy within this feature.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 8: Player System (`player/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Manages video playback using ExoPlayer, including UI controls, state management, media source handling, and subtitle integration.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/player/`
    *   `app/src/main/java/com/rdwatch/androidtv/player/controls/`
    *   `app/src/main/java/com/rdwatch/androidtv/player/error/`
    *   `app/src/main/java/com/rdwatch/androidtv/player/state/`
    *   `app/src/main/java/com/rdwatch/androidtv/player/subtitle/`

### 2. Specific Review Focus Points for This Area

*   Robustness of ExoPlayer integration and lifecycle management.
*   Usability and responsiveness of TV player controls.
*   Accuracy and performance of subtitle fetching, parsing, and display.
*   Handling of various playback states and errors.
*   Efficiency of media source preparation.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 9: Scraper System (`scraper/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Handles fetching, parsing, validating, and caching scraper manifests. Includes error handling like circuit breakers.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/scraper/`
    *   `app/src/main/java/com/rdwatch/androidtv/scraper/cache/`
    *   `app/src/main/java/com/rdwatch/androidtv/scraper/config/`
    *   `app/src/main/java/com/rdwatch/androidtv/scraper/error/`
    *   `app/src/main/java/com/rdwatch/androidtv/scraper/formats/`
    *   `app/src/main/java com/rdwatch/androidtv/scraper/models/`
    *   `app/src/main/java com/rdwatch/androidtv/scraper/parser/`
    *   `app/src/main/java com/rdwatch/androidtv/scraper/repository/`
    *   `app/src/main/java com/rdwatch/androidtv/scraper/validation/`

### 2. Specific Review Focus Points for This Area

*   Robustness of manifest parsing and validation against different formats.
*   Effectiveness of caching strategy for manifests.
*   Clarity of error handling and circuit breaker logic.
*   Modularity of scraper components (e.g., can new scraper types be added easily?).

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 10: General UI & Presentation (presentation/, ui/ - core parts)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Covers overall navigation structure (`AppNavigation`, `Screen`), base ViewModels, shared UI components, and screen-specific ViewModels/UIs for Home, Browse, Details, Search, Settings, Profile sections. Excludes highly specialized UI areas like FileBrowser or Player.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/presentation/navigation/`
    *   `app/src/main/java/com/rdwatch/androidtv/presentation/viewmodel/BaseViewModel.kt`
    *   `app/src/main/java/com/rdwatch/androidtv/presentation/components/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/home/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/browse/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/details/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/search/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/settings/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/profile/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/common/`
    *   `app/src/main/java/com/rdwatch/androidtv/ui/components/` (shared UI elements)
    *   `app/src/main/java/com/rdwatch/androidtv/ui/focus/`

### 2. Specific Review Focus Points for This Area

*   Consistency in UI design and UX patterns across different screens.
*   Effectiveness of focus management for TV navigation.
*   Reusability and composability of shared UI components.
*   Clarity of state management in screen-level ViewModels.
*   Performance of Compose layouts, especially lists and complex screens.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods** (especially in Composables or ViewModels)
*   [ ] **Duplicated Code (DRY Violations)** (in UI elements or ViewModel logic)
*   [ ] **Magic Numbers/Strings** (in layout dimensions, keys, etc.)
*   [ ] **Dead Code** (unused Composables or ViewModel logic)
*   [ ] **Insufficient Error Handling** (in UI state updates)
*   [ ] **Lack of Modularity/Tight Coupling** (screens too dependent on each other)
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests** (UI tests, ViewModel tests)
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 11: Core Utilities & Reactive Programming (`core/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Provides common error handling mechanisms (`AppException`, `ErrorHandler`), coroutine dispatchers (`DispatcherProvider`), and utility functions for Kotlin Flow.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/core/error/`
    *   `app/src/main/java/com/rdwatch/androidtv/core/reactive/`

### 2. Specific Review Focus Points for This Area

*   Usefulness and correctness of Flow extensions and operators.
*   Effectiveness of the common error handling framework.
*   Proper definition and usage of `DispatcherProvider`.
*   General utility functions: are they truly generic and well-tested?

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods**
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling** (within the utilities themselves)
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests** (critical for utility code)
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 12: Background Workers (`workers/`)

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Manages background tasks using WorkManager, such as `RealDebridSyncWorker`.
*   **Key Files/Directories:**
    *   `app/src/main/java/com/rdwatch/androidtv/workers/`

### 2. Specific Review Focus Points for This Area

*   Correctness of WorkManager implementation (constraints, retry policies, unique work).
*   Efficiency and reliability of background tasks.
*   Error handling and reporting for background work.
*   Impact on battery life and system resources.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions**
*   [ ] **Lack of/Meaningless Comments**
*   [ ] **Overly Complex Functions/Methods** (within the worker's `doWork()`)
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code**
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling**
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests**
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 13: Legacy Code & Transition Areas

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   This area covers any files explicitly identified as "legacy leanback files" or parts of the codebase still undergoing active transition from Android Leanback to Jetpack Compose, as mentioned in `CLAUDE-architecture.md` or identified during review.
*   **Key Files/Directories:**
    *   (To be identified by reviewer, e.g., by searching for Leanback imports or specific legacy patterns)
    *   `[legacy leanback files]` (placeholder from architecture doc)

### 2. Specific Review Focus Points for This Area

*   Identifying code that is truly legacy and can be scheduled for removal/refactoring.
*   Assessing adherence to new patterns in areas being transitioned.
*   Interoperability issues between legacy and new code.
*   Presence of outdated practices or libraries.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions** (often a mix of old and new)
*   [ ] **Lack of/Meaningless Comments** (especially on why old code exists)
*   [ ] **Overly Complex Functions/Methods** (common in older code)
*   [ ] **Duplicated Code (DRY Violations)**
*   [ ] **Magic Numbers/Strings**
*   [ ] **Dead Code** (high probability)
*   [ ] **Insufficient Error Handling**
*   [ ] **Lack of Modularity/Tight Coupling** (monolithic structures)
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests** (often true for legacy code)
*   [ ] **Over-reliance on Global State**
*   [ ] **"Cargo Cult Programming"**
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments** (often marking transition points)

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

## Area 14: Build & Configuration

**Last Reviewer:**
**Last Reviewed Date:**

### 1. Description & Scope

*   Covers build scripts (`build.gradle.kts`), dependency management (`libs.versions.toml`), project settings (`settings.gradle.kts`), Android Manifest, and Proguard rules.
*   **Key Files/Directories:**
    *   `app/build.gradle.kts`
    *   `gradle/libs.versions.toml`
    *   `settings.gradle.kts`
    *   `app/src/main/AndroidManifest.xml`
    *   `app/proguard-rules.pro`

### 2. Specific Review Focus Points for This Area

*   Clarity and efficiency of build scripts.
*   Up-to-dateness and necessity of dependencies.
*   Correctness of Android Manifest configurations (permissions, intent filters, TV features).
*   Effectiveness and maintenance of Proguard rules.
*   Build speed and optimization opportunities.

### 3. Common Tech Debt Checklist (AI/Vibe Coding Focus)

*   [ ] **Inconsistent Naming Conventions** (in custom Gradle tasks, etc.)
*   [ ] **Lack of/Meaningless Comments** (in build logic)
*   [ ] **Overly Complex Functions/Methods** (in build scripts)
*   [ ] **Duplicated Code (DRY Violations)** (in Gradle files)
*   [ ] **Magic Numbers/Strings** (hardcoded versions, paths)
*   [ ] **Dead Code** (unused dependencies, old build logic)
*   [ ] **Insufficient Error Handling** (less applicable, but check for build warnings)
*   [ ] **Lack of Modularity/Tight Coupling** (build scripts hard to manage)
*   [ ] **Inconsistent Formatting**
*   [ ] **Suboptimal Algorithm Choices**
*   [ ] **Missing or Inadequate Tests** (for build logic, if complex)
*   [ ] **Over-reliance on Global State** (in Gradle project properties)
*   [ ] **"Cargo Cult Programming"** (copied Gradle snippets)
*   [ ] **Inconsistent API Design (if applicable)**
*   [ ] **Numerous unresolved `TODO`/`FIXME` comments**

### 4. Findings & Action Items

| File | Line(s) | Issue Description | Tech Debt Category | Suggested Action | Priority (H/M/L) | Status (Open/In Progress/Resolved) | Assignee | Notes |
|------|---------|-------------------|--------------------|------------------|--------------------|------------------------------------|----------|-------|
|      |         |                   |                    |                  |                    | Open                               |          |       |

### 5. Reviewer General Notes & Observations

*   

---

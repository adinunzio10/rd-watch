# CLAUDE.md

This file provides essential guidance to Claude Code when working with this Android TV application.

## Project Overview

**RD Watch** is an Android TV application built with Jetpack Compose and Android Leanback library, designed for Android TV devices with remote control navigation and 10-foot UI experience.

- **Package**: `com.rdwatch.androidtv`
- **Min SDK**: 23 (Android 6.0) | **Target SDK**: 35 (Android 15)
- **Build Tool**: Gradle with Kotlin DSL

## Essential Commands

```bash
# Build & Run
./gradlew build                    # Build project
./gradlew installDebug            # Install to Android TV device/emulator
./gradlew clean                   # Clean build

# Code Quality
./gradlew lint                    # Full lint check
./ktlint-summary.sh               # KtLint format & check
./lint-check.sh                   # Minimal lint output

# Dependencies
./gradlew dependencyUpdates       # Check for updates
./gradlew dependencies            # View dependency tree
```

## Documentation Structure

This project uses modular documentation for optimal context management:

- **[CLAUDE-architecture.md](CLAUDE-architecture.md)**: Application structure, technology stack, development patterns
- **[claude-workflows.md](claude-workflows.md)**: Git/GitHub workflows, branching strategy, commit conventions
- **[claude-taskmaster.md](claude-taskmaster.md)**: Task-Master CLI integration, commands, and workflow patterns
- **[claude-tests.md](claude-tests.md)**: Testing strategy, test commands, and test maintenance
- **[claude-debugging-methodology.md](claude-debugging-methodology.md)**: Systematic problem-solving approaches
- **[claude-android-patterns.md](claude-android-patterns.md)**: Android TV app specific debugging patterns
- **[claude-session-management.md](claude-session-management.md)**: Best practices for effective development sessions
- **[planning-docs/](planning-docs/)**: Planning mode outputs for major changes

## Self-Maintenance Rules

**CRITICAL**: Claude Code must follow these rules:

1. **NEVER modify CLAUDE.md directly** - this file should remain stable
2. **Auto-maintain specialized documentation** when making code changes:
   - Update `CLAUDE-architecture.md` when changing app structure or dependencies
   - Update `claude-tests.md` when adding/modifying tests
   - Update `claude-workflows.md` when changing development processes
3. **Use planning mode** for major changes - save plans to `planning-docs/`
4. **Reference documentation by filename:line** when discussing code locations

## Key Directories

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main Compose UI entry point
├── Movie.kt                     # Data model
├── MovieList.kt                 # Static data provider
├── ui/theme/                    # Material3 theme configuration
└── [legacy leanback files]      # Transitioning away from these
```

## Linting Notes

- Remember to lint using `ktlint-summary.sh` for code formatting and style checking

---

*This streamlined CLAUDE.md reduces context overhead by 90%. Detailed information is available in specialized documentation files listed above.*

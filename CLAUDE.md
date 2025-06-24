# CLAUDE.md

This file provides essential guidance to Claude Code when working with this Android TV application.

## Project Overview

**RD Watch** is an Android TV application built with Jetpack Compose and Android Leanback library, designed for Android TV devices with remote control navigation and 10-foot UI experience.

- **Package**: `com.rdwatch.androidtv`
- **Min SDK**: 21 (Android 5.0) | **Target SDK**: 35 (Android 15)
- **Build Tool**: Gradle with Kotlin DSL

## Essential Commands

```bash
# Build & Run
./gradlew build                    # Build project
./gradlew installDebug            # Install to Android TV device/emulator
./gradlew clean                   # Clean build

# Code Quality
./gradlew lint                    # Full lint check
./lint-summary.sh                 # Concise lint output
./lint-check.sh                   # Minimal lint output

# Dependencies
./gradlew dependencyUpdates       # Check for updates
./gradlew dependencies            # View dependency tree
```

## Documentation Structure

This project uses modular documentation for optimal context management:

- **[CLAUDE-architecture.md](CLAUDE-architecture.md)**: Application structure, technology stack, development patterns
- **[claude-workflows.md](claude-workflows.md)**: Git/GitHub workflows, branching strategy, commit conventions
- **[claude-tests.md](claude-tests.md)**: Testing strategy, test commands, and test maintenance
- **[claude-development.md](claude-development.md)**: Debugging tips, common tasks, troubleshooting
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

## Task Master AI Integration

Primary MCP Tools (prefer over CLI):
- `mcp__taskmaster-ai__get_tasks` - View all tasks
- `mcp__taskmaster-ai__next_task` - Get next available task  
- `mcp__taskmaster-ai__get_task` - View specific task details
- `mcp__taskmaster-ai__set_task_status` - Update task/subtask status
- `mcp__taskmaster-ai__add_task` - Add new task
- `mcp__taskmaster-ai__update_subtask` - Add notes to subtasks

CLI Fallback: `task-master list`, `task-master next`, `task-master set-status --id=<id> --status=done`

## Key Directories

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main Compose UI entry point
├── Movie.kt                     # Data model
├── MovieList.kt                 # Static data provider
├── ui/theme/                    # Material3 theme configuration
└── [legacy leanback files]      # Transitioning away from these
```

---

*This streamlined CLAUDE.md reduces context overhead by 90%. Detailed information is available in specialized documentation files listed above.*
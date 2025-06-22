# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RD Watch** is an Android TV application built with Jetpack Compose and Android Leanback library. The app is designed for Android TV devices with remote control navigation and 10-foot UI experience.

## Development Commands

### Build & Run

```bash
# Build the project
./gradlew build

# Install debug build to connected Android TV device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Lint check
./gradlew lint

# Lint summary (concise output)
./lint-summary.sh

# Basic lint check (minimal output) 
./lint-check.sh
```

### Testing

```bash
# Currently no tests configured - this is a development opportunity
# Standard Android test commands would be:
# ./gradlew test              # Unit tests
# ./gradlew connectedCheck    # Instrumentation tests
```

### Dependency Management

```bash
# Check for dependency updates
./gradlew dependencyUpdates

# View dependency tree
./gradlew dependencies
```

### Git & GitHub CLI

```bash
# Check repository status
git status
git log --oneline -10

# GitHub CLI operations
gh repo view                    # View repository info
gh issue list                   # List open issues
gh pr list                      # List open pull requests
gh pr status                    # Check PR status
gh workflow list                # List GitHub Actions workflows
```

## Architecture & Code Structure

### Application Structure

- **Package**: `com.rdwatch.androidtv`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 35 (Android 15)
- **Build Tool**: Gradle with Kotlin DSL

### Key Directories

```
app/src/main/java/com/rdwatch/androidtv/
├── MainActivity.kt              # Main entry point with Compose UI
├── Movie.kt                     # Data model for video content
├── MovieList.kt                 # Static data provider
├── ui/theme/
│   ├── Theme.kt                # Material3 theme configuration
│   └── Type.kt                 # TV-optimized typography
├── BrowseErrorActivity.kt       # Legacy leanback error handling
├── DetailsActivity.kt           # Legacy leanback detail view
├── PlaybackActivity.kt          # Legacy leanback video playback
└── [other legacy leanback files]
```

### Technology Stack

- **UI Framework**: Jetpack Compose with Material3
- **TV Framework**: Android Leanback (transitioning from)
- **Image Loading**: Glide 4.11.0
- **Navigation**: Focus-based D-pad navigation
- **Language**: Kotlin with Java 11 compatibility

### Current Architecture State

The app is in **transition** from traditional Android Leanback framework to modern Jetpack Compose:

- **New Code**: Uses Compose with Material3 (MainActivity, Theme, Type)
- **Legacy Code**: Traditional Leanback activities still present
- **Data**: Simple static data provider with sample Google videos

## Key Configuration Files

### Build Configuration

- **app/build.gradle.kts**: Main app module build configuration
- **gradle/libs.versions.toml**: Version catalog for all dependencies
- **settings.gradle.kts**: Project structure and repositories

### Android Configuration

- **AndroidManifest.xml**: TV-specific permissions and intent filters
- **proguard-rules.pro**: Code obfuscation rules (currently default)

### Development Tools

- **package.json**: Task Master AI integration for project management. Instructions are found in [AGENTS](AGENTS.md)
- **.mcp.json**: MCP server configuration for Claude Code

## Android TV Specific Considerations

### Focus Management

- All UI components must support D-pad navigation
- Focus states are critical for user experience
- Use `focusRequester` and `onFocusChanged` modifiers in Compose

### Screen Layout

- **Orientation**: Locked to landscape
- **Safe Area**: Account for TV overscan areas
- **Typography**: Larger font sizes for 10-foot viewing distance
- **Colors**: High contrast for TV displays

### Leanback Integration

- **Intent Filter**: `LEANBACK_LAUNCHER` category for Android TV launcher
- **Features**: `android.software.leanback` required
- **Permissions**: Internet access for streaming content

## Development Patterns

### Adding New Screens

1. Create Composable functions following TV design patterns
2. Implement proper focus management
3. Use Material3 TV-optimized components
4. Test with D-pad navigation
5. Consider overscan and safe areas

### Data Management

Current: Static data in `MovieList.kt`
Future considerations:

- Repository pattern for data access
- ViewModels for state management
- Retrofit for API communication
- Room for local caching

### Testing Strategy (Not Yet Implemented)

Recommended test structure:

- **Unit Tests**: Data models, business logic
- **Compose Tests**: UI component testing with focus simulation
- **Integration Tests**: Navigation flows, D-pad interaction
- **TV-Specific Tests**: Leanback compatibility, remote control simulation

## Common Development Tasks

### Adding Video Content

1. Update `Movie.kt` data model if needed
2. Add content to `MovieList.kt` or implement dynamic data source
3. Update UI components to handle new content types
4. Test with focus navigation

### Modifying UI Components

1. Locate Compose components in `MainActivity.kt`
2. Follow Material3 design guidelines
3. Ensure TV accessibility (focus, sizing, contrast)
4. Test on actual TV device or emulator

### Theme Customization

1. Modify colors in `ui/theme/Theme.kt`
2. Update typography in `ui/theme/Type.kt`
3. Ensure sufficient contrast for TV viewing
4. Test in both light and dark themes

## Dependencies & Versions

### Core Dependencies

- **Jetpack Compose BOM**: 2024.12.01
- **Compose Compiler**: 1.5.15
- **Android Leanback**: 1.2.0
- **AndroidX Core KTX**: 1.16.0
- **Activity Compose**: 1.9.3
- **Glide**: 4.11.0

### Future TV-Specific Dependencies

Available when stable (currently alpha):

- **TV Foundation**: 1.0.0-alpha11
- **TV Material**: 1.0.0-alpha11

## Debugging & Development Tips

### Android TV Emulator Setup

1. Create Android TV emulator with API 21+
2. Enable developer options
3. Use ADB for debugging: `adb connect <tv-ip>:5555`

### Common Issues

- **Focus Problems**: Use layout inspector to debug focus traversal
- **Overscan**: Test on actual TV hardware, not just emulator
- **Performance**: Profile on lower-end TV devices
- **Remote Control**: Test all D-pad directions and buttons

### Build Troubleshooting

- **Lint Errors**: Currently set to non-blocking (`abortOnError = false`)
- **Compose Issues**: Check Compose compiler version compatibility
- **TV-Specific**: Verify leanback feature requirements in manifest

## Future Development Roadmap

### Immediate Opportunities

1. **Testing Infrastructure**: Add unit and integration tests
2. **Dynamic Content**: Replace static data with API integration
3. **Navigation**: Implement proper fragment/screen navigation
4. **Error Handling**: Improve error states and offline functionality

### Architectural Improvements

1. **MVVM Pattern**: Implement ViewModels and state management
2. **Dependency Injection**: Add Hilt or Koin
3. **Repository Pattern**: Abstract data access layer
4. **TV Compose Migration**: Full migration from Leanback when stable

## Task Master AI Integration

This project includes Task Master AI for development workflow management. When using Claude Code, prefer MCP tools over CLI commands:

**Primary MCP Tools:**
- `mcp__taskmaster-ai__get_tasks` - View all tasks (replaces `task-master list`)
- `mcp__taskmaster-ai__next_task` - Get next available task (replaces `task-master next`)
- `mcp__taskmaster-ai__get_task` - View specific task details (replaces `task-master show <id>`)
- `mcp__taskmaster-ai__set_task_status` - Update task/subtask status (replaces `task-master set-status`)
- `mcp__taskmaster-ai__add_task` - Add new task (replaces `task-master add-task`)
- `mcp__taskmaster-ai__update_subtask` - Add notes to subtasks (replaces `task-master update-subtask`)

**CLI Fallback:**
```bash
# Use CLI if MCP is unavailable
task-master list                                    # View current tasks
task-master next                                    # Get next task to work on
task-master set-status --id=<id> --status=done     # Mark task complete
task-master add-task --prompt="description"        # Add new development task
```

See the MCP integration in `.mcp.json` for Claude Code task management tools.

## Git/GitHub Workflow with Task Master Integration

### Branch Strategy

Each Task Master task should have its own feature branch following this naming convention:

```bash
# Create branch for task (replace X with task ID)
git checkout -b task/X-short-description

# Examples:
git checkout -b task/1-implement-user-auth
git checkout -b task/2.3-add-video-player-controls
```

### Development Workflow

#### 1. Starting a New Task

```bash
# Get next task from Task Master (use MCP tool)
# Use: mcp__taskmaster-ai__next_task

# Create and switch to new branch for the task
git checkout -b task/[task-id]-[short-description]

# Mark task as in progress (use MCP tool)
# Use: mcp__taskmaster-ai__set_task_status with id=[task-id] and status=in-progress
```

#### 2. Working on Subtasks

For each subtask, commit your changes before moving to the next subtask:

```bash
# After completing a subtask
git add .
git commit -m "feat(task-[task-id]): complete subtask [subtask-id] - [description]

Implements [brief description of what was done]

Task: [task-id]
Subtask: [subtask-id]"

# Mark subtask as complete (use MCP tool)
# Use: mcp__taskmaster-ai__set_task_status with id=[task-id].[subtask-id] and status=done

# Update subtask with implementation notes (use MCP tool)
# Use: mcp__taskmaster-ai__update_subtask with id=[task-id].[subtask-id] and prompt="Completed: [what was implemented and any important notes]"
```

#### 3. Completing a Task

When all subtasks are complete:

```bash
# Final commit if needed
git add .
git commit -m "feat(task-[task-id]): complete task - [task title]

All subtasks completed:
- [subtask 1 summary]
- [subtask 2 summary]
- [subtask 3 summary]

Task: [task-id]"

# Mark task as complete (use MCP tool)
# Use: mcp__taskmaster-ai__set_task_status with id=[task-id] and status=done

# Push branch to remote
git push -u origin task/[task-id]-[short-description]
```

#### 4. Creating Pull Request

```bash
# Create PR with GitHub CLI
gh pr create \
  --title "Task [task-id]: [task title]" \
  --body "## Summary
Implements Task [task-id]: [task title]

## Subtasks Completed
- [x] [subtask 1]
- [x] [subtask 2] 
- [x] [subtask 3]

## Testing
- [x] Build passes: \`./gradlew build\`
- [x] Lint passes: \`./gradlew lint\`
- [ ] Manual testing on Android TV emulator
- [ ] Focus navigation tested

## Task Master Reference
Task ID: [task-id]
Branch: task/[task-id]-[short-description]

Generated with Task Master AI integration." \
  --assignee @me

# View the created PR
gh pr view
```

### GitHub CLI Commands

#### Repository Management
```bash
# Clone repository
gh repo clone [owner]/[repo]

# View repository information
gh repo view

# Fork repository
gh repo fork
```

#### Issue Management
```bash
# List issues
gh issue list
gh issue list --state closed
gh issue list --assignee @me

# Create issue
gh issue create --title "Bug: [description]" --body "[detailed description]"

# View issue
gh issue view [issue-number]

# Close issue
gh issue close [issue-number]
```

#### Pull Request Management
```bash
# List PRs
gh pr list
gh pr list --state closed
gh pr list --author @me

# View PR details
gh pr view [pr-number]
gh pr view --web                # Open in browser

# Review PR
gh pr review [pr-number] --approve
gh pr review [pr-number] --request-changes --body "[feedback]"

# Merge PR
gh pr merge [pr-number] --squash
gh pr merge [pr-number] --merge
gh pr merge [pr-number] --rebase

# Check PR status
gh pr status
```

#### Workflow Management
```bash
# List workflows
gh workflow list

# View workflow runs
gh run list
gh run list --workflow=[workflow-name]

# View specific run
gh run view [run-id]

# Re-run failed workflow
gh run rerun [run-id]
```

### Commit Message Conventions

Follow this format for consistency:

```
type(scope): brief description

Longer description if needed explaining what and why.

Task: [task-id]
Subtask: [subtask-id] (if applicable)
```

**Types:**
- `feat`: New feature implementation
- `fix`: Bug fixes
- `refactor`: Code refactoring without behavior change
- `style`: UI/styling changes
- `test`: Adding or updating tests
- `docs`: Documentation updates
- `chore`: Build process, dependency updates

**Scopes:**
- `task-[id]`: For task-specific work
- `ui`: UI components and styling
- `tv`: Android TV specific features
- `compose`: Jetpack Compose related changes
- `leanback`: Leanback library related changes
- `build`: Build configuration changes

### Pre-Commit Checklist

Before each commit, ensure:

```bash
# Build passes
./gradlew build

# Lint passes  
./gradlew lint

# Code is properly formatted (if using ktlint)
./gradlew ktlintFormat

# Update Task Master with progress (use MCP tool)
# Use: mcp__taskmaster-ai__update_subtask with id=[subtask-id] and prompt="[implementation notes]"
```

### Branch Management

#### Keeping Branch Updated
```bash
# Switch to main and pull latest
git checkout main
git pull origin main

# Switch back to feature branch and rebase
git checkout task/[task-id]-[description]
git rebase main

# Force push if needed (only for feature branches)
git push --force-with-lease
```

#### Cleaning Up After Merge
```bash
# After PR is merged, clean up local branches
git checkout main
git pull origin main
git branch -d task/[task-id]-[description]

# Clean up remote tracking branches
git remote prune origin
```

### Integration with Android Studio

Configure Android Studio for optimal Git workflow:

1. **Enable Git Integration**: File → Settings → Version Control → Git
2. **Commit Templates**: Use the commit message format above
3. **Branch Naming**: Use the task/[id]-[description] convention
4. **Pre-commit Hooks**: Set up automatic linting before commits

### Troubleshooting

#### Common Git Issues
```bash
# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Fix commit message
git commit --amend -m "new message"

# Stash changes temporarily
git stash
git stash pop

# View commit history
git log --oneline --graph
```

#### GitHub CLI Issues
```bash
# Login/re-authenticate
gh auth login
gh auth status

# Switch between accounts
gh auth switch

# Check current configuration
gh config list
```

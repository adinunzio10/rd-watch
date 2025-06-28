# Claude Workflows Documentation

This file contains Git/GitHub workflow patterns and conventions for the RD Watch project. It is automatically maintained by Claude Code as development processes evolve.

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
# Get next task from Task Master
task-master next

# Create and switch to new branch for the task
git checkout -b task/[task-id]-[short-description]

# Mark task as in progress
task-master set-status --id=[task-id] --status=in-progress
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

# Mark subtask as complete
task-master set-status --id=[task-id].[subtask-id] --status=done

# Update subtask with implementation notes
task-master update-subtask --id=[task-id].[subtask-id] --prompt="Completed: [what was implemented and any important notes]"
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

# Mark task as complete
task-master set-status --id=[task-id] --status=done

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

## GitHub CLI Commands

### Repository Management
```bash
# Clone repository
gh repo clone [owner]/[repo]

# View repository information
gh repo view

# Fork repository
gh repo fork
```

### Issue Management
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

### Pull Request Management
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

### Workflow Management
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

## Commit Message Conventions

Follow this format for consistency:

```
type(scope): brief description

Longer description if needed explaining what and why.

Task: [task-id]
Subtask: [subtask-id] (if applicable)
```

### Types
- `feat`: New feature implementation
- `fix`: Bug fixes
- `refactor`: Code refactoring without behavior change
- `style`: UI/styling changes
- `test`: Adding or updating tests
- `docs`: Documentation updates
- `chore`: Build process, dependency updates

### Scopes
- `task-[id]`: For task-specific work
- `ui`: UI components and styling
- `tv`: Android TV specific features
- `compose`: Jetpack Compose related changes
- `leanback`: Leanback library related changes
- `build`: Build configuration changes

## Pre-Commit Checklist

Before each commit, ensure:

```bash
# Build passes
./gradlew build

# Lint passes  
./gradlew lint

# Code is properly formatted (if using ktlint)
./gradlew ktlintFormat

# Update Task Master with progress
task-master update-subtask --id=[subtask-id] --prompt="[implementation notes]"
```

## Branch Management

### Keeping Branch Updated
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

### Cleaning Up After Merge
```bash
# After PR is merged, clean up local branches
git checkout main
git pull origin main
git branch -d task/[task-id]-[description]

# Clean up remote tracking branches
git remote prune origin
```

## Integration with Android Studio

Configure Android Studio for optimal Git workflow:

1. **Enable Git Integration**: File → Settings → Version Control → Git
2. **Commit Templates**: Use the commit message format above
3. **Branch Naming**: Use the task/[id]-[description] convention
4. **Pre-commit Hooks**: Set up automatic linting before commits

## Troubleshooting

### Common Git Issues
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

### GitHub CLI Issues
```bash
# Login/re-authenticate
gh auth login
gh auth status

# Switch between accounts
gh auth switch

# Check current configuration
gh config list
```

## Maintenance Notes

*This file is automatically maintained by Claude Code. When updating development processes:*

1. *Update workflow steps and conventions*
2. *Add new GitHub CLI commands as needed*
3. *Keep commit message conventions current*
4. *Document any new tools or integrations*

---

**Last Updated**: Auto-maintained by Claude Code  
**Related Files**: [CLAUDE.md](CLAUDE.md), [CLAUDE-architecture.md](CLAUDE-architecture.md)
# Task-Master CLI Integration for Claude Code

This file provides comprehensive guidance for Claude Code when working with the Task-Master CLI tool. Task-Master enables AI-powered task management with dependency tracking, complexity analysis, and structured development workflows.

## Essential Claude Code Integration

### When to Use Task-Master

**Use task-master when:**
- Starting new development sessions to find the next task
- Breaking down complex features into manageable subtasks
- Tracking progress on multi-step implementations
- Managing task dependencies and coordination
- Logging implementation notes and progress

**Don't use task-master for:**
- Simple one-step tasks that don't need tracking
- Quick fixes or trivial changes
- Pure research or exploration tasks

### Core Daily Workflow Commands

```bash
# 1. Start Development Session
task-master next                           # Find next available task
task-master show <id>                      # View detailed task information
task-master set-status --id=<id> --status=in-progress  # Mark task as started

# 2. During Implementation
task-master update-subtask --id=<id> --prompt="Implementation notes..."
task-master set-status --id=<subtask-id> --status=done  # Complete subtasks

# 3. Complete Task
task-master set-status --id=<id> --status=done
```

## Command Reference

### Task Navigation & Viewing

#### `task-master list [options]`
**Purpose**: Display all tasks with status overview
**Key Options**:
- `--status=<status>` - Filter by status (pending, done, in-progress, etc.)
- `--with-subtasks` - Show subtasks for each task
- `--tag=<tag>` - Filter by tag context

**Output**: Rich dashboard with progress metrics, dependency status, and task table

#### `task-master next [options]`
**Purpose**: Find the next available task based on dependencies
**Output**: Single task recommendation with details and suggested actions

#### `task-master show <id> [options]`
**Purpose**: Display detailed information about specific tasks
**Key Options**:
- `--id=<id>` - Task ID (can be comma-separated for multiple)
- `--status=<status>` - Filter subtasks by status

**Output**: Complete task details including description, implementation details, test strategy, and subtasks

### Task Status Management

#### `task-master set-status --id=<id> --status=<status>`
**Purpose**: Update task status
**Required Parameters**: 
- `--id=<id>` - Task ID (supports comma-separated for multiple tasks)
- `--status=<status>` - One of: pending, done, in-progress, review, deferred, cancelled

**Claude Code Usage**: Mark tasks as in-progress before starting, complete when done

### Task Enhancement

#### `task-master update-subtask --id=<parentId.subtaskId> --prompt="<context>"`
**Purpose**: Append timestamped implementation notes to subtasks
**Required Parameters**:
- `--id=<parentId.subtaskId>` - Subtask ID in format "1.2" 
- `--prompt="<context>"` - Implementation notes, progress updates, or findings

**Claude Code Usage**: Log progress, document blockers, record implementation decisions

#### `task-master expand --id=<id> [options]`
**Purpose**: Break down complex tasks into subtasks
**Key Options**:
- `--id=<id>` - Task ID to expand
- `--num=<number>` - Number of subtasks to generate
- `--research` - Enable research-backed generation
- `--force` - Force expansion even if subtasks exist

**Claude Code Usage**: Use for complex tasks that need breakdown before implementation

### Task Creation

#### `task-master add-task --prompt="<description>" [options]`
**Purpose**: Create new tasks using AI assistance
**Key Options**:
- `--prompt="<text>"` - Task description (required)
- `--dependencies=<ids>` - Comma-separated dependency IDs
- `--priority=<priority>` - high, medium, low (default: medium)
- `--research` - Use research capabilities for task creation

## Status Values and Meanings

| Status | Description | When to Use |
|--------|-------------|-------------|
| `pending` | Ready to work on | Default for new tasks |
| `in-progress` | Currently being worked on | Mark when starting implementation |
| `done` | Completed and verified | Mark when fully implemented and tested |
| `review` | Needs code review | After implementation, before merge |
| `deferred` | Postponed | When blocked or deprioritized |
| `cancelled` | No longer needed | When requirements change |

## Task ID Format

- **Main tasks**: `1`, `2`, `3`, etc.
- **Subtasks**: `1.1`, `1.2`, `2.1`, etc. 
- **Sub-subtasks**: `1.1.1`, `1.1.2`, etc.

## Output Format Understanding

### List Command Output
- **Progress bars**: Visual representation of completion
- **Dashboard metrics**: Task counts by status and priority
- **Dependency analysis**: Shows dependency health and next recommendations
- **Task table**: ID, title, status, priority, dependencies, complexity

### Next Command Output
- **Single task focus**: Shows one recommended task
- **Dependency context**: Why this task is ready
- **Action suggestions**: Commands to start working

### Show Command Output
- **Complete task details**: Full description and implementation details
- **Test strategy**: How to verify completion
- **Subtask breakdown**: If subtasks exist
- **Action suggestions**: Next steps for the task

## Claude Code Command Integration

### Tagged Lists Workflow Commands

The `.claude/commands/` directory contains tag-aware workflow commands that leverage Tagged Lists capabilities:

**Core Development Workflow:**
- **`/next-task`**: Task identification, complexity analysis, and implementation approach recommendations
- **`/single-agent`**: Traditional coordinated development in single context with structured agent handoffs
- **`/multi-agent`**: Parallel agent development using isolated tag contexts for complex tasks

**Specialized Workflows:**
- **`/add-task`**: Enhanced with tag support for cross-context task creation
- **`/feature-branch`**: Create feature-specific tag contexts for isolated development
- **`/experiment-context`**: Safe experimentation workflows without affecting main development
- **`/team-collaboration`**: Multi-developer task management using tagged contexts  
- **`/release-planning`**: Version-specific task organization and release management

**Development Flow:**
1. Use `/next-task` to identify task and get complexity-based recommendations
2. Choose `/single-agent` for coordinated development or `/multi-agent` for parallel development
3. Leverage specialized workflows for feature development, experimentation, or team coordination

These commands provide structured workflows that make full use of Tagged Lists multi-context capabilities, including both traditional coordinated development and true parallel agent development without conflicts.

## Integration with Claude Code Workflows

### 1. Session Start Pattern
```bash
# Check current project status and context
task-master list
task-master tags

# Identify next task and get recommendations
/next-task

# Choose implementation approach based on complexity
/single-agent    # For traditional coordinated development
# OR
/multi-agent     # For parallel development in isolated contexts
```

### 2. Implementation Pattern
```bash
# During implementation - log progress
task-master update-subtask --id=<id> --prompt="Implemented navigation logic, testing focus handling"

# Complete subtasks as you go
task-master set-status --id=<subtask-id> --status=done

# Final completion
task-master set-status --id=<id> --status=done
```

### 3. Complex Task Pattern  
```bash
# Break down complex tasks first
task-master expand --id=<id> --research

# Work through subtasks systematically
task-master show <id>  # Review subtasks
# Implement each subtask...
task-master set-status --id=<subtask-id> --status=done

# Complete parent task
task-master set-status --id=<id> --status=done
```

### 4. Blocked Task Pattern
```bash
# When blocked, document the issue
task-master update-subtask --id=<id> --prompt="Blocked by missing API key configuration"

# Mark as deferred if needed
task-master set-status --id=<id> --status=deferred

# Find alternative work
task-master next
```

## Advanced Features

### Tagged Lists - Multi-Context Task Management

**Tagged Lists transforms task-master into a multi-context powerhouse**, enabling parallel development workflows, team collaboration, and project experimentation without conflicts.

#### Core Tag Architecture

- **Legacy Format**: `{ "tasks": [...] }`
- **New Tagged Format**: `{ "master": { "tasks": [...], "metadata": {...} }, "feature-xyz": { "tasks": [...], "metadata": {...} } }`
- **Automatic Migration**: Existing projects migrate seamlessly to tagged format with zero intervention
- **State Management**: `.taskmaster/state.json` tracks current tag, last switched time, migration status

#### Tag Management Commands

**List and View Tags:**
```bash
task-master tags                    # List all tags with task counts and completion stats
task-master tags --show-metadata    # Include creation dates and descriptions
```

**Create and Manage Tags:**
```bash
task-master add-tag <name>                    # Create empty tag
task-master add-tag <name> --copy-from-current # Copy tasks from active tag
task-master add-tag <name> --copy-from=<tag>   # Copy from specific tag
task-master add-tag <name> --from-branch       # Create tag using git branch name
task-master add-tag <name> --description="Feature work" # Add custom description
```

**Switch and Navigate Tags:**
```bash
task-master use-tag <name>          # Switch contexts and see next available task
task-master rename-tag <old> <new>  # Rename tags with automatic reference updates
task-master copy-tag <source> <target> # Duplicate tag contexts for experimentation
task-master delete-tag <name>       # Delete tags (with confirmation protection)
```

#### Universal --tag Flag Support

**Every task operation supports tag-specific execution:**
```bash
# Task management with tags
task-master list --tag=feature-branch      # View tasks in specific context
task-master next --tag=experiment          # Get next task from specific tag
task-master add-task --tag=v2-redesign --prompt="..." # Create tasks in specific tag
task-master set-status --tag=hotfix --id=5 --status=done # Update tasks in specific contexts
task-master expand --tag=research --id=3   # Break down tasks within tag contexts
task-master show --tag=feature-auth --id=1 # View task details in specific context
```

**Analysis and Reports:**
```bash
task-master analyze-complexity --tag=performance-work  # Tag-specific complexity analysis
task-master complexity-report --tag=feature-xyz        # Generate tag-specific reports
# Reports saved as: task-complexity-report_tagname.json
# Master tag uses default filename: task-complexity-report.json
```

#### Multi-Context Workflow Patterns

**Feature Development Workflow:**
```bash
# 1. Create feature-specific context
task-master add-tag feature-search --copy-from-current
task-master use-tag feature-search

# 2. Add feature-specific tasks
task-master add-task --prompt="Implement search UI" --priority=high
task-master add-task --prompt="Add search backend" --priority=high

# 3. Work in isolated context
task-master next    # Shows next task in feature context
# ... implement tasks ...

# 4. Switch back to main development
task-master use-tag master
```

**Parallel Development:**
```bash
# Main development in master
task-master use-tag master
task-master next

# Quick experiment in parallel
task-master add-tag experiment --description="Performance testing"
task-master add-task --tag=experiment --prompt="Test lazy loading optimization"
task-master use-tag experiment
task-master next    # Work on experimental task

# Switch between contexts as needed
task-master use-tag master      # Back to main work
task-master use-tag experiment  # Back to experiments
```

**Team Collaboration:**
```bash
# Create teammate-specific contexts
task-master add-tag alice --copy-from-current --description="Alice's work context"
task-master add-tag bob --copy-from=master --description="Bob's tasks"

# Assign work to specific contexts
task-master add-task --tag=alice --prompt="Frontend components" --priority=high
task-master add-task --tag=bob --prompt="Backend API" --priority=high

# Team members switch to their contexts
task-master use-tag alice   # Alice's workflow
task-master use-tag bob     # Bob's workflow
```

**Release Management:**
```bash
# Version-specific planning
task-master add-tag v2.0 --description="Next major release"
task-master parse-prd release-spec.txt --tag=v2.0

# Release branch preparation
task-master copy-tag master v2.1
task-master use-tag v2.1

# Emergency fixes
task-master add-tag hotfix --description="Critical fixes"
task-master use-tag hotfix
```

#### State Management & File Organization

**Automatic State Tracking:**
- Current active tag persists across terminal sessions
- Last switched time tracking for context awareness  
- Branch-tag mapping foundation for Git integration
- Migration status tracking

**File Isolation:**
- Tag-specific complexity reports: `task-complexity-report_tagname.json`
- Master tag uses default filenames: `task-complexity-report.json`
- Automatic file isolation prevents cross-tag contamination
- Clean separation of tag data and internal state

#### Best Practices for Tagged Lists

**Tag Naming Conventions:**
- **Features**: `feature-auth`, `feature-search`, `feature-payments`
- **Experiments**: `experiment`, `experiment-ui`, `experiment-perf`
- **Versions**: `v2.0`, `v2.1`, `next-release`
- **Team**: `alice`, `bob`, `frontend-team`, `backend-team`
- **Phases**: `research`, `implementation`, `testing`, `deployment`

**Workflow Recommendations:**
1. **Keep master clean**: Use master for stable, validated tasks
2. **Feature isolation**: Create feature-specific tags for major work
3. **Experiment safely**: Use experiment tags for risky changes
4. **Team coordination**: Use personal tags for individual work contexts
5. **Version planning**: Use version tags for release planning

### Research Mode
Add `--research` flag to commands for AI-enhanced task creation and expansion using external research.

### Dependency Analysis
Use `task-master list` to understand dependency bottlenecks and priority blocking.

## Error Handling

- **Command not found**: Ensure task-master is installed globally
- **No tasks file**: Run `task-master init` to initialize project
- **Invalid task ID**: Use `task-master list` to see available IDs
- **API errors**: Check AI model configuration with `task-master models`

## Best Practices for Claude Code

### Core Task Management
1. **Always check next task**: Use `task-master next` to get AI-recommended priority
2. **Log progress frequently**: Use `update-subtask` to maintain context across sessions
3. **Complete incrementally**: Mark subtasks done as you complete them
4. **Expand complex tasks**: Don't implement large tasks without subtask breakdown
5. **Update status promptly**: Keep status current for accurate dependency tracking
6. **Use detailed prompts**: Provide context when updating or creating tasks

### Tagged Lists Workflows
7. **Check context first**: Always run `task-master tags` to understand available contexts
8. **Isolate feature work**: Create feature-specific tags for major development work
9. **Experiment safely**: Use experiment tags for risky changes that might not work
10. **Switch contexts deliberately**: Use `task-master use-tag` to focus on specific work
11. **Tag task operations**: Use `--tag=<name>` for cross-context task management
12. **Clean tag management**: Delete completed feature tags to reduce clutter

### Multi-Context Development
13. **Master stays stable**: Keep master tag for validated, stable tasks
14. **Feature branches align**: Create tags that match feature branch names
15. **Parallel work**: Use separate tags for parallel development streams
16. **Team coordination**: Use personal tags for individual work contexts
17. **Version planning**: Use version tags for release-specific task organization

### Multi-Agent Development
18. **Use `/multi-agent` for complexity 4+**: Leverage parallel agent development for complex tasks
19. **Agent context isolation**: Each agent works in dedicated tag context to prevent conflicts
20. **Integration agent coordination**: Use integration agent to manage cross-agent dependencies
21. **Systematic agent cleanup**: Archive and delete agent contexts after task completion
22. **Cross-agent dependency tracking**: Use task dependencies between agent contexts for proper sequencing

---

*This documentation enables Claude Code to effectively integrate task-master CLI for structured, dependency-aware development workflows.*
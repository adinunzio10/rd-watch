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

## Integration with Claude Code Workflows

### 1. Session Start Pattern
```bash
# Check current project status
task-master list

# Find next task
task-master next

# Get detailed task info
task-master show <id>

# Mark as in-progress
task-master set-status --id=<id> --status=in-progress
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

### Tag Management
Use `--tag=<tag>` with commands to work within specific project contexts or feature branches.

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

1. **Always check next task**: Use `task-master next` to get AI-recommended priority
2. **Log progress frequently**: Use `update-subtask` to maintain context across sessions
3. **Complete incrementally**: Mark subtasks done as you complete them
4. **Expand complex tasks**: Don't implement large tasks without subtask breakdown
5. **Update status promptly**: Keep status current for accurate dependency tracking
6. **Use detailed prompts**: Provide context when updating or creating tasks

---

*This documentation enables Claude Code to effectively integrate task-master CLI for structured, dependency-aware development workflows.*
Add a new task to Task Master with proper integration and prioritization.

**Step 1: Check Current Task Context**
```bash
task-master list
# Optional: Check other tag contexts
task-master tags
```

**Step 2: Add New Task**
```bash
task-master add-task --prompt="[task description]" --priority=[priority] --dependencies=[ids] --research
# Or add to specific tag context:
task-master add-task --prompt="[task description]" --tag=[tag-name] --priority=[priority] --research
```

**Priority Options:**
- `--priority=high` - Makes this the next task (if no blocking dependencies)
- `--priority=medium` - Standard priority (default)
- `--priority=low` - Lower priority, worked on later

**Dependencies:**
- `--dependencies=1,2,3` - Comma-separated task IDs that must complete first
- If dependencies exist, those become the next tasks instead

**Tag Options (Tagged Lists Support):**
- No `--tag` flag - Adds to current active tag (usually "master")
- `--tag=feature-xyz` - Adds to specific existing tag
- `--tag=new-feature` - Creates new tag if it doesn't exist
- `--tag=experiment` - Isolate experimental tasks from main workflow

**Step 3: Analyze Task Complexity**
```bash
task-master analyze-complexity --research
task-master complexity-report
```

**Step 4: Expand Task (if needed)**
Based on complexity score:
```bash
# For complexity 4+ or complex tasks
task-master expand --id=<new-task-id> --num=<appropriate-number> --research
```

**Step 5: Verify Task Integration**
```bash
task-master next
# Or check specific tag context:
task-master next --tag=[tag-name]
```
This confirms:
- If your new task is next (high priority, no dependencies)
- Or if dependencies are now the next tasks to work on
- Task placement in correct tag context

**Step 6: Show Task Details**
```bash
task-master show <new-task-id>
# Or view task in specific tag:
task-master show <new-task-id> --tag=[tag-name]
```

**Multi-Context Workflow Examples:**

**Feature Development:**
```bash
# Create feature-specific context with base tasks
task-master add-tag feature-auth --copy-from-current
task-master add-task --tag=feature-auth --prompt="Implement user authentication" --priority=high
```

**Experimentation:**
```bash
# Safe experimentation without affecting main workflow
task-master add-task --tag=experiment --prompt="Test new UI library" --priority=medium
```

**Cross-Context Planning:**
```bash
# Add task to different context for later work
task-master add-task --tag=v2-features --prompt="Advanced search functionality" --priority=low
```

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and best practices.

**Expected Outcome:**
- New task properly integrated into correct tag context
- Correct priority ordering maintained within tag
- Dependencies handled appropriately
- Task ready for tag-aware `/next-task` workflow
- Multi-context development capabilities enabled

Arguments: $ARGUMENTS (task description, priority, dependencies, optional tag)
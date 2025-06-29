Add a new task to Task Master with proper integration and prioritization.

**Step 1: Check Current Task Context**
```bash
task-master list
```

**Step 2: Add New Task**
```bash
task-master add-task --prompt="[task description]" --priority=[priority] --dependencies=[ids] --research
```

**Priority Options:**
- `--priority=high` - Makes this the next task (if no blocking dependencies)
- `--priority=medium` - Standard priority (default)
- `--priority=low` - Lower priority, worked on later

**Dependencies:**
- `--dependencies=1,2,3` - Comma-separated task IDs that must complete first
- If dependencies exist, those become the next tasks instead

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
```
This confirms:
- If your new task is next (high priority, no dependencies)
- Or if dependencies are now the next tasks to work on

**Step 6: Show Task Details**
```bash
task-master show <new-task-id>
```

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and best practices.

**Expected Outcome:**
- New task properly integrated into task workflow
- Correct priority ordering maintained
- Dependencies handled appropriately
- Task ready for `/next-task` workflow

Arguments: $ARGUMENTS (task description, priority, dependencies)
Set up parallel agent development using Tagged Lists for complex tasks.

**Usage:**
```bash
# Quick setup (zero-token approach)
scripts/ultra-fast-multi-agent.sh <task-id> [complexity]

# Manual setup (if needed)
scripts/multi-agent-setup.sh <task-id> <complexity>
```

**Complexity Levels:**
- 1-3: Single agent
- 4-5: Frontend, Backend, Testing
- 6-7: + Architecture, Documentation  
- 8-9: + Integration, Domain specialists

**Agent Context Pattern:**
- `task-<id>-coord` - Main coordination
- `task-<id>-<agent>` - Agent-specific work
- Auto-generated workflow guide: `task-<id>-workflow.md`

**Key Commands:**
```bash
task-master use-tag task-<id>-<agent>    # Switch context
task-master next                         # Get next task
task-master set-status --id=<id> --status=in-progress
```

**Expected Outcome:** Parallel agent development with zero setup token usage.

Arguments: $ARGUMENTS (task id, complexity level)

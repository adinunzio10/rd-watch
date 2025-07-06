Implement tasks using coordinated single-context development with structured agent handoffs.

**Prerequisites:**
- Task identified via `/next-task`
- Complexity analyzed and ready to start

**Setup:**
```bash
task-master set-status --id=<id> --status=in-progress
task-master expand --id=<id> --num=<appropriate-number> --research  # if needed
```

**Agent Assignment by Complexity:**
- **1-3**: Implementation + Testing + Documentation
- **4-6**: Core team + 1-2 domain specialists  
- **7-9**: Core team + multiple specialists + Integration

**Always Include:**
- **Testing Agent**: Create/update tests
- **Documentation Agent**: Update relevant docs

**Coordination Pattern:**
1. Architecture Agent → Backend Agent → Frontend Agent → Testing Agent → Documentation Agent
2. Clear handoffs with validation at each step
3. Use `task-master update-subtask` to log progress

**Quality Gates:**
- Each agent validates previous work
- Integration testing at handoff points
- Complete documentation of changes

**Completion:**
```bash
task-master set-status --id=<id> --status=done
# Create branch, commit, and PR
```

**When to Use:**
- Complexity 1-5 tasks
- Tight integration requirements
- Speed over parallelization
- Unclear requirements needing exploration

Arguments: $ARGUMENTS (task id, complexity level)

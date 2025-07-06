What is our next task?

**Step 1: Get Next Task from Task Master**
```bash
task-master next
```

**Step 2: Get Detailed Task Information**
```bash
task-master show <id>
```

**Step 3: Check and Handle Complexity Analysis**
- If complexity shows "N/A", run complexity analysis:
  ```bash
  task-master analyze-complexity --research
  task-master complexity-report
  ```

**Step 4: Handle Task Expansion (if needed)**
- **If NO subtasks exist:** Expand task based on complexity level:
  ```bash
  task-master expand --id=<id> --num=<appropriate-number> --research
  ```
- **If subtasks exist:** Verify subtask count matches complexity:
  - Complexity 1-3: 2-4 subtasks expected
  - Complexity 4-6: 4-8 subtasks expected  
  - Complexity 7-9: 6+ subtasks expected
- **If setup already complete:** Proceed to implementation

**Step 5: Mark Task as In-Progress**
```bash
task-master set-status --id=<id> --status=in-progress
```

**Analyze Complexity & Strategy:**
Based on final Task Master complexity score:

- **Complexity 1-3**: Core team (Implementation + Testing + Documentation agents)
- **Complexity 4-6**: Extended team (Core team + 1-2 domain specialists)  
- **Complexity 7-9**: Full swarm (Core team + multiple domain specialists + Integration agent)

**Mandatory Agent Assignment (All Tasks):**
- **Testing Agent**: Always create/update tests for implemented features
- **Documentation Agent**: Always update relevant documentation (architecture, API, user-facing)

**Additional Specialist Role Assignment:**
Based on task content and dependencies:
- **Architecture Agent**: Data models, DI patterns, MVVM foundation
- **Frontend Agent**: UI/Compose components, TV focus management
- **Backend Agent**: API integration, networking, data persistence
- **Integration Agent**: Coordination and conflict resolution (complexity 6+)

**Risk Assessment:**
- **Security**: Authentication, data exposure, permissions
- **Breaking Changes**: API modifications, schema changes, dependency updates
- **Performance**: Memory usage, TV focus management, lazy loading
- **Integration**: Dependencies on completed tasks (check task.dependencies array)

**Implementation Plan:**

Based on the task complexity and analysis above, create a detailed plan that includes:

1. **Agent Assignment & Coordination:**
   - List specific agents needed (always include Testing + Documentation agents)
   - Define clear responsibilities for each agent
   - Specify handoff points between agents
   - Include coordination strategy for parallel work

2. **Implementation Sequence:**
   - Break down the task into agent-specific work phases  
   - Define dependencies between agent deliverables
   - Include testing and documentation integration points

3. **Quality Gates:**
   - Specify what each agent must deliver
   - Define acceptance criteria for each phase
   - Include integration testing requirements

4. **Agent Coordination Details:**
   - How agents will communicate progress
   - Conflict resolution approach for overlapping work
   - Final integration and validation process

The plan must be detailed enough that during implementation, the multi-agent approach is clearly followed rather than defaulting to single-agent work.

**During Implementation:**
- Use `task-master update-subtask --id=<subtask-id> --prompt="progress notes"` to log progress
- Mark subtasks complete: `task-master set-status --id=<subtask-id> --status=done`
- Reference `claude-taskmaster.md` for detailed command usage and patterns

**After Implementation:**
- Mark main task complete: `task-master set-status --id=<main-id> --status=done`
- Create a branch for the work you did. Commit/push your work and create a pull request using github cli.

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for proper command usage and workflow integration.

Implement the identified task using traditional coordinated single-context development with structured agent handoffs.

**Prerequisites:**
- Task has been identified using `/next-task`
- Complexity and implementation approach have been analyzed
- Task is ready to start (dependencies completed)

**Step 1: Mark Task as In-Progress**
```bash
task-master set-status --id=<id> --status=in-progress
```

**Step 2: Handle Task Expansion (if needed)**
- **If NO subtasks exist:** Expand task based on complexity level:
  ```bash
  task-master expand --id=<id> --num=<appropriate-number> --research
  ```
- **If subtasks exist:** Verify subtask count matches complexity:
  - Complexity 1-3: 2-4 subtasks expected
  - Complexity 4-6: 4-8 subtasks expected  
  - Complexity 7-9: 6+ subtasks expected
- **If setup already complete:** Proceed to implementation

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
   - Include coordination strategy for sequential work

2. **Implementation Sequence:**
   - Break down the task into agent-specific work phases  
   - Define dependencies between agent deliverables
   - Include testing and documentation integration points

3. **Quality Gates:**
   - Specify what each agent must deliver
   - Define acceptance criteria for each phase
   - Include integration testing requirements

4. **Agent Coordination Details:**
   - How agents will communicate progress within single context
   - Conflict resolution approach for overlapping work
   - Final integration and validation process

The plan must be detailed enough that during implementation, the coordinated agent approach is clearly followed with proper handoffs.

**During Implementation:**
- Use `task-master update-subtask --id=<subtask-id> --prompt="progress notes"` to log progress
- Mark subtasks complete: `task-master set-status --id=<subtask-id> --status=done`
- Reference `claude-taskmaster.md` for detailed command usage and patterns

**Single-Context Agent Coordination:**

**Sequential Development Pattern:**
1. **Architecture Agent** (if needed): Establish foundational patterns
2. **Backend Agent**: Implement data layer and APIs
3. **Frontend Agent**: Build UI components using backend APIs
4. **Testing Agent**: Create comprehensive tests for integrated solution
5. **Documentation Agent**: Document the complete implementation
6. **Integration Agent** (complexity 6+): Final validation and optimization

**Coordinated Handoff Points:**
- Architecture → Backend: Design patterns and data models established
- Backend → Frontend: APIs implemented and documented
- Frontend → Testing: UI components ready for test creation
- Testing → Documentation: Test coverage validates functionality
- Documentation → Integration: Complete solution ready for final review

**Progress Tracking in Single Context:**
```bash
# Log progress for coordinated work
task-master update-subtask --id=1.1 --prompt="Backend API complete, ready for frontend integration"
task-master update-subtask --id=1.2 --prompt="Frontend components implemented, needs testing"
task-master update-subtask --id=1.3 --prompt="Tests passing, documentation in progress"
```

**Quality Assurance Workflow:**
- Each agent validates previous agent's work before proceeding
- Integration points have explicit validation criteria
- Testing agent verifies end-to-end functionality
- Documentation agent ensures all changes are properly documented

**After Implementation:**
- Mark main task complete: `task-master set-status --id=<main-id> --status=done`
- Create a branch for the work you did. Commit/push your work and create a pull request using github cli.

**Single-Agent vs Multi-Agent Trade-offs:**

**Single-Agent Benefits:**
- Simpler coordination and communication
- No context switching overhead
- Easier conflict resolution in single context
- Proven workflow for most development tasks
- Better for tasks with tight integration requirements

**When to Choose Single-Agent:**
- Complexity 1-5 tasks
- Tasks requiring tight integration between components  
- When development speed is more important than parallelization
- Teams new to multi-agent workflows
- Tasks with unclear requirements that need exploration

**Complexity-Specific Approaches:**

**Complexity 1-3 (Simple):**
- Primary agent handles implementation
- Testing agent validates functionality
- Documentation agent updates relevant docs
- Minimal coordination overhead

**Complexity 4-6 (Moderate):**
- Clear agent role separation
- Structured handoff points
- Regular progress validation
- Integration testing emphasis

**Complexity 7-9 (Complex in Single Context):**
- Comprehensive planning phase
- Detailed agent coordination
- Multiple validation checkpoints
- Integration agent manages overall coherence
- Consider if multi-agent approach would be more efficient

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for proper command usage and workflow integration.

**Expected Outcomes:**
- Systematic implementation with clear agent responsibilities
- Proper coordination and handoffs between agents
- Comprehensive testing and documentation
- Single-context development with proven workflow patterns
- Complete implementation ready for code review and deployment

Arguments: $ARGUMENTS (task id, complexity level, coordination preferences)
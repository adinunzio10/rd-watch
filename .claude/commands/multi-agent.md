Set up parallel agent development using Tagged Lists for complex task implementation without conflicts.

**Step 1: Identify Task for Multi-Agent Development**
```bash
task-master next                        # Get next task
task-master show <id>                   # Check task complexity and details
task-master analyze-complexity          # Ensure complexity is analyzed
```

**Complexity-Based Agent Assignment:**
- **Complexity 1-3**: Single agent (use standard workflow)
- **Complexity 4-5**: Core team (Frontend + Backend + Testing)
- **Complexity 6-7**: Extended team (Core + Documentation + Architecture)
- **Complexity 8-9**: Full swarm (Extended + Integration + Domain specialists)

**Step 2: Create Multi-Agent Context Structure**
```bash
# Create coordination context (main task management)
task-master add-tag task-<id>-coord --copy-from-current --description="Task <id> coordination and integration"

# Create agent-specific contexts
task-master add-tag task-<id>-frontend --copy-from=task-<id>-coord --description="Task <id> frontend development"
task-master add-tag task-<id>-backend --copy-from=task-<id>-coord --description="Task <id> backend development"
task-master add-tag task-<id>-testing --copy-from=task-<id>-coord --description="Task <id> testing and validation"

# For complex tasks, add additional specialist contexts
task-master add-tag task-<id>-docs --copy-from=task-<id>-coord --description="Task <id> documentation"
task-master add-tag task-<id>-arch --copy-from=task-<id>-coord --description="Task <id> architecture"
task-master add-tag task-<id>-integration --copy-from=task-<id>-coord --description="Task <id> integration management"
```

**Step 3: Break Down Task into Agent-Specific Work**
```bash
# Switch to coordination context
task-master use-tag task-<id>-coord
task-master expand --id=<id> --research --num=8    # Break into agent-appropriate subtasks

# Assign subtasks to agent contexts
task-master add-task --tag=task-<id>-frontend --prompt="Implement UI components for task <id>" --priority=high
task-master add-task --tag=task-<id>-backend --prompt="Create API endpoints for task <id>" --priority=high
task-master add-task --tag=task-<id>-testing --prompt="Create comprehensive tests for task <id>" --priority=high
task-master add-task --tag=task-<id>-docs --prompt="Document task <id> implementation" --priority=medium
```

**Agent-Specific Workflow Examples:**

**Frontend Agent Context:**
```bash
task-master use-tag task-<id>-frontend
task-master next                        # Get next frontend task

# Frontend-specific tasks
task-master add-task --prompt="Design responsive UI components" --priority=high
task-master add-task --prompt="Implement user interaction handlers" --priority=high
task-master add-task --prompt="Add accessibility features" --priority=medium
task-master add-task --prompt="Create component unit tests" --priority=medium
task-master add-task --prompt="Optimize component performance" --priority=low

# Frontend agent workflow
task-master set-status --id=<frontend-task-id> --status=in-progress
# ... implement frontend work ...
task-master update-subtask --id=<frontend-subtask-id> --prompt="Completed responsive design, needs backend integration"
task-master set-status --id=<frontend-task-id> --status=done
```

**Backend Agent Context:**
```bash
task-master use-tag task-<id>-backend
task-master next                        # Get next backend task

# Backend-specific tasks
task-master add-task --prompt="Design and implement API endpoints" --priority=high
task-master add-task --prompt="Create data models and validation" --priority=high
task-master add-task --prompt="Implement business logic layer" --priority=high
task-master add-task --prompt="Add error handling and logging" --priority=medium
task-master add-task --prompt="Optimize database queries" --priority=medium
task-master add-task --prompt="Create API documentation" --priority=low

# Backend agent workflow
task-master set-status --id=<backend-task-id> --status=in-progress
# ... implement backend work ...
task-master update-subtask --id=<backend-subtask-id> --prompt="API endpoints complete, ready for frontend integration"
task-master set-status --id=<backend-task-id> --status=done
```

**Testing Agent Context:**
```bash
task-master use-tag task-<id>-testing
task-master next                        # Get next testing task

# Testing-specific tasks
task-master add-task --prompt="Create unit tests for all components" --priority=high
task-master add-task --prompt="Implement integration tests" --priority=high
task-master add-task --prompt="Add end-to-end testing scenarios" --priority=medium
task-master add-task --prompt="Performance and load testing" --priority=medium
task-master add-task --prompt="Security testing and validation" --priority=medium
task-master add-task --prompt="Test coverage analysis and reporting" --priority=low

# Testing agent workflow
task-master set-status --id=<testing-task-id> --status=in-progress
# ... implement testing work ...
task-master update-subtask --id=<testing-subtask-id> --prompt="Unit tests complete, integration tests need backend completion"
```

**Step 4: Coordination and Integration Management**
```bash
# Integration agent manages overall progress
task-master use-tag task-<id>-integration
task-master add-task --prompt="Monitor cross-agent dependencies" --priority=high
task-master add-task --prompt="Resolve integration conflicts" --priority=high
task-master add-task --prompt="Coordinate agent handoffs" --priority=high
task-master add-task --prompt="Validate integrated solution" --priority=high
task-master add-task --prompt="Final end-to-end testing" --priority=medium

# Check progress across all agent contexts
task-master list --tag=task-<id>-frontend --status=done
task-master list --tag=task-<id>-backend --status=done
task-master list --tag=task-<id>-testing --status=pending
```

**Step 5: Advanced Multi-Agent Patterns**

**Domain Specialist Contexts:**
```bash
# For specialized domains (complexity 8-9)
task-master add-tag task-<id>-security --copy-from=task-<id>-coord --description="Security implementation"
task-master add-tag task-<id>-performance --copy-from=task-<id>-coord --description="Performance optimization"
task-master add-tag task-<id>-accessibility --copy-from=task-<id>-coord --description="Accessibility compliance"

# Assign domain-specific work
task-master add-task --tag=task-<id>-security --prompt="Implement authentication and authorization" --priority=high
task-master add-task --tag=task-<id>-performance --prompt="Optimize rendering and memory usage" --priority=high
task-master add-task --tag=task-<id>-accessibility --prompt="Ensure full accessibility compliance" --priority=medium
```

**Cross-Agent Dependencies:**
```bash
# Create dependencies between agent contexts
task-master use-tag task-<id>-frontend
task-master add-task --prompt="Integrate with backend API" --dependencies=[backend-api-task-id] --priority=high

task-master use-tag task-<id>-testing
task-master add-task --prompt="Test integrated frontend-backend flow" --dependencies=[frontend-integration-id,backend-api-id] --priority=high
```

**Step 6: Integration and Merge Workflow**

**Phase 1: Individual Agent Completion**
```bash
# Each agent completes their work independently
task-master use-tag task-<id>-frontend && task-master list --status=done
task-master use-tag task-<id>-backend && task-master list --status=done
task-master use-tag task-<id>-testing && task-master list --status=pending
```

**Phase 2: Integration Agent Coordination**
```bash
task-master use-tag task-<id>-integration
task-master add-task --prompt="Merge frontend and backend implementations" --priority=high
task-master add-task --prompt="Resolve any integration conflicts" --priority=high
task-master add-task --prompt="Run comprehensive integration tests" --priority=high
task-master add-task --prompt="Validate all requirements met" --priority=medium

# Integration work
task-master set-status --id=<integration-task-id> --status=in-progress
# ... handle integration conflicts, test integration ...
task-master update-subtask --id=<integration-subtask-id> --prompt="Integration complete, all tests passing"
```

**Phase 3: Final Validation and Cleanup**
```bash
# Final coordination check
task-master use-tag task-<id>-coord
task-master set-status --id=<main-task-id> --status=done

# Document multi-agent work
task-master use-tag task-<id>-docs
task-master add-task --prompt="Document multi-agent implementation approach" --priority=low
task-master add-task --prompt="Create architecture decision records" --priority=low
```

**Step 7: Multi-Agent Context Management**

**Progress Monitoring:**
```bash
# Check progress across all agent contexts
task-master tags | grep "task-<id>"
task-master list --tag=task-<id>-frontend
task-master list --tag=task-<id>-backend  
task-master list --tag=task-<id>-testing
task-master list --tag=task-<id>-integration
```

**Context Switching for Multi-Agent Work:**
```bash
# Quick context switching during development
task-master use-tag task-<id>-frontend    # Focus on frontend work
task-master use-tag task-<id>-backend     # Switch to backend work
task-master use-tag task-<id>-integration # Coordinate integration
task-master use-tag task-<id>-coord       # Overall task management
```

**Step 8: Cleanup and Completion**

**Successful Multi-Agent Task Completion:**
```bash
# Archive agent-specific contexts
task-master copy-tag task-<id>-frontend archive-task-<id>-frontend
task-master copy-tag task-<id>-backend archive-task-<id>-backend
task-master copy-tag task-<id>-testing archive-task-<id>-testing

# Clean up active contexts
task-master delete-tag task-<id>-frontend
task-master delete-tag task-<id>-backend
task-master delete-tag task-<id>-testing
task-master delete-tag task-<id>-integration
task-master delete-tag task-<id>-coord

# Return to main development context
task-master use-tag master
```

**Multi-Agent Complexity Examples:**

**Complexity 4-5 (Core Team):**
```bash
# Standard three-agent setup
task-<id>-frontend    # UI/UX implementation
task-<id>-backend     # API/data layer
task-<id>-testing     # Comprehensive testing
```

**Complexity 6-7 (Extended Team):**
```bash
# Core team + specialists
task-<id>-frontend    # UI/UX implementation
task-<id>-backend     # API/data layer
task-<id>-testing     # Comprehensive testing
task-<id>-docs        # Documentation
task-<id>-arch        # Architecture decisions
```

**Complexity 8-9 (Full Swarm):**
```bash
# Extended team + domain specialists + integration
task-<id>-frontend       # UI/UX implementation
task-<id>-backend        # API/data layer
task-<id>-testing        # Comprehensive testing
task-<id>-docs           # Documentation
task-<id>-arch           # Architecture decisions
task-<id>-security       # Security implementation
task-<id>-performance    # Performance optimization
task-<id>-integration    # Integration management
```

**Expected Outcomes:**
- True parallel agent development without conflicts
- Clear separation of agent responsibilities
- Systematic integration and conflict resolution
- Comprehensive testing across all implementations
- Complete documentation of multi-agent process
- Scalable approach for complex task management

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and Tagged Lists best practices.

Arguments: $ARGUMENTS (task id, complexity level, agent assignment preferences)
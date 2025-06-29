Set up multi-developer task management using Tagged Lists for team collaboration.

**Step 1: Check Current Project Context**
```bash
task-master tags                    # See existing contexts
task-master list                    # Check current project tasks
```

**Step 2: Create Team Member Contexts**
```bash
# Create individual developer contexts
task-master add-tag [developer-name] --copy-from-current --description="[Developer name]'s work context"

# Example team setup:
task-master add-tag alice --copy-from-current --description="Alice's frontend work"
task-master add-tag bob --copy-from-current --description="Bob's backend development"  
task-master add-tag charlie --copy-from-current --description="Charlie's testing and QA"
```

**Step 3: Create Team-Specific Contexts**
```bash
# Create role-based contexts
task-master add-tag frontend-team --copy-from-current --description="Frontend development tasks"
task-master add-tag backend-team --copy-from-current --description="Backend development tasks"
task-master add-tag qa-team --copy-from-current --description="Quality assurance tasks"
task-master add-tag devops-team --description="DevOps and deployment tasks"
```

**Step 4: Assign Tasks to Team Members**
```bash
# Assign tasks to specific developers
task-master add-task --tag=alice --prompt="Implement user profile component" --priority=high
task-master add-task --tag=bob --prompt="Create user authentication API" --priority=high
task-master add-task --tag=charlie --prompt="Write integration tests for auth flow" --priority=medium

# Assign tasks to teams
task-master add-task --tag=frontend-team --prompt="Standardize component styling" --priority=medium
task-master add-task --tag=backend-team --prompt="Optimize database queries" --priority=low
```

**Team Workflow Examples:**

**Feature Team Collaboration:**
```bash
# Create feature-specific team context
task-master add-tag team-auth-feature --copy-from-current --description="Authentication feature team"

# Break down feature into team member tasks
task-master use-tag team-auth-feature
task-master add-task --prompt="UI Components - Alice" --priority=high
task-master add-task --prompt="API Endpoints - Bob" --priority=high  
task-master add-task --prompt="Integration Tests - Charlie" --priority=medium
task-master add-task --prompt="Security Review - David" --priority=medium

# Team members work in their contexts
task-master copy-tag team-auth-feature alice-auth
task-master copy-tag team-auth-feature bob-auth
task-master copy-tag team-auth-feature charlie-auth
```

**Sprint Planning Workflow:**
```bash
# Create sprint-specific contexts
task-master add-tag sprint-24-alice --copy-from=alice --description="Alice's Sprint 24 tasks"
task-master add-tag sprint-24-bob --copy-from=bob --description="Bob's Sprint 24 tasks"

# Plan sprint tasks
task-master use-tag sprint-24-alice
task-master add-task --prompt="Complete user dashboard design" --priority=high
task-master add-task --prompt="Implement responsive navigation" --priority=medium

task-master use-tag sprint-24-bob  
task-master add-task --prompt="Finish payment processing API" --priority=high
task-master add-task --prompt="Add error handling middleware" --priority=medium
```

**Cross-Team Dependencies:**
```bash
# Manage dependencies between team members
task-master use-tag alice
task-master add-task --prompt="Frontend auth UI (depends on Bob's API)" --dependencies=[bob-api-task-id] --priority=high

task-master use-tag bob
task-master add-task --prompt="User API endpoint (blocks Alice's UI)" --priority=high

task-master use-tag charlie
task-master add-task --prompt="Test auth flow (needs Alice+Bob completion)" --dependencies=[alice-ui-task-id,bob-api-task-id] --priority=medium
```

**Step 5: Team Coordination Patterns**

**Daily Standup Preparation:**
```bash
# Each team member checks their context
task-master use-tag alice
task-master list --status=in-progress    # Alice's current work
task-master list --status=done          # Alice's completed work

task-master use-tag bob
task-master list --status=in-progress    # Bob's current work
task-master next                         # Bob's next planned task
```

**Code Review Coordination:**
```bash
# Create review-specific contexts
task-master add-tag review-alice --description="Alice's code reviews"
task-master add-tag review-bob --description="Bob's code reviews"

# Add review tasks
task-master add-task --tag=review-alice --prompt="Review Bob's authentication PR" --priority=high
task-master add-task --tag=review-bob --prompt="Review Alice's UI components PR" --priority=high
```

**Blocking and Unblocking:**
```bash
# Mark blocked tasks with context
task-master use-tag alice
task-master set-status --id=<blocked-task-id> --status=deferred
task-master update-subtask --id=<task-id> --prompt="Blocked waiting for Bob's API completion"

# Unblock when dependencies complete
task-master use-tag bob  
task-master set-status --id=<api-task-id> --status=done
task-master update-subtask --id=<api-task-id> --prompt="API complete, Alice can proceed"

task-master use-tag alice
task-master set-status --id=<blocked-task-id> --status=pending  # Unblock Alice's task
```

**Step 6: Team Lead Coordination**

**Overview of All Team Work:**
```bash
# Team lead checks all contexts
task-master tags                        # See all team member contexts
task-master list --tag=alice           # Alice's tasks
task-master list --tag=bob             # Bob's tasks  
task-master list --tag=charlie         # Charlie's tasks

# Check team progress
task-master list --tag=frontend-team   # Frontend progress
task-master list --tag=backend-team    # Backend progress
```

**Load Balancing:**
```bash
# Check team member workload
task-master list --tag=alice --status=pending    # Alice's pending work
task-master list --tag=bob --status=pending      # Bob's pending work

# Reassign tasks if needed
task-master copy-tag alice bob-extra              # Copy Alice's overload to Bob
task-master use-tag alice
task-master delete-tag alice-overload             # Clean up Alice's context
```

**Step 7: Integration and Merge Workflows**

**Feature Integration:**
```bash
# Merge individual work back to team context
task-master copy-tag alice-auth team-auth-feature
task-master copy-tag bob-auth team-auth-feature  
task-master copy-tag charlie-auth team-auth-feature

# Create integration tasks
task-master use-tag team-auth-feature
task-master add-task --prompt="Integrate all auth components" --priority=high
task-master add-task --prompt="End-to-end auth testing" --priority=high
```

**Release Preparation:**
```bash
# Create release context with all team input
task-master add-tag release-v2 --copy-from=frontend-team
task-master copy-tag backend-team release-v2
task-master copy-tag qa-team release-v2

# Add release-specific tasks
task-master use-tag release-v2
task-master add-task --prompt="Final integration testing" --priority=high
task-master add-task --prompt="Release documentation" --priority=medium
task-master add-task --prompt="Deployment verification" --priority=high
```

**Step 8: Cleanup and Maintenance**

**Regular Cleanup:**
```bash
# Archive completed personal contexts
task-master delete-tag alice-sprint-23   # After sprint completion
task-master delete-tag bob-feature-old   # After feature merge

# Maintain active contexts
task-master rename-tag alice-sprint-24 alice-current
task-master rename-tag bob-sprint-24 bob-current
```

**Onboarding New Team Members:**
```bash
# Create context for new team member
task-master add-tag diana --copy-from=master --description="Diana's onboarding and initial tasks"

# Add onboarding tasks
task-master use-tag diana
task-master add-task --prompt="Set up development environment" --priority=high
task-master add-task --prompt="Review codebase and architecture docs" --priority=high
task-master add-task --prompt="Complete first small feature task" --priority=medium
```

**Remote Team Coordination:**
```bash
# Time zone aware task assignment
task-master add-tag team-us --description="US team tasks"
task-master add-tag team-europe --description="Europe team tasks"
task-master add-tag team-asia --description="Asia team tasks"

# Handoff tasks between time zones
task-master copy-tag team-us team-europe    # Handoff work to Europe
task-master add-task --tag=team-europe --prompt="Continue US team work on feature X" --priority=high
```

**Expected Outcomes:**
- Clear task ownership and responsibility
- Reduced conflicts and coordination overhead  
- Visibility into all team member work
- Efficient dependency management
- Smooth handoffs and collaboration
- Scalable team workflow management

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and Tagged Lists best practices.

Arguments: $ARGUMENTS (team member names, team structure, collaboration pattern)
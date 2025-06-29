Create a feature-specific tag context for isolated development work.

**Step 1: Check Current Context**
```bash
task-master tags                    # See available contexts
task-master list                    # Check current tasks
```

**Step 2: Create Feature Tag Context**
```bash
# Create feature tag with copy of current tasks (recommended)
task-master add-tag feature-[name] --copy-from-current --description="[Feature description]"

# Or create feature tag with copy from specific tag
task-master add-tag feature-[name] --copy-from=master --description="[Feature description]"

# Or create empty feature tag for fresh start
task-master add-tag feature-[name] --description="[Feature description]"
```

**Step 3: Switch to Feature Context**
```bash
task-master use-tag feature-[name]  # Shows next available task after switching
```

**Step 4: Add Feature-Specific Tasks**
```bash
# Add tasks specific to this feature
task-master add-task --prompt="[Feature task 1]" --priority=high --research
task-master add-task --prompt="[Feature task 2]" --priority=medium --research

# Or parse PRD/requirements into feature context
task-master parse-prd feature-spec.txt --tag=feature-[name]
```

**Step 5: Analyze and Expand Feature Tasks**
```bash
task-master analyze-complexity --research   # Generates feature-specific complexity report
task-master next                           # Get next task in feature context
task-master expand --id=<id> --research    # Break down complex feature tasks
```

**Feature Development Workflow Examples:**

**Authentication Feature:**
```bash
task-master add-tag feature-auth --copy-from-current --description="User authentication system"
task-master use-tag feature-auth
task-master add-task --prompt="Implement login UI with validation" --priority=high
task-master add-task --prompt="Add JWT token management" --priority=high
task-master add-task --prompt="Create user session handling" --priority=medium
```

**Search Functionality:**
```bash
task-master add-tag feature-search --copy-from-current --description="Advanced search capabilities"
task-master use-tag feature-search
task-master add-task --prompt="Design search UI components" --priority=high
task-master add-task --prompt="Implement search backend API" --priority=high
task-master add-task --prompt="Add search result filtering" --priority=medium
```

**Performance Optimization:**
```bash
task-master add-tag feature-perf --description="Performance improvements"
task-master use-tag feature-perf
task-master add-task --prompt="Optimize lazy loading" --priority=high
task-master add-task --prompt="Implement caching strategy" --priority=medium
task-master add-task --prompt="Reduce bundle size" --priority=medium
```

**Step 6: Work Within Feature Context**
```bash
# Standard task workflow within feature context
task-master next                          # Get next feature task
task-master set-status --id=<id> --status=in-progress
# ... implement feature ...
task-master set-status --id=<id> --status=done

# Log feature-specific progress
task-master update-subtask --id=<subtask-id> --prompt="Feature implementation notes"
```

**Step 7: Switch Between Contexts**
```bash
# Switch back to main development
task-master use-tag master
task-master next

# Switch to feature work
task-master use-tag feature-[name]
task-master next

# Check all contexts
task-master tags
```

**Step 8: Feature Completion Cleanup**
```bash
# After feature is complete and merged
task-master use-tag master              # Switch back to master
task-master delete-tag feature-[name]   # Clean up completed feature tag
```

**Branch Integration Pattern:**
```bash
# Create feature tag matching git branch
git checkout -b feature/user-auth
task-master add-tag feature-user-auth --from-branch --copy-from-current

# Work in feature context
task-master use-tag feature-user-auth
# ... feature development ...

# After feature completion
git checkout main
task-master use-tag master
task-master delete-tag feature-user-auth
```

**Expected Outcomes:**
- Isolated feature development context
- Feature-specific task organization
- Clean separation from main development workflow
- Easy context switching between feature and main work
- Feature-specific complexity analysis and reporting

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and Tagged Lists best practices.

Arguments: $ARGUMENTS (feature name, description, base context)
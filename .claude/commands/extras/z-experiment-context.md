Create safe experimentation contexts for testing new approaches without affecting main development.

**Step 1: Check Current Context**
```bash
task-master tags                    # See available contexts
task-master list                    # Check current stable tasks
```

**Step 2: Create Experiment Context**
```bash
# Create experiment context for safe testing
task-master add-tag experiment --description="Safe experimentation context"

# Or create specific experiment context
task-master add-tag experiment-[type] --description="[Experiment description]"

# Copy current tasks if experiment builds on existing work
task-master add-tag experiment-[type] --copy-from-current --description="[Experiment description]"
```

**Step 3: Switch to Experiment Context**
```bash
task-master use-tag experiment          # Switch to general experiments
# Or:
task-master use-tag experiment-[type]   # Switch to specific experiment
```

**Step 4: Add Experimental Tasks**
```bash
# Add experimental tasks with clear scope
task-master add-task --prompt="[Experimental approach description]" --priority=high
task-master add-task --prompt="Measure performance impact" --priority=medium
task-master add-task --prompt="Document experiment results" --priority=medium
```

**Experiment Types & Examples:**

**Performance Testing:**
```bash
# Create performance experiment context
task-master add-tag experiment-perf --description="Performance optimization experiments"
task-master use-tag experiment-perf

# Add performance-focused tasks
task-master add-task --prompt="Test lazy loading implementation" --priority=high
task-master add-task --prompt="Benchmark memory usage improvements" --priority=high
task-master add-task --prompt="Compare bundle size optimizations" --priority=medium
task-master add-task --prompt="Measure UI responsiveness changes" --priority=medium
```

**UI/UX Experimentation:**
```bash
# Create UI experiment context
task-master add-tag experiment-ui --description="UI component and interaction experiments"
task-master use-tag experiment-ui

# Add UI experimentation tasks
task-master add-task --prompt="Test new navigation component design" --priority=high
task-master add-task --prompt="Experiment with focus management approach" --priority=high
task-master add-task --prompt="Try alternative color scheme" --priority=low
task-master add-task --prompt="Test gesture controls" --priority=medium
```

**Architecture Experiments:**
```bash
# Create architecture experiment context
task-master add-tag experiment-arch --description="Architecture and design pattern experiments"
task-master use-tag experiment-arch

# Add architecture experimentation tasks
task-master add-task --prompt="Test new state management approach" --priority=high
task-master add-task --prompt="Experiment with different data flow pattern" --priority=high
task-master add-task --prompt="Try alternative dependency injection setup" --priority=medium
task-master add-task --prompt="Test modular architecture refactor" --priority=medium
```

**Technology Evaluation:**
```bash
# Create tech evaluation context
task-master add-tag experiment-tech --description="New technology and library evaluation"
task-master use-tag experiment-tech

# Add technology evaluation tasks
task-master add-task --prompt="Evaluate new Compose library features" --priority=high
task-master add-task --prompt="Test alternative networking library" --priority=medium
task-master add-task --prompt="Experiment with new build tool configuration" --priority=low
task-master add-task --prompt="Assess migration to newer SDK version" --priority=medium
```

**Step 5: Experimental Development Workflow**
```bash
# Work through experiments systematically
task-master next                    # Get next experiment task
task-master expand --id=<id>        # Break down complex experiments
task-master set-status --id=<id> --status=in-progress

# Log experimental findings
task-master update-subtask --id=<subtask-id> --prompt="Experiment shows 40% performance improvement"
task-master update-subtask --id=<subtask-id> --prompt="Approach failed: memory usage too high"
```

**Step 6: Experiment Evaluation**
```bash
# Document experiment outcomes
task-master add-task --prompt="Document experiment results and recommendations" --priority=high
task-master add-task --prompt="Create implementation plan if experiment successful" --priority=medium

# Analyze experimental complexity
task-master analyze-complexity      # Generates experiment-specific complexity report
```

**Step 7: Experiment Resolution**

**Successful Experiment - Integration:**
```bash
# Move successful experimental tasks to main context
task-master copy-tag experiment-[type] implementation-[type]
task-master use-tag master
task-master add-task --prompt="Integrate successful [experiment] results" --priority=high --dependencies=[relevant-task-ids]
```

**Failed Experiment - Documentation:**
```bash
# Document failed experiment for future reference
task-master add-task --prompt="Document failed [experiment] approach and lessons learned" --priority=low
task-master set-status --id=<failed-experiment-id> --status=cancelled
```

**Step 8: Context Management**
```bash
# Switch between experiment and main work
task-master use-tag master              # Back to stable development
task-master use-tag experiment-[type]   # Back to experiments

# Check all experimental contexts
task-master tags | grep experiment

# Clean up completed experiments
task-master delete-tag experiment-[type]  # After experiment is resolved
```

**Parallel Experimentation Pattern:**
```bash
# Run multiple experiments in parallel
task-master add-tag experiment-ui --description="UI experiments"
task-master add-tag experiment-perf --description="Performance experiments"
task-master add-tag experiment-arch --description="Architecture experiments"

# Work on different experiments based on focus
task-master use-tag experiment-ui    # Morning: UI work
task-master use-tag experiment-perf  # Afternoon: Performance work
task-master use-tag master          # Evening: Stable development
```

**Risk Management Examples:**

**High-Risk Experiments:**
```bash
# Create isolated context for risky changes
task-master add-tag experiment-risky --description="High-risk architectural changes"
task-master add-task --tag=experiment-risky --prompt="Complete rewrite of core module" --priority=high
# No copy-from-current to avoid contaminating stable work
```

**Low-Risk Experiments:**
```bash
# Create context with current tasks for safe experiments
task-master add-tag experiment-safe --copy-from-current --description="Safe UI improvements"
task-master add-task --tag=experiment-safe --prompt="Test minor color adjustments" --priority=medium
```

**Expected Outcomes:**
- Safe experimentation without risk to main development
- Clear separation of experimental and stable work
- Systematic evaluation of new approaches
- Documentation of experiment results (successful and failed)
- Easy cleanup of experimental contexts
- Parallel experimentation capabilities

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and Tagged Lists best practices.

Arguments: $ARGUMENTS (experiment type, description, risk level)
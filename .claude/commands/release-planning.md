Organize and manage release-specific tasks using version-tagged contexts for systematic release planning.

**Step 1: Check Current Release Context**
```bash
task-master tags                        # See existing version contexts
task-master list                        # Check current development state
task-master analyze-complexity          # Understand current complexity
```

**Step 2: Create Release Tag Context**
```bash
# Create version-specific context
task-master add-tag v[X.Y] --description="Version [X.Y] release planning"

# Copy current stable tasks to release context
task-master add-tag v[X.Y] --copy-from-current --description="Version [X.Y] with current tasks"

# Create major version context from master
task-master add-tag v[X.0] --copy-from=master --description="Major version [X.0] release"

# Create patch release context
task-master add-tag v[X.Y.Z] --copy-from=v[X.Y] --description="Patch release [X.Y.Z]"
```

**Step 3: Switch to Release Context**
```bash
task-master use-tag v[X.Y]              # Switch to release context
task-master next                        # See next release task
```

**Release Planning Examples:**

**Major Version Release (v2.0):**
```bash
# Create major version context
task-master add-tag v2.0 --copy-from-current --description="Major release v2.0 - new architecture"

# Add major version tasks
task-master use-tag v2.0
task-master add-task --prompt="Complete architectural refactor" --priority=high
task-master add-task --prompt="Migrate to new UI framework" --priority=high
task-master add-task --prompt="Breaking changes documentation" --priority=high
task-master add-task --prompt="Migration guide for users" --priority=medium
task-master add-task --prompt="Performance testing for new architecture" --priority=high
task-master add-task --prompt="Beta release testing" --priority=medium
```

**Minor Version Release (v1.5):**
```bash
# Create minor version context
task-master add-tag v1.5 --copy-from-current --description="Minor release v1.5 - new features"

# Add feature completion tasks
task-master use-tag v1.5
task-master add-task --prompt="Complete advanced search feature" --priority=high
task-master add-task --prompt="Add user preference settings" --priority=high
task-master add-task --prompt="Implement offline mode" --priority=medium
task-master add-task --prompt="Add analytics dashboard" --priority=medium
task-master add-task --prompt="Feature documentation updates" --priority=low
```

**Patch Release (v1.4.3):**
```bash
# Create patch context for critical fixes
task-master add-tag v1.4.3 --description="Patch release v1.4.3 - critical fixes"

# Add critical fix tasks
task-master use-tag v1.4.3
task-master add-task --prompt="Fix memory leak in video player" --priority=high
task-master add-task --prompt="Resolve authentication timeout issue" --priority=high
task-master add-task --prompt="Fix crashes on older Android versions" --priority=high
task-master add-task --prompt="Update security dependencies" --priority=medium
```

**Step 4: Release Task Categories**

**Development Completion Tasks:**
```bash
task-master use-tag v[X.Y]
task-master add-task --prompt="Complete all planned features" --priority=high
task-master add-task --prompt="Resolve all critical bugs" --priority=high
task-master add-task --prompt="Code review completion" --priority=high
task-master add-task --prompt="Refactor legacy code modules" --priority=medium
```

**Quality Assurance Tasks:**
```bash
task-master add-task --prompt="Comprehensive regression testing" --priority=high
task-master add-task --prompt="Performance testing and optimization" --priority=high
task-master add-task --prompt="Security audit and fixes" --priority=high
task-master add-task --prompt="Device compatibility testing" --priority=medium
task-master add-task --prompt="Load testing for release scale" --priority=medium
```

**Documentation Tasks:**
```bash
task-master add-task --prompt="Update API documentation" --priority=medium
task-master add-task --prompt="Write release notes" --priority=high
task-master add-task --prompt="Update user guides" --priority=medium
task-master add-task --prompt="Create migration documentation" --priority=medium
task-master add-task --prompt="Update developer setup docs" --priority=low
```

**Deployment Tasks:**
```bash
task-master add-task --prompt="Prepare release build configuration" --priority=high
task-master add-task --prompt="Set up release deployment pipeline" --priority=high
task-master add-task --prompt="Configure release monitoring" --priority=medium
task-master add-task --prompt="Prepare rollback procedures" --priority=medium
task-master add-task --prompt="Schedule release deployment" --priority=low
```

**Step 5: Multi-Version Management**

**Parallel Version Development:**
```bash
# Work on multiple versions simultaneously
task-master add-tag v1.6 --copy-from=master --description="Next minor release"
task-master add-tag v2.0 --description="Major version planning"
task-master add-tag hotfix-v1.5.2 --description="Critical hotfix"

# Switch between versions as needed
task-master use-tag v1.6        # Current development
task-master use-tag v2.0        # Future planning
task-master use-tag hotfix-v1.5.2 # Critical fixes
```

**Version Dependency Management:**
```bash
# Create dependencies between versions
task-master use-tag v2.0
task-master add-task --prompt="Architecture refactor for v2.0" --dependencies=[v1.6-completion-tasks] --priority=high

# Check version readiness
task-master list --tag=v1.6 --status=pending    # Tasks blocking v1.6
task-master list --tag=v2.0 --status=ready      # Tasks ready for v2.0
```

**Step 6: Release Workflow Management**

**Pre-Release Checklist:**
```bash
task-master use-tag v[X.Y]
task-master add-task --prompt="Feature freeze enforcement" --priority=high
task-master add-task --prompt="Version number updates" --priority=high
task-master add-task --prompt="Changelog generation" --priority=high
task-master add-task --prompt="Release candidate build" --priority=high
task-master add-task --prompt="Release candidate testing" --priority=high
```

**Release Day Tasks:**
```bash
task-master add-task --prompt="Final build verification" --priority=high
task-master add-task --prompt="Deploy to production" --priority=high
task-master add-task --prompt="Monitor deployment health" --priority=high
task-master add-task --prompt="Announce release to users" --priority=medium
task-master add-task --prompt="Update marketing materials" --priority=low
```

**Post-Release Tasks:**
```bash
task-master add-task --prompt="Monitor production metrics" --priority=high
task-master add-task --prompt="Address user feedback" --priority=medium
task-master add-task --prompt="Document lessons learned" --priority=low
task-master add-task --prompt="Plan next release cycle" --priority=low
```

**Step 7: Emergency Release Management**

**Hotfix Release Process:**
```bash
# Create emergency hotfix context
task-master add-tag hotfix-v[X.Y.Z] --description="Emergency hotfix for critical issue"

# Add critical fix tasks
task-master use-tag hotfix-v[X.Y.Z]
task-master add-task --prompt="Fix critical security vulnerability" --priority=high
task-master add-task --prompt="Emergency testing verification" --priority=high
task-master add-task --prompt="Fast-track deployment" --priority=high
task-master add-task --prompt="Post-fix monitoring" --priority=high
```

**Rollback Planning:**
```bash
task-master add-task --prompt="Prepare rollback procedures" --priority=high
task-master add-task --prompt="Test rollback process" --priority=medium
task-master add-task --prompt="Document rollback triggers" --priority=medium
```

**Step 8: Release Completion and Cleanup**

**Version Release Completion:**
```bash
# Mark release tasks complete
task-master use-tag v[X.Y]
task-master set-status --id=<release-task-id> --status=done

# Document release completion
task-master add-task --prompt="Archive release documentation" --priority=low
task-master add-task --prompt="Clean up temporary release assets" --priority=low
```

**Context Cleanup:**
```bash
# Archive completed release contexts
task-master copy-tag v[X.Y] archive-v[X.Y]      # Archive for reference
task-master delete-tag v[X.Y]                   # Clean up active context

# Prepare for next release
task-master add-tag v[X.Y+1] --copy-from=master --description="Next release planning"
```

**Step 9: Release Metrics and Analysis**

**Release Planning Analysis:**
```bash
# Generate release-specific complexity reports
task-master analyze-complexity --tag=v[X.Y] --research
task-master complexity-report --tag=v[X.Y]

# Review release task distribution
task-master list --tag=v[X.Y] --with-subtasks
```

**Cross-Release Learning:**
```bash
# Compare release complexity across versions
# Files generated: task-complexity-report_v[X.Y].json
task-master add-task --prompt="Compare v[X.Y] vs v[X.Y-1] complexity" --priority=low
task-master add-task --prompt="Document release process improvements" --priority=low
```

**Expected Outcomes:**
- Systematic release planning and execution
- Clear version-specific task organization
- Parallel version development capability
- Emergency release management procedures
- Release quality assurance workflows
- Historical release data for planning improvements

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and Tagged Lists best practices.

Arguments: $ARGUMENTS (version number, release type, timeline)
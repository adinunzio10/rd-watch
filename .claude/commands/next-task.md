What is our next task?

**Step 1: Check Context & Get Next Task**
```bash
# Check current tag context and available tags
task-master tags

# Get next task from current active tag
task-master next

# Or get next task from specific tag context
task-master next --tag=[tag-name]
```

**Step 2: Get Detailed Task Information**
```bash
task-master show <id>
# Or show task from specific tag:
task-master show <id> --tag=[tag-name]
```

**Step 3: Analyze Task Complexity**
```bash
# If complexity shows "N/A", run complexity analysis
task-master analyze-complexity --research
task-master complexity-report
# Tag-specific complexity reports are automatically generated
# For specific tag: task-complexity-report_[tag-name].json will be created
```

**Step 4: Evaluate Implementation Approach**

**Complexity-Based Recommendations:**

**Complexity 1-3 (Simple Tasks):**
- **Recommendation**: Single-agent approach
- **Reason**: Straightforward implementation, minimal coordination needed
- **Suggested command**: `/single-agent`

**Complexity 4-5 (Moderate Tasks):**
- **Recommendation**: Consider multi-agent for parallel development
- **Core team**: Frontend + Backend + Testing agents
- **Suggested commands**: `/single-agent` or `/multi-agent` based on preference

**Complexity 6-7 (Complex Tasks):**
- **Recommendation**: Multi-agent approach preferred
- **Extended team**: Core team + Documentation + Architecture agents
- **Reason**: Benefits from parallel specialized development
- **Suggested command**: `/multi-agent`

**Complexity 8-9 (Very Complex Tasks):**
- **Recommendation**: Multi-agent approach required
- **Full team**: Extended team + Domain specialists + Integration agent
- **Reason**: Too complex for single-agent coordination
- **Suggested command**: `/multi-agent`

**Step 5: Check Dependencies and Readiness**
```bash
# Verify task dependencies are complete
task-master list --status=done  # Check completed dependencies
task-master show <id>           # Review task dependencies

# Check if task is ready to start
task-master next                # Confirms this is the recommended next task
```

**Step 6: Implementation Decision**

**Based on complexity analysis and recommendations above:**

**For Simple to Moderate Tasks (Complexity 1-5):**
- Use `/single-agent` for traditional coordinated development in single context
- All agents work together with structured handoffs
- Proven approach for most development tasks

**For Complex Tasks (Complexity 6+):**
- Use `/multi-agent` for parallel development in isolated tag contexts
- Each agent works independently to prevent conflicts
- Integration agent coordinates final delivery
- Better scaling for complex implementations

**Task Summary Output Format:**
```
Task: [ID] - [Title]
Complexity: [Score] (Analysis: [brief-description])
Dependencies: [IDs] ([status])
Priority: [level]
Recommendation: [single-agent/multi-agent] approach
Reason: [complexity-justification]
Ready to start: [Yes/No]
```

**Next Steps:**
- Choose implementation approach based on complexity and preference
- Use `/single-agent` for coordinated single-context development
- Use `/multi-agent` for parallel multi-context development

**Context Switching Examples:**

**Working in Feature Context:**
```bash
task-master use-tag feature-auth
task-master next  # Get next task in feature context
# ... analyze complexity and choose approach ...
```

**Cross-Context Task Planning:**
```bash
task-master next --tag=master           # Check main development
task-master next --tag=feature-search   # Check feature work
task-master next --tag=experiment       # Check experimental work
# Choose context and task based on priority
```

**Task-Master Reference:**
Follow the patterns in `claude-taskmaster.md` for detailed command usage and Tagged Lists best practices.

**Expected Outcome:**
- Clear identification of next prioritized task
- Complexity-based implementation recommendations  
- Dependency verification and readiness assessment
- Informed decision between single-agent and multi-agent approaches

Arguments: $ARGUMENTS (optional tag context)
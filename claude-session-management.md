# Claude Session Management

Best practices for effective Claude Code development sessions, based on successful debugging and development patterns.

## Session Planning and Structure

### Effective Session Initiation

**Start Every Session with Context Gathering**
1. **Git Status Review**: Understand current branch and uncommitted changes
2. **Recent History**: Review last 5-10 commits for context
3. **Current State**: Check build status and any existing issues
4. **User Goal**: Clearly understand the specific problem or task

**Example Opening Sequence:**
```bash
git status                    # Current working state
git log --oneline -10        # Recent changes context
./lint-summary.sh            # Current code quality status
```

### Task Complexity Assessment

**Use TodoWrite for Multi-Step Tasks**

**When to Use TodoWrite:**
- Tasks requiring 3+ distinct steps
- Complex debugging with multiple investigation areas
- Feature implementation with multiple components
- Any task where progress tracking adds value

**When NOT to Use TodoWrite:**
- Single, straightforward changes
- Quick fixes or minor adjustments
- Simple informational queries

**Effective Task Breakdown Example:**
```
High Priority Tasks:
1. Add comprehensive logging to track API patterns
2. Fix flow collection race conditions  
3. Implement request deduplication
4. Add proper job cancellation
5. Test fixes and verify resolution

Medium Priority:
6. Update documentation with findings
7. Optimize performance based on learnings
```

## Momentum and Context Maintenance

### Maintaining Investigation Flow

**Systematic Approach to Complex Problems:**
1. **Gather Evidence**: Logs, git history, code patterns
2. **Form Hypothesis**: Based on evidence and patterns
3. **Test Hypothesis**: Add logging, make targeted changes
4. **Verify Results**: Compilation, testing, log analysis
5. **Document Findings**: Update TodoWrite, prepare for next steps

**Avoid Context Switching:**
- Complete investigation phases before moving to implementation
- Finish related tasks together (e.g., all logging additions at once)
- Document findings immediately while context is fresh

### Progress Tracking Patterns

**TodoWrite Best Practices:**
- Mark tasks as `in_progress` BEFORE starting work
- Update to `completed` IMMEDIATELY after finishing
- Add new tasks as they're discovered during work
- Use priority levels to guide work order

**Example Progress Flow:**
```
Start: 8 tasks pending
→ Mark task 1 as in_progress
→ Complete task 1, mark completed
→ Discover 2 new tasks from task 1 work
→ Add new tasks to list
→ Move to task 2
```

## Decision Making Patterns

### When to Use Planning Mode

**Use Planning Mode For:**
- Major architectural changes
- Complex multi-file refactoring
- Tasks requiring careful coordination
- When user specifically requests planning

**Direct Execution For:**
- Debugging and investigation
- Targeted bug fixes
- Adding logging or comments
- Build and testing operations

### Problem-Solving Approach

**Root Cause vs. Symptom Focus:**
- Always dig deeper than the surface issue
- Look for patterns across multiple failures
- Trace complete data flows from source to UI
- Consider timing, concurrency, and lifecycle issues

**Example: "Episodes showing 0" Problem:**
- Symptom: UI displays wrong counts
- Investigation: API calls, database operations, state management  
- Root Cause: Database entity mapping losing episode data
- Solution: Fix mapping, not UI display logic

## Communication and Documentation

### Effective Problem Communication

**When Receiving Problem Reports:**
1. Clarify the specific symptoms observed
2. Understand the expected vs. actual behavior
3. Gather any relevant logs or error messages
4. Identify when the problem started (git history context)

**Example Response Pattern:**
```
"I see the issue with TV episodes not loading. Let me:
1. Check recent commits for context
2. Add logging to trace the data flow  
3. Identify where the episode data is being lost
4. Fix the root cause and verify the solution"
```

### Commit Message Excellence

**Effective Commit Structure:**
```
fix: brief summary of what was fixed

Detailed explanation of:
- What the problem was (symptoms observed)
- What the root cause was (technical details)
- What the solution does (specific changes)
- Why this approach was chosen

Example logs showing before/after behavior

🤖 Generated with [Claude Code](https://claude.ai/code)
Co-Authored-By: Claude <noreply@anthropic.com>
```

**Include Context for Future Debugging:**
- Link symptoms to root causes
- Explain why other approaches were rejected
- Provide debugging breadcrumbs for similar future issues

## Session Quality Indicators

### Signs of an Effective Session

**Technical Indicators:**
- [ ] Root causes identified, not just symptoms treated
- [ ] Comprehensive logging added for future debugging
- [ ] Changes tested and verified with compilation/linting
- [ ] Multiple related issues addressed systematically

**Process Indicators:**
- [ ] TodoWrite used effectively for task tracking
- [ ] Git history provided valuable context
- [ ] Investigation was methodical and documented
- [ ] Solution addresses the fundamental problem

**Communication Indicators:**
- [ ] User's actual needs were understood and addressed
- [ ] Technical explanations were clear and actionable
- [ ] Next steps and verification approaches were provided
- [ ] Documentation was updated appropriately

### Session Anti-Patterns to Avoid

**Technical Anti-Patterns:**
- Making changes without understanding the problem
- Fixing symptoms while ignoring root causes
- Not testing changes before committing
- Adding complexity without clear benefit

**Process Anti-Patterns:**
- Working on multiple unrelated issues simultaneously
- Not tracking progress on complex tasks
- Losing momentum due to poor task breakdown
- Making assumptions without evidence

**Communication Anti-Patterns:**
- Not clarifying user requirements before starting
- Providing solutions without explaining the problem
- Moving too quickly without ensuring understanding
- Not documenting findings for future reference

## Session Recovery and Optimization

### When Sessions Get Stuck

**Debugging Investigation Stalls:**
1. Step back and review TodoWrite progress
2. Return to git history and log analysis
3. Add more comprehensive logging
4. Break down complex tasks into smaller steps

**Implementation Blocks:**
1. Verify current changes compile and lint
2. Commit working progress to avoid losing work
3. Test one small change at a time
4. Return to root cause analysis if needed

### Continuous Improvement

**After Each Successful Session:**
- Note what approaches worked well
- Identify any process improvements needed
- Update documentation with new patterns discovered
- Consider how similar problems could be prevented

**Example Session Retrospective:**
```
What Worked Well:
- Starting with git history provided crucial context
- TodoWrite kept complex debugging organized
- Comprehensive logging revealed the actual root cause
- Systematic testing prevented regression

Improvements for Next Time:
- Could have added logging earlier in the process
- Should have checked database mapping issues sooner
- More frequent compilation checks during development
```

This systematic approach to session management helps maintain effectiveness and momentum while ensuring thorough problem resolution.
# Claude Debugging Methodology

This document captures the systematic debugging approach that leads to effective problem resolution, based on successful debugging sessions.

## Core Debugging Principles

### 1. Start with Historical Context
**Always begin with git history analysis**
```bash
git log --oneline -10           # Recent commits
git show HEAD --stat            # Latest changes
git diff HEAD~1..HEAD          # Specific changes
```

**Why this works:**
- Provides context for what changed recently
- Identifies patterns in previous fix attempts
- Reveals the evolution of problems and solutions
- Helps avoid repeating failed approaches

### 2. Systematic Problem Analysis

**Use TodoWrite for Complex Issues**
- Break multi-step debugging into tracked tasks
- Mark tasks as in_progress/completed to maintain momentum
- Provides clear progress visibility
- Prevents losing track of partially completed work

**Root Cause vs Symptom Analysis**
- Don't just fix what's broken - understand WHY it's broken
- Look for patterns across multiple failures
- Trace data flow from source to UI to identify breakdown points
- Example: "TV shows show 0 episodes" → dig deeper → database mapping issue

### 3. Comprehensive Logging Strategy

**Add Logging Before Making Assumptions**
```kotlin
android.util.Log.d("Component", "=== Debug Section ===")
android.util.Log.d("Component", "Variable: $value")
android.util.Log.d("Component", "State: ${object.property}")
```

**Key Logging Patterns:**
- API call tracking: `"API CALL: Fetching X for Y"`
- State transitions: `"State changing from X to Y"`
- Data validation: `"Input: X, Output: Y, Valid: Z"`
- Flow control: `"Entering/Exiting method with state X"`

### 4. Methodical Investigation Process

**Phase 1: Understand the Problem**
1. Analyze logs for patterns and sequences
2. Review recent git history for context
3. Identify all components involved in the data flow
4. Create TodoWrite tasks for investigation areas

**Phase 2: Isolate the Root Cause**
1. Add comprehensive logging to suspected areas
2. Trace data from API → Repository → ViewModel → UI
3. Look for data transformation/mapping issues
4. Check for race conditions and flow collection problems

**Phase 3: Implement and Verify Fixes**
1. Fix root cause, not just symptoms
2. Test compilation and linting
3. Verify fixes with comprehensive logging
4. Document the solution in commit messages

## Common Anti-Patterns to Avoid

### 1. Premature Solutions
- ❌ Making changes before understanding the problem
- ❌ Fixing symptoms without investigating root cause
- ❌ Assuming the problem is where you first look

### 2. Insufficient Context
- ❌ Ignoring git history and recent changes
- ❌ Not understanding the complete data flow
- ❌ Missing dependencies between components

### 3. Poor Progress Tracking
- ❌ Working on multiple issues simultaneously without tracking
- ❌ Not documenting investigation findings
- ❌ Losing track of what's been tried vs what worked

## Debugging Session Checklist

### Before Starting
- [ ] Review git history for context
- [ ] Understand the complete user workflow
- [ ] Create TodoWrite tasks for complex issues
- [ ] Identify all components in the data flow

### During Investigation
- [ ] Add comprehensive logging to suspected areas
- [ ] Test one change at a time
- [ ] Document findings as you discover them
- [ ] Update TodoWrite progress regularly

### Before Committing
- [ ] Verify fixes with compilation and linting
- [ ] Test the complete user workflow
- [ ] Write descriptive commit messages
- [ ] Mark TodoWrite tasks as completed

## Example: Successful Debugging Session

**Problem**: TV shows showing "0 episodes" despite successful API responses

**Investigation Process:**
1. **Git History**: Found recent commits attempting to fix episode loading
2. **Log Analysis**: Discovered pattern: API success → Database save → Load returns empty
3. **Root Cause**: Database entity mapping only stored season names, lost episode data
4. **Solution**: Enhanced entity mapping to preserve season metadata
5. **Verification**: Added logging to confirm data persistence

**Key Success Factors:**
- Started with historical context
- Used TodoWrite to track progress
- Added comprehensive logging
- Found actual root cause vs treating symptoms
- Verified fix with proper testing

This systematic approach led to identifying and fixing the fundamental database mapping issue that was causing the episode loading problems.
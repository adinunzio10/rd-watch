Analyze all GitHub issues and identify the top 5 that should be prioritized ASAP.

**Step 1: Fetch All Open Issues**
```bash
gh issue list --state open --limit 100 --json number,title,labels,assignees,createdAt,updatedAt,body,milestone,state
```

**Step 2: Analyze and Prioritize**
Evaluate each issue based on:
- **Impact**: User-blocking bugs, core functionality, accessibility
- **Urgency**: Production blockers, regressions, security issues  
- **Complexity**: Implementation difficulty and risk
- **Dependencies**: Blocks other work or affects multiple systems

**Step 3: Categorize Issues**
- 🔥 **Critical**: User-blocking bugs, production issues
- 📋 **High**: Important features, significant bugs
- 🔧 **Medium**: Improvements, minor bugs
- 📚 **Low**: Documentation, tech debt

**Step 4: Provide Top 5 Recommendations**
For each issue, include:
- Issue number and title
- Priority emoji and rationale
- Brief impact description
- Why it should be done ASAP

**Prioritization Criteria:**
1. **User-blocking bugs** (infinite loading, crashes, navigation breaks)
2. **Resource leaks** (memory, audio continuing after exit)
3. **Data consistency issues** (incorrect state, sync problems)  
4. **Accessibility problems** (focus navigation, screen reader issues)
5. **Production readiness** (missing tests for new features)

**Output Format:**
```
## Top 5 Issues for Immediate Action

### 1. #XXX - Issue Title 🔥
Brief description of why this is critical

### 2. #XXX - Issue Title 🔥  
Brief description of impact

### 3. #XXX - Issue Title 📋
Brief description of importance

### 4. #XXX - Issue Title 📋
Brief description of priority

### 5. #XXX - Issue Title 📋
Brief description of rationale
```

Focus on issues that directly impact user experience and app stability.
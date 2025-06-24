Execute current task using Task Master integrated multi-agent strategy.

**Get Task Details:**
```bash
TASK_ID=$(task-master next | grep "id:" | cut -d: -f2 | tr -d ' ')
task-master show $TASK_ID
```

**Branch Creation & Start:**
```bash
git checkout -b task/$TASK_ID-[short-description]
task-master set-status --id=$TASK_ID --status=in-progress
```

**Execution Strategy Based on Complexity:**

## Single Agent (Complexity 1-3)
- Work through subtasks sequentially
- Follow existing patterns from CLAUDE-architecture.md
- Commit after each subtask completion:
  ```bash
  git add . && git commit -m "feat(task-$TASK_ID): complete subtask [id] - [description]"
  task-master set-status --id=$TASK_ID.[subtask-id] --status=done
  ```

## Domain Specialists (Complexity 4-6)
- **Spawn 2-3 specialist agents** using Task tool based on subtask domains
- **Coordinate parallel work** with clear handoffs between specialists
- **Track progress** through Task Master CLI as agents complete work
- **Integrate outputs** ensuring consistency across specialist contributions

**Example orchestration:**
```
Agent 1 (Architecture): Handles data models and DI setup
Agent 2 (Frontend): Implements UI components and TV focus
Agent 3 (Testing): Creates comprehensive test coverage
```

## Full Swarm (Complexity 7-9)
- **Spawn 4+ specialist agents** using Task tool for maximum parallelization
- **Architecture agent establishes interfaces** and contracts first
- **Specialists work in parallel** with regular sync checkpoints
- **Integration agent manages conflicts** and ensures overall cohesion
- **Commit logical groupings** with clear agent attribution in messages

**Quality Gates (All Strategies):**
- Follow patterns in CLAUDE-architecture.md and related docs
- Ensure tests pass: `./gradlew build && ./gradlew lint`
- Update documentation when making architectural changes
- Maintain focus on Android TV-specific considerations

**Task Completion & PR:**
```bash
# Mark task complete
task-master set-status --id=$TASK_ID --status=done

# Push and create PR
git push -u origin task/$TASK_ID-[description]
gh pr create \
  --title "Task $TASK_ID: [task title]" \
  --body "## Summary
Implements Task $TASK_ID with [strategy used]

## Approach
[Single agent | Domain specialists | Full swarm]

## Agent Contributions
[List agent roles and contributions if multi-agent]

## Testing
- [x] Build passes: \`./gradlew build\`
- [x] Lint passes: \`./gradlew lint\`
- [x] TV focus navigation tested
- [x] Task Master status updated

Task Master Reference: $TASK_ID"
```

**Multi-Agent Coordination Notes:**
- Use Task tool to spawn specialist agents with specific domain expertise
- Ensure each agent understands project architecture and TV-specific requirements
- Coordinate handoffs between agents (e.g., Architecture → Frontend → Testing)
- Integration agent responsible for final quality assurance and conflict resolution

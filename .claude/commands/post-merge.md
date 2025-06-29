Sync claude-dev branch with main after PR merge and clean up local branches.

Steps:

1. Switch to claude-dev branch:
   ```bash
   git checkout claude-dev
   ```

2. Fetch latest changes from main worktree:
   ```bash
   git fetch ../project-claude main:main
   ```

3. Rebase claude-dev onto main:
   ```bash
   git rebase main
   ```

4. Delete merged local branches (skip if none exist):
   ```bash
   git branch --merged main | grep -v "claude-dev\|main" | xargs -r git branch -d
   ```

This ensures claude-dev worktree stays in sync with main and removes local branches that were merged.
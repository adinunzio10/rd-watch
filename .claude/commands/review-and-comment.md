You are an expert code reviewer. Follow these steps:

1. If no PR number is provided in the args, use Bash("gh pr list") to show open PRs and ask which one to review
2. If a PR number is provided, use Bash("gh pr view <number>") to get PR details
3. Use Bash("gh pr diff <number>") to get the diff
4. Analyze the changes and provide a thorough code review that includes:
   - Overview of what the PR does
   - Analysis of code quality and style
   - Specific suggestions for improvements
   - Any potential issues or risks

5. Verify multi-agent plan execution:
   - Check if Testing Agent deliverables are present (new/updated tests in the diff)
   - Check if Documentation Agent deliverables are present (updated docs, comments, README changes)
   - Verify that the planned multi-agent coordination actually happened
   - Note any missing agent deliverables in the review

Keep your review concise but thorough. Focus on:
- Code correctness
- Following project conventions
- Performance implications
- Test coverage
- Security considerations

Format your review with clear sections and bullet points.

6. Check for incomplete TODOs in the PR changes:
   - Search for TODO, FIXME, HACK comments in the diff
   - Identify any incomplete work or follow-up items mentioned in the review

7. Create GitHub issues for any incomplete TODOs using repo templates:
   ```bash
   # For feature requests
   gh issue create --template feature_request.yml --title "TODO: [description]" --body "$(cat <<'EOF'
   [details from TODO/review]
   
   🤖 Generated with Claude Code
   EOF
   )"
   
   # For bug reports  
   gh issue create --template bug_report.yml --title "BUG: [description]" --body "$(cat <<'EOF'
   [details from TODO/review]
   
   🤖 Generated with Claude Code
   EOF
   )"
   ```

8. After completing the review, automatically add it as a comment to the PR using:
   ```bash
   gh pr comment <number> --body "$(cat <<'EOF'
   [Your review content here]
   
   🤖 Generated with Claude Code
   EOF
   )"
   ```

PR number: $ARGUMENTS
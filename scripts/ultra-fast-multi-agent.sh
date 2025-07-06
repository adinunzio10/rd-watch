#!/bin/bash

# Ultra-Fast Multi-Agent Setup
# Optimized for minimal token usage and maximum speed

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

ultra_fast_setup() {
    local task_id="$1"
    local complexity="${2:-7}"
    
    print_status "⚡ Ultra-fast multi-agent setup for task $task_id"
    start_time=$(date +%s)
    
    # Determine agents
    case "$complexity" in
        1|2|3) agents="single" ;;
        4|5) agents="frontend,backend,testing" ;;
        6|7) agents="frontend,backend,testing,arch,docs" ;;
        8|9) agents="frontend,backend,testing,arch,docs,integration" ;;
        *) agents="frontend,backend,testing" ;;
    esac
    
    if [[ "$agents" == "single" ]]; then
        print_success "Single-agent approach - no setup needed"
        return 0
    fi
    
    # Create coordination tag
    task-master add-tag "task-${task_id}-coord" --copy-from-current >/dev/null 2>&1 || true
    
    # Create all agent tags in parallel
    IFS=',' read -ra AGENT_ARRAY <<< "$agents"
    for agent in "${AGENT_ARRAY[@]}"; do
        agent=$(echo "$agent" | xargs)
        (task-master add-tag "task-${task_id}-${agent}" --copy-from="task-${task_id}-coord" >/dev/null 2>&1 || true) &
    done
    wait
    
    # Mark main task in-progress
    task-master use-tag "task-${task_id}-coord" >/dev/null 2>&1
    task-master set-status --id="$task_id" --status=in-progress >/dev/null 2>&1
    
    # Auto-assign pre-defined tasks using templates
    if [[ -f "scripts/templates/complexity-${complexity}.sh" ]]; then
        print_status "Assigning complexity $complexity tasks..."
        source "scripts/templates/complexity-${complexity}.sh"
        assign_complexity_${complexity}_tasks "$task_id" &
    else
        print_status "Using default task assignment for complexity $complexity"
        # Fallback to basic task creation
        for agent in "${AGENT_ARRAY[@]}"; do
            agent=$(echo "$agent" | xargs)
            (
                task-master use-tag "task-${task_id}-${agent}" >/dev/null 2>&1
                task-master add-task --prompt="[${agent^}] Implement ${agent} components for task ${task_id}" --priority=high >/dev/null 2>&1
            ) &
        done
    fi
    wait
    
    end_time=$(date +%s)
    execution_time=$((end_time - start_time))
    
    print_success "Ultra-fast setup completed in ${execution_time}s"
    print_status "Agents: $agents"
    
    # Create simple workflow file
    cat > "task-${task_id}-workflow.md" << EOF
# Task $task_id Multi-Agent Workflow

## Agent Tags Created:
$(echo "$agents" | tr ',' '\n' | sed 's/^/- task-'${task_id}'-/')

## Quick Commands:
\`\`\`bash
# Switch to agent context
task-master use-tag task-${task_id}-<agent>

# Get next task
task-master next

# Start working
task-master set-status --id=<task-id> --status=in-progress

# Complete task
task-master set-status --id=<task-id> --status=done
\`\`\`

## Ready for Agent Deployment!
Each agent can now work independently in their tag context.
Agents will create detailed tasks as needed during development.
EOF
    
    print_success "Workflow guide: task-${task_id}-workflow.md"
    echo
    echo "🎯 Ready for parallel agent deployment!"
    echo "   Execution time: ${execution_time}s"
    echo "   Token usage: ~0 (no AI calls)"
    echo
    echo "📋 Agent Tags:"
    task-master tags | grep "task-${task_id}" | head -10
}

# Handle command line
if [[ $# -eq 0 ]]; then
    echo "Usage: $0 <task-id> [complexity]"
    echo "Example: $0 22 8"
    exit 1
fi

ultra_fast_setup "$1" "$2"
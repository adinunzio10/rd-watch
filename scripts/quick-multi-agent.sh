#!/bin/bash

# Quick Multi-Agent Deployment Script
# Ultra-fast setup for immediate multi-agent deployment

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }

# Ultra-fast setup function
quick_setup() {
    local task_id="$1"
    local complexity="${2:-7}"
    
    print_status "🚀 Quick multi-agent setup for task $task_id (complexity $complexity)"
    
    # Start timer
    start_time=$(date +%s)
    
    # Determine agents based on complexity
    case "$complexity" in
        1|2|3) agents="single" ;;
        4|5) agents="frontend,backend,testing" ;;
        6|7) agents="frontend,backend,testing,arch,docs" ;;
        8|9) agents="frontend,backend,testing,arch,docs,integration" ;;
        *) agents="frontend,backend,testing" ;;
    esac
    
    if [[ "$agents" == "single" ]]; then
        print_warning "Complexity $complexity suggests single-agent approach"
        print_success "No multi-agent setup needed"
        return 0
    fi
    
    # Create tags rapidly
    print_status "Creating agent tags..."
    task-master add-tag "task-${task_id}-coord" --copy-from-current --description="Task $task_id coordination" >/dev/null 2>&1 || true
    
    IFS=',' read -ra AGENT_ARRAY <<< "$agents"
    for agent in "${AGENT_ARRAY[@]}"; do
        agent=$(echo "$agent" | xargs)
        task-master add-tag "task-${task_id}-${agent}" --copy-from="task-${task_id}-coord" --description="Task $task_id $agent" >/dev/null 2>&1 || true
    done
    
    # Mark main task as in-progress
    task-master use-tag "task-${task_id}-coord" >/dev/null 2>&1
    task-master set-status --id="$task_id" --status=in-progress >/dev/null 2>&1
    
    # Use template for task assignment
    template_file="scripts/templates/complexity-${complexity}.sh"
    if [[ -f "$template_file" ]]; then
        source "$template_file"
        assign_complexity_${complexity}_tasks "$task_id"
    else
        # Fallback to basic assignment
        print_warning "Template not found, using basic assignment"
        for agent in "${AGENT_ARRAY[@]}"; do
            agent=$(echo "$agent" | xargs)
            if [[ "$agent" != "integration" ]]; then
                task-master use-tag "task-${task_id}-${agent}" >/dev/null 2>&1
                task-master add-task --prompt="[${agent^}] Implement core functionality for task $task_id" --priority=high >/dev/null 2>&1
            fi
        done
    fi
    
    # Calculate execution time
    end_time=$(date +%s)
    execution_time=$((end_time - start_time))
    
    # Success message
    print_success "Multi-agent setup completed in ${execution_time}s"
    print_status "Agents: $agents"
    print_status "Ready for parallel development!"
    
    # Show quick start commands
    echo
    echo "🎯 Quick Start Commands:"
    echo "  task-master use-tag task-${task_id}-<agent>"
    echo "  task-master next"
    echo
    echo "📋 Available Tags:"
    task-master tags | grep "task-${task_id}" | head -10
}

# Main execution
if [[ $# -eq 0 ]]; then
    echo "Usage: $0 <task-id> [complexity]"
    echo "Example: $0 15 7"
    exit 1
fi

quick_setup "$1" "$2"
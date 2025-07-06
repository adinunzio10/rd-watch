#!/bin/bash

# Multi-Agent Setup Automation Script
# Optimized for fast, token-efficient multi-agent deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_AGENTS_DIR="scripts/agents"
DEFAULT_TEMPLATES_DIR="scripts/templates"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to display usage
show_usage() {
    cat << EOF
Multi-Agent Setup Automation Script

USAGE:
    $0 [OPTIONS] <task-id>

OPTIONS:
    -c, --complexity <1-9>     Task complexity (default: auto-detect)
    -t, --template <name>      Use specific template (default: auto-select)
    -a, --agents <list>        Comma-separated agent list (default: auto-select)
    -q, --quick               Skip confirmations and use defaults
    -v, --verbose             Enable verbose output
    -h, --help                Show this help message

EXAMPLES:
    $0 --complexity 7 --agents frontend,backend,testing,arch,docs 10
    $0 --quick --template complexity-6 15
    $0 --verbose 22

COMPLEXITY LEVELS:
    1-3: Simple (single-agent recommended)
    4-5: Moderate (frontend, backend, testing)
    6-7: Complex (+ architecture, docs)
    8-9: Very Complex (+ integration, specialists)

AVAILABLE TEMPLATES:
    complexity-4, complexity-5, complexity-6, complexity-7, complexity-8, complexity-9
    minimal, standard, extended, full-swarm
EOF
}

# Function to detect task complexity
detect_complexity() {
    local task_id="$1"
    
    # Check if complexity report exists
    if [[ -f ".taskmaster/reports/task-complexity-report.json" ]]; then
        complexity=$(jq -r ".tasks[] | select(.id == \"$task_id\") | .complexity" .taskmaster/reports/task-complexity-report.json 2>/dev/null)
        if [[ "$complexity" != "null" && "$complexity" != "" ]]; then
            echo "$complexity"
            return 0
        fi
    fi
    
    # Fallback: analyze task from tasks.json
    local subtask_count=$(task-master show "$task_id" | grep -c "│.*pending" || echo "0")
    if [[ $subtask_count -gt 15 ]]; then
        echo "8"
    elif [[ $subtask_count -gt 10 ]]; then
        echo "7"
    elif [[ $subtask_count -gt 5 ]]; then
        echo "6"
    else
        echo "5"
    fi
}

# Function to select agents based on complexity
select_agents() {
    local complexity="$1"
    
    case "$complexity" in
        1|2|3)
            echo "single"
            ;;
        4|5)
            echo "frontend,backend,testing"
            ;;
        6|7)
            echo "frontend,backend,testing,arch,docs"
            ;;
        8|9)
            echo "frontend,backend,testing,arch,docs,integration"
            ;;
        *)
            echo "frontend,backend,testing"
            ;;
    esac
}

# Function to get template name
get_template_name() {
    local complexity="$1"
    echo "complexity-$complexity"
}

# Function to create agent tags quickly
create_agent_tags() {
    local task_id="$1"
    local agents="$2"
    
    print_status "Creating agent tags for task $task_id..."
    
    # Create coordination tag if it doesn't exist
    if ! task-master tags | grep -q "task-${task_id}-coord"; then
        task-master add-tag "task-${task_id}-coord" --copy-from-current --description="Task $task_id coordination and integration" >/dev/null 2>&1 || true
    fi
    
    # Create agent-specific tags
    IFS=',' read -ra AGENT_ARRAY <<< "$agents"
    for agent in "${AGENT_ARRAY[@]}"; do
        agent=$(echo "$agent" | xargs) # trim whitespace
        if [[ "$agent" != "single" ]]; then
            if ! task-master tags | grep -q "task-${task_id}-${agent}"; then
                task-master add-tag "task-${task_id}-${agent}" --copy-from="task-${task_id}-coord" --description="Task $task_id $agent development" >/dev/null 2>&1 || true
            fi
        fi
    done
    
    print_success "Agent tags created successfully"
}

# Function to assign tasks to agents efficiently
assign_tasks_to_agents() {
    local task_id="$1"
    local agents="$2"
    local template_file="$3"
    
    print_status "Assigning tasks to agents..."
    
    # Load template if available
    if [[ -f "$template_file" ]]; then
        source "$template_file"
    else
        # Use default assignment logic
        assign_default_tasks "$task_id" "$agents"
    fi
    
    print_success "Tasks assigned to agents"
}

# Function to assign tasks using default logic
assign_default_tasks() {
    local task_id="$1"
    local agents="$2"
    
    # Switch to coordination tag
    task-master use-tag "task-${task_id}-coord" >/dev/null 2>&1
    
    # Mark main task as in-progress
    task-master set-status --id="$task_id" --status=in-progress >/dev/null 2>&1
    
    # Create agent-specific tasks without expensive AI generation
    IFS=',' read -ra AGENT_ARRAY <<< "$agents"
    for agent in "${AGENT_ARRAY[@]}"; do
        agent=$(echo "$agent" | xargs)
        if [[ "$agent" != "single" ]]; then
            task-master use-tag "task-${task_id}-${agent}" >/dev/null 2>&1
            
            # Create basic tasks for each agent
            case "$agent" in
                frontend)
                    task-master add-task --prompt="[Frontend] Implement UI components for task $task_id" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Frontend] Handle user interactions and navigation" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Frontend] Implement responsive design and accessibility" --priority=medium >/dev/null 2>&1
                    ;;
                backend)
                    task-master add-task --prompt="[Backend] Implement API endpoints for task $task_id" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Backend] Create data models and validation" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Backend] Implement caching and optimization" --priority=medium >/dev/null 2>&1
                    ;;
                testing)
                    task-master add-task --prompt="[Testing] Create unit tests for task $task_id" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Testing] Implement integration tests" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Testing] Add end-to-end testing" --priority=medium >/dev/null 2>&1
                    ;;
                arch)
                    task-master add-task --prompt="[Architecture] Design system architecture for task $task_id" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Architecture] Create technical specifications" --priority=medium >/dev/null 2>&1
                    ;;
                docs)
                    task-master add-task --prompt="[Documentation] Create user documentation for task $task_id" --priority=medium >/dev/null 2>&1
                    task-master add-task --prompt="[Documentation] Write API documentation" --priority=medium >/dev/null 2>&1
                    ;;
                integration)
                    task-master use-tag "task-${task_id}-coord" >/dev/null 2>&1
                    task-master add-task --prompt="[Integration] Monitor cross-agent dependencies" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Integration] Coordinate agent handoffs" --priority=high >/dev/null 2>&1
                    task-master add-task --prompt="[Integration] Validate integrated solution" --priority=medium >/dev/null 2>&1
                    ;;
            esac
        fi
    done
}

# Function to create workflow instructions
create_workflow_instructions() {
    local task_id="$1"
    local agents="$2"
    
    local instructions_file="task-${task_id}-workflow-instructions.md"
    
    cat > "$instructions_file" << EOF
# Task $task_id Multi-Agent Workflow Instructions

## Agent Contexts
$(echo "$agents" | tr ',' '\n' | while read agent; do
    agent=$(echo "$agent" | xargs)
    if [[ "$agent" != "single" ]]; then
        echo "- **$agent**: \`task-master use-tag task-${task_id}-${agent}\`"
    fi
done)

## Quick Start Commands

### Check Next Task
\`\`\`bash
task-master use-tag task-${task_id}-<agent>
task-master next
\`\`\`

### Start Working
\`\`\`bash
task-master set-status --id=<task-id> --status=in-progress
# ... do work ...
task-master set-status --id=<task-id> --status=done
\`\`\`

### Check Progress
\`\`\`bash
task-master use-tag task-${task_id}-coord
task-master list
\`\`\`

## Agent Responsibilities

$(echo "$agents" | tr ',' '\n' | while read agent; do
    agent=$(echo "$agent" | xargs)
    case "$agent" in
        frontend)
            echo "### Frontend Agent"
            echo "- UI components and user interactions"
            echo "- Responsive design and accessibility"
            echo "- Navigation and focus management"
            echo ""
            ;;
        backend)
            echo "### Backend Agent"
            echo "- API endpoints and data models"
            echo "- Business logic and validation"
            echo "- Database operations and caching"
            echo ""
            ;;
        testing)
            echo "### Testing Agent"
            echo "- Unit, integration, and E2E tests"
            echo "- Test coverage and quality assurance"
            echo "- Performance and security testing"
            echo ""
            ;;
        arch)
            echo "### Architecture Agent"
            echo "- System design and technical specifications"
            echo "- Code structure and patterns"
            echo "- Performance optimization strategies"
            echo ""
            ;;
        docs)
            echo "### Documentation Agent"
            echo "- User guides and API documentation"
            echo "- Technical specifications"
            echo "- Code comments and inline docs"
            echo ""
            ;;
        integration)
            echo "### Integration Agent"
            echo "- Cross-agent coordination"
            echo "- Dependency management"
            echo "- Final validation and deployment"
            echo ""
            ;;
    esac
done)

## Generated: $(date)
EOF
    
    print_success "Workflow instructions created: $instructions_file"
}

# Main function
main() {
    local task_id=""
    local complexity=""
    local template=""
    local agents=""
    local quick=false
    local verbose=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--complexity)
                complexity="$2"
                shift 2
                ;;
            -t|--template)
                template="$2"
                shift 2
                ;;
            -a|--agents)
                agents="$2"
                shift 2
                ;;
            -q|--quick)
                quick=true
                shift
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            -*)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                if [[ -z "$task_id" ]]; then
                    task_id="$1"
                else
                    print_error "Multiple task IDs provided"
                    exit 1
                fi
                shift
                ;;
        esac
    done
    
    # Validate required parameters
    if [[ -z "$task_id" ]]; then
        print_error "Task ID is required"
        show_usage
        exit 1
    fi
    
    # Auto-detect complexity if not provided
    if [[ -z "$complexity" ]]; then
        complexity=$(detect_complexity "$task_id")
        print_status "Auto-detected complexity: $complexity"
    fi
    
    # Auto-select agents if not provided
    if [[ -z "$agents" ]]; then
        agents=$(select_agents "$complexity")
        print_status "Auto-selected agents: $agents"
    fi
    
    # Auto-select template if not provided
    if [[ -z "$template" ]]; then
        template=$(get_template_name "$complexity")
    fi
    
    # Check if this is a single-agent task
    if [[ "$agents" == "single" ]]; then
        print_warning "Task complexity ($complexity) suggests single-agent approach"
        if [[ "$quick" == false ]]; then
            read -p "Continue with single-agent setup? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                print_status "Exiting..."
                exit 0
            fi
        fi
        print_success "Single-agent setup - no multi-agent structure needed"
        exit 0
    fi
    
    # Show configuration summary
    print_status "Multi-Agent Setup Configuration:"
    echo "  Task ID: $task_id"
    echo "  Complexity: $complexity"
    echo "  Template: $template"
    echo "  Agents: $agents"
    
    if [[ "$quick" == false ]]; then
        read -p "Continue with this configuration? (Y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Nn]$ ]]; then
            print_status "Exiting..."
            exit 0
        fi
    fi
    
    # Start timer
    start_time=$(date +%s)
    
    # Execute setup steps
    print_status "Starting multi-agent setup..."
    
    # Step 1: Create agent tags
    create_agent_tags "$task_id" "$agents"
    
    # Step 2: Assign tasks to agents
    template_file="$DEFAULT_TEMPLATES_DIR/${template}.sh"
    assign_tasks_to_agents "$task_id" "$agents" "$template_file"
    
    # Step 3: Create workflow instructions
    create_workflow_instructions "$task_id" "$agents"
    
    # Calculate execution time
    end_time=$(date +%s)
    execution_time=$((end_time - start_time))
    
    # Final summary
    print_success "Multi-agent setup completed in ${execution_time}s"
    print_status "Next steps:"
    echo "  1. Clear chat and start fresh"
    echo "  2. Use 'task-master use-tag task-${task_id}-<agent>' to switch contexts"
    echo "  3. Use 'task-master next' to get next task for each agent"
    echo "  4. Agents can work in parallel without conflicts"
    
    # Show available tags
    print_status "Available agent tags:"
    task-master tags | grep "task-${task_id}" | head -10
}

# Run main function
main "$@"
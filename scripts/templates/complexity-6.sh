#!/bin/bash

# Complexity 6 Template - Extended Team
# Agents: frontend, backend, testing, arch, docs

BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

assign_complexity_6_tasks() {
    local task_id="$1"
    
    print_status "Assigning complexity 6 tasks..."
    
    # Frontend Agent Tasks
    task-master use-tag "task-${task_id}-frontend" >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Design and implement UI components" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Handle user interactions and state management" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Implement responsive design and accessibility" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Add navigation and focus management" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Optimize performance and animations" --priority=low >/dev/null 2>&1
    
    # Backend Agent Tasks
    task-master use-tag "task-${task_id}-backend" >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement API endpoints and services" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Create data models and validation" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement database operations" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Add caching and optimization" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Handle error scenarios and logging" --priority=medium >/dev/null 2>&1
    
    # Testing Agent Tasks
    task-master use-tag "task-${task_id}-testing" >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Create comprehensive unit tests" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement integration tests" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Add end-to-end testing scenarios" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Performance and load testing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Security and accessibility testing" --priority=low >/dev/null 2>&1
    
    # Architecture Agent Tasks
    task-master use-tag "task-${task_id}-arch" >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Design system architecture and patterns" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Create technical specifications" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Define component interfaces" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Optimize performance strategies" --priority=medium >/dev/null 2>&1
    
    # Documentation Agent Tasks
    task-master use-tag "task-${task_id}-docs" >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create user documentation" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Write API documentation" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Document architecture decisions" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create troubleshooting guides" --priority=low >/dev/null 2>&1
    
    # Integration Tasks (in coordination tag)
    task-master use-tag "task-${task_id}-coord" >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Monitor cross-agent dependencies" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Coordinate agent handoffs" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Validate integrated solution" --priority=medium >/dev/null 2>&1
    
    print_success "Complexity 6 tasks assigned successfully"
}

# Call the function if script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    assign_complexity_6_tasks "$1"
fi
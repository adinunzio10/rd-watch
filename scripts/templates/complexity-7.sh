#!/bin/bash

# Complexity 7 Template - Extended Team with Specialization
# Agents: frontend, backend, testing, arch, docs

BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

assign_complexity_7_tasks() {
    local task_id="$1"
    
    print_status "Assigning complexity 7 tasks..."
    
    # Frontend Agent Tasks (More specialized for complexity 7)
    task-master use-tag "task-${task_id}-frontend" >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Design complex UI layout with advanced interactions" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Implement state management and data flow" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Build responsive components with accessibility" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Handle advanced navigation and focus management" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Implement real-time updates and synchronization" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Optimize rendering performance and memory usage" --priority=low >/dev/null 2>&1
    
    # Backend Agent Tasks (More comprehensive for complexity 7)
    task-master use-tag "task-${task_id}-backend" >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement comprehensive API layer with validation" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Design and implement data models with relationships" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Build robust database operations with transactions" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement multi-layer caching strategy" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Add comprehensive error handling and recovery" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement background processing and queuing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Add monitoring and observability" --priority=low >/dev/null 2>&1
    
    # Testing Agent Tasks (More thorough for complexity 7)
    task-master use-tag "task-${task_id}-testing" >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Create comprehensive unit test suite" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement integration tests for all components" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Build end-to-end testing with multiple scenarios" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Add performance benchmarking and load testing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement security and vulnerability testing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Create accessibility and usability testing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Add regression testing and CI/CD integration" --priority=low >/dev/null 2>&1
    
    # Architecture Agent Tasks (More detailed for complexity 7)
    task-master use-tag "task-${task_id}-arch" >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Design scalable system architecture" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Create detailed technical specifications" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Define component interfaces and contracts" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Design data flow and state management patterns" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Plan performance optimization strategies" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Document architectural decision records (ADRs)" --priority=medium >/dev/null 2>&1
    
    # Documentation Agent Tasks (More comprehensive for complexity 7)
    task-master use-tag "task-${task_id}-docs" >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create comprehensive user guides" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Write detailed API documentation with examples" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Document system architecture and design decisions" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create developer onboarding documentation" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Write troubleshooting and maintenance guides" --priority=low >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create deployment and configuration docs" --priority=low >/dev/null 2>&1
    
    # Integration Tasks (Enhanced for complexity 7)
    task-master use-tag "task-${task_id}-coord" >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Monitor complex cross-agent dependencies" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Coordinate sophisticated agent handoffs" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Resolve integration conflicts and issues" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Validate comprehensive integrated solution" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Conduct final system testing and validation" --priority=medium >/dev/null 2>&1
    
    print_success "Complexity 7 tasks assigned successfully"
}

# Call the function if script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    assign_complexity_7_tasks "$1"
fi
#!/bin/bash

# Complexity 8 Template - Full Swarm with Specialists
# Agents: frontend, backend, testing, arch, docs, integration

BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

assign_complexity_8_tasks() {
    local task_id="$1"
    
    print_status "Assigning complexity 8 tasks (full swarm)..."
    
    # Frontend Agent Tasks (Highly specialized)
    task-master use-tag "task-${task_id}-frontend" >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Design advanced UI architecture with micro-frontends" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Implement complex state management with multiple stores" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Build dynamic responsive components with advanced interactions" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Implement sophisticated navigation and routing" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Handle real-time updates with WebSocket integration" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Implement advanced accessibility and internationalization" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Optimize rendering with virtualization and lazy loading" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Frontend] Add progressive web app features" --priority=low >/dev/null 2>&1
    
    # Backend Agent Tasks (Highly comprehensive)
    task-master use-tag "task-${task_id}-backend" >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Design microservices architecture with API gateway" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement comprehensive API layer with versioning" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Build complex data models with advanced relationships" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement distributed database operations" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Design multi-tier caching with Redis and CDN" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement robust error handling and circuit breakers" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Add message queuing and event-driven architecture" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Implement comprehensive monitoring and alerting" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Backend] Add security hardening and threat protection" --priority=low >/dev/null 2>&1
    
    # Testing Agent Tasks (Exhaustive coverage)
    task-master use-tag "task-${task_id}-testing" >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Create comprehensive unit test suite with high coverage" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement integration tests for all service interactions" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Build end-to-end testing with complex user journeys" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement contract testing for API compatibility" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Add comprehensive performance and load testing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement security testing and vulnerability scanning" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Create accessibility and usability testing automation" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Add chaos engineering and resilience testing" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Testing] Implement regression testing with automated CI/CD" --priority=low >/dev/null 2>&1
    
    # Architecture Agent Tasks (Highly detailed)
    task-master use-tag "task-${task_id}-arch" >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Design enterprise-scale system architecture" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Create comprehensive technical specifications" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Define service interfaces and API contracts" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Design data architecture and migration strategies" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Plan scalability and performance architecture" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Design security architecture and compliance" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Create deployment and infrastructure architecture" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Architecture] Document comprehensive ADRs and design rationale" --priority=low >/dev/null 2>&1
    
    # Documentation Agent Tasks (Extensive documentation)
    task-master use-tag "task-${task_id}-docs" >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create comprehensive user documentation portal" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Write detailed API documentation with SDKs" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Document system architecture and design decisions" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create developer onboarding and contribution guides" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Write operational runbooks and troubleshooting" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create deployment and configuration documentation" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Document security and compliance procedures" --priority=low >/dev/null 2>&1
    task-master add-task --prompt="[Documentation] Create training materials and tutorials" --priority=low >/dev/null 2>&1
    
    # Integration Agent Tasks (Dedicated integration management)
    task-master use-tag "task-${task_id}-integration" >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Monitor complex multi-agent dependencies" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Coordinate sophisticated cross-team handoffs" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Resolve integration conflicts and compatibility issues" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Orchestrate deployment pipeline integration" --priority=high >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Validate comprehensive system integration" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Conduct final end-to-end system validation" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Manage rollout strategy and deployment" --priority=medium >/dev/null 2>&1
    task-master add-task --prompt="[Integration] Create post-deployment monitoring and alerts" --priority=low >/dev/null 2>&1
    
    print_success "Complexity 8 tasks assigned successfully (full swarm deployment)"
}

# Call the function if script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    assign_complexity_8_tasks "$1"
fi
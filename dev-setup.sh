#!/bin/bash
# Create new session named 'dev'
tmux new-session -d -s dev

# First split horizontally to create top and bottom sections
# Bottom pane should be 12 lines tall
tmux split-window -v -l 12

# Select the top pane and split it into 3 columns
tmux select-pane -t 0
tmux split-window -h -l 133 # This creates right section (77+55=132 columns for middle+right)
tmux split-window -h -l 55  # This splits the right section, creating the rightmost pane

# Start programs in each pane
tmux send-keys -t 0 'lazygit' Enter
tmux send-keys -t 1 'nvim' Enter
tmux send-keys -t 2 'claude' Enter
tmux send-keys -t 3 'clear' Enter

# Focus on Neovim pane
tmux select-pane -t 1

# Attach to session
tmux attach-session -t dev

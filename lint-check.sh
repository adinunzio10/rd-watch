#!/bin/bash

# Android Lint Summary Script
# Runs lint and provides a concise summary of issues

echo "🔍 Running Android Lint..."

# Run lint quietly and capture output
LINT_OUTPUT=$(./gradlew lint --quiet 2>/dev/null)

# Check if lint passed
if [ $? -eq 0 ]; then
    echo "✅ Lint completed successfully"
    
    # Extract HTML report path
    HTML_REPORT=$(echo "$LINT_OUTPUT" | grep -o "file://.*\.html")
    
    if [ -n "$HTML_REPORT" ]; then
        # Convert file:// URL to local path
        REPORT_PATH=$(echo "$HTML_REPORT" | sed 's|file://||')
        
        if [ -f "$REPORT_PATH" ]; then
            echo "📊 Analyzing lint results..."
            
            # Extract summary from HTML report
            ERRORS=$(grep -o 'Priority 1: [0-9]* errors' "$REPORT_PATH" | grep -o '[0-9]*' | head -1)
            WARNINGS=$(grep -o 'Priority 2: [0-9]* warnings' "$REPORT_PATH" | grep -o '[0-9]*' | head -1)
            INFOS=$(grep -o 'Priority 3: [0-9]* informational' "$REPORT_PATH" | grep -o '[0-9]*' | head -1)
            
            # Default to 0 if no issues found
            ERRORS=${ERRORS:-0}
            WARNINGS=${WARNINGS:-0}
            INFOS=${INFOS:-0}
            
            echo ""
            echo "📋 Lint Summary:"
            echo "   🔴 Errors: $ERRORS"
            echo "   🟡 Warnings: $WARNINGS"
            echo "   🔵 Info: $INFOS"
            echo ""
            
            # Show top issues if any exist
            if [ "$ERRORS" -gt 0 ] || [ "$WARNINGS" -gt 0 ]; then
                echo "🔍 Top Issues:"
                # Extract issue details using grep and sed
                grep -A 2 -B 1 'class="issue"' "$REPORT_PATH" | \
                    grep -E '(class="issue"|Location:)' | \
                    sed 's/<[^>]*>//g' | \
                    sed 's/^[[:space:]]*//' | \
                    head -10
                echo ""
                echo "📄 Full report: $REPORT_PATH"
            else
                echo "🎉 No issues found!"
            fi
        else
            echo "⚠️  Could not find lint report at: $REPORT_PATH"
        fi
    else
        echo "🎉 No issues found!"
    fi
else
    echo "❌ Lint failed to run"
    exit 1
fi
#!/bin/bash

# Advanced Android Lint Summary Script
# Runs lint and provides detailed but concise issue summary

echo "🔍 Running Android Lint..."

# Run lint and capture output
LINT_OUTPUT=$(./gradlew lint --quiet 2>&1)
LINT_EXIT_CODE=$?

# Check if lint passed (note: lint can succeed but still have issues)
if [ $LINT_EXIT_CODE -eq 0 ]; then
    echo "✅ Lint completed"
    
    # Look for XML report first (more detailed)
    XML_REPORT="app/build/reports/lint-results-debug.xml"
    
    if [ -f "$XML_REPORT" ]; then
        echo "📊 Analyzing lint results..."
        
        # Count issues by severity
        ERRORS=$(grep -c 'severity="Error"' "$XML_REPORT")
        WARNINGS=$(grep -c 'severity="Warning"' "$XML_REPORT")
        INFOS=$(grep -c 'severity="Informational"' "$XML_REPORT")
        
        echo ""
        echo "📋 Lint Summary:"
        echo "   🔴 Errors: $ERRORS"
        echo "   🟡 Warnings: $WARNINGS" 
        echo "   🔵 Info: $INFOS"
        echo ""
        
        # Show specific issues if any exist
        if [ "$ERRORS" -gt 0 ]; then
            echo "🚨 Error Details:"
            # Extract error details with file and line info
            grep -A 10 'severity="Error"' "$XML_REPORT" | \
                grep -E '(message=|file=|line=)' | \
                sed 's/.*message="\([^"]*\)".*/📍 \1/' | \
                sed 's/.*file="\([^"]*\)".*/   📁 \1/' | \
                sed 's/.*line="\([^"]*\)".*/   📍 Line \1/' | \
                head -15
            echo ""
        fi
        
        if [ "$WARNINGS" -gt 0 ]; then
            echo "⚠️  Warning Summary:"
            # Show unique warning types
            grep 'severity="Warning"' "$XML_REPORT" | \
                sed 's/.*id="\([^"]*\)".*/\1/' | \
                sort | uniq -c | sort -rn | \
                sed 's/^[[:space:]]*\([0-9]*\)[[:space:]]*\(.*\)/   \1x \2/' | \
                head -5
            echo ""
        fi
        
        # Show files with most issues
        echo "📁 Files with issues:"
        grep -o 'file="[^"]*"' "$XML_REPORT" | \
            sed 's/file=".*\/\([^/]*\)"/\1/' | \
            sort | uniq -c | sort -rn | \
            sed 's/^[[:space:]]*\([0-9]*\)[[:space:]]*\(.*\)/   \1 issues: \2/' | \
            head -5
        echo ""
        
        if [ "$ERRORS" -gt 0 ] || [ "$WARNINGS" -gt 0 ]; then
            echo "📄 Full reports:"
            echo "   XML: $XML_REPORT"
            echo "   HTML: app/build/reports/lint-results-debug.html"
        else
            echo "🎉 No issues found!"
        fi
    else
        echo "⚠️  Could not find lint XML report"
    fi
else
    echo "❌ Lint failed to run"
    
    # Check if it's a compilation error
    if echo "$LINT_OUTPUT" | grep -q "Compilation error\|compileDebugKotlin\|Unresolved reference"; then
        echo ""
        echo "🚨 Compilation errors detected - preventing lint analysis:"
        echo ""
        
        # Show compilation errors in a cleaner format
        echo "$LINT_OUTPUT" | grep -E "^e: file://" | head -10 | while read -r line; do
            # Extract file path and error message
            if [[ $line =~ e:\ file://([^:]+):([0-9]+):([0-9]+)\ (.+) ]]; then
                file_path="${BASH_REMATCH[1]}"
                line_num="${BASH_REMATCH[2]}"
                col_num="${BASH_REMATCH[3]}"
                error_msg="${BASH_REMATCH[4]}"
                
                # Get just the filename
                filename=$(basename "$file_path")
                echo "   📁 $filename:$line_num:$col_num"
                echo "   🔴 $error_msg"
                echo ""
            fi
        done
        
        # Count total errors
        ERROR_COUNT=$(echo "$LINT_OUTPUT" | grep -c "^e: file://")
        if [ "$ERROR_COUNT" -gt 10 ]; then
            echo "   ... and $((ERROR_COUNT - 10)) more compilation errors"
            echo ""
        fi
        
        echo "💡 Fix compilation errors first, then run lint again"
        echo "   Try: ./gradlew build to see all compilation issues"
    else
        echo ""
        echo "🔍 Lint error details:"
        echo "$LINT_OUTPUT" | head -10
    fi
    
    exit 1
fi
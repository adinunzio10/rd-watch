#!/bin/bash

# Advanced Android Lint Summary Script
# Runs lint and provides detailed but concise issue summary

echo "🔍 Running Android Lint..."

# Run lint quietly
./gradlew lint --quiet > /dev/null 2>&1

# Check if lint passed (note: lint can succeed but still have issues)
if [ $? -eq 0 ]; then
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
    exit 1
fi
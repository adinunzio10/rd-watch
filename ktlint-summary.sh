#!/bin/bash

# KtLint Format & Check Summary Script
# Runs ktlint formatting and checking with detailed summary

echo "🔍 Running KtLint..."

# First, run format to auto-fix issues
echo "🔧 Formatting code with ktlintFormat..."
FORMAT_OUTPUT=$(./gradlew ktlintFormat 2>&1)
FORMAT_EXIT_CODE=$?

if [ $FORMAT_EXIT_CODE -eq 0 ]; then
    echo "✅ Formatting completed successfully"
else
    echo "⚠️  Formatting encountered issues that cannot be auto-fixed"
    
    # Extract specific errors from format output
    UNFIXABLE_ISSUES=$(echo "$FORMAT_OUTPUT" | grep "cannot be auto-corrected" | head -5)
    if [ -n "$UNFIXABLE_ISSUES" ]; then
        echo ""
        echo "📌 Issues requiring manual fix:"
        echo "$UNFIXABLE_ISSUES" | while IFS= read -r line; do
            echo "   • $line"
        done
    fi
fi

echo ""
echo "🔍 Checking code style with ktlintCheck..."

# Run ktlintCheck and capture output
KTLINT_OUTPUT=$(./gradlew ktlintCheck 2>&1)
KTLINT_EXIT_CODE=$?

# Check if ktlint passed
if [ $KTLINT_EXIT_CODE -eq 0 ]; then
    echo "✅ KtLint check passed!"
    echo "🎉 No style issues found!"
else
    echo "❌ KtLint found style issues"
    echo ""
    echo "📋 Issue Summary:"
    
    # Count total issues by looking for file:line:column pattern
    ISSUE_COUNT=$(echo "$KTLINT_OUTPUT" | grep -E "^/.+:[0-9]+:[0-9]+" | wc -l)
    echo "   🔴 Total issues: $ISSUE_COUNT"
    echo ""
    
    # Show issues by file
    if [ "$ISSUE_COUNT" -gt 0 ]; then
        echo "📁 Files with issues:"
        echo "$KTLINT_OUTPUT" | grep -E "^/.+:[0-9]+:[0-9]+" | \
            sed 's/^\(.*\):[0-9]*:[0-9]*.*/\1/' | \
            sort | uniq -c | sort -rn | \
            sed 's/^[[:space:]]*\([0-9]*\)[[:space:]]*\(.*\)/   \1 issues: \2/' | \
            head -10
        echo ""
        
        # Show specific issues (first 10)
        echo "🔍 Issue Details (first 10):"
        echo "$KTLINT_OUTPUT" | grep -E "^/.+:[0-9]+:[0-9]+" | head -10 | while IFS= read -r line; do
            # Extract file path, line number, and error message
            if [[ $line =~ ^(.+):([0-9]+):([0-9]+)[[:space:]]+(.+) ]]; then
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
        
        if [ "$ISSUE_COUNT" -gt 10 ]; then
            echo "   ... and $((ISSUE_COUNT - 10)) more issues"
            echo ""
        fi
    fi
    
    # Check for report file location
    REPORT_FILE=$(echo "$KTLINT_OUTPUT" | grep -oE "/[^[:space:]]+\.txt" | head -1)
    if [ -n "$REPORT_FILE" ]; then
        echo "📄 Full report available at:"
        echo "   $REPORT_FILE"
        echo ""
    fi
    
    echo "💡 Run './gradlew ktlintFormat' to auto-fix most issues"
    echo "   Then run './gradlew ktlintCheck' to verify remaining issues"
    
    exit 1
fi
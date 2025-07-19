#!/usr/bin/env python3
"""
Android TV Log Cleanup Script
Cleans up verbose Android TV logs for easier sharing and analysis.
"""

import re
import sys
import argparse
from pathlib import Path
from typing import List, Set

class AndroidLogCleaner:
    def __init__(self):
        # Patterns to remove (noise) - be more selective
        self.noise_patterns = [
            # Very repetitive buffer pool stats
            r'.*BufferPoolAccessor2\.0.*bufferpool2.*total buffers.*',
            # Excessive MediaCodec internal details but keep key ones
            r'.*ReflectedParamUpdater.*extent\(\) != 1 for single value type.*',
            r'.*c2::u32 (raw\.sar\.|raw\.rotation\.|algo\.buffers\.).*',
            r'.*c2::i32 raw\.rotation\.(flip|value) = \d+.*',
            # HTTP headers but keep important ones
            r'.*content-type: application/json.*',
            r'.*cache-control:.*',
            r'.*etag:.*',
            r'.*age:.*',
            r'.*vary:.*',
            r'.*server: (nginx|cloudflare).*',
            r'.*date: [A-Z][a-z]{2}, \d{2}.*',
            r'.*alt-svc:.*',
            r'.*via:.*',
            r'.*x-cache:.*',
            # Specific JSON fields that add noise
            r'.*"adult":false.*',
            r'.*"gender":\d+.*',
            r'.*"popularity":\d+\.\d+.*',
            r'.*"profile_path".*',
            r'.*"backdrop_path".*',
            r'.*"poster_path".*',
            r'.*"vote_average".*',
            r'.*"vote_count".*',
        ]
        
        # Patterns to keep (important)
        self.important_patterns = [
            # Core app flow
            r'.*MovieDetailsViewModel.*',
            r'.*PlaybackViewModel.*',
            r'.*ExoPlayer.*',
            r'.*MediaCodec.*',
            # TV show related
            r'.*SeasonSelector.*',
            r'.*TVDetailsViewModel.*(?:Season|Episode).*',
            r'.*TMDbTVRepository.*',
            # Network and API
            r'.*NetworkBoundResource.*API call.*',
            r'.*API CALL:.*',
            r'.*API RESULT:.*',
            r'.*shouldFetch.*',
            r'.*URL:.*',
            r'.*Using URL.*',
            # Playback and streaming
            r'.*Source selected.*',
            r'.*playback.*',
            r'.*ExoPlayerImpl.*Init.*',
            r'.*MediaSourceFactory.*',
            r'.*CCodec.*allocate.*',
            r'.*Created component.*',
            # Errors and important events
            r'.*Error.*',
            r'.*WARNING.*',
            r'.*WARN.*',
            r'.*failed.*',
            r'.*CORRUPTED.*',
            r'.*BAD_INDEX.*',
            r'.*===.*===.*',  # Debug section headers
            # State and UI updates
            r'.*UI state.*',
            r'.*Sources state.*',
            r'.*DEBUG \[.*\]:.*',
        ]
        
        # Keywords that indicate important log lines
        self.important_keywords = [
            'ERROR', 'WARNING', 'EXCEPTION', 'CRASH', 'FAIL', 'failed',
            'Season Selection', 'Episode Selection', 'Loading',
            'API CALL', 'API RESULT', 'shouldFetch', 'validation',
            'SeasonSelector', 'episodes.size', 'episodeCount',
            'ExoPlayer', 'playback', 'Source selected', 'MediaCodec',
            'CCodec', 'allocate', 'Init', 'Created component',
            'UI state', 'Sources state', 'MovieDetails', 'PlaybackViewModel'
        ]

    def is_noise(self, line: str) -> bool:
        """Check if a line should be filtered out as noise."""
        for pattern in self.noise_patterns:
            if re.match(pattern, line, re.IGNORECASE):
                return True
        return False

    def is_important(self, line: str) -> bool:
        """Check if a line contains important information."""
        # Check against important patterns
        for pattern in self.important_patterns:
            if re.match(pattern, line, re.IGNORECASE):
                return True
        
        # Check for important keywords
        for keyword in self.important_keywords:
            if keyword.lower() in line.lower():
                return True
        
        return False

    def simplify_json_response(self, line: str) -> str:
        """Simplify long JSON responses to just show summary."""
        if '{"' in line and len(line) > 200:
            # Extract the basic info
            timestamp_match = re.match(r'^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})', line)
            timestamp = timestamp_match.group(1) if timestamp_match else ""
            
            if '"episodes":[' in line:
                episode_count = line.count('"episode_number":')
                return f"{timestamp}  [JSON Response] Season data with {episode_count} episodes (truncated)"
            elif '"cast":[' in line:
                cast_count = line.count('"character":')
                return f"{timestamp}  [JSON Response] Cast/crew data with {cast_count} cast members (truncated)"
            elif '"results":[' in line:
                result_count = line.count('"id":')
                return f"{timestamp}  [JSON Response] Search results with {result_count} items (truncated)"
            else:
                return f"{timestamp}  [JSON Response] Large JSON payload (truncated)"
        
        return line

    def clean_logs(self, input_text: str) -> str:
        """Clean the log text and return filtered version."""
        lines = input_text.strip().split('\n')
        cleaned_lines = []
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
            
            # Skip obvious noise first
            if self.is_noise(line):
                continue
            
            # Simplify long JSON responses
            simplified_line = self.simplify_json_response(line)
            
            # Keep important lines
            if self.is_important(line):
                cleaned_lines.append(simplified_line)
                continue
            
            # Keep Android TV app logs that aren't too verbose
            if 'com.rdwatch.androidtv' in line:
                # Keep most logs under 800 chars unless they're clearly verbose internals
                if len(line) < 800:
                    # Skip extremely verbose MediaCodec config dumps
                    if not ('Dict {' in line and 'c2::' in line and len(line) > 400):
                        cleaned_lines.append(simplified_line)
                elif any(keyword in line.lower() for keyword in ['error', 'warning', 'fail', 'exception']):
                    # Always keep errors even if long
                    cleaned_lines.append(simplified_line)
        
        return '\n'.join(cleaned_lines)

    def add_summary(self, cleaned_text: str) -> str:
        """Add a summary section at the beginning."""
        lines = cleaned_text.split('\n')
        
        # Extract key information
        season_selections = [l for l in lines if 'Season Selection' in l or 'Selecting season' in l]
        api_calls = [l for l in lines if 'API CALL:' in l]
        errors = [l for l in lines if 'Error' in l or 'WARNING' in l or 'WARN' in l]
        
        summary = ["=== LOG SUMMARY ==="]
        summary.append(f"Total lines after cleanup: {len(lines)}")
        summary.append(f"Season selections: {len(season_selections)}")
        summary.append(f"API calls: {len(api_calls)}")
        summary.append(f"Errors/Warnings: {len(errors)}")
        summary.append("")
        
        if errors:
            summary.append("=== ERRORS/WARNINGS ===")
            for error in errors[:5]:  # Show first 5 errors
                summary.append(error)
            if len(errors) > 5:
                summary.append(f"... and {len(errors) - 5} more errors")
            summary.append("")
        
        summary.append("=== CLEANED LOG DATA ===")
        summary.append("")
        
        return '\n'.join(summary) + cleaned_text

def main():
    parser = argparse.ArgumentParser(description='Clean Android TV logs for easier sharing')
    parser.add_argument('input_file', nargs='?', help='Input log file (or use stdin)')
    parser.add_argument('-o', '--output', help='Output file (default: stdout)')
    parser.add_argument('-s', '--summary', action='store_true', help='Add summary section')
    parser.add_argument('-v', '--verbose', action='store_true', help='Verbose output')
    
    args = parser.parse_args()
    
    # Read input
    if args.input_file:
        try:
            with open(args.input_file, 'r', encoding='utf-8') as f:
                input_text = f.read()
        except FileNotFoundError:
            print(f"Error: File '{args.input_file}' not found", file=sys.stderr)
            sys.exit(1)
        except Exception as e:
            print(f"Error reading file: {e}", file=sys.stderr)
            sys.exit(1)
    else:
        input_text = sys.stdin.read()
    
    if args.verbose:
        print(f"Processing {len(input_text.split())} lines...", file=sys.stderr)
    
    # Clean logs
    cleaner = AndroidLogCleaner()
    cleaned_text = cleaner.clean_logs(input_text)
    
    # Add summary if requested
    if args.summary:
        cleaned_text = cleaner.add_summary(cleaned_text)
    
    # Write output
    if args.output:
        try:
            with open(args.output, 'w', encoding='utf-8') as f:
                f.write(cleaned_text)
            if args.verbose:
                print(f"Cleaned logs written to '{args.output}'", file=sys.stderr)
        except Exception as e:
            print(f"Error writing output file: {e}", file=sys.stderr)
            sys.exit(1)
    else:
        print(cleaned_text)

if __name__ == '__main__':
    main()

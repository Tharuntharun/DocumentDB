import re
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import matplotlib.cm as cm
import glob

# --------------------------------------------------------------
# Function: parse_log_files
# Purpose : Reads log files matching the given pattern, extracts
#           operations (Insert, Update, Read) and their durations.
# Notes   : Groups entries by "run" using "All Operations completed".
# --------------------------------------------------------------
def parse_log_files(file_pattern):
    entry_pattern = re.compile(r'\[Second (\d+)\].*?(Insert|Update|Read) completed in (\d+) ms')
    run_end_pattern = re.compile(r'All Operations completed')

    runs = []
    current_run = []

    for filepath in sorted(glob.glob(file_pattern)):
        with open(filepath, 'r') as file:
            lines = file.readlines()

        for line in lines:
            if run_end_pattern.search(line):
                if current_run:
                    runs.append(current_run)
                    current_run = []
            else:
                match = entry_pattern.search(line)
                if match:
                    second = int(match.group(1))
                    operation = match.group(2)
                    duration = int(match.group(3))
                    current_run.append({'second': second, 'operation': operation, 'duration': duration})

    if current_run:
        runs.append(current_run)

    return runs

# --------------------------------------------------------------
# Function: adjust_cumulative_seconds
# Purpose : Ensures time continuity across multiple runs by adding
#           cumulative offsets to seconds so that plots are aligned.
# --------------------------------------------------------------
def adjust_cumulative_seconds(runs):
    adjusted_entries = []
    cumulative_offset = 0

    for run_index, run in enumerate(runs, start=1):
        for entry in run:
            adjusted_entry = entry.copy()
            adjusted_entry['run'] = run_index
            adjusted_entry['cumulative_second'] = entry['second'] + cumulative_offset
            adjusted_entries.append(adjusted_entry)
        if run:
            max_second = max(entry['second'] for entry in run)
            cumulative_offset += max_second + 1

    return adjusted_entries

# --------------------------------------------------------------
# Function: get_run_colors
# Purpose : Assigns distinct colors to each run using matplotlib colormap.
# --------------------------------------------------------------
def get_run_colors(num_runs):
    cmap = cm.get_cmap('tab10', num_runs)
    return [cmap(i) for i in range(num_runs)]

# --------------------------------------------------------------
# Function: plot_combined_operations
# Purpose : Plots all operations (Insert, Update, Read) together in one graph
#           with runs differentiated by color.
# --------------------------------------------------------------
def plot_combined_operations(entries):
    if not entries:
        print("No entries to plot.")
        return

    operation_types = ['Insert', 'Update', 'Read']
    run_ids = sorted(set(e['run'] for e in entries))
    run_colors = dict(zip(run_ids, get_run_colors(len(run_ids))))

    plt.figure(figsize=(12, 6))
    for op in operation_types:
        for run in run_ids:
            filtered = [(e['cumulative_second'], e['duration']) for e in entries if e['operation'] == op and e['run'] == run]
            if filtered:
                x, y = zip(*sorted(filtered))
                plt.scatter(x, y, label=f'{op} - Run {run}', alpha=0.6, color=run_colors[run])

    plt.xlabel('Cumulative Time (s)')
    plt.ylabel('Duration (ms)')
    plt.title('Combined Operation Durations Over Time')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig('combined_operation_durations_xcap.png')
    plt.show()

# --------------------------------------------------------------
# Function: plot_separate_operation_graphs
# Purpose : Plots one graph per operation type, showing durations across runs.
# --------------------------------------------------------------
def plot_separate_operation_graphs(entries):
    if not entries:
        print("No entries to plot.")
        return

    operation_types = ['Insert', 'Update', 'Read']
    run_ids = sorted(set(entry['run'] for entry in entries))
    run_colors = dict(zip(run_ids, get_run_colors(len(run_ids))))

    for op_type in operation_types:
        plt.figure(figsize=(10, 5))
        for run in run_ids:
            filtered = [(entry['cumulative_second'], entry['duration'])
                        for entry in entries if entry['operation'] == op_type and entry['run'] == run]
            if filtered:
                x, y = zip(*sorted(filtered))
                plt.plot(x, y, marker='o', linestyle='-', label=f'Run {run}', color=run_colors[run])
        plt.xlabel('Cumulative Time (s)')
        plt.ylabel(f'{op_type} Duration (ms)')
        plt.title(f'{op_type} Operation Durations Over Time')
        plt.legend()
        plt.grid(True)
        plt.tight_layout()
        plt.savefig(f'{op_type.lower()}_operation_durations_xcap.png')
        plt.show()

# --------------------------------------------------------------
# Function: export_to_excel
# Purpose : Exports raw operation data and summary statistics to Excel.
# Sheets  :
#   - Raw Data (all logs with run/operation/durations)
#   - Run Statistics (per run per operation stats)
#   - Consolidated Summary (overall operation stats)
# --------------------------------------------------------------
def export_to_excel(entries, output_file='operation_stats.xlsx'):
    if not entries:
        print("No entries to export.")
        return

    df = pd.DataFrame(entries)

    # Raw data
    raw_data = df[['run', 'second', 'cumulative_second', 'operation', 'duration']]
    raw_data.columns = ['Run', 'Second (s)', 'Cumulative Second (s)', 'Operation', 'Duration (ms)']

    # Run-wise stats
    df_nonzero = df[df['duration'] > 0]
    run_stats = df_nonzero.groupby(['run', 'operation'])['duration'].agg(
        Count='count',
        Mean_ms='mean',
        Min_ms='min',
        Max_ms='max'
    ).reset_index()
    run_stats.columns = ['Run', 'Operation', 'Count', 'Mean Duration (ms)', 'Min Duration (ms)', 'Max Duration (ms)']

    # Consolidated stats
    consolidated = df_nonzero.groupby(['operation'])['duration'].agg(
        Total_Count='count',
        Average_Duration='mean',
        Minimum_Duration='min',
        Maximum_Duration='max'
    ).reset_index()
    consolidated.columns = [ 'Operation', 'Total Count', 'Average Duration (ms)', 'Minimum Duration (ms)', 'Maximum Duration (ms)']

    # Write to Excel
    with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
        raw_data.to_excel(writer, sheet_name='Raw Data', index=False)
        run_stats.to_excel(writer, sheet_name='Run Statistics', index=False)
        consolidated.to_excel(writer, sheet_name='Consolidated Summary', index=False)

# --------------------------------------------------------------
# Main Script Entry
# Purpose : Adjust log pattern, run parsing, plotting, and export.
# --------------------------------------------------------------
if __name__ == '__main__':
    log_pattern = 'spring.log.*'  # Adjust this pattern to match your log files
    runs = parse_log_files(log_pattern)
    adjusted_entries = adjust_cumulative_seconds(runs)
    plot_combined_operations(adjusted_entries)
    plot_separate_operation_graphs(adjusted_entries)
    export_to_excel(adjusted_entries)

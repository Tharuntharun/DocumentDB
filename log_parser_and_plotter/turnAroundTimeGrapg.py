import re
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import glob
from sklearn.linear_model import LinearRegression

# --------------------------------------------------------------
# Function: parse_log_files
# Purpose : Parse log files matching the given pattern and extract
#           operation type (Insert, Update, Read), execution second,
#           and duration in ms. Groups entries into runs separated
#           by "All Operations completed".
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
            # Detect end of a run
            if run_end_pattern.search(line):
                if current_run:
                    runs.append(current_run)
                    current_run = []
            else:
                # Extract operation, second, and duration
                match = entry_pattern.search(line)
                if match:
                    second = int(match.group(1))
                    operation = match.group(2)
                    duration = int(match.group(3))
                    current_run.append({'second': second, 'operation': operation, 'duration': duration})

    # Append last run if not already added
    if current_run:
        runs.append(current_run)

    return runs

# --------------------------------------------------------------
# Function: adjust_cumulative_seconds
# Purpose : Adjust seconds across runs so that time is continuous
#           when plotting multiple runs sequentially.
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
# Function: plot_tat_growth_trend
# Purpose : Remove statistical outliers, fit linear regression lines
#           for each operation type, and plot trendlines to show how
#           duration changes over cumulative time.
# --------------------------------------------------------------
def plot_tat_growth_trend(entries):
    if not entries:
        print("No data to plot.")
        return

    df = pd.DataFrame(entries)
    df['cumulative_ms'] = df['cumulative_second']  # Still in seconds, misnamed as ms

    # Outlier removal using IQR per operation type
    filtered_df = pd.DataFrame()
    for op in df['operation'].unique():
        subset = df[df['operation'] == op]
        Q1 = subset['duration'].quantile(0.25)
        Q3 = subset['duration'].quantile(0.75)
        IQR = Q3 - Q1
        filtered = subset[(subset['duration'] >= Q1 - 1.5 * IQR) & (subset['duration'] <= Q3 + 1.5 * IQR)]
        filtered_df = pd.concat([filtered_df, filtered], ignore_index=True)

    # Plot regression trendlines per operation
    plt.figure(figsize=(14, 7))
    colors = {'Insert': 'blue', 'Update': 'green', 'Read': 'orange'}

    for op in filtered_df['operation'].unique():
        op_data = filtered_df[filtered_df['operation'] == op]
        x = op_data['cumulative_ms'].values.reshape(-1, 1)
        y = op_data['duration'].values

        # Fit linear regression model
        model = LinearRegression().fit(x, y)
        y_pred = model.predict(x)
        slope = model.coef_[0]

        # Plot predicted regression line
        sorted_idx = np.argsort(x.flatten())
        plt.plot(
            x.flatten()[sorted_idx],
            y_pred[sorted_idx],
            linestyle='--',
            color=colors.get(op, 'gray'),
            label=f'{op} Trend (slope={slope:.6f})'
        )

    plt.xlabel('Cumulative Time (s)')   # Should be seconds, not ms
    plt.ylabel('Duration (ms)')
    plt.title('TAT Growth Trend by Operation Type (Outliers Removed)')
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig('tat_growth_trend_by_operation.png')
    plt.show()

# --------------------------------------------------------------
# Main Script Entry
# Purpose : Parse logs, adjust cumulative times, and plot growth trends.
# --------------------------------------------------------------
if __name__ == '__main__':
    log_pattern = 'spring.log.*'  # Adjust this pattern to match your log files
    runs = parse_log_files(log_pattern)
    adjusted_entries = adjust_cumulative_seconds(runs)
    plot_tat_growth_trend(adjusted_entries)

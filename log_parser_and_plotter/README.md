# Log Analysis & Visualization Tools

This repository contains two Python scripts for analyzing application log files that record database operations (`Insert`, `Update`, `Read`) with execution times.

---

## 📂 Programs

### 1. `tat_growth_trend.py`
**Purpose:**  
- Parses logs and extracts per-operation durations.  
- Removes statistical outliers using IQR.  
- Fits **linear regression trendlines** for each operation type.  
- Visualizes **growth trends** (slopes) to identify whether operation times are increasing across runs.  

**Key Features:**  
- Multiple log runs handled (`All Operations completed` delimiter).  
- Continuous cumulative time axis across runs.  
- Outlier removal to prevent skewed trends.  
- Trendlines with slope annotation per operation.

**Outputs include**
- tat_growth_trend_by_operation.png

**Usage:**  
bash
python tat_growth_trend.py

### 2. `operation_analysis.py`
**Purpose**
- Parses logs and aggregates execution times across multiple runs.
- Generates scatter plots and line plots for each operation.
- Exports raw data and summary statistics to Excel.

**Key Features**
- Combined scatter plot of all operations across runs.
- Separate line plots per operation type.
- Automatic run color assignment for clarity.
- Excel report (operation_stats.xlsx) with:
    Raw Data: Every log entry with run/operation info.
    Run Statistics: Per-run summary (count, min, max, mean).
    Consolidated Summary: Overall stats across all runs.

**Outputs include**
- combined_operation_durations_xcap.png
- insert_operation_durations_xcap.png
- update_operation_durations_xcap.png
- read_operation_durations_xcap.png
- operation_stats.xlsx

**Usage:**
bash
python operation_analysis.py

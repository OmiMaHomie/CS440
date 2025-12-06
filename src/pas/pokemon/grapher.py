import re
import matplotlib.pyplot as plt

def parse_training_log(log_text):
    """Parse training log to extract cycle, utility, and win rate"""
    cycles = []
    utilities = []
    win_rates = []
    
    lines = log_text.strip().split('\n')
    
    for line in lines:
        if line.startswith('after cycle='):
            # Parse: after cycle=0 avg(utility)=-786.1339744243467 avg(num_wins)=0.0
            match = re.search(r'after cycle=(\d+)\s+avg\(utility\)=([-\d\.]+)\s+avg\(num_wins\)=([\d\.]+)', line)
            if match:
                cycles.append(int(match.group(1)))
                utilities.append(float(match.group(2)))
                win_rates.append(float(match.group(3)))
    
    return cycles, utilities, win_rates

def plot_training_progress(cycles, utilities, win_rates):
    """Create visualization of training progress"""
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    
    # Plot 1: Raw utility over time
    axes[0, 0].plot(cycles, utilities, 'b-', alpha=0.7, linewidth=1)
    axes[0, 0].set_xlabel('Training Cycle')
    axes[0, 0].set_ylabel('Average Utility')
    axes[0, 0].set_title('Utility Over Time')
    axes[0, 0].grid(True, alpha=0.3)
    
    # Plot 2: Moving average of utility (window=10)
    window = 10
    if len(utilities) > window:
        moving_avg = [sum(utilities[max(0, i-window):i+1])/min(i+1, window) 
                     for i in range(len(utilities))]
        axes[0, 1].plot(cycles, moving_avg, 'r-', linewidth=2)
        axes[0, 1].set_xlabel('Training Cycle')
        axes[0, 1].set_ylabel(f'{window}-Cycle Moving Avg Utility')
        axes[0, 1].set_title('Smoothed Utility Trend')
        axes[0, 1].grid(True, alpha=0.3)
    
    # Plot 3: Win rate over time
    axes[1, 0].plot(cycles, win_rates, 'g-', alpha=0.7, linewidth=1)
    axes[1, 0].axhline(y=0.5, color='r', linestyle='--', alpha=0.5, label='50% threshold')
    axes[1, 0].set_xlabel('Training Cycle')
    axes[1, 0].set_ylabel('Win Rate')
    axes[1, 0].set_title('Win Rate Over Time')
    axes[1, 0].set_ylim([-0.05, 1.05])
    axes[1, 0].legend()
    axes[1, 0].grid(True, alpha=0.3)
    
    # Plot 4: Moving average of win rate (window=10)
    if len(win_rates) > window:
        moving_win = [sum(win_rates[max(0, i-window):i+1])/min(i+1, window) 
                     for i in range(len(win_rates))]
        axes[1, 1].plot(cycles, moving_win, 'purple', linewidth=2)
        axes[1, 1].axhline(y=0.5, color='r', linestyle='--', alpha=0.5, label='50% threshold')
        axes[1, 1].axhline(y=0.75, color='orange', linestyle='--', alpha=0.5, label='75% threshold')
        axes[1, 1].set_xlabel('Training Cycle')
        axes[1, 1].set_ylabel(f'{window}-Cycle Moving Avg Win Rate')
        axes[1, 1].set_title('Smoothed Win Rate Trend')
        axes[1, 1].set_ylim([-0.05, 1.05])
        axes[1, 1].legend()
        axes[1, 1].grid(True, alpha=0.3)
    
    plt.tight_layout()
    return fig

# If you want to run this directly on your log file:
def analyze_from_file(log_file_path='training_log.txt'):
    """Load and analyze from file"""
    with open(log_file_path, 'r') as f:
        log_text = f.read()
    
    cycles, utilities, win_rates = parse_training_log(log_text)
    
    print(f"Total cycles: {len(cycles)}")
    print(f"Final cycle: {cycles[-1]}")
    print(f"Final utility: {utilities[-1]:.2f}")
    print(f"Final win rate: {win_rates[-1]*100:.1f}%")
    
    # Calculate overall improvement
    if len(utilities) > 10:
        early_avg = sum(utilities[:10]) / 10
        late_avg = sum(utilities[-10:]) / 10
        print(f"\nEarly avg (first 10): {early_avg:.2f}")
        print(f"Late avg (last 10): {late_avg:.2f}")
        print(f"Improvement: {late_avg - early_avg:.2f}")
    
    fig = plot_training_progress(cycles, utilities, win_rates)
    plt.show()
    
    return cycles, utilities, win_rates

# Quick analysis of your current progress
print("=== Current Training Analysis ===")
print("Best win rate observed: 80% (cycle 57, 250)")
print("Best utility observed: 523.91 (cycle 84)")
print("Worst utility observed: -510.87 (cycle 237)")
print("\nLast 10 cycles average win rate: ~40%")
print("Overall trend: Learning but high variance - typical for RL")

# Run the analysis
# Uncomment to run:
cycles, utilities, win_rates = analyze_from_file('/home/omimahomie/cs440/training_log.txt')
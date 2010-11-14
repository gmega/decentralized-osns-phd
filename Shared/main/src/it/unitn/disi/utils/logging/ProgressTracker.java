package it.unitn.disi.utils.logging;

public abstract class ProgressTracker {
	
	private final String fTaskTitle;
	
	private final int fTotalTicks;
	
	private final double fUpdateInterval;
	
	private int fTicks;
	
	private double fUntilUpdate;
	
    ProgressTracker(String taskTitle, int totalTicks) {
        fTotalTicks = totalTicks;
        fUpdateInterval = Math.max(1.0, totalTicks/100.0);
        fTaskTitle = taskTitle;
        
        fTicks = 0;
        fUntilUpdate = 0;
    }
        
    public void startTask() {
        this.displayWidget();
    }
        
    public void tick() {
        this.tick(1);
    }
    
    public void tick(int ticks) {
        this.fTicks += ticks;
        this.fUntilUpdate -= ticks;
        this.updateProgress();
    }
    
    public void done() {
        this.disposeWidget();
    }
    
    public void updateProgress() {
        if (fUntilUpdate < 0){
        	double dTicks = (double)fTicks;
        	double dTotal = (double)fTotalTicks;
        	this.reportProgress(Math.round((dTicks/dTotal)*100.0));
            this.fUntilUpdate = fUpdateInterval;
        }
    }
    
    public String title() {
    	return fTaskTitle;
    }
    
    protected abstract void reportProgress(double percentage);
        
    protected abstract void disposeWidget();
    
    protected abstract void displayWidget();
}
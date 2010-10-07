package it.unitn.disi.utils.logging;

class TextProgressTracker extends ProgressTracker {
	
	TextProgressTracker(String taskTitle, int totalTicks) {
		super(taskTitle, totalTicks);
	}
    
	@Override
	protected void displayWidget() {
    	System.err.println("Now starting task <<" + title() + ">>.");
    }
        
	@Override
	protected void disposeWidget() {
		System.err.println("[" + title() + "]: Done.");
	}

	@Override
	protected void reportProgress(double percentage) {
		System.err.println(String.format("[%1$s]: %2$.2f %% complete.", title(), percentage));
	}

}

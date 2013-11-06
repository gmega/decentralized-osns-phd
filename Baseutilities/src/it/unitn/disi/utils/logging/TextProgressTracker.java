package it.unitn.disi.utils.logging;

abstract class TextProgressTracker extends ProgressTracker {
	
	TextProgressTracker(String taskTitle, int totalTicks) {
		super(taskTitle, totalTicks);
	}
    
	@Override
	protected void displayWidget() {
    	out("Now starting task <<" + title() + ">>.");
    }
        
	@Override
	protected void disposeWidget() {
		out("[" + title() + "]: Done.");
	}

	@Override
	protected void reportProgress(double percentage) {
		out(String.format("[%1$s]: %2$.2f %% complete.", title(), percentage));
	}
	
	protected abstract void out(String out);

}

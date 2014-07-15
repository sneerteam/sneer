package sneer.commons;

public abstract class InfiniteLoop extends Thread {

	private boolean quitLoop;

	{
		setDaemon(true);
		start();
	}
	
	
	@Override public void run() {
		while (!quitLoop) iterate();
	}

	
	protected abstract void iterate();

	
	protected void quitLoop() {
		quitLoop = true;
	}
}

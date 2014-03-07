package it.unitn.disi.utils.streams;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

/**
 * Simple utility class that periodically flushes a (set of)
 * {@link OutputStream}s.
 * 
 * @author giuliano
 */
public class TimedFlusher implements Runnable {

	private static final Logger fLogger = Logger.getLogger(TimedFlusher.class);

	private final long fFlushInterval;

	private final CopyOnWriteArrayList<Flushable> fFlushables = new CopyOnWriteArrayList<Flushable>();

	public TimedFlusher(long flushInterval) {
		fFlushInterval = flushInterval;
	}

	public void add(Flushable flushable) {
		fFlushables.add(flushable);
	}

	public void remove(Flushable flushable) {
		fFlushables.remove(flushable);
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {

			for (Flushable flushable : fFlushables) {
				try {
					flushable.flush();
				} catch (IOException ex) {
					fLogger.error("Failed to flush buffer", ex);
				}
			}

			try {
				Thread.sleep(fFlushInterval);
			} catch (InterruptedException e) {
				// Restore interruption state.
				Thread.currentThread().interrupt();
			}

		}
	}
}
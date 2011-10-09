package it.unitn.disi.utils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class ParallelDecompressingStream extends InputStream implements
		Runnable {

	private static final int BUFFER_SIZE = 8192;

	private final BlockingQueue<ReadBlock> fReadyBlocks;

	private final BlockingQueue<ReadBlock> fPooledBlocks;

	private final SafeBufferReader fReader;
	
	private ReadBlock fCurrent;
	
	private int fIdx;

	public ParallelDecompressingStream(GZIPInputStream source) {
		this(50, source);
	}

	public ParallelDecompressingStream(int blocks, GZIPInputStream stream) {
		fReadyBlocks = new LinkedBlockingQueue<ReadBlock>();
		fPooledBlocks = new LinkedBlockingQueue<ReadBlock>();
		fReader = new SafeBufferReader(stream);

		for (int i = 0; i < blocks; i++) {
			fPooledBlocks.add(new ReadBlock());
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			ReadBlock block = null;
			try {
				block = fPooledBlocks.poll(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}

			fillBlock(block);

			try {
				fReadyBlocks.offer(block, Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				break;
			}

			if (block.error != null || fReader.eof()) {
				break;
			}
		}
	}

	private void fillBlock(ReadBlock blk) {
		try {
			blk.filled = fReader.fillBuffer(blk.buffer, blk.buffer.length);
		} catch (IOException ex) {
			blk.filled = -1;
			blk.error = ex;
		}
	}

	@Override
	public int read() throws IOException {
		
		if (fCurrent == null) {
	//		fCurrent = nextBlock();
		}
		
		while (fIdx == fCurrent.filled) {
//			fCurrent = nextBlock();
			fIdx = 0;
		}
		
		fIdx++;
		return fCurrent.buffer[fIdx - 1];
	}

	class ReadBlock {
		byte[] buffer;
		int filled;
		IOException error;

		public ReadBlock() {
			buffer = new byte[BUFFER_SIZE];
		}
	}
}

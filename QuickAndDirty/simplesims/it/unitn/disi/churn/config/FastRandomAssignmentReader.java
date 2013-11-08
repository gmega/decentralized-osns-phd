package it.unitn.disi.churn.config;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FastRandomAssignmentReader {

	private static final int RECORD_LENGTH = Double.SIZE * 2;

	private final RandomAccessFile fFile;
	
	private int fIndex;

	private double fLi;

	private double fDi;

	public FastRandomAssignmentReader(File file) throws IOException {
		fFile = new RandomAccessFile(file, "r");
	}

	public void select(int id) throws IOException {
		fFile.seek(id * RECORD_LENGTH);
		fLi = fFile.readDouble();
		fDi = fFile.readDouble();
		fIndex = id;
	}

	public double ai(int id) throws IOException {
		if (id != fIndex) {
			select(id);
		}
		return ai();
	}

	public double li() {
		return fLi;
	}

	public double di() {
		return fDi;
	}

	public double ai() {
		return fLi / (fLi + fDi);
	}
}

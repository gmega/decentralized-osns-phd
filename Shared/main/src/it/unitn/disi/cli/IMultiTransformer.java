package it.unitn.disi.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IMultiTransformer {
	public void execute (InputStream [] istreams, OutputStream [] ostreams) throws IOException;
}

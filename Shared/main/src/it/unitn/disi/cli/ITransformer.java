package it.unitn.disi.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ITransformer {
	public void execute(InputStream is, OutputStream oup) throws IOException;
}

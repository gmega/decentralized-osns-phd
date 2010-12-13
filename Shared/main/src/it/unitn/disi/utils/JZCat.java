package it.unitn.disi.utils;

import it.unitn.disi.cli.ITransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;

import peersim.config.AutoConfig;

@AutoConfig
public class JZCat implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		try {
			execute0(is, oup);
		} catch (java.io.EOFException ex) {
			System.err.println("Unexpected end-of-file found.");
		}
	}

	private void execute0(InputStream is, OutputStream oup) throws IOException {
		InputStreamReader reader = new InputStreamReader(
				new GZIPInputStream(is));

		OutputStreamWriter writer = new OutputStreamWriter(oup);

		char[] buffer = new char[1024];
		int read = 0;

		while (read != -1) {
			read = reader.read(buffer);
			writer.write(buffer, 0, read);
		}
	}
}

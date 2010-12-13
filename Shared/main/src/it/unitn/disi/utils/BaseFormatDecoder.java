package it.unitn.disi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class BaseFormatDecoder {
	public static InputStream open(File file) throws IOException{
		String name = file.getName();
		InputStream is = new FileInputStream(file);
		if (name.endsWith(".gz")) {
			return new GZIPInputStream(is);
		}
		
		return is;
	}
}

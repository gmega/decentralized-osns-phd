package it.unitn.disi.protocol;

import java.io.IOException;
import java.io.InputStream;

import peersim.core.Protocol;

public interface IInputStreamProvider extends Protocol{
	public InputStream get() throws IOException;
}

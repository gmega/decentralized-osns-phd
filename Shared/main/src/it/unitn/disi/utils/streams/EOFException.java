package it.unitn.disi.utils.streams;

public class EOFException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EOFException(String message) {
		super(message);
	}

	public EOFException(Throwable cause) {
		super(cause);
	}

}

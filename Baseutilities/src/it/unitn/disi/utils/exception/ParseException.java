package it.unitn.disi.utils.exception;

public class ParseException extends RuntimeException {

	private static final long serialVersionUID = -8530669382324247933L;

	public ParseException(String message) {
		super(message);
	}

	public ParseException(Throwable cause) {
		super(cause);
	}

}

package scheduler.exception;

import java.rmi.RemoteException;

public class NoSuchElementException extends RemoteException {

	private static final long serialVersionUID = -8406556713193884323L;

	public NoSuchElementException(String s) {
		super(s);
	}

}

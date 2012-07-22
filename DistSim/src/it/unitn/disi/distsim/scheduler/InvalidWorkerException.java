package it.unitn.disi.distsim.scheduler;

import java.rmi.RemoteException;

public class InvalidWorkerException extends RemoteException {

	private static final long serialVersionUID = 1L;

	public InvalidWorkerException() {
		super();
	}

	public InvalidWorkerException(String s, Throwable cause) {
		super(s, cause);
	}

	public InvalidWorkerException(String s) {
		super(s);
	}

}

package it.unitn.disi.distsim.dataserver;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class DataManagerImpl implements IDataManager {

	private static final Logger fLogger = Logger
			.getLogger(DataManagerImpl.class);

	private final File fChkpFolder;

	private final File fConfigsFolder;

	private final String fSimId;

	private final ConcurrentHashMap<Integer, Boolean> fPending = new ConcurrentHashMap<Integer, Boolean>();

	public DataManagerImpl(File chkpFolder, File configsFolder, String simId) {
		fChkpFolder = chkpFolder;
		fConfigsFolder = configsFolder;
		fSimId = simId;
	}

	@Override
	public void writeCheckpoint(int id, byte[] state) throws RemoteException {
		acquire(id);

		FileOutputStream oStream = null;
		try {
			oStream = new FileOutputStream(chkpFile(id));
			oStream.write(state);
		} catch (IOException ex) {
			fLogger.error("Failed to create checkpoint " + id + " for sim "
					+ fSimId, ex);
		} finally {
			close(oStream);
			release(id);
		}

	}

	@Override
	public WorkUnit workUnit(int expId, String wtype) throws RemoteException {
		try {
			return new WorkUnit(readConfig(expId, wtype), readCheckpoint(expId));
		} catch (IOException ex) {
			throw new RemoteException("Failed to read config file "
					+ configFile(expId, wtype).getAbsolutePath() + ".");
		}
	}

	private byte[] readCheckpoint(int id) throws RemoteException {
		acquire(id);

		FileInputStream iStream = null;

		try {

			File f = chkpFile(id);
			if (!f.exists()) {
				return null;
			}

			iStream = new FileInputStream(f);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int read;
			while ((read = iStream.read()) != -1) {
				buffer.write(read);
			}

			return buffer.toByteArray();

		} catch (IOException ex) {
			fLogger.error("Failed to read checkpoint " + id + " for sim "
					+ fSimId, ex);
			return null;
		} finally {
			close(iStream);
			release(id);
		}
	}

	@Override
	public void clearCheckpoints(int wid) throws RemoteException {
		acquire(wid);
		try {
			File file = chkpFile(wid);
			if (file != null) {
				file.delete();
			}
		} finally {
			release(wid);
		}
	}

	private Properties readConfig(int id, String wtype) throws IOException {
		FileInputStream iStream = null;
		try {
			File cFile = configFile(id, wtype);
			if (cFile == null) {
				return null;
			}
			iStream = new FileInputStream(cFile);
			Properties props = new Properties();
			props.load(iStream);
			return props;
		} finally {
			close(iStream);
		}
	}

	public void close(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ex) {
			fLogger.error("Failed to close file stream (" + fSimId + ").", ex);
		}
	}

	private File chkpFile(int id) {
		return new File(fChkpFolder, id + ".ckp");
	}

	private File configFile(int id, String wtype) {
		// First, try worker-specific config file.
		File file = new File(fConfigsFolder, wtype + "-" + fSimId
				+ ".properties");

		// If doesn't exist, retries with simulation-wide config file.
		if (!file.exists()) {
			file = new File(fConfigsFolder, fSimId + ".properties");
		}

		// Nope, no config file.
		if (!file.exists()) {
			file = null;
		}

		return file;
	}

	public void release(int id) {
		fPending.remove(id);
	}

	public void acquire(int id) throws ConcurrentAccessException {
		if (fPending.putIfAbsent(id, true) != null) {
			throw new ConcurrentAccessException();
		}
	}

	public static class ConcurrentAccessException extends RemoteException {
		private static final long serialVersionUID = 1L;
	}

}

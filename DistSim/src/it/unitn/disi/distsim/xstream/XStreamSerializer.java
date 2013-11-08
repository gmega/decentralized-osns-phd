package it.unitn.disi.distsim.xstream;

import it.unitn.disi.distsim.control.ISerializer;
import it.unitn.disi.distsim.dataserver.CheckpointManager;
import it.unitn.disi.distsim.scheduler.Scheduler;
import it.unitn.disi.distsim.streamserver.StreamServer;
import it.unitn.disi.utils.MiscUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.management.NotificationBroadcasterSupport;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XStreamSerializer implements ISerializer {

	private XStream fStream = new XStream(new DomDriver());
	
	public XStreamSerializer() {
		configure();
	}

	public void addClass(Class<?> klass) {
		fStream.processAnnotations(klass);
	}

	private void configure() {
		fStream.omitField(NotificationBroadcasterSupport.class, "notifInfo");
		fStream.omitField(NotificationBroadcasterSupport.class, "executor");
		fStream.omitField(NotificationBroadcasterSupport.class, "listenerList");

		fStream.registerConverter(new NotificationBroadcastConverter(fStream
				.getMapper(), fStream.getReflectionProvider()));
		fStream.processAnnotations(Scheduler.class);
		fStream.processAnnotations(CheckpointManager.class);
		fStream.processAnnotations(StreamServer.class);
	}

	@Override
	public void saveObject(Object object, File file)
			throws FileNotFoundException {
		FileOutputStream oStream = null;
		try {
			oStream = new FileOutputStream(file);
			fStream.toXML(object, oStream);
		} finally {
			MiscUtils.safeClose(oStream, true);
		}
	}

	@Override
	public Object loadObject(File file) throws FileNotFoundException {
		FileInputStream iStream = null;
		try {
			iStream = new FileInputStream(file);
			return fStream.fromXML(file);
		} finally {
			MiscUtils.safeClose(iStream, true);
		}
	}
}

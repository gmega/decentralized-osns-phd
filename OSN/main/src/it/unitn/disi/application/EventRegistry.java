package it.unitn.disi.application;


import java.util.HashMap;

public class EventRegistry {
	
	private static final EventRegistry fInstance = new EventRegistry();
	
	private HashMap<Tweet, Info> fTime = new HashMap<Tweet, Info>();
	
	public static EventRegistry getInstance() {
		return fInstance;
	}
	
	public void addEvent(Tweet evt, Info info) {
		fTime.put(evt, info);
	}
	
	public Integer getTime(Tweet evt) {
		return fTime.get(evt).fTime;
	}
	
	public void received(Tweet evt) {
		Info info = fTime.get(evt);
		info.fRecipients--;
		if (info.fRecipients == 0) {
			fTime.remove(evt);
		}
	}
	
	public static class Info {
		int fTime;
		int fRecipients;
		
		public Info(long time, int recipients) {
			fTime = (int) time;
			fRecipients = recipients;
		}
	}
}


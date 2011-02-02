package it.unitn.disi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class SimpleScheduler implements Iterable<Integer> {

	private ArrayList<Integer> fSchedule;

	public SimpleScheduler(@Attribute("schedule") String schedule) {
		try {
			fSchedule = loadSchedule(schedule);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private ArrayList<Integer> loadSchedule(String schedule) throws IOException {
		File schedFile = new File(schedule);
		BufferedReader reader = new BufferedReader(new FileReader(schedFile));
		String line;
		while ((line = reader.readLine()) != null) {
			fSchedule.add(Integer.parseInt(line));
		}
		return fSchedule;
	}

	@Override
	public Iterator<Integer> iterator() {
		return fSchedule.iterator();
	}

}

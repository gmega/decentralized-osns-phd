package it.unitn.disi.newscasting.experiments.schedulers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * {@link IDListScheduler} takes node IDs from a list on a file and schedules them as they
 * are.
 */
@AutoConfig
public class IDListScheduler implements IStaticSchedule {

	private ArrayList<Integer> fSchedule;

	public IDListScheduler(@Attribute("schedule") String schedule) {
		try {
			fSchedule = loadSchedule(schedule);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private ArrayList<Integer> loadSchedule(String scheduleFileName) throws IOException {
		File schedFile = new File(scheduleFileName);
		BufferedReader reader = new BufferedReader(new FileReader(schedFile));
		ArrayList<Integer> schedule = new ArrayList<Integer>();
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.equals("")) {
				continue;
			}
			schedule.add(Integer.parseInt(line));
		}
		return schedule;
	}

	@Override
	public IScheduleIterator iterator() {
		return new StaticScheduleIterator(this);
	}

	@Override
	public int size() {
		return fSchedule.size();
	}

	@Override
	public int get(int index) {
		return fSchedule.get(index);
	}

}

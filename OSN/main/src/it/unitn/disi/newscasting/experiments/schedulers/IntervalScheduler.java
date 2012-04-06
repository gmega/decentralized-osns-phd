package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.ArrayList;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class IntervalScheduler implements ISchedule {
	
	private final ArrayList<Integer> fIntervals = new ArrayList<Integer>();
	
	private int fSize;
	
	public IntervalScheduler(@Attribute("idlist") String idlist) {
		this(idlist.split(" "));
	}
	
	public IntervalScheduler(String [] intervals) {
		for (int i = 0; i < intervals.length; i++){
			String [] interval = intervals[i].split(",");
			
			if (interval.length != 2) {
				throw new RuntimeException("Malformed interval " + intervals[i] + ".");
			}
			
			this.addInterval(Integer.parseInt(interval[0]), Integer.parseInt(interval[1]));
		}
	}
	
	public void addInterval(int start, int end) {
		int last = Integer.MIN_VALUE;
		
		if (!fIntervals.isEmpty()) {
			last = fIntervals.get(fIntervals.size() - 1);
		}
		
		if (start <= last) {
			throw new IllegalArgumentException("Intervals must be sorted and non-overlapping.");
		}
		
		fIntervals.add(start);
		fIntervals.add(end);
		
		fSize += (end - start + 1);
	}
	
	public int size () {
		return fSize;
	}
	
	public IScheduleIterator iterator() {
		
		return new IScheduleIterator () {
			
			private int fCurrent = fIntervals.get(0) - 1;

			private int fIndex;
			
			@Override
			public Object nextIfAvailable() {
				if (upper() >= fIntervals.size()) {
					return IScheduleIterator.DONE;
				}
				
				fCurrent++;
				// If we reached the upper bound, switch to the
				// next interval.
				if (fCurrent > fIntervals.get(upper())) {
					fIndex++;
					if (lower() < fIntervals.size()) {
						fCurrent = fIntervals.get(lower());
					} else {
						return IScheduleIterator.DONE;
					}
				}

				return fCurrent;
			}

			@Override
			public int remaining() {
				return fSize - fIndex;
			}
			
			private int lower() {
				return 2*fIndex;
			}
			
			private int upper() {
				return 2*fIndex + 1;
			}
		};
	}
}
	
package it.unitn.disi.utils;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.google.common.collect.PeekingIterator;

public class IntervalScheduler implements Iterable<Integer> {
	
	public static IntervalScheduler createScheduler(String idList) {
		String [] intervals = idList.split(" ");
		IntervalScheduler scheduler = new IntervalScheduler();
		
		for (int i = 0; i < intervals.length; i++){
			String [] interval = intervals[i].split(",");
			
			if (interval.length != 2) {
				throw new RuntimeException("Malformed interval " + intervals[i] + ".");
			}
			
			scheduler.addInterval(Integer.parseInt(interval[0]), Integer.parseInt(interval[1]));
		}
		
		return scheduler;
	}
	
	private final ArrayList<Integer> fIntervals = new ArrayList<Integer>();
	
	public void addInterval(int start, int end) {
		int last = Integer.MIN_VALUE;
		
		if (!fIntervals.isEmpty()) {
			last = fIntervals.get(fIntervals.size() - 1);
		}
		
		if (start < last) {
			throw new IllegalArgumentException("Intervals must be sorted.");
		}
		
		fIntervals.add(start);
		fIntervals.add(end);
		
	}
	
	public PeekingIterator<Integer> iterator() {
		
		return new PeekingIterator<Integer>() {
			
			private int fCurrent = fIntervals.get(0);

			private int fIndex;
			
			@Override
			public boolean hasNext() {
				return lower() < fIntervals.size();
			}

			@Override
			public Integer next() {
				if(!hasNext()) {
					throw new NoSuchElementException();
				}
				
				int next = fCurrent;
				
				fCurrent++;
				// If we reached the upper bound, switch to the 
				// next interval.
				if (fCurrent > fIntervals.get(upper())) {
					fIndex++;
					if (hasNext()) {
						fCurrent = fIntervals.get(lower());
					}
				}
				
				return next;
			}
			
			@Override
			public Integer peek() {
				if(!hasNext()) {
					throw new NoSuchElementException();
				}
				return fCurrent;
			}
			
			private int lower() {
				return 2*fIndex;
			}
			
			private int upper() {
				return 2*fIndex + 1;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
	
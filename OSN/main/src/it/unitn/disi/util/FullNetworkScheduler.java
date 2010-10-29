package it.unitn.disi.util;

import it.unitn.disi.utils.collections.PeekingIteratorAdapter;

import java.util.Iterator;

import peersim.config.AutoConfig;
import peersim.core.Network;

@AutoConfig
public class FullNetworkScheduler implements Iterable<Integer> {

	public FullNetworkScheduler() {
	}

	@Override
	public Iterator<Integer> iterator() {
		return new PeekingIteratorAdapter<Integer>(new Iterator<Integer>() {

			private int fCurrent = 0;

			@Override
			public boolean hasNext() {
				return fCurrent < Network.size();
			}

			@Override
			public Integer next() {
				return fCurrent++;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		});
	}
}

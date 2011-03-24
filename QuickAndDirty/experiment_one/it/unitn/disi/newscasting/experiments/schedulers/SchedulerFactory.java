package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.core.Node;

public class SchedulerFactory {

	private static final String PAR_ORDERING = "ordering";

	enum SchedulerType {
		FULL_NETWORK, ORDERED_FULLNETWORK;
	}

	private static final SchedulerFactory fInstance = new SchedulerFactory();

	public static SchedulerFactory getInstance() {
		return fInstance;
	}

	private SchedulerFactory() {
	}

	public Iterable<Integer> createScheduler(IResolver resolver, String prefix) {
		String type = resolver.getString(prefix, IResolver.NULL_KEY);
		switch (SchedulerType.valueOf(type.toUpperCase())) {

		case FULL_NETWORK:
			return new FullNetworkScheduler();
		
		case ORDERED_FULLNETWORK:
			@SuppressWarnings("unchecked")
			Comparator<Node> ordering = (Comparator<Node>) Configuration.getInstance(prefix + "." + PAR_ORDERING);
			return new OrderedFullNetworkScheduler(ordering);
			
		default:
			throw new IllegalArgumentException("Unknown ordering type " + type + ".");
			
		}
	}
}

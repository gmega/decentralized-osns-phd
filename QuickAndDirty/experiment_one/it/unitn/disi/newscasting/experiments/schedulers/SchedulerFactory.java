package it.unitn.disi.newscasting.experiments.schedulers;

import it.unitn.disi.utils.peersim.INodeRegistry;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.core.Node;

public class SchedulerFactory {

	private static final String PAR_ORDERING = "ordering";

	enum SchedulerType {
		FULL_NETWORK, ORDERED_FULLNETWORK, DEGREE_CLASS_SAMPLING;
	}

	private static final SchedulerFactory fInstance = new SchedulerFactory();

	public static SchedulerFactory getInstance() {
		return fInstance;
	}

	private SchedulerFactory() {
	}

	public Iterable<Integer> createScheduler(IResolver resolver, String prefix,
			INodeRegistry registry) {
		String type = resolver.getString(prefix, IResolver.NULL_KEY);
		switch (SchedulerType.valueOf(type.toUpperCase())) {

		case FULL_NETWORK:
			return new AliveBitmapScheduler(new OrderedFullNetworkScheduler(),
					registry);

		case ORDERED_FULLNETWORK:
			@SuppressWarnings("unchecked")
			Comparator<Node> ordering = (Comparator<Node>) Configuration
					.getInstance(prefix + "." + PAR_ORDERING);
			return new AliveBitmapScheduler(new OrderedFullNetworkScheduler(
					ordering), registry);

		case DEGREE_CLASS_SAMPLING:
			return new AliveBitmapScheduler(ObjectCreator.createInstance(
					DegreeClassScheduler.class, prefix, resolver), registry);

		default:
			throw new IllegalArgumentException("Unknown ordering type " + type
					+ ".");

		}
	}
}

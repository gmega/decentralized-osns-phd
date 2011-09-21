package it.unitn.disi.newscasting.experiments.schedulers;

import it.unitn.disi.utils.peersim.INodeRegistry;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;
import peersim.core.Node;

public class SchedulerFactory {

	private static final String PAR_ORDERING = "ordering";

	private static final String PAR_REPETITIONS = "repetitions";

	enum SchedulerType {
		FULL_NETWORK, ORDERED_FULLNETWORK, DEGREE_CLASS_SAMPLING, IDLIST, INTERVAL
	}

	private static final SchedulerFactory fInstance = new SchedulerFactory();

	public static SchedulerFactory getInstance() {
		return fInstance;
	}

	private SchedulerFactory() {
	}

	public ISchedule createScheduler(IResolver resolver, String prefix,
			INodeRegistry registry) {
		String type = resolver.getString(prefix, IResolver.NULL_KEY);
		ISchedule base;
		switch (SchedulerType.valueOf(type.toUpperCase())) {

		case IDLIST:
			base = ObjectCreator.createInstance(IDListScheduler.class, prefix,
					resolver);
			break;

		case FULL_NETWORK:
			base = new AliveBitmapScheduler(new OrderedFullNetworkScheduler(),
					registry);
			break;

		case ORDERED_FULLNETWORK:
			@SuppressWarnings("unchecked")
			Comparator<Node> ordering = (Comparator<Node>) Configuration
					.getInstance(prefix + "." + PAR_ORDERING);
			base = new AliveBitmapScheduler(new OrderedFullNetworkScheduler(
					ordering), registry);
			break;
			
		case INTERVAL:
			base = ObjectCreator.createInstance(IntervalScheduler.class, prefix, resolver);
			break;

		case DEGREE_CLASS_SAMPLING:
			base = new AliveBitmapScheduler(ObjectCreator.createInstance(
					DegreeClassScheduler.class, prefix, resolver), registry);
			break;

		default:
			throw new IllegalArgumentException("Unknown ordering type " + type
					+ ".");

		}

		int repetitions = 0;
		try {
			repetitions = resolver.getInt(prefix, PAR_REPETITIONS);
		} catch (MissingParameterException ex) {
			// Don't care.
		}

		if (repetitions != 0) {
			base = new RepetitionDecorator(base, repetitions);
		}

		return base;
	}
}

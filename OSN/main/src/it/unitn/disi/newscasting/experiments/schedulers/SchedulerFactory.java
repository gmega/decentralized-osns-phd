package it.unitn.disi.newscasting.experiments.schedulers;

import it.unitn.disi.distsim.scheduler.SchedulerClient;
import it.unitn.disi.distsim.scheduler.generators.IDListScheduler;
import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.IntervalScheduler;
import it.unitn.disi.distsim.scheduler.generators.RepetitionDecorator;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;
import peersim.core.Node;

public class SchedulerFactory {

	private static final String PAR_SCHEDULERTYPE = "scheduler.type";
	
	private static final String PAR_ORDERING = "ordering";

	private static final String PAR_REPETITIONS = "repetitions";

	private static final String PAR_LOOP = "loop";

	enum SchedulerType {
		FULL_NETWORK, ORDERED_FULLNETWORK, DEGREE_CLASS_SAMPLING, IDLIST, INTERVAL, DISTRIBUTED
	}

	private static final SchedulerFactory fInstance = new SchedulerFactory();

	public static SchedulerFactory getInstance() {
		return fInstance;
	}

	private SchedulerFactory() {
	}

	public ISchedule createScheduler(IResolver resolver, String prefix) {
		String type = resolver.getString(prefix, PAR_SCHEDULERTYPE);
		ISchedule base;
		switch (SchedulerType.valueOf(type.toUpperCase())) {

		case IDLIST:
			base = ObjectCreator.createInstance(IDListScheduler.class, prefix,
					resolver);
			break;

		case FULL_NETWORK:
			base = new AliveBitmapScheduler(new OrderedFullNetworkScheduler(),
					resolver);
			break;

		case ORDERED_FULLNETWORK:
			@SuppressWarnings("unchecked")
			Comparator<Node> ordering = (Comparator<Node>) Configuration
					.getInstance(prefix + "." + PAR_ORDERING);
			base = new AliveBitmapScheduler(new OrderedFullNetworkScheduler(
					ordering), resolver);
			break;

		case INTERVAL:
			base = ObjectCreator.createInstance(IntervalScheduler.class,
					prefix, resolver);
			break;

		case DEGREE_CLASS_SAMPLING:
			base = new AliveBitmapScheduler(ObjectCreator.createInstance(
					DegreeClassScheduler.class, prefix, resolver), resolver);
			break;

		case DISTRIBUTED:
			base = ObjectCreator.createInstance(
					SchedulerClient.class, prefix, resolver);
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
			base = new RepetitionDecorator(base, repetitions,
					resolver.getBoolean(prefix, PAR_LOOP));
		}

		return base;
	}
}

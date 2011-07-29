package peersim.config.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import peersim.config.IResolver;
import peersim.config.MissingParameterException;
import peersim.config.ObjectCreator;
import peersim.config.resolvers.CompositeResolver;
import peersim.config.resolvers.NullResolver;
import peersim.graph.Graph;
import peersim.graph.NeighbourListGraph;

public class PluginContainer extends NullResolver {

	private final IPluginDescriptor[] fDescriptors;

	private final IResolver fResolver;

	private final Map<String, Object> fPluginRegistry;

	private final Map<String, IPluginDescriptor> fDescriptorRegistry;

	private boolean fStarted = false;

	public PluginContainer(IResolver resolver, IPluginDescriptor... descriptors) {
		fDescriptors = descriptors;
		fResolver = resolver;
		fPluginRegistry = Collections
				.synchronizedMap(new HashMap<String, Object>());
		fDescriptorRegistry = Collections
				.synchronizedMap(new HashMap<String, IPluginDescriptor>());
	}

	public void start() {
		if (fStarted) {
			throw new IllegalStateException(
					"Plugin container cannot be started twice.");
		}
		/*
		 * Well it's not really started yet, but if something goes wrong here
		 * we're not making provisions to clean up the state anyway, and this
		 * code is not meant to be thread-safe.
		 */
		fStarted = true;

		IPluginDescriptor[] descriptors = topologicalSort(reverseDependencies(fDescriptors));

		CompositeResolver composite = new CompositeResolver();
		composite.addResolver(fResolver, this);
		ObjectCreator creator = new ObjectCreator(composite.asResolver());

		// Registers shutdown hook before we start firing plugins.
		Runtime.getRuntime().addShutdownHook(new Thread(new PluginShutdown()));

		for (int i = 0; i < descriptors.length; i++) {
			IPluginDescriptor descriptor = descriptors[i];
			try {
				// 1. Registers the descriptor.
				fDescriptorRegistry.put(descriptor.id() + ".descriptor",
						descriptor);

				lifecycleMessage("create instance of", descriptor);

				// 2. Create.
				Object object = creator.create(
						descriptor.configurationPrefix(),
						descriptor.pluginClass());

				// 3. Start.
				String what = what(object);
				lifecycleMessage("start " + what, descriptor);
				if (isPlugin(object)) {
					IPlugin plugin = (IPlugin) object;
					fPluginRegistry.put(plugin.id(), plugin);
					plugin.start(fResolver);
				} else if (isInitializer(object)) {
					IGenericServiceInitializer initializer = (IGenericServiceInitializer) object;
					initializer.run(this);
				} else {
					throw new InternalError();
				}

			} catch (Exception ex) {
				if (ex instanceof RuntimeException) {
					throw (RuntimeException) ex;
				}
				throw new RuntimeException(ex);
			}
		}
	}

	@Override
	public Object getObject(String prefix, String key) {
		if (key.endsWith(".descriptor")) {
			return lookup(fDescriptorRegistry, key);
		} else {
			return lookup(fPluginRegistry, key);
		}
	}

	public void registerObject(String fullName, Object object) {
		fPluginRegistry.put(fullName, object);
	}

	private void lifecycleMessage(String message, IPluginDescriptor descriptor) {
		System.err.println(PluginContainer.class.getSimpleName() + ": "
				+ message + " " + descriptor.id() + " ("
				+ descriptor.pluginClass().getName() + ").");
	}

	private boolean isPlugin(Object obj) {
		return IPlugin.class.isAssignableFrom(obj.getClass());
	}

	private boolean isInitializer(Object obj) {
		return IGenericServiceInitializer.class
				.isAssignableFrom(obj.getClass());
	}

	private String what(Object obj) {
		if (isPlugin(obj)) {
			return "plug-in";
		} else if (isInitializer(obj)) {
			return "initializer";
		} else {
			throw new IllegalArgumentException("Class "
					+ obj.getClass().getName()
					+ " must implement either plugin "
					+ "or initializer interfaces.");
		}
	}

	private Object lookup(Map<String, ? extends Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			throw new MissingParameterException(key);
		}
		return value;
	}

	private Graph reverseDependencies(IPluginDescriptor[] plugins) {
		NeighbourListGraph deps = new NeighbourListGraph(true);
		Map<String, Integer> idxMap = new HashMap<String, Integer>();

		for (int i = 0; i < plugins.length; i++) {
			String id = plugins[i].id();
			deps.addNode(plugins[i]);
			idxMap.put(id, i);
		}

		// Goes through plugins and marks the deps.
		for (int i = 0; i < plugins.length; i++) {
			for (String dependency : plugins[i].depends()) {
				Integer idx = idxMap.get(dependency);
				if (idx == null) {
					throw new IllegalStateException("Unknown dependency "
							+ dependency + " for plugin " + plugins[i].id());
				}
				deps.setEdge(i, idx);
			}
		}

		return deps;
	}

	class PluginShutdown implements Runnable {

		@Override
		public void run() {
			IPlugin current = null;
			for (Object object : fPluginRegistry.values()) {
				try {
					if (object instanceof IPlugin) {
						current = (IPlugin) object;
						current.stop();
					}
				} catch (Exception ex) {
					System.err.println("Error while stopping plugin "
							+ current.id());
					ex.printStackTrace();
				}
			}
		}

	}

	// ------------------------------------------------------------------------
	// Machinery for handling dependency graphs.
	// ------------------------------------------------------------------------

	// Tuple for DFS stackframes.
	static class DFSFrame {
		Iterator<Integer> counter;
		int id;

		public DFSFrame(Iterator<Integer> counter, int id) {
			this.counter = counter;
			this.id = id;
		}
	}

	// Tuple for DFS state.
	static class DFSState {

		Graph graph;

		boolean[] processing;
		boolean[] processed;

		ArrayList<DFSFrame> stack;
		ArrayList<Integer> sorted;

		public DFSState(Graph graph) {
			stack = new ArrayList<DFSFrame>();
			sorted = new ArrayList<Integer>();
			processing = new boolean[graph.size()];
			processed = new boolean[graph.size()];
			this.graph = graph;
		}
	}

	private IPluginDescriptor[] topologicalSort(Graph graph) {
		DFSState s = new DFSState(graph);
		for (int i = 0; i < graph.size(); i++) {
			if (s.processed[i]) {
				continue;
			}
			singleDFS(s, i);
		}

		IPluginDescriptor[] sorted = new IPluginDescriptor[graph.size()];
		for (int i = 0; i < sorted.length; i++) {
			sorted[i] = (IPluginDescriptor) graph.getNode(s.sorted.get(i));
		}

		return sorted;
	}

	private void singleDFS(DFSState s, int root) throws IllegalStateException {

		// Pushes the root node onto the stack.
		s.processing[root] = true;
		s.stack.add(new DFSFrame(s.graph.getNeighbours(root).iterator(), root));

		// Iterative DFS.
		while (s.stack.size() != 0) {
			DFSFrame current = s.stack.get(s.stack.size() - 1);
			if (current.counter.hasNext()) {
				Integer neighbor = current.counter.next();
				// Ooops, cycle found.
				if (s.processing[neighbor]) {
					throw new IllegalStateException(
							"Plugin dependencies have cycles: "
									+ cycleToString(s.stack, s.graph));
				}

				if (!s.processed[neighbor]) {
					s.stack.add(new DFSFrame(s.graph.getNeighbours(neighbor)
							.iterator(), neighbor));
				}
			} else {
				DFSFrame done = s.stack.remove(s.stack.size() - 1);
				s.sorted.add(done.id);
				s.processed[done.id] = true;
				s.processing[done.id] = false;
			}
		}
	}

	private String cycleToString(List<DFSFrame> aCycle, Graph graph) {
		StringBuffer buffer = new StringBuffer();
		String first = null;
		for (DFSFrame dfsFrame : aCycle) {
			IPluginDescriptor plugin = (IPluginDescriptor) graph
					.getNode(dfsFrame.id);
			buffer.append("(");
			buffer.append(plugin.id());
			buffer.append(") <- ");
			if (first == null) {
				first = plugin.id();
			}
		}

		buffer.append("(");
		buffer.append(first);
		buffer.append(")");

		return buffer.toString();
	}

}

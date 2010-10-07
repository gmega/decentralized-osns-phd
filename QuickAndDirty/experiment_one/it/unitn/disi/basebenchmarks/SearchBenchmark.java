package it.unitn.disi.basebenchmarks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class SearchBenchmark {
	
	static Random random = new Random();
	
	public static void main(String[] args) {
		int max = Integer.parseInt(args[0]);
		int searches = Integer.parseInt(args[1]);
		Object[] object = new Object[max];

		for (int i = 0; i < max; i++) {
			object[i] = new Object();
		}

		ArrayList<Object> list = new ArrayList<Object>();
		HashSet<Object> set = new HashSet<Object>();

		for (int i = 0; i < max; i++) {
			long time = System.currentTimeMillis();
			list.clear();
			listExperiment(searches, list, object);
			time = System.currentTimeMillis() - time;
			System.out.println("LIST:" + time);

			time = System.currentTimeMillis();
			set.clear();
			setExperiment(searches, set, object);
			time = System.currentTimeMillis() - time;
			System.out.println("SET:" + time);
		}
	}

	private static void listExperiment(int searches, ArrayList<Object> list, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			list.set(i, array[i]);
		}
		
		for (int i = 0; i < searches; i++) {
			list.contains(array[random.nextInt(array.length)]);
		}
	}
	
	private static void setExperiment(int searches, HashSet<Object> set, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			set.add(array[i]);
		}
		
		for (int i = 0; i < searches; i++) {
			set.contains(array[random.nextInt(array.length)]);
		}
	}

}
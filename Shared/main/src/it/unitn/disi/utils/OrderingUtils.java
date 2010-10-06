package it.unitn.disi.utils;


import it.unitn.disi.utils.collections.IExchanger;

import java.util.Random;

public class OrderingUtils {

	/**
	 * Given an array <b>A</b> and an integer <b>k</b>, reorders <b>A</b> such
	 * that:
	 * 
	 * <ol>
	 * <li>the kth largest element of A is in position A[k - 1];
	 * <li>elements from A[0...k-2] are all smaller than A[k - 1];
	 * <li>elements from A[k...|A| - 1] are all larger than A[k - 1].
	 * </ol>
	 * 
	 * In other words, this method puts the k-th largest element of array
	 * <b>A</b> in position <b>A[k - 1]</b>.
	 * 
	 * @param k
	 *            the value of k to use.
	 * @param start
	 *            the beginning of the subarray on which to operate (will
	 *            consider that the array begins at <b>start</b>).
	 * @param end
	 *            the end of the subarray on which to operate (will consider
	 *            that the array ends at <b>end</b>
	 * @param array
	 *            the actual array to operate on.
	 * @param exchanger
	 *            an {@link IExchanger} implementor, which should perform the
	 *            array swap operations.
	 * @param r
	 *            a random number generator ({@link Random}).
	 */
	public static int orderByKthLargest(int k, int start, int end, int[] array,
			IExchanger exchanger, Random r) {

		int cStart = start;
		int cEnd = end;
		int pivot = -1;

		while (pivot != k) {
			pivot = cStart + r.nextInt((cEnd - cStart) + 1);
			pivot = partition(cStart, cEnd, pivot, array, exchanger);
			if (k < pivot) {
				cEnd = pivot - 1;
			} else {
				cStart = pivot + 1;
			}
		}
		return array[k];
	}

	public static int partition(int start, int end, int pivot, int[] array,
			IExchanger xchg) {
		int pivotValue = array[pivot];
		xchg.exchange(pivot, end);
		int j = start;
		for (int i = start; i < end; i++) {
			if (array[i] < pivotValue) {
				if (i != j) {
					xchg.exchange(i, j);
				}
				j++;
			}
		}
		xchg.exchange(end, j);
		return j;
	}

	/**
	 * Generates one of the n! permutations of an array with uniform
	 * probability.
	 * 
	 * @param start
	 *            index where to start permuting (inclusive)
	 * @param end
	 *            index where to stop permuting (exclusive)
	 * @param exchanger
	 *            an {@link IExchanger} implementor.
	 * @param r
	 *            a {@link Random} instance.
	 */
	public static void permute(int start, int end, IExchanger exchanger,
			Random r) {
		for (int i = start; i < end; i++) {
			int j = i + r.nextInt(end - i);
			if (i != j) {
				exchanger.exchange(i, j);
			}
		}
	}

	/**
	 * Same as {@link #permute(int, int, IExchanger, Random)}, except that
	 * instead of taking an exchanger, can operate directly into an array.
	 * 
	 * This method exists purely for efficiency reasons, since calling permute
	 * in an inner loop can become quite costly.
	 */
	public static void permute(int start, int end, int [] array,
			Random r) {
		for (int i = start; i < end; i++) {
			int j = i + r.nextInt(end - i);
			if (i != j) {
				int tmp = array[i];
				array[i] = array[j];
				array[j] = tmp;
			}
		}
	}
}

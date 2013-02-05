package it.unitn.disi.churn.intersync.markov;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;

public class PhaseTypeDistribution {

	private final double[][] fMatrix;

	private final double[] fAlpha;

	public PhaseTypeDistribution(double[][] matrix, double[] alpha) {
		fMatrix = matrix;
		fAlpha = alpha;
	}

	public PhaseTypeDistribution sum(PhaseTypeDistribution other) {
		final double[][] T = fMatrix;
		final double[][] S = other.fMatrix;

		final double[] alpha = fAlpha;
		final double[] beta = other.fAlpha;

		int dim = S.length + T.length - 1;

		double[][] L = new double[dim][dim];

		// Copies transitions among transients.
		transfer(T, L, 0, 0, T.length - 1, T.length - 1);
		// Matrix S gets copied whole because the new "shared absorbing state"
		// is the one from S.
		transfer(S, L, (T.length - 1), (T.length - 1), (S.length - 1), S.length);

		// Links to absorbing state in T get rewired into S.
		populate(L, 0, (T.length - 1), (T.length - 1), S.length, new ITransfer() {
			@Override
			public double value(int i, int j) {
				return T[i][T[i].length - 1] * beta[j];
			}
		});

		double[] gamma = new double[alpha.length + beta.length - 1];
		for (int i = 0; i < (alpha.length - 1); i++) {
			gamma[i] = alpha[i];
		}

		for (int i = 0; i < (beta.length - 1); i++) {
			gamma[i + alpha.length - 1] = beta[i] * alpha[alpha.length - 1];
		}
		
		double sum = 0.0;
		for (int i = 0; i < gamma.length; i++) {
			sum += gamma[i];
		}
		gamma[gamma.length - 1] = 1 - sum;
	
		return new PhaseTypeDistribution(L, gamma);
	}

	public double expectation() {
		DenseMatrix S = new DenseMatrix(fMatrix.length - 1, fMatrix.length - 1);
		for (int i = 0; i < (fMatrix.length - 1); i++) {
			for (int j = 0; j < (fMatrix.length - 1); j++) {
				S.set(i, j, fMatrix[i][j]);
			}
		}

		DenseMatrix I = Matrices.identity(S.numColumns());
		DenseMatrix SI = I.copy();
		S.solve(I, SI);

		// 1 x (m - 1)
		DenseMatrix alpha = new DenseMatrix(1, fAlpha.length - 1);
		for (int i = 0; i < (fAlpha.length - 1); i++) {
			alpha.set(0, i, -fAlpha[i]);
		}

		// (m - 1) x 1
		DenseMatrix one = new DenseMatrix(S.numColumns(), 1);
		for (int j = 0; j < one.numRows(); j++) {
			one.set(j, 0, 1);
		}

		DenseMatrix tmp = new DenseMatrix(1, S.numColumns());
		tmp = (DenseMatrix) alpha.mult(SI, tmp);

		DenseMatrix value = new DenseMatrix(1, 1);
		value = (DenseMatrix) tmp.mult(one, value);
		return value.get(0, 0);
	}

	private void transfer(final double[][] source, final double[][] target,
			int rowOff, int colOff, int rowSize, int colSize) {
		populate(target, rowOff, colOff, rowSize, colSize, new ITransfer() {

			@Override
			public double value(int i, int j) {
				return source[i][j];
			}

		});
	}

	private void populate(double[][] target, int rowOff, int colOff, int rowSize,
			int colSize, ITransfer elements) {

		for (int i = 0; i < rowSize; i++) {
			for (int j = 0; j < colSize; j++) {
				target[i + rowOff][j + colOff] = elements.value(i, j);
				//System.err.println(i + "," + j + ":" + target[i + rowOff][j + colOff]);
			}
		}

	}

	private interface ITransfer {
		public double value(int i, int j);
	}
}

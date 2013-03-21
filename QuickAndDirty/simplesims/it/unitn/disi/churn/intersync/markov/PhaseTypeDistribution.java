package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.math.MatrixUtils;
import jphase.DenseContPhaseVar;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;

public class PhaseTypeDistribution {

	private final DenseMatrix fMatrix;

	private final DenseVector fAlpha;

	public PhaseTypeDistribution(double[][] matrix, double[] alpha) {
		this(new DenseMatrix(matrix), new DenseVector(alpha));
	}

	public PhaseTypeDistribution(DenseMatrix matrix, DenseVector alpha) {
		fMatrix = matrix;
		fAlpha = alpha;
	}

	public PhaseTypeDistribution sum(PhaseTypeDistribution other) {
		final DenseMatrix T = fMatrix;
		final DenseMatrix S = other.fMatrix;

		final DenseVector alpha = fAlpha;
		final DenseVector beta = other.fAlpha;

		int dim = S.numRows() + T.numRows() - 1;

		DenseMatrix L = new DenseMatrix(dim, dim);

		// Copies transitions among transients.
		transfer(T, L, 0, 0, T.numRows() - 1, T.numRows() - 1);
		// Matrix S gets copied whole because the new "shared absorbing state"
		// is the one from S.
		transfer(S, L, (T.numRows() - 1), (T.numRows() - 1), (S.numRows() - 1),
				S.numRows());

		// Links to absorbing state in T get rewired into S.
		populate(L, 0, (T.numRows() - 1), (T.numRows() - 1), S.numRows(),
				new ITransfer() {
					@Override
					public double value(int i, int j) {
						return T.get(i, T.numColumns() - 1) * beta.get(j);
					}
				});

		DenseVector gamma = new DenseVector(alpha.size() + beta.size() - 1);
		for (int i = 0; i < (alpha.size() - 1); i++) {
			gamma.set(i, alpha.get(i));
		}

		for (int i = 0; i < (beta.size() - 1); i++) {
			gamma.set(i + alpha.size() - 1,
					beta.get(i) * alpha.get(alpha.size() - 1));
		}

		double sum = 0.0;
		for (int i = 0; i < gamma.size(); i++) {
			sum += gamma.get(i);
		}
		gamma.set(gamma.size() - 1, 1 - sum);

		return new PhaseTypeDistribution(L, gamma);
	}

	public int size() {
		return fMatrix.numColumns();
	}

	public PhaseTypeDistribution min(PhaseTypeDistribution other, long maxSize) {

		// Projected size of the matrix after the Kronecker product.
		long projected = fMatrix.numColumns()
				* ((long) other.fMatrix.numColumns()) * fMatrix.numRows()
				* other.fMatrix.numRows();

		System.err.println(projected + ", " + maxSize);

		if ((projected * 2) > maxSize) {
			return null;
		}

		// New matrix is the tensor sum of the two previous matrices.
		DenseMatrix innerS = MatrixUtils.kroneckerSum(getS(), other.getS());

		// Same for the new alpha vector (which we call gamma...).
		DenseVector innerGamma = MatrixUtils.kronecker(getAlpha(),
				other.getAlpha());

		DenseMatrix S = new DenseMatrix(innerS.numRows() + 1,
				innerS.numColumns() + 1);

		for (int i = 0; i < innerS.numColumns(); i++) {
			double rowSum = 0;
			for (int j = 0; j < innerS.numRows(); j++) {
				double value = innerS.get(i, j);
				S.set(i, j, value);
				rowSum += value;
			}

			// S is a generator matrix so rows sum to zero.
			S.set(i, S.numColumns() - 1, -rowSum);
		}

		DenseVector gamma = new DenseVector(innerGamma.size() + 1);
		double sum = 0;
		for (int i = 0; i < innerGamma.size(); i++) {
			double value = innerGamma.get(i);
			gamma.set(i, value);
			sum += value;
		}
		gamma.set(gamma.size() - 1, 1.0 - sum);

		return new PhaseTypeDistribution(S, gamma);
	}

	public DenseContPhaseVar getJPhaseDistribution() {
		return new DenseContPhaseVar(getAlpha(), getS());
	}

	private DenseVector getAlpha() {
		DenseVector alpha = new DenseVector(fAlpha.size() - 1);
		for (int i = 0; i < (fAlpha.size() - 1); i++) {
			alpha.set(i, fAlpha.get(i));
		}

		return alpha;
	}

	private DenseMatrix getS() {
		DenseMatrix S = new DenseMatrix(fMatrix.numRows() - 1,
				fMatrix.numColumns() - 1);
		for (int i = 0; i < (fMatrix.numRows() - 1); i++) {
			for (int j = 0; j < (fMatrix.numColumns() - 1); j++) {
				S.set(i, j, fMatrix.get(i, j));
			}
		}

		return S;
	}

	public double expectation() {
		DenseMatrix S = getS();

		DenseMatrix I = Matrices.identity(S.numColumns());
		DenseMatrix SI = I.copy();
		S.solve(I, SI);

		// 1 x (m - 1)
		DenseMatrix alpha = new DenseMatrix(1, fAlpha.size() - 1);
		for (int i = 0; i < (fAlpha.size() - 1); i++) {
			alpha.set(0, i, -fAlpha.get(i));
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

	private void transfer(final DenseMatrix source, final DenseMatrix target,
			int rowOff, int colOff, int rowSize, int colSize) {
		populate(target, rowOff, colOff, rowSize, colSize, new ITransfer() {

			@Override
			public double value(int i, int j) {
				return source.get(i, j);
			}

		});
	}

	private void populate(DenseMatrix target, int rowOff, int colOff,
			int rowSize, int colSize, ITransfer elements) {

		for (int i = 0; i < rowSize; i++) {
			for (int j = 0; j < colSize; j++) {
				target.set(i + rowOff, j + colOff, elements.value(i, j));
			}
		}

	}

	private interface ITransfer {
		public double value(int i, int j);
	}
}

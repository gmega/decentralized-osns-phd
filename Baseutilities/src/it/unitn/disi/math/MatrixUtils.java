package it.unitn.disi.math;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;

public class MatrixUtils {

	public static DenseMatrix kroneckerSum(DenseMatrix A, DenseMatrix B) {
		DenseMatrix AI = kronecker(A, getI(B));
		B = kronecker(getI(A), B);

		return sum(AI, B, B);
	}

	public static DenseMatrix sum(DenseMatrix A, DenseMatrix B, DenseMatrix C) {
		if (!dimensionsMatch(A, B) || !dimensionsMatch(B, C)) {
			throw new IllegalArgumentException(
					"Can't sum matrices of different dimensions.");
		}

		for (int i = 0; i < A.numColumns(); i++) {
			for (int j = 0; j < A.numRows(); j++) {
				C.set(i, j, A.get(i, j) + B.get(i, j));
			}
		}

		return C;
	}

	private static boolean dimensionsMatch(Matrix A, Matrix B) {
		return (A.numColumns() == B.numColumns())
				&& (A.numRows() == B.numRows());
	}

	public static DenseVector kronecker(DenseVector A, DenseVector B) {
		Matrix Am = rowMatrix(A);
		Matrix Bm = rowMatrix(B);

		return new DenseVector(kronecker(Am, Bm).getData());
	}

	public static DenseMatrix kronecker(Matrix A, Matrix B) {
		DenseMatrix C = new DenseMatrix(A.numRows() * B.numRows(),
				A.numColumns() * B.numColumns());

		for (int i = 0; i < A.numRows(); i++) {
			for (int j = 0; j < A.numColumns(); j++) {
				block(C, i * B.numRows(), j * B.numColumns(), A.get(i, j), B);
			}
		}

		return C;
	}

	private static void block(Matrix result, int rowOff, int colOff, double d,
			Matrix b) {
		for (int i = 0; i < b.numRows(); i++) {
			for (int j = 0; j < b.numColumns(); j++) {
				result.set(rowOff + i, colOff + j, d * b.get(i, j));
			}
		}
	}

	public static int col(double[][] m) {
		return m[0].length;
	}

	public static int row(double[][] m) {
		return m.length;
	}

	public static DenseMatrix rowMatrix(DenseVector a) {
		DenseMatrix matrix = new DenseMatrix(1, a.size());
		for (int i = 0; i < a.size(); i++) {
			matrix.set(0, i, a.get(i));
		}
		return matrix;
	}

	private static Matrix getI(Matrix A) {
		return new SimpleIdentityMatrix(A.numColumns());
	}

}

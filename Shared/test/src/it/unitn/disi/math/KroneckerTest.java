package it.unitn.disi.math;

import junit.framework.Assert;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;

import org.junit.Test;

public class KroneckerTest {
	@Test
	public void testKroneckerSum() {
		double [][] Ar = {
				{2, 3},
				{4, 5}
		};
		
		double [][] Br = {
				{6, 7},
				{8, 9}
		};
		
		double [][] Rr = {
				{Ar[0][0] + Br[0][0],	Br[0][1],				Ar[0][1],				0					},
				{Br[1][0],				Ar[0][0] + Br[1][1],	0,						Ar[0][1]			},
				{Ar[1][0],				0,						Ar[1][1] + Br[0][0],	Br[0][1]			},
				{0,						Ar[1][0],				Br[1][0],				Ar[1][1] + Br[1][1]	}
		};
		
		DenseMatrix A = new DenseMatrix(Ar);
		DenseMatrix B = new DenseMatrix(Br);
		DenseMatrix R = new DenseMatrix(Rr);

		Matrix C = MatrixUtils.kroneckerSum(A, B);

		Assert.assertEquals(R.numColumns(), C.numColumns());
		Assert.assertEquals(R.numRows(), C.numRows());

		for (int i = 0; i < R.numColumns(); i++) {
			for (int j = 0; j < R.numRows(); j++) {
				Assert.assertEquals(R.get(i, j), C.get(i, j));
			}
		}
		
	}
}

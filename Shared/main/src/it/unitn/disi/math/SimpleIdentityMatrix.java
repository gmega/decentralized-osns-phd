package it.unitn.disi.math;

import java.util.Iterator;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.MatrixNotSPDException;
import no.uib.cipr.matrix.MatrixSingularException;
import no.uib.cipr.matrix.Vector;

/**
 * Memory-efficient, readonly identity matrix.
 * 
 * @author giuliano
 */
public class SimpleIdentityMatrix implements Matrix {
	
	private final int fM;
	
	public SimpleIdentityMatrix(int dim) {
		fM = dim;
	}

	@Override
	public int numRows() {
		return fM;
	}

	@Override
	public int numColumns() {
		return fM;
	}

	@Override
	public boolean isSquare() {
		return true;
	}

	@Override
	public double get(int row, int column) {
		return (row == column) ? 1 : 0;
	}

	@Override
	public void set(int row, int column, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int row, int column, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<MatrixEntry> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix copy() {
		return this;
	}

	@Override
	public Matrix zero() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector mult(Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector mult(double alpha, Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector multAdd(Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector multAdd(double alpha, Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector transMult(Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector transMult(double alpha, Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector transMultAdd(Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector transMultAdd(double alpha, Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector solve(Vector b, Vector x) throws MatrixSingularException,
			MatrixNotSPDException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector transSolve(Vector b, Vector x)
			throws MatrixSingularException, MatrixNotSPDException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank1(Vector x) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank1(double alpha, Vector x) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank1(Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank1(double alpha, Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank2(Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank2(double alpha, Vector x, Vector y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix mult(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix mult(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix multAdd(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix multAdd(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transAmult(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transAmult(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transAmultAdd(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transAmultAdd(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transBmult(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transBmult(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transBmultAdd(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transBmultAdd(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transABmult(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transABmult(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transABmultAdd(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transABmultAdd(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix solve(Matrix B, Matrix X) throws MatrixSingularException,
			MatrixNotSPDException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transSolve(Matrix B, Matrix X)
			throws MatrixSingularException, MatrixNotSPDException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank1(Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank1(double alpha, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transRank1(Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transRank1(double alpha, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank2(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix rank2(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transRank2(Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transRank2(double alpha, Matrix B, Matrix C) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix scale(double alpha) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix set(Matrix B) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix set(double alpha, Matrix B) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix add(Matrix B) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix add(double alpha, Matrix B) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transpose() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Matrix transpose(Matrix B) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double norm(Norm type) {
		// TODO Auto-generated method stub
		return 0;
	}

}

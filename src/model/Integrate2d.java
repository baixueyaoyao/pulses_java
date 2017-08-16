package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import Jama.Matrix;

abstract public class Integrate2d{
	public int T,n_chemical,I,J,group;
	public double spanI,spanJ,hs,ht;
	public double[] k_R, c0,k_D;
	public String fullfilename;
	public Matrix[] data_t;
	public Matrix[][] M;

	public Integrate2d(){}
	abstract public void addPerturb();
	abstract public double[] f_R(double[] u);
	
	public void integrate(){
		this.data_t = new Matrix[this.n_chemical];
		for (int s=0; s<this.n_chemical; s++) this.data_t[s] = new Matrix(this.I,this.J,this.c0[s]); // initialize with homogenous concentration
		addPerturb();// add perturbation
		// setup diffusion matrix
		this.M = new Matrix[this.n_chemical][4];
		for (int s=0; s<this.n_chemical; s++) diffuse_ADI_matrix(s);
		try {
			// open output stream
			FileOutputStream fout = new FileOutputStream(new File(this.fullfilename),true);
			ObjectOutputStream oout = new ObjectOutputStream(fout);
			// write configuration parameters
			oout.writeObject(this.c0); oout.writeObject(ht); oout.writeObject(hs); 
			oout.writeObject(this.I); oout.writeObject(this.J); oout.writeObject(this.group*this.T); 
			// time step
			for (int k=0; k<(this.group*this.T); k++){
				react_diffuse(M);
				for (int s=0; s<this.n_chemical; s++) oout.writeObject(this.data_t[s].getArrayCopy());//write to file
			}
			oout.close();
			fout.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	public void react_diffuse(Matrix[][] M){
		// react
		for (int i=0; i<I; i++){
			for (int j=0; j<J; j++){
				double[] cell = new double[this.n_chemical];
				for (int s=0; s<this.n_chemical; s++) cell[s] = this.data_t[s].get(i,j);
				cell = RK4(cell,this.ht);
				for (int s=0; s<this.n_chemical; s++) this.data_t[s].set(i,j,cell[s]); 
			}
		}
		// diffuse
		for (int s=0; s<this.n_chemical; s++) this.data_t[s] = diffuse_ADI(this.data_t[s], M[s]);
	}

	public double[] RK4(double[] v, double ht){
		// dimension of u: 1xn_chemical
		Matrix u = new Matrix(v,1);
		Matrix A = new Matrix(4,4); A.set(1,0,0.5); A.set(2,1,0.5); A.set(3,2,1);
		Matrix B = new Matrix(1,4); B.set(0,0,1.0/6); B.set(0,1,1.0/3); B.set(0,2,1.0/3); B.set(0,3,1.0/6); 
		double[] C = {0,0.5,0.5,1};
		Matrix K = new Matrix(4,u.getColumnDimension());
		int[] index = new int[1];
		for (int i=0; i<4; i++){
			index[0] = i;
			K.setMatrix(index, 0, u.getColumnDimension()-1, f_R(A.getMatrix(index, 0, 3).times(K).times(ht*C[i]).plus(u)));
		}
		return B.times(K).times(ht).plus(u).getRowPackedCopy();
	}
	
	public void diffuse_ADI_matrix(int s){
		double alpha = 2*this.hs*this.hs/(this.k_D[s]*this.ht);
		
		Matrix matII = new Matrix(I,I), matJJ = new Matrix(J,J), matMII = new Matrix(I,I), matMJJ = new Matrix(J,J);
		for (int i=0; i<this.I; i++){
			matII.set(i,i,1);
			matMII.set(i,i,-2);
			matMII.set(i,periodicIndex(i-1,this.I),1);
			matMII.set(i,periodicIndex(i+1,this.I),1);
		}
		for (int j=0; j<J; j++){
			matJJ.set(j,j,1);
			matMJJ.set(j,j,-2);
			matMJJ.set(j,periodicIndex(j-1,this.J),1);
			matMJJ.set(j,periodicIndex(j+1,this.J),1);
		}
		this.M[s][0] = matJJ.times(alpha).plus(matMJJ);
		this.M[s][1] = matII.times(alpha).minus(matMII);
		this.M[s][2] = matII.times(alpha).plus(matMII);
		this.M[s][3] = matJJ.times(alpha).minus(matMJJ).inverse();
	}
	public static int periodicIndex(int i, int I){
		if (i<0) i += I; 
		else if (i>(I-1)) i -= I;
		return i;
	}

	public void diffuse_ADI(Grid U0, Matrix[] X){
		int I = U0.getRowDimension(), J = U0.getColumnDimension();
		Matrix U1 = new Matrix(I,J), U2 = new Matrix(I,J), U = new Matrix(I,J);
		int[] index = new int[1];
		for (int i=0; i<I; i++){
			index[0] = i;
			U.setMatrix(index,0,J-1,U0.getMatrix(index,0,J-1).times(X[0]));
		}
		for (int j=0; j<J; j++){
			index[0] = j;
			U1.setMatrix(0,I-1,index,X[1].solve(U.getMatrix(0,I-1,index)));
		}
		for (int j=0; j<J; j++){
			index[0] = j;
			U.setMatrix(0,I-1,index,X[2].times(U1.getMatrix(0,I-1,index)));
		}
		for (int i=0; i<I; i++){
			index[0] = i;
			U2.setMatrix(index,0,J-1,U.getMatrix(index,0,J-1).times(X[3]));
		}
		return new Grid(U2);
	}
	
}

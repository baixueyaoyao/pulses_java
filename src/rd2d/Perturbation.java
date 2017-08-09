package rd2d;

public class Perturbation {
	int chemical;
	double amp, t_start, t_end, t_mid, lifetime;
	int ci,cj,di,dj;
	public Perturbation(int chemical, double amp, double t_start, double t_end, int ci, int cj, int di, int dj){
		this.chemical = chemical; this.amp = amp; this.t_start = t_start; this.t_end = t_end;
		this.t_mid = (t_start+t_end)/2.0; this.lifetime = t_end-t_start;
		this.ci = ci; this.cj = cj; this.di = di; this.dj = dj;
	}
	public Grid[] getValue(Grid[] data_t, double t){
		if ((t>this.t_start)&&(t<this.t_end)){
			double amp_scale = 1 - Math.abs(t-this.t_mid) / (this.lifetime/2.0);
			data_t[this.chemical].square_perturbation(this.amp * amp_scale,this.ci,this.cj,this.di,this.dj);
		}
		return data_t;
	}
}
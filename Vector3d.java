class Vector3d{
	public double x,y,z;
	public Vector3d(){
		x=0;
		y=0;
		z=0;
	}
	public Vector3d(double nx,double ny, double nz){
		x=nx;
		y=ny;
		z=nz;
	}
	public Vector3d(final Vector3d old){
		x=old.x;
		y=old.y;
		z=old.z;
	}
	public Vector3d abs(){
		if (x<0) x=-x;
		if (y<0) y=-y;
		if (z<0) z=-z;
		return this;
	}
	public Vector3d add(final Vector3d v){
		x+=v.x;
		y+=v.y;
		z+=v.z;
		return this;
	}
	public Vector3d add(final double f){
		x+=f;
		y+=f;
		z+=f;
		return this;
	}
	public Vector3d multiply(final double f){
		x*=f;
		y*=f;
		z*=f;
		return this;
	}
	public Vector3d multiply(final Vector3d v){
		x*=v.x;
		y*=v.y;
		z*=v.z;
		return this;
	}
	public double length(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	public Vector3d clone(){
		return new Vector3d(this);
	}
}

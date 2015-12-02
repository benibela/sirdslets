
import java.util.ArrayList;

public class Vector3d implements JSONSerializable{
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
	public Vector3d(final Vector3i old){
		x=old.x;
		y=old.y;
		z=old.z;
	}
	public Vector3d(final ArrayList<Number> v){
		jsonDeserialize(v);
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
	public Vector3d add(final Vector3i v){
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
	public Vector3d sub(final Vector3d v){
		x-=v.x;
		y-=v.y;
		z-=v.z;
		return this;
	}
	public Vector3d sub(final Vector3i v){
		x-=v.x;
		y-=v.y;
		z-=v.z;
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
	public Vector3d round(){
		x=Math.round(x);
		y=Math.round(y);
		z=Math.round(z);
		return this;
	}
	public Vector3d blend(double alphaThis, Vector3d other){
		x=alphaThis*x + (1-alphaThis)*other.x;
		y=alphaThis*y + (1-alphaThis)*other.y;
		z=alphaThis*z + (1-alphaThis)*other.z;
		return this;
	}
	public Vector3d cloneBlend(double alphaThis, Vector3d other){
		return clone().blend(alphaThis, other);
	}
	public Vector3d blend(double alphaThis, Vector3i other){
		x=alphaThis*x + (1-alphaThis)*other.x;
		y=alphaThis*y + (1-alphaThis)*other.y;
		z=alphaThis*z + (1-alphaThis)*other.z;
		return this;
	}
	public Vector3d cloneBlend(double alphaThis, Vector3i other){
		return clone().blend(alphaThis, other);
	}
	public Vector3i toVec3i(){
		return new Vector3i(this);
	}
	public double length(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	public Vector3d clone(){
		return new Vector3d(this);
	}
	public Vector3d projectOnX(){
		return new Vector3d(x, 0, 0);
	}
	public Vector3d projectOnY(){
		return new Vector3d(0, y, 0);
	}
	public Vector3d projectOnZ(){
		return new Vector3d(0, 0, z);
	}

	public void assign(Vector3d v){
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public Object jsonSerialize() {
		return new double[]{x,y,z};
	}

	public void jsonDeserialize(Object obj) {
		ArrayList<Number> aln = (ArrayList<Number>)obj;
		x=aln.get(0).doubleValue();
		y=aln.get(1).doubleValue();
		z=aln.get(2).doubleValue();
	}

    @Override
	public String toString() {
		return "("+x+", "+y+", "+z+")";
	}
}

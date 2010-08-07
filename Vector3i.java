
import java.util.ArrayList;
import java.util.Map;

public class Vector3i implements JSONSerializable{
	public int x,y,z;
	public Vector3i(){
		x=0;
		y=0;
		z=0;
	}
	public Vector3i(int nx, int ny, int nz){
		x=nx;
		y=ny;
		z=nz;
	}
	public Vector3i(final Vector3i old){
		x=old.x;
		y=old.y;
		z=old.z;
	}
	public Vector3i(final Vector3d old){
		x=(int)Math.round(old.x);
		y=(int)Math.round(old.y);
		z=(int)Math.round(old.z);
	}
	public Vector3i(final ArrayList<Number> v){
		jsonDeserialize(v);
	}
	public Vector3i abs(){
		if (x<0) x=-x;
		if (y<0) y=-y;
		if (z<0) z=-z;
		return this;
	}
	public Vector3i add(final Vector3i v){
		x+=v.x;
		y+=v.y;
		z+=v.z;
		return this;
	}
	public Vector3i add(final int f){
		x+=f;
		y+=f;
		z+=f;
		return this;
	}
	public Vector3i sub(final Vector3i v){
		x-=v.x;
		y-=v.y;
		z-=v.z;
		return this;
	}
	public Vector3i sub(final int f){
		x-=f;
		y-=f;
		z-=f;
		return this;
	}
	public Vector3i multiply(final int f){
		x*=f;
		y*=f;
		z*=f;
		return this;
	}
	public Vector3d cloneMultiply(final double v){
		Vector3d res=new Vector3d(this);
		return res.multiply(v);
	}
	public int lengthSqr(){
		return x*x+y*y+z*z;
	}
	public double length(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	@Override
	public Vector3i clone(){
		return new Vector3i(this);
	}

	public Vector3d toVec3d(){
		return new Vector3d(this);
	}

	public Object jsonSerialize() {
		return new int[]{x,y,z};
	}

	public void jsonDeserialize(Object obj) {
		ArrayList<Number> aln = (ArrayList<Number>)obj;
		x=aln.get(0).intValue();
		y=aln.get(1).intValue();
		z=aln.get(2).intValue();
	}
}

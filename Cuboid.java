
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class Cuboid implements ScenePrimitive, JSONSerializable{
	//static final long serialVersionUID = -6773890043854716267L;
	int minx, maxx, miny, maxy, minz, maxz;
	int perspectiveOffset=32;
	public void setSize(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz){
		if (nmaxx<=nminx) throw new IllegalArgumentException("invalid x size: "+nminx+" - "+nmaxx);
		if (nmaxy<=nminy) throw new IllegalArgumentException("invalid y size: "+nminy+" - "+nmaxy);
		if (nmaxz<=nminz) throw new IllegalArgumentException("invalid z size: "+nminz+" - "+nmaxz);
		//System.out.println("nminz:"+nminz+" nmaxz:"+nmaxz);
		minx=nminx;
		maxx=nmaxx;
		miny=nminy;
		maxy=nmaxy;
		minz=nminz;
		maxz=nmaxz;
	}
	public Cuboid(){}
	public Cuboid(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz){
		setSize(nminx,  nmaxx, nminy, nmaxy, nminz, nmaxz);
	}
	public Cuboid(Cuboid other){
		minx=other.minx;
		maxx=other.maxx;
		miny=other.miny;
		maxy=other.maxy;
		minz=other.minz;
		maxz=other.maxz;
	}

	public Cuboid fastClone(){
		return new Cuboid(this);
	}

	public void drawTo(ZDraw map, int dx, int dy){
		int nminx=minx+dx;
		int nmaxx=maxx+dx;

		int fx=nminx;
		if (fx<0) fx=0;
		if (fx>=map.w) return; //culling
		int tx=nmaxx;
		if (tx<0) return;
		if (tx>=map.w) tx=map.w-1;
		if (tx<fx) return;

		int fy=miny+dy;
		if (fy<0) fy=0;
		if (fy>=map.h) return;
		int ty=maxy+dy;
		if (ty<0) return;
		if (ty>=map.h) ty=map.h-1;
		if (ty<fy) return;
		int deltaZ=maxz-minz;
		//System.out.println(deltaZ+ " "+minz+" "+maxz);
		for (int y=fy; y<=ty; y++){
			int b=map.getLineIndex(y);
			for (int x=fx;x<=tx; x++)
				if (x<nminx+perspectiveOffset) map.customPut(b+x, (deltaZ*(x-nminx))/perspectiveOffset+minz);
				else if (x>nmaxx-perspectiveOffset) map.customPut(b+x, (deltaZ*(nmaxx-x))/perspectiveOffset+minz);
				else map.customPut(b+x, maxz);
		}
	}
	public boolean intersect(ZSprite sprite, int dx, int dy){
		return intersect(sprite, dx, dy, false);
	}
	public boolean intersect(ZSprite sprite, int dx, int dy, boolean removeIntersection){
		//transform in the local coordinates of the sprite
		int nminx=minx+dx-sprite.x;
		int nmaxx=maxx+dx-sprite.x;

		int fx=nminx;
		if (fx<0) fx=0;
		if (fx>=sprite.w) return false;
		int tx=nmaxx;
		if (tx<0) return false;
		if (tx>=sprite.w) tx=sprite.w-1;
		if (tx<fx) return false;

		int fy=miny+dy-sprite.y;
		if (fy<0) fy=0;
		if (fy>=sprite.h) return false;
		int ty=maxy+dy-sprite.y;
		if (ty<0) return false;
		if (ty>=sprite.h) ty=sprite.h-1;
		if (ty<fy) return false;

		removeIntersection = removeIntersection && sprite.transparent;

		int deltaZ=maxz-minz;
		//check the area (fx,tx)*(fy,ty) for equal z coordinates
		boolean result=false;
		for (int y=fy; y<=ty; y++){
			int b=sprite.getLineIndex(y);
			for (int x=fx;x<=tx; x++) {
				int sz=sprite.data[b+x]+sprite.z;
				if (sz<minz) continue;
				int myz;
				if (x<nminx+perspectiveOffset) myz=(deltaZ*(x-nminx))/perspectiveOffset+minz;
				else if (x>nmaxx-perspectiveOffset) myz=(deltaZ*(nmaxx-x))/perspectiveOffset+minz;
				else myz=maxz;
				if (sz<=myz && (!sprite.transparent || sprite.dataVisible[b+x])) {
					result=true;
					if (removeIntersection) sprite.dataVisible[b+x]=false;
					else return true;
				}
			}
		}
		return result;
	}
	public boolean intersect(Cuboid o, int dx, int dy){
		if (o.minx > maxx + dx || o.maxx < minx + dx) return false;
		if (o.miny > maxy + dy || o.maxy < miny + dy) return false;
		if (o.minz > maxz || o.maxz < minz) return false;
		return true;
	}
	public boolean containsPoint(int x, int y){
		return minx<=x&&miny<=y && maxx>=x && maxy>=y;
	}



//Interface implementation
	public Vector3i centerI(){
		return new Vector3i((minx+maxx)/2, (miny+maxy)/2, (minz+maxz)/2);
	}
	public void move(int x, int y, int z){
		minx+=x;
		maxx+=x;
		miny+=y;
		maxy+=y;
		minz+=z;
		maxz+=z;
	}

	public void moveTo(Vector3i to){
		Vector3i center=centerI();
		move(to.x-center.x,to.y-center.y,to.z-center.z);
	}
	public void moveTo(int x, int y, int z){
		Vector3i center=centerI();
		move(x-center.x,y-center.y,z-center.z);
	}
	public Vector3i cornerLTF(){
		return new Vector3i(minx, miny, minz);
	}
	public Vector3i cornerRBN(){
		return new Vector3i(maxx, maxy, maxz);
	}

	public int zAt(int wx, int wy){
		if (!containsPoint(wx, wy)) return -1;
		return maxz;
	}

	public Object jsonSerialize(){
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "Cuboid");
		tm.put("corners", new int[]{minx, miny, minz, maxx, maxy, maxz});
		return tm;
	}

	public void jsonDeserialize(Object obj){
		ArrayList<Number> c = (ArrayList<Number>) ((Map<String, Object>)obj).get("corners");
		assert(c.size()==6);
		minx = c.get(0).intValue();
		miny = c.get(1).intValue();
		minz = c.get(2).intValue();
		maxx = c.get(3).intValue();
		maxy = c.get(4).intValue();
		maxz = c.get(5).intValue();
	}
}
/*
class HoledCuboid extends Cuboid{
	static final long serialVersionUID = -7398540760922297999L;
	int holeminy, holemaxy, holeminz, holemaxz;
	Cuboid upper=new Cuboid();
	Cuboid down=new Cuboid();
	Cuboid below=new Cuboid();
	Cuboid above=new Cuboid();

	public void setSize(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz, int nholeminy, int nholemaxy, int nholeminz, int nholemaxz){
		super.setSize(nminx,  nmaxx, nminy, nmaxy, nminz, nmaxz);
		//System.out.println("nholeminy:"+nholeminy+ " nholemaxy:"+nholemaxy+ " nholeminz:"+nholeminz +" nholemaxz:"+nholemaxz);
		int aboveOffset=perspectiveOffset+8;
		holeminy=nholeminy;
		holemaxy=nholemaxy;
		holeminz=nholeminz;
		holemaxz=nholemaxz;
		upper.setSize(nminx,  nmaxx, nminy, holeminy, nminz, nmaxz);
		down.setSize(nminx,  nmaxx, holemaxy, maxy, nminz, nmaxz);
		below.setSize(nminx,  nmaxx, nminy, nmaxy, nminz, holeminz);
		above.setSize(nminx+aboveOffset,  nmaxx-aboveOffset, holeminy, holemaxy, holemaxz, maxz);
		below.perspectiveOffset=upper.perspectiveOffset*(holeminy-miny)/(maxy-miny);
	}

	public HoledCuboid(){}
	public HoledCuboid(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz, int nholeminy, int nholemaxy, int nholeminz, int nholemaxz){
		setSize(nminx,  nmaxx, nminy, nmaxy, nminz, nmaxz, nholeminy,nholemaxy,nholeminz,nholemaxz);
	}

	public void drawTo(ZDraw map, int dx, int dy){
		//don't draw myself
		down.drawTo(map,dx,dy);
		upper.drawTo(map,dx,dy);
		above.drawTo(map,dx,dy);
		below.drawTo(map,dx,dy);
	}

	public boolean intersect(ZSprite sprite, int dx, int dy, boolean removeIntersection){
		boolean result=false;
		result|=down.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		result|=upper.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		result|=above.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		result|=below.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		return result;
	}
}
*/
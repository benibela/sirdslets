
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class ZSprite extends ZDraw implements ScenePrimitive, JSONSerializable{
	//transparent ZDraw
	int x,y,z;
	boolean transparent; //transparent <=> dataVisible!=null (should be)
	boolean dataVisible[];
	
	
	public ZSprite(){}
	//reads a heightmap from the given array and normalize it
	public ZSprite(int[] heightMap, int w, int h){
		transparent=true;
		setSize(w,h);
		readHeightMapFrom(heightMap);
		normalizeZ();
	}
	
	public ZSprite(int width, int height)
	{
		transparent=false;
		setSize(width, height);
	}

	//O(1) shared copy (should never be modified)
	public ZSprite fastClone(){
		ZSprite res = new ZSprite();
		res.data = data;
		res.w=w;
		res.h=h;
		res.start=start;
		res.stride=stride;
		res.transparent = transparent;
		res.dataVisible = dataVisible;
		return res;
	}

	@Override
	public void setSize(int nw, int nh){	
		super.setSize(nw,nh);
		if (transparent)
			dataVisible=new boolean[start+stride*nh];
	}	
	
	//subtracts the z value of the first invisible point (!!!!!!!) from every z-vaue
	public void normalizeZ(){
		int baseZ=0;
		int b=getLineIndex(0);
		for (int x=0;x<w;x++)
			if (transparent && !dataVisible[b+x]) {
				baseZ=data[b+x];
				break;
			}
		normalizeZ(baseZ);
	}
	
	//subtracts baseZ from every z value
	public void normalizeZ(int baseZ){
		if (baseZ==0) return;
		for (int y=0; y<h; y++)
		{
			int b = getLineIndex(y);
			for (int x=0; x<w; x++)
				data[b+x]-=baseZ;
		}
	}
	
	public void readHeightMapFrom(int[] from){
		assert transparent;
		//read height data
		super.readHeightMapFrom(from);
		//set inverse alpha channel
		for (int y=0; y<h; y++)
		{
			int b = getLineIndex(y);
			for (int x=0; x<w; x++)
				dataVisible[b+x]=((from[b+x]&0xff000000)>>>24) > 0x80;
		}
	}
	
	public void drawTo(ZDraw zbuffer, int xo, int yo){
	//	System.out.println("print to: " +x+"/"+y+" size: "+w+":"+h);
		int fx=0;
		int fy=0;
		int tx=w;
		int ty=h;

		int rx=x+xo-fx;//relativ offset (local->global system)
		int ry=y+yo-fy;
		for (int cy=fy; cy<ty; cy++)
		{
			int b = getLineIndex(cy);
			int bo = zbuffer.getLineIndex(cy+ry)+rx;
			for (int cx=fx; cx<tx; cx++)
				if ((!transparent || dataVisible[b+cx]) && zbuffer.inBounds(rx+cx,ry+cy)){
					zbuffer.customPut(bo+cx,data[b+cx]+z);
//					System.out.println("!");
				}
		}
	}



	public Vector3i centerI(){
		return new Vector3i(x,y,z);
	}
	public void move(int x, int y, int z){
		this.x+=x;
		this.y+=y;
		this.z+=z;
	}

	public void moveTo(Vector3i to){
		this.x=to.x;
		this.y=to.y;
		this.z=to.z;
	}
	public void moveTo(int x, int y, int z){
		this.x=x;
		this.y=y;
		this.z=z;
	}

	public boolean intersect(ZSprite sprite, int dx, int dy, boolean removeIntersectionInThis){
		//calculate intersection rect in local coords
		int ox = sprite.x - x + dx;
		int il = Math.max(0, ox);
		int ir = Math.min(w, ox + sprite.w);
		if (il >= ir)
			return false;
		int oy = sprite.y - y + dy;
		int it = Math.max(0, oy);
		int ib = Math.min(h, sprite.h + oy);
		if (it >= ib)
			return false;

		//sprite equal to bounding rect
		if (!transparent && !sprite.transparent)
			return true;

		//pixel check
		removeIntersectionInThis = removeIntersectionInThis && transparent;

		boolean result=false;

		int oz=sprite.z - z;
		int zeps = 2;
		for (int y=it;y<ib;y++){
			int b = getLineIndex(y);
			int b2 = sprite.getLineIndex(y - oy);
			for (int x=il;x<ir;x++)
				if (dataVisible[b+x] && sprite.dataVisible[b2+x-ox]&&
				    Math.abs(data[b+x]    - sprite.data[b2+x-ox]  - oz) <= zeps){
					result=true;
					if (removeIntersectionInThis) dataVisible[b+x]=false;
					else return result;
				}
		}
		return result;
	}

	public Object jsonSerialize(){
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "ZSprite");
		tm.put("position", new int[]{x, y, z});
		return tm;
	}
	public void jsonDeserialize(Object obj){
		Map<String, Object> map = ((Map<String, Object>)obj);
		assert ("ZSprite".equals( map.get("type")));
		ArrayList<Number> p = (ArrayList<Number>) map.get("position");
		x=p.get(0).intValue();
		y=p.get(1).intValue();
		z=p.get(2).intValue();
	}

}
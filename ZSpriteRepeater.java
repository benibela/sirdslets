import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class ZSpriteRepeater implements ScenePrimitive, JSONSerializable{
	//transparent ZDraw
	int repx, repy;
	ZSprite sprite;



	public ZSpriteRepeater(ZSprite sprite){
		this.sprite = sprite;
		repx = 1;
		repy = 1;
	}
	
	public ZSpriteRepeater(ZSprite sprite, int repeatx, int repeaty){
		repx = repeatx;
		repy = repeaty;
		this.sprite = sprite;
	}


	//O(1) shared copy (should never be modified)
	public ZSpriteRepeater fastClone(){
		ZSpriteRepeater res = new ZSpriteRepeater(sprite.fastClone(), repx, repy);
		return res;
	}

	public void drawTo(ZDraw zbuffer, int xo, int yo){
	//	System.out.println("print to: " +x+"/"+y+" size: "+w+":"+h);
		for (int rx = 0; rx < repx; rx++ )
			for (int ry = 0; ry < repy; ry++ )
				sprite.drawTo(zbuffer, xo + rx*sprite.w, yo + ry * sprite.h);
	}



	public Vector3i centerI(){
		return new Vector3i(sprite.x+sprite.w*(repx)/2, sprite.y+sprite.h*(repy)/2, sprite.z);
	}
	public void move(int x, int y, int z){
		sprite.move(x, y, z);
	}

	public void moveTo(Vector3i to){
		sprite.x=to.x-sprite.w*repx/2;
		sprite.y=to.y-sprite.h*repy/2;
		sprite.z=to.z;
	}
	public void moveTo(int x, int y, int z){
		sprite.x=x-sprite.w*repx/2;
		sprite.y=y-sprite.h*repy/2;
		sprite.z=z;
	}

	public Vector3i cornerLTF(){
		return sprite.cornerLTF();
	}
	public Vector3i cornerRBN(){
		return new Vector3i(sprite.x + sprite.w * repx, sprite.y + sprite.h * repy, ZDraw.MAXZ);
	}

	public boolean inBounds(int x, int y)
	{
		return !((x < 0) || (x >= sprite.w * repx) || (y < 0) || (y >= sprite.h*repy));
	}

	public int zAt(int wx, int wy){
		if (!inBounds(wx-sprite.x, wy-sprite.y)) return -1;
		return sprite.zAt((wx - sprite.x) % sprite.w + sprite.x, (wy - sprite.y) % sprite.h + sprite.y);
	}

	public boolean intersect(ZSprite sprite, int dx, int dy){
		/*//calculate intersection rect in local coords
		int ox = sprite.x - x + dx;
		int il = Math.max(0, ox);
		int ir = Math.min(w, ox + sprite.w);
		if (il >= ir)
			return false;
		int oy = sprite.y - y + dy;
		int it = Math.max(0, oy);
		int ib = Math.min(h, sprite.h + oy);
		if (it >= ib)
			return false;*/


		for (int rx = 0; rx < repx; rx++ )
			for (int ry = 0; ry < repy; ry++)
				if (this.sprite.intersect(sprite, dx - rx*sprite.w, dy - ry * sprite.h, false))
					return true;

		return false;
	}

	public boolean intersectReversed(ZSprite sprite, int dx, int dy, boolean removeIntersection){
		/*//calculate intersection rect in local coords
		int ox = sprite.x - x + dx;
		int il = Math.max(0, ox);
		int ir = Math.min(w, ox + sprite.w);
		if (il >= ir)
			return false;
		int oy = sprite.y - y + dy;
		int it = Math.max(0, oy);
		int ib = Math.min(h, sprite.h + oy);
		if (it >= ib)
			return false;*/


		boolean result = false;
		for (int rx = 0; rx < repx; rx++ )
			for (int ry = 0; ry < repy; ry++)
				if (sprite.intersect(this.sprite, dx + rx*sprite.w, dy + ry * sprite.h, removeIntersection)) {
					result = true;
					if (!removeIntersection) break;
				}

		return result;
	}

	public Object jsonSerialize(){
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "ZSpriteRepeater");
		tm.put("position", new int[]{sprite.x, sprite.y, sprite.z});
		tm.put("repeat", new int[]{repx, repy});
		return tm;
	}
	public void jsonDeserialize(Object obj){
		Map<String, Object> map = ((Map<String, Object>)obj);
		assert ("ZSpriteRepeater".equals( map.get("type")));
		ArrayList<Number> p = (ArrayList<Number>) map.get("position");
		sprite.x=p.get(0).intValue();
		sprite.y=p.get(1).intValue();
		sprite.z=p.get(2).intValue();
		ArrayList<Number> r = (ArrayList<Number>) map.get("repeat");
		repx = r.get(0).intValue();
		repy = r.get(1).intValue();
	}

}
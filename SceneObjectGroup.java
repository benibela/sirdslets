import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class SceneObjectGroup implements ScenePrimitive{
	void clear(){
		primitives.clear();
		namedPrimitives.clear();
	}

	//==================In-Scene Objects=============================
	/*protected*/ public ArrayList<ScenePrimitive> primitives = new ArrayList<ScenePrimitive>();
	/*protected*/ public TreeMap<String,ScenePrimitive> namedPrimitives = new TreeMap<String,ScenePrimitive>();

	//Base
	public ScenePrimitive getPrimitive(int id){
		return primitives.get(id);

	}
	public ScenePrimitive getPrimitive(String id){
		return namedPrimitives.get(id);
	}
	public void setPrimitive(int id, ScenePrimitive p){
		if (id==primitives.size()) primitives.add(p);
		else primitives.set(id, p);
	}
	public void setPrimitive(String id, ScenePrimitive p){
		namedPrimitives.put(id,p);
	}
	public void removePrimitive(ScenePrimitive p){
		primitives.remove(p);
	}


	//Utilityfunctions
	void addPrimitive(ScenePrimitive p) {
		setPrimitive(primitives.size(), p);
	}
	public ZSprite getZSprite(int id){
		ScenePrimitive p=getPrimitive(id);
		if (p instanceof ZSprite) return (ZSprite)p;
		return null;
	}
	public ZSprite getZSprite(String id){
		ScenePrimitive p=getPrimitive(id);
		if (p instanceof ZSprite) return (ZSprite)p;
		return null;
	}
	public ZSprite setZSprite(String id, ZSprite s){
		setPrimitive(id, s);
		return s;
	}
	public ZSprite setZSprite(int id, ZSprite s){
		setPrimitive(id, s);
		return s;
	}


	@Override
	public void drawTo(ZDraw map, int dx, int dy) {
		for (ScenePrimitive prim: primitives)
			prim.drawTo(map, dx, dy);
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			(sprite.getValue()).drawTo(map, dx, dy);
	}

	@Override
	public Vector3i centerI() {
		Vector3i res = new Vector3i();
		for (ScenePrimitive prim: primitives)
			res.add(prim.centerI());
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			res.add(sprite.getValue().centerI());
		res.x /= primitives.size() + namedPrimitives.size();
		res.y /= primitives.size() + namedPrimitives.size();
		res.z /= primitives.size() + namedPrimitives.size();
		return res;
	}

	@Override
	public void move(int x, int y, int z) {
		for (ScenePrimitive prim: primitives)
			prim.move(x,y,z);
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			sprite.getValue().move(x,y,z);
	}

	@Override
	public void moveTo(Vector3i to) {
		moveTo(to.x, to.y, to.z);
	}

	@Override
	public void moveTo(int x, int y, int z) {
		Vector3i center = centerI();
		move(x - center.x, y - center.y, z - center.z );
	}

	@Override
	public Vector3i cornerLTF() {
		Vector3i res = null;
		for (ScenePrimitive prim: primitives)
			if (res == null) res = prim.cornerLTF();
			else res.min(prim.cornerLTF());
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			if (res == null) res = sprite.getValue().cornerLTF();
			else res.min(sprite.getValue().cornerLTF());
		return res;
	}

	@Override
	public Vector3i cornerRBN() {
		Vector3i res = null;
		for (ScenePrimitive prim: primitives)
			if (res == null) res = prim.cornerRBN();
			else res.min(prim.cornerRBN());
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			if (res == null) res = sprite.getValue().cornerRBN();
			else res.min(sprite.getValue().cornerRBN());
		return res;
	}

	@Override
	public int zAt(int x, int y) {
		assert(false);
		//todo: ...
		return 0;
	}

	@Override
	public ScenePrimitive fastClone() {
		SceneObjectGroup sog = new SceneObjectGroup();
		for (int i=0;i<primitives.size();i++)
			sog.setPrimitive(i, primitives.get(i).fastClone());
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			sog.setPrimitive(sprite.getKey(), sprite.getValue());
		return sog;
	}

	@Override
	public Object jsonSerialize() {
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "SceneObjectGroup");
		ArrayList<Object> ser = new ArrayList<Object>();
		for (ScenePrimitive prim: primitives) ser.add(prim.jsonSerialize());
		tm.put("primitives", ser);
		TreeMap<String,Object> namedSer = new TreeMap<String,Object>();
		for (Map.Entry<String,ScenePrimitive> sprite: namedPrimitives.entrySet())
			namedSer.put(sprite.getKey(), sprite.getValue().jsonSerialize());
		tm.put("namedPrimitives", namedSer);
		return tm;
	}

	@Override
	public void jsonDeserialize(Object obj) {
		Map<String, Object> map = ((Map<String, Object>)obj);
		assert ("SceneObjectGroup".equals( map.get("type")));
		assert (false);
		//todo: see SIRDSFlighter. addSerializedObject
	}
}

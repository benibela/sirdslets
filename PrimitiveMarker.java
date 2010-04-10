
import java.util.Map;
import java.util.TreeMap;

public class PrimitiveMarker implements PrimitiveModifier {
	PrimitiveMarker(){
	}
	PrimitiveMarker(ScenePrimitive sp){
		prim=sp;
	}
	public void setPrimitive(ScenePrimitive sp){
		prim=(ZSprite)sp;
	}
	public PrimitiveMarker clone(ScenePrimitive sp){
		PrimitiveMarker pm = new PrimitiveMarker(sp);
		for (Map.Entry<String, Object> ent: properties.entrySet())
			pm.properties.put(ent.getKey(),ent.getValue());
		return pm;
	}

	public Map<String,Object> properties = new TreeMap<String,Object>();

	public ScenePrimitive prim;
	public void calculate(int timeStep){
	}

	public Map<String, Object> jsonSerialize() {
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		for (Map.Entry<String, Object> ent: properties.entrySet())
			tm.put(ent.getKey(),ent.getValue());
		tm.put("type","PrimitiveMarker");
		return tm;
	}

	public void jsonDeserialize(Object obj) {
		Map<String, Object> map=(Map<String, Object>) obj;
		properties.clear();
		for (Map.Entry<String, Object> ent: map.entrySet())
			properties.put(ent.getKey(),ent.getValue());
	}

}

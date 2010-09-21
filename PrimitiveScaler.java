
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class PrimitiveScaler implements PrimitiveModifier, JSONSerializable {
	PrimitiveScaler(){
	}
	PrimitiveScaler(ScenePrimitive sp){
		setPrimitive(sp);
	}
	public void setPrimitive(ScenePrimitive sp){
		if (!(sp instanceof Cuboid))
			throw new IllegalArgumentException("only cuboids can be scaled");
		prim=(Cuboid)sp;
	}
	public PrimitiveModifier clone(ScenePrimitive sp){
		PrimitiveScaler scaler = new PrimitiveScaler(sp);
		scaler.velocity = velocity;
		scaler.origin = origin;
		for (ArrayList<Number> v: scales){
			ArrayList<Number> n= new ArrayList<Number>();
			for (Number vn: v)
				n.add(vn.doubleValue());
			scaler.scales.add(n);
		}
		return scaler;
	}

	public enum ScaleOrigin {SO_CENTER,SO_LEFT_TOP_FAR,SO_LEFT_TOP_NEAR};
	public ArrayList<ArrayList<Number>> scales=new ArrayList<ArrayList<Number>>();
	public float velocity;
	public ScaleOrigin origin;

	public float time;
	public ScenePrimitive prim;
	public void calculate(double timeStep){
		if (scales.size()<2) return;
		time+=timeStep*velocity/1000.0f;

		int count=scales.size();
		while (time>count) time-=count;
		while (time<0) time+=count;

		int part=(int)Math.floor(time);
		float partTime=time-part;
		Cuboid c = (Cuboid)prim;
		float a = (1-partTime);
		float b = partTime;
		ArrayList<Number> l=scales.get(part);
		ArrayList<Number> r=scales.get((part+1)%count);
		if (l.size()==6 && r.size()==6) {
			c.setSize((int)Math.round(a*l.get(0).floatValue()+b*r.get(0).floatValue()),
				  (int)Math.round(a*l.get(1).floatValue()+b*r.get(1).floatValue()),
				  (int)Math.round(a*l.get(2).floatValue()+b*r.get(2).floatValue()),
				  (int)Math.round(a*l.get(3).floatValue()+b*r.get(3).floatValue()),
				  (int)Math.round(a*l.get(4).floatValue()+b*r.get(4).floatValue()),
				  (int)Math.round(a*l.get(5).floatValue()+b*r.get(5).floatValue()));
		} else if (l.size()==3 && r.size() == 3) {
			int nsx = (int)Math.round(a*l.get(0).floatValue()+b*r.get(0).floatValue());
			int nsy = (int)Math.round(a*l.get(1).floatValue()+b*r.get(1).floatValue());
			int nsz = (int)Math.round(a*l.get(2).floatValue()+b*r.get(2).floatValue());
			switch(origin){
				case SO_CENTER:
					Vector3i center = c.centerI();
					c.setSize(0,nsx,0,nsy,0,nsz);
					c.moveTo(center);
					break;
				case SO_LEFT_TOP_FAR:
					c.setSize(c.minx,c.minx+nsx,
						  c.miny,c.miny+nsy,
						  c.minz,c.minz+nsz);
					break;
				case SO_LEFT_TOP_NEAR:
					c.setSize(c.minx,c.minx+nsx,
						  c.miny,c.miny+nsy,
						  c.maxz-nsz,c.maxz);
					break;
				default: throw new IllegalArgumentException("invalid origin");
			}
		} else throw new IllegalArgumentException("invalid scale size: "+l.size()+":"+r.size());
	}


	public Map<String, Object> jsonSerialize() {
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "PrimitiveScaler");
		tm.put("scales", scales);
		tm.put("velocity", velocity);
		tm.put("origin",origin==ScaleOrigin.SO_CENTER?"center":(
			        origin==ScaleOrigin.SO_LEFT_TOP_FAR?"left-top":(
				origin==ScaleOrigin.SO_LEFT_TOP_NEAR?"left-top-near":"")));
		return tm;
	}

	public void jsonDeserialize(Object obj) {
		Map<String, Object> map=(Map<String, Object>) obj;
		scales = (ArrayList<ArrayList<Number>>)map.get("scales");
		/*ArrayList<Object> pos = (ArrayList<Object>)map.get("scales");
		scales.clear();
		for (Object p: pos) {
			Vector3d v=new Vector3d();
			v.jsonDeserialize(p);
			scales.add(v);
		}*/
		velocity=((Number) map.get("velocity")).floatValue();
		origin=ScaleOrigin.SO_CENTER;
		if (map.containsKey("origin")){
			String so = (String)map.get("origin");
			if ("center".equals(so)) origin=ScaleOrigin.SO_CENTER;
			else if ("left-top".equals(so)) origin=ScaleOrigin.SO_LEFT_TOP_FAR;
			else if ("left-top-far".equals(so)) origin=ScaleOrigin.SO_LEFT_TOP_FAR;
			else if ("left-top-near".equals(so)) origin=ScaleOrigin.SO_LEFT_TOP_NEAR;
			else throw new IllegalArgumentException("wrong origin");

		}
	}
}


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class PrimitiveMover implements PrimitiveModifier, JSONSerializable {
	PrimitiveMover(){
		method = MoveMethod.MM_LINEAR;
	}
	PrimitiveMover(ScenePrimitive sp){
		prim=sp;
		method = MoveMethod.MM_LINEAR;
	}
	public void setPrimitive(ScenePrimitive sp){
		prim=(ZSprite)sp;
	}
	public PrimitiveModifier clone(ScenePrimitive sp){
		PrimitiveMover pm = new PrimitiveMover(sp);
		pm.velocity = velocity;
		pm.method = method;
		for (Vector3i v: positions)
			pm.positions.add(v.clone());
		return pm;
	}

	public enum MoveMethod {MM_LINEAR, MM_BSPLINE};
	public ArrayList<Vector3i> positions=new ArrayList<Vector3i>();
	public float velocity;
	public MoveMethod method;

	public float time;
	public ScenePrimitive prim;
	public void calculate(double timeStep){
		if (positions.size()<2) return;
		final int pointsToUse=4;
		time+=timeStep*velocity/1000.0f;

		int splineN=positions.size();
		while (time>splineN) time-=splineN;
		while (time<0) time+=splineN;

		int part=(int)Math.floor(time);
		float partTime=time-part;
		switch (method) {
			case MM_LINEAR:
				prim.moveTo(positions.get(part).toVec3d().cloneBlend(1-partTime, positions.get((part+1)%splineN)).toVec3i());
				break;
			case MM_BSPLINE:
				Vector3d newPos=new Vector3d();
				for (int i=part;i<part+pointsToUse;i++){
					int ai = i % splineN;
					newPos.add(positions.get(ai).cloneMultiply(bezier(pointsToUse, i-part, partTime)));
				}
				prim.moveTo(newPos.toVec3i());
				break;
		}
	}

	protected float bezier(int n, int k, float f){
		int fac=1;
		for (int i=k+1;i<=n;i++) fac*=i;
		for (int i=1;i<=n-k;i++) fac/=i;
		return (float)(fac*Math.pow(f, k)*Math.pow(1-f,n-k));
	}

	public Map<String, Object> jsonSerialize() {
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "PrimitiveMover");
		tm.put("positions", positions);
		tm.put("velocity", velocity);
		tm.put("method", method==MoveMethod.MM_LINEAR?"linear":(
				 method==MoveMethod.MM_BSPLINE?"bspline":""));
		return tm;
	}

	public void jsonDeserialize(Object obj) {
		Map<String, Object> map=(Map<String, Object>) obj;
		ArrayList<Object> pos = (ArrayList<Object>)map.get("positions");
		positions.clear();
		for (Object p: pos) {
			Vector3i v=new Vector3i();
			v.jsonDeserialize(p);
			positions.add(v);
		}
		velocity=((Number) map.get("velocity")).floatValue();
		String m=(String)map.get("method");
		if (m.equals("linear")) method=MoveMethod.MM_LINEAR;
		else if (m.equals("bspline")) method=MoveMethod.MM_BSPLINE;

	}
}

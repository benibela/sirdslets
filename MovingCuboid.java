
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author benito
 */
public class MovingCuboid extends Cuboid {
	ArrayList<Vector3d> centers;
	//ArrayList<Vector3d> centers;

	@Override
	public Map<String, Object> jsonSerialize(){
		TreeMap<String,Object> tm = new TreeMap<String,Object>();
		tm.put("type", "MovingCuboid");
		tm.put("centers", new int[]{minx, miny, minz, maxx, maxy, maxz});
		return tm;
	}

	@Override
	public void jsonDeserialize(Map<String, Object> obj){
		ArrayList<Number> c = (ArrayList<Number>) obj.get("corners");
		assert(c.size()==6);
		minx = c.get(0).intValue();
		miny = c.get(1).intValue();
		minz = c.get(2).intValue();
		maxx = c.get(3).intValue();
		maxy = c.get(4).intValue();
		maxz = c.get(5).intValue();
	}

}

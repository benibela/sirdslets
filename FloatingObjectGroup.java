import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class FloatingObjectGroup implements FloatingObject {
	/*protected*/ public ArrayList<FloatingObject> floaters = new ArrayList<FloatingObject>();
	/*protected*/ public TreeMap<String,FloatingObject> namedFloaters = new TreeMap<String,FloatingObject>();

	public void clear(){
		floaters.clear();
		//namedFloaters.clear();
		Iterator<String> it = namedFloaters.keySet().iterator();
		while (it.hasNext())
			if (!it.next().startsWith("~"))
				it.remove();
	}

	@Override
	public FloatingObject fastClone() {
		FloatingObjectGroup fog = new FloatingObjectGroup();
		for (FloatingObject f: floaters)
			fog.floaters.add(f.fastClone());
		for (Map.Entry<String,FloatingObject> it : namedFloaters.entrySet())
			fog.namedFloaters.put(it.getKey(),it.getValue());
		return fog;
	}

	@Override
	public void draw(IntArrayImage output) {
		for (FloatingObject f: floaters)
			f.draw(output);
		for (Map.Entry<String,FloatingObject> it : namedFloaters.entrySet())
			it.getValue().draw(output);
	}

	@Override
	public void drawSimple(IntArrayImage output) {
		for (FloatingObject f: floaters)
			f.draw(output);
		for (Map.Entry<String,FloatingObject> it : namedFloaters.entrySet())
			it.getValue().draw(output);
	}


	//Base
	public void removeFloater(int id){
		floaters.remove(id);
	}
	public FloatingObject getFloater(int id){
		if (id<0 || id>=floaters.size()) return null;
		return floaters.get(id);
	}
	public Floater setFloater(int id, Floater f){
		if (id>=floaters.size()) floaters.add(f);
		else floaters.set(id,f);
		return f;
	}

	public void removeFloater(String id){
		namedFloaters.remove(id);
	}
	public FloatingObject getFloater(String id){
		return namedFloaters.get(id);
	}
	public Floater setFloater(String id, Floater f){
		namedFloaters.put(id,f);
		return f;
	}

}

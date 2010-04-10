

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

public class SceneManager {
	public enum MessageType {MESSAGE_NOTIFY, MESSAGE_WARNING, MESSAGE_ERROR, MESSAGE_FATAL};
	int width, height;
	int cameraX, cameraY, cameraZ;
	private URL mCodeBase;
	private FontMetrics mFontMetrics;

	public void clear(){
		cameraX=0;
		cameraY=0;
		cameraZ=0;
//		mZBuffer.clear();
		primitives.clear();
		namedPrimitives.clear();
		/*Iterator<String> it = namedPrimitives.keySet().iterator();
		while (it.hasNext())
			if (!it.next().startsWith("~"))
				it.remove();*/
		floaters.clear();
		//namedFloaters.clear();
		mModifiers.clear();
		Iterator<String> it = namedFloaters.keySet().iterator();
		while (it.hasNext())
			if (!it.next().startsWith("~"))
				it.remove();
	}

	public void calculateFrame(int timePerFrame){
		Iterator<Map.Entry<String,Floater>> it = namedFloaters.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,Floater> pairs = it.next();
			if (pairs.getValue() instanceof TimedFloater){
				TimedFloater tf=(TimedFloater)pairs.getValue();
				tf.timetolive-=timePerFrame;
				if (tf.timetolive<0)
					tf.visible=false; //TODO: lockup docu
			}
		}

		for (PrimitiveModifier pm: mModifiers)
			pm.calculate(timePerFrame);
	}

	public void setCameraPosition(int x, int y, int z){
		cameraX=x;
		cameraY=y;
		cameraZ=z;
	}

	public void setBaseURL(URL codeBase){
		mCodeBase=codeBase;
	}

	public void setBaseFontMetric(FontMetrics fontMetrics){
		mFontMetrics=fontMetrics;
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



//===================Overlays/Floater============================
	/*protected*/ public  ArrayList<Floater> floaters = new ArrayList<Floater>();
	/*protected*/ public  TreeMap<String,Floater> namedFloaters = new TreeMap<String,Floater>();

//Base
	public void removeFloater(int id){
		floaters.remove(id);
	}
	public Floater getFloater(int id){
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
	public Floater getFloater(String id){
		return namedFloaters.get(id);
	}
	public Floater setFloater(String id, Floater f){
		namedFloaters.put(id,f);
		return f;
	}

//Utility
	public Floater setFloaterText(String id, String text){
		Floater newText=createTextFloater(text);
		Floater old=getFloater(id);
		if (old==null) return setFloater(id, newText);
		else old.assignNoCopy(newText);
		return old;
	}
	public Floater setFloaterText(String id, String text, int backgroundColor){
		Floater f=setFloaterText(id, text);
		f.mergeColor(backgroundColor);
		return f;
	}

	//Graphic Utility Functions
	public int[] loadImageData(String name){
		BufferedImage img = loadImage(name);
		if (img==null) return null;
		return img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth());
	}
	public BufferedImage loadImage(String name){
		return loadImage(mCodeBase, name);
	}
	public static BufferedImage loadImage(URL baseUrl, String name){
		try {
			return loadImage(new URL(baseUrl,name));
		} catch (java.net.MalformedURLException e){
		}
		return null;
	}
	public static BufferedImage loadImage(URL url){
		BufferedImage img = null;
		try {
			//img = ImageIO.read(new File(fileName));
			img = ImageIO.read(url);
			//URL url = new URL(getCodeBase(), "strawberry.jpg");
		} catch (IOException e) {
		}
		return img;
	}
	public URL getFileURL(String name){
		try {
			//System.out.println(new URL(getCodeBase(),name));
			return new URL(mCodeBase,name);
		} catch (Exception e){}
		return null;
	}


	public Floater createFloater(String name){
		Floater floater=new Floater();
		BufferedImage img=loadImage(name);
		if (img==null) throw new IllegalArgumentException("couldn't find image: "+name);
		floater.setToImage(img);
		return floater;
	}
	public Floater createTextFloater(String text){
		return createTextFloater(text,Color.BLACK);
	}
	public Floater createTextFloater(String text, Color foregroundColor){
		Floater floater=new Floater();
		floater.setToString(text, mFontMetrics, foregroundColor);
		return floater;
	}
	public Floater createTextFloater(String text, int backgroundColor){
		Floater floater=new Floater();
		floater.setToString(text, mFontMetrics, Color.BLACK);
		floater.mergeColor(backgroundColor);
		return floater;
	}


	public void showFloaterMessage(String message){
		showFloaterMessage(message, MessageType.MESSAGE_NOTIFY);
	}
	public void showFloaterMessage(String message, MessageType type){
		TimedFloater f = new TimedFloater();
		int color=0xffddddcc;
		if (type == MessageType.MESSAGE_ERROR || type==MessageType.MESSAGE_FATAL) color=0xffff0000;
		f.setToString(message, mFontMetrics, Color.BLACK);
		f.mergeColor(color);
		f.y=(height-f.h)/2;
		f.timetolive=5*1000;
		setFloater("~message",f);

		if (!false){
			System.out.println(type+": "+message);
		}
	}


	public ZSprite createZSprite(String fileName){
		BufferedImage img=loadImage(fileName);
		if (img==null) throw new IllegalArgumentException("couldn't find image: "+fileName);
		ZSprite sprite=new ZSprite(img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()),
									img.getWidth(),
									img.getHeight());
		return sprite;
	}

//===========================Modifier===========================
	private ArrayList<PrimitiveModifier> mModifiers=new ArrayList<PrimitiveModifier>();

	void addPrimitiveModifier(PrimitiveModifier pm){
		mModifiers.add(pm);
	}
	void removePrimitiveModifier(PrimitiveModifier pm){
		mModifiers.remove(pm);
	}




	PrimitiveModifier deserializePrimitiveModifier(Map<String, Object> map, ScenePrimitive parent){
		PrimitiveModifier pm=null;
		String type=(String)map.get("type");
		if (type.equals("PrimitiveMover")) pm=new PrimitiveMover(parent);
		else if (type.equals("PrimitiveAnimator")) pm=new PrimitiveAnimator(parent);
		else if (type.equals("PrimitiveMarker")) pm=new PrimitiveMarker(parent);
		else throw new IllegalArgumentException("invalid type: "+type)
		pm.jsonDeserialize(map);
		return pm;
	}
}

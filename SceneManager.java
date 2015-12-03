

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

public class SceneManager extends SceneObjectGroup {
	public enum MessageType {MESSAGE_NOTIFY, MESSAGE_WARNING, MESSAGE_ERROR, MESSAGE_FATAL};
	int width, height;
	int cameraX, cameraY, cameraZ;
	private URL mCodeBase = null;
	private FontMetrics mFontMetrics;

	public void clear(){
		super.clear();
		cameraX=0;
		cameraY=0;
		cameraZ=0;
//		mZBuffer.clear();
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

	public void calculateFrame(double timePerFrame){
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
		if (mCodeBase == null)
			return loadImage(getClass().getResource(name));
		else{
			try {
				return loadImage(new URL(mCodeBase,name));
			} catch (java.net.MalformedURLException e){
			}
			return null;
		}
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
			if (mCodeBase == null)
				return SceneManager.class.getResource(name);
			else
				return new URL(mCodeBase,name);
		} catch (Exception e){}
		throw new IllegalArgumentException("File not found: "+name);
		//return null;
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


	public Floater showFloaterMessage(String message){
		return showFloaterMessage(message, MessageType.MESSAGE_NOTIFY);
	}
	public Floater showFloaterMessage(String message, MessageType type){
		TimedFloater f = new TimedFloater();
		int color=0xffddddcc;
		if (type == MessageType.MESSAGE_ERROR || type==MessageType.MESSAGE_FATAL) color=0xffff0000;
		f.setToString(message, mFontMetrics, Color.BLACK);
		f.mergeColor(color);
		f.y=(height-f.h)/2;
		f.timetolive=5*1000;
		return setFloater("~message",f);
		      /*
		if (!false){
			System.out.println(type+": "+message);
		}
		*/
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
		else if (type.equals("PrimitiveScaler")) pm=new PrimitiveScaler(parent);
		else if (type.equals("PrimitiveMarker")) pm=new PrimitiveMarker(parent);
		else throw new IllegalArgumentException("invalid type: "+type);
		pm.jsonDeserialize(map);
		return pm;
	}
}

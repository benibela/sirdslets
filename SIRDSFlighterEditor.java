import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.JOptionPane;


public class SIRDSFlighterEditor extends SIRDSFlighter{
	protected int mCurrentSelection=-1;
	@Override
	public void start(Object manager){
		super.start(manager);
		
		mManager.setShowFloaterCursor(true);
		
		Floater f=mScene.setFloaterText("level","level: ?", 0xffddddcc);
		int lh=f.h;
		mScene.setFloaterText("scroll","scroll: 0", 0xffddddcc).y=lh;
		mScene.setFloaterText("cursel","cursel: -1", 0xffddddcc).y=2*lh;
		mScene.getFloater("life").visible=false;
	
		startLevel(0);
	}
	@Override
	public void stop(){
		super.stop();
	}
	@Override
	public void startLevel(int level){
		super.startLevel(level);
		mScene.setFloaterText("level","level: "+mLevel, 0xffddddcc);
	}
	protected void saveLevel(){
		try{
		//System.out.println(mManager.getFileURL("flighter/level"+mLevel+".ser").toString());
			ArrayList<Object> levelSer = new ArrayList<Object>();
			for (int i=0;i<mLevelPrimitives.size();i++){
				JSONSerializable ser=mLevelPrimitives.get(i);
				Map<String, Object> map= (Map<String,Object>)ser.jsonSerialize();
				if (mLevelModifier.get(i)!=null)
					map.put("mover", mLevelModifier.get(i));
				levelSer.add(map);
			}
			JSONWriter json = new JSONWriter();
			BufferedWriter bw = new BufferedWriter(new FileWriter(mScene.getFileURL("flighter/level"+mLevel+".lev").getFile()));
			bw.write(json.write(levelSer));
			bw.close();
			mScene.showFloaterMessage("saved level "+mLevel,SceneManager.MessageType.MESSAGE_NOTIFY);
		} catch (IOException e){
			mScene.showFloaterMessage("Couldn't save"+":"+e.toString(),SceneManager.MessageType.MESSAGE_ERROR);
		}
	}

	@Override
	public void calculateFrame(long timeMS){
		//no level processing

		//--------------keyevents--------------------------
		//final double acceleration=1;
		boolean ctrl=mManager.isKeyPressed(KeyEvent.VK_CONTROL);
		boolean shift=mManager.isKeyPressed(KeyEvent.VK_SHIFT);

		if (mManager.isKeyPressed(KeyEvent.VK_A))
			mManager.setFloaterCursorZ(mManager.getFloaterCursorZ()+1);

		if (mManager.isKeyPressed(KeyEvent.VK_S))
			if (ctrl&&shift) { //save
				saveLevel();
			} else //mouse control
				mManager.setFloaterCursorZ(mManager.getFloaterCursorZ()-1);

		//create
		if (mManager.isKeyPressed(KeyEvent.VK_C))
			addCuboidAtCursor();
		//delete
		if (mManager.isKeyPressed(KeyEvent.VK_D))
			removeSelectedCuboid();

		//resize
		if (mManager.isKeyPressed(KeyEvent.VK_X) ||
		    mManager.isKeyPressed(KeyEvent.VK_Y) ||
		    mManager.isKeyPressed(KeyEvent.VK_Z))
			resizeSelectedCuboid(mManager.isKeyPressed(KeyEvent.VK_X), mManager.isKeyPressed(KeyEvent.VK_Y), mManager.isKeyPressed(KeyEvent.VK_Z), shift, ctrl);

		if (mManager.isKeyPressed(KeyEvent.VK_M) && mManager.isKeyPressedChanged(KeyEvent.VK_M)){
			ArrayList<PrimitiveModifier> apm = mLevelModifier.get(mCurrentSelection);
			String s = (String)JOptionPane.showInputDialog(null,"Modifiers:","",JOptionPane.PLAIN_MESSAGE,null,null,apm!=null?(new JSONWriter()).write(apm):"");
			if (s!=null)
				if (s.equals("")) {
					if (apm!=null){
						for (PrimitiveModifier mod: apm)
							mScene.removePrimitiveModifier(mod);
						mLevelModifier.set(mCurrentSelection, null);
					}
				} else {
					if (apm==null) apm=new ArrayList<PrimitiveModifier>();
					else for (PrimitiveModifier mod: apm)
						mScene.removePrimitiveModifier(mod);

					ArrayList<Map<String, Object>> temp=(ArrayList<Map<String, Object>>)((new JSONReader()).read(s));
					for (Map<String, Object> map: temp){
						String type=(String)map.get("type");
						PrimitiveModifier pm=null;
						if (type.equals("PrimitiveMover")) pm=new PrimitiveMover(mLevelPrimitives.get(mCurrentSelection));
						else if (type.equals("PrimitiveAnimator")) pm=new PrimitiveAnimator(mLevelPrimitives.get(mCurrentSelection));
						pm.jsonDeserialize(map);
						apm.add(pm);
					}

					mLevelModifier.set(mCurrentSelection, apm);
				}
		}

		//scroll/change level
		if (mManager.isKeyPressed(KeyEvent.VK_LEFT))
			if (ctrl && mLevel>=1) startLevel(mLevel-1);
			else scrollLevelDelta(+15);
		if (mManager.isKeyPressed(KeyEvent.VK_RIGHT))
			if (ctrl) startLevel(mLevel+1);
			else scrollLevelDelta(-15);
		if (mManager.isKeyPressed(KeyEvent.VK_PAGE_DOWN))
			scrollLevelDelta(-500);
		if (mManager.isKeyPressed(KeyEvent.VK_PAGE_UP))
			scrollLevelDelta(+500);
		mScene.setFloaterText("scroll","scroll: "+mLevelScroll,0xffddddcc);

		//-----------------mouse events-----------------------
		int rx=mManager.getMouseX()+mScene.cameraX;
		int ry=mManager.getMouseY()+mScene.cameraY;
		if (mManager.isMousePressed(MouseEvent.BUTTON1)){
			int j=-1;
			for (int i=0;i<mLevelPrimitives.size();i++){
				ScenePrimitive sp = mLevelPrimitives.get(i);
				if (sp instanceof Cuboid) {
					Cuboid c = (Cuboid)sp;
					if (c.containsPoint(rx, ry) && (j==-1 || c.maxz>c.maxz)){
						j=i;
					}
				}
			}
			setCurrentSelection(j);
		}

		int z=-1;
		for (ScenePrimitive sp: mLevelPrimitives){
			if (sp instanceof Cuboid){
				Cuboid c=(Cuboid)sp;
				if (c.containsPoint(rx, ry))
					if (c.maxz>z)
						z = c.maxz;
			}
		}
		//int z=mZBuffer.data[mZBuffer.getLineIndex(y)+x];
		mScene.setFloaterText("mousez:",rx+"/"+ry+"/"+z,0xffddddcc).y=mZBufferYStart+500;
	}
	

	protected void scrollLevelDelta(int delta){
		mLevelScroll+=delta;
		mScene.cameraX=-mLevelScroll;
	}
	
	protected void setCurrentSelection(int currentSelection){
		mCurrentSelection=currentSelection;
		mScene.setFloaterText("cursel","cursel: "+mCurrentSelection, 0xffddddcc);
	}
	
	public void removeSelectedCuboid(){
		if (mCurrentSelection==-1) return;
		mManager.suspendRendering();
		mLevelPrimitives.remove(mCurrentSelection);
		mLevelModifier.remove(mCurrentSelection);
		mManager.resumeRendering();
		setCurrentSelection(-1);
	}
	
	private final static int cuboidDelta = 10; 
	public void addCuboidAtCursor(){
		Floater f=mManager.getFloaterCursor();
		mManager.suspendRendering();
		int minx=f.x-mLevelScroll-50; if (minx%cuboidDelta!=0) minx-=minx%cuboidDelta;
		int maxx=f.x-mLevelScroll+50; if (maxx%cuboidDelta!=0) maxx-=maxx%cuboidDelta;
		int miny=Math.max(0,f.y-mZBufferYStart-100);  if (miny % cuboidDelta!=0) miny-=miny%cuboidDelta;
		int maxy=Math.min(mZBufferH,f.y-mZBufferYStart+100);if (maxy % cuboidDelta!=0) maxy-=maxy%cuboidDelta;
		int minz=Math.max(0,f.z-5);
		int maxz=Math.min(ZDraw.MAXZ,f.z+5);
		mLevelPrimitives.add(new Cuboid(minx,maxx,miny,maxy,minz,maxz));
		mLevelModifier.add(new ArrayList<PrimitiveModifier>());
		setCurrentSelection(mLevelPrimitives.size()-1);
		mManager.resumeRendering();
	}
	
	public void resizeSelectedCuboid(boolean resizeX, boolean resizeY, boolean resizeZ, boolean enlarge, boolean minimum){
		if (mCurrentSelection==-1) return;
		ScenePrimitive sp = mLevelPrimitives.get(mCurrentSelection);
		if (!(sp instanceof Cuboid)) return;
		Cuboid c = (Cuboid)sp;
		int inc=enlarge?1:-1;
		//1-2d
		if (resizeX)
			if (minimum) c.minx+=inc;
			else c.maxx+=inc;
		if (resizeY)
			if (minimum) c.miny=Math.min(c.maxy,Math.max(0,c.miny+inc));
			else c.maxy=Math.max(c.miny,Math.min(mZBufferH,c.maxy+inc));
		//3d
		if (resizeZ){
			inc*=cuboidDelta;
			if (minimum) c.minz=Math.min(c.maxz,Math.max(0,c.minz+inc));
			else c.maxz=Math.max(c.minz,Math.min(ZDraw.MAXZ,c.maxz+inc));
		}
	}
		
	public String getSIRDletName(){
		return "SIRDS Flighter (Editor)";
	}
}
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class SIRDSFlighterEditor extends SIRDSFlighter{
	protected int mCurrentSelection=-1;
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
			ArrayList<Map> levelSer = new ArrayList<Map>();
			for (JSONSerializable ser: mLevelCuboids)
				levelSer.add(ser.jsonSerialize());
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

		//scroll/change level
		if (mManager.isKeyPressed(KeyEvent.VK_LEFT))
			if (ctrl && mLevel>=1) startLevel(mLevel-1);
			else scrollLevelDelta(+5);
		if (mManager.isKeyPressed(KeyEvent.VK_RIGHT))
			if (ctrl) startLevel(mLevel+1);
			else scrollLevelDelta(-5);
		if (mManager.isKeyPressed(KeyEvent.VK_PAGE_DOWN))
			scrollLevelDelta(-500);
		if (mManager.isKeyPressed(KeyEvent.VK_PAGE_UP))
			scrollLevelDelta(+500);
		mScene.setFloaterText("scroll","scroll: "+mLevelScroll,0xffddddcc);

		//-----------------mouse events-----------------------
		int rx=mManager.getMouseX()-mScene.cameraX;
		int ry=mManager.getMouseX()-mScene.cameraY;
		if (mManager.isMousePressed(MouseEvent.BUTTON1)){
			for (int i=0;i<mLevelCuboids.size();i++)
				if (mLevelCuboids.get(i).minx<=rx && mLevelCuboids.get(i).maxx>=rx &&
					mLevelCuboids.get(i).miny<=ry && mLevelCuboids.get(i).maxy>=ry){
					setCurrentSelection(i);
				}
		}

		int z=-1;
		for (Cuboid c: mLevelCuboids)
			if (c.containsPoint(rx, ry))
				if (c.maxz>z) z = c.maxz;
		//int z=mZBuffer.data[mZBuffer.getLineIndex(y)+x];
		mScene.setFloaterText("mousez:","cur-z:"+z,0xffddddcc).y=mZBufferYStart+500;
	}
	

	protected void scrollLevelDelta(int delta){
		mLevelScroll+=delta;
		//mShip.x=300-mShip.w/2+mLevelScroll;
		mLevelEnd.x+=delta;
	}
	
	protected void setCurrentSelection(int currentSelection){
		mCurrentSelection=currentSelection;
		mScene.setFloaterText("cursel","cursel: "+mCurrentSelection, 0xffddddcc);
	}
	
	public void removeSelectedCuboid(){
		if (mCurrentSelection==-1) return;
		mManager.suspendRendering();
		mLevelCuboids.remove(mCurrentSelection);
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
		mLevelCuboids.add(new Cuboid(minx,maxx,miny,maxy,minz,maxz));
		setCurrentSelection(mLevelCuboids.size()-1);
		mManager.resumeRendering();
	}
	
	public void resizeSelectedCuboid(boolean resizeX, boolean resizeY, boolean resizeZ, boolean enlarge, boolean minimum){
		if (mCurrentSelection==-1) return;
		Cuboid c = mLevelCuboids.get(mCurrentSelection);
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
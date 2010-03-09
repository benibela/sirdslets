import java.awt.event.*;
import java.io.*;

public class SIRDSFlighterEditor extends SIRDSFlighter implements MouseListener, MouseMotionListener{
	protected int mCurrentSelection=-1;
	public void start(Object manager){
		super.start(manager);
		
		mManager.addMouseListener(this);
		mManager.addMouseMotionListener(this);
		
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
		mManager.removeMouseListener(this);
	}
	@Override
	public void startLevel(int level){
		super.startLevel(level);
		mScene.setFloaterText("level","level: "+mLevel, 0xffddddcc);
	}
	protected void saveLevel(){
		try{
		//System.out.println(mManager.getFileURL("flighter/level"+mLevel+".ser").toString());
			ObjectOutputStream levelStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(mScene.getFileURL("flighter/level"+mLevel+".ser").getFile())));
			levelStream.writeObject(mLevelCuboids);
			levelStream.close(); 		
			mScene.showFloaterMessage("saved level "+mLevel,SceneManager.MessageType.MESSAGE_NOTIFY);
		} catch (IOException e){
			mScene.showFloaterMessage("Couldn't save"+":"+e.toString(),SceneManager.MessageType.MESSAGE_ERROR);
		}
	}

	@Override
	public void calculateFrame(){
		//no level processing
		
	}

	public void mousePressed(MouseEvent e)
	{
		int rx=e.getX()-mLevelScroll;
		int ry=e.getY()-mZBufferYStart;
		for (int i=0;i<mLevelCuboids.size();i++)
			if (mLevelCuboids.get(i).minx<=rx && mLevelCuboids.get(i).maxx>=rx &&	
				mLevelCuboids.get(i).miny<=ry && mLevelCuboids.get(i).maxy>=ry){
				setCurrentSelection(i);
			}
	}
	

	public void mouseReleased(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseClicked(MouseEvent e){}
	
	public void mouseDragged(MouseEvent e){
		mouseMoved(e);
	}
	public void mouseMoved(MouseEvent e){
		int x=e.getX()-mScene.cameraX;
		int y=e.getY()-mScene.cameraY;
		int z=-1;
		for (Cuboid c: mLevelCuboids)
			if (c.containsPoint(x, y))
				if (c.maxz>z) z = c.maxz;
		//int z=mZBuffer.data[mZBuffer.getLineIndex(y)+x];
		mScene.setFloaterText("mousez:","cur-z:"+z,0xffddddcc).y=mZBufferYStart+500;
	}


	protected void scrollLevelDelta(int delta){
		mLevelScroll+=delta;
		mShip.x=300-mShip.w/2+mLevelScroll;
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
	
	public void resizeSelectedCuboid(int dir, boolean enlarge, boolean minimum){
		if (mCurrentSelection==-1) return;
		Cuboid c = mLevelCuboids.get(mCurrentSelection);
		int inc=enlarge?1:-1;
		if (dir!=2) inc*=cuboidDelta;
		if (dir==0 && minimum)       c.minx+=inc;
		else if (dir==0 && !minimum) c.maxx+=inc;
		else if (dir==1 &&  minimum) c.miny=Math.min(c.maxy,Math.max(0,c.miny+inc));
		else if (dir==1 && !minimum) c.maxy=Math.max(c.miny,Math.min(mZBufferH,c.maxy+inc));
		else if (dir==2 &&  minimum) c.minz=Math.min(c.maxz,Math.max(0,c.minz+inc));
		else if (dir==2 && !minimum) c.maxz=Math.max(c.minz,Math.min(ZDraw.MAXZ,c.maxz+inc));
	}
	
	@Override
	public void keyPressed(KeyEvent e){
		final double acceleration=1;
		boolean ctrl=(e.getModifiers() & InputEvent.CTRL_MASK)!=0;
		boolean shift=(e.getModifiers() & InputEvent.SHIFT_MASK)!=0;
		switch  (e.getKeyCode()) {
			case KeyEvent.VK_A:
				mManager.setFloaterCursorZ(mManager.getFloaterCursorZ()+1);
				break;
			case KeyEvent.VK_S:
				if (ctrl&&shift) { //save
					saveLevel();
				} else //mouse control
					mManager.setFloaterCursorZ(mManager.getFloaterCursorZ()-1);
				break;
			//create
			case KeyEvent.VK_C:
				addCuboidAtCursor();
				break;
			//delete
			case KeyEvent.VK_D:
				removeSelectedCuboid();
				break;
			//resize
			case KeyEvent.VK_X: case KeyEvent.VK_Y: case KeyEvent.VK_Z:
				resizeSelectedCuboid(e.getKeyCode()-KeyEvent.VK_X, (e.getModifiers() & InputEvent.SHIFT_MASK)==0, (e.getModifiers() & InputEvent.CTRL_MASK)!=0);
				break;
			//scroll/change level
			case KeyEvent.VK_LEFT:
				if (ctrl && mLevel>=1) startLevel(mLevel-1);
				else scrollLevelDelta(+5);
				break;
			case KeyEvent.VK_RIGHT:
				if (ctrl) startLevel(mLevel+1);
				else scrollLevelDelta(-5);
				break;
			case KeyEvent.VK_PAGE_DOWN:
				scrollLevelDelta(-500);
				break;
			case KeyEvent.VK_PAGE_UP:
				scrollLevelDelta(+500);
				break;
		}
		mScene.setFloaterText("scroll","scroll: "+mLevelScroll,0xffddddcc);
	}

		
	public String getSIRDletName(){
		return "SIRDS Flighter (Editor)";
	}
}
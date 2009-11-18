import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class SIRDSFlighterEditor extends SIRDSFlighter implements MouseListener, MouseMotionListener{
	protected int mCurrentSelection=-1;
	public void start(Object manager){
		super.start(manager);
		
		mManager.addMouseListener(this);
		mManager.addMouseMotionListener(this);
		
		mManager.setShowFloaterCursor(true);
		
		Floater f=mManager.setFloaterText("level","level: ?", 0xffddddcc);
		int lh=f.h;
		mManager.setFloaterText("scroll","scroll: 0", 0xffddddcc).y=lh;
		mManager.setFloaterText("cursel","cursel: -1", 0xffddddcc).y=2*lh;
		mManager.getFloater("life").visible=false;
	
		startLevel(0);
	}
	public void stop(){
		super.stop();
		mManager.removeMouseListener(this);
	}
	public void startLevel(int level){
		super.startLevel(level);
		mManager.setFloaterText("level","level: "+mLevel, 0xffddddcc);
	}
	protected void saveLevel(){
		try{
		//System.out.println(mManager.getFileURL("flighter/level"+mLevel+".ser").toString());
			ObjectOutputStream levelStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(mManager.getFileURL("flighter/level"+mLevel+".ser").getFile())));
			levelStream.writeObject(mLevelCuboids);
			levelStream.close(); 		
			mManager.showFloaterMessage("saved level "+mLevel,SIRDSAppletManager.MessageType.MESSAGE_NOTIFY);
		} catch (IOException e){
			mManager.showFloaterMessage("Couldn't save"+":"+e.toString(),SIRDSAppletManager.MessageType.MESSAGE_ERROR);
		}
	}

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
		int x=e.getX();
		int y=e.getY();
		if (x<0 || y <0 || x>=mZBuffer.w || y>=mZBuffer.h) 
			return;
		int z=mZBuffer.data[mZBuffer.getLineIndex(y)+x];			
		mManager.setFloaterText("mousez:","cur-z:"+z,0xffddddcc).y=mZBufferYStart+500;
	}


	protected void scrollLevelDelta(int delta){
		mLevelScroll+=delta;
		mShip.x=300-mShip.w/2+mLevelScroll;
		mLevelEnd.x+=delta;
	}
	
	protected void setCurrentSelection(int currentSelection){
		mCurrentSelection=currentSelection;
		mManager.setFloaterText("cursel","cursel: "+mCurrentSelection, 0xffddddcc);
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
		mManager.setFloaterText("scroll","scroll: "+mLevelScroll,0xffddddcc);
	}

		
	public String getSIRDletName(){
		return "SIRDS Flighter (Editor)";
	}
}
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class SIRDSFlighter implements SIRDSlet, KeyListener{
	protected SIRDSAppletManager mManager;
	protected SceneManager mScene;
	protected ZSprite mShip, mLevelEnd;
	private Vector3d mShipA,mShipV,mShipP;
	protected int mZBufferYStart; 
	protected final int mZBufferH=500;
	private final int MAXFLYZ=19;//ZDraw.MAXZ-z-Shipheight
	protected int mLevelScroll, mLevel, mLevelLength;
	private int mInitialLife, mCurrentLife;
	protected ArrayList<Cuboid> mLevelCuboids;
	public void start(Object manager){
		mManager=(SIRDSAppletManager)manager;
		mScene=mManager.getSceneManager();
		mManager.addKeyListener(this);

		mZBufferYStart=(mScene.height-mZBufferH)/2;
				
		startLevel(1);
	}
	protected void startLevel(int level){
		mScene.clear();
		mManager.suspendRendering();
		mScene.removeFloater("zerror");
		//reset ship position
		mShip=mScene.setZSprite("ship",mScene.createZSprite("flighter/ship.png"));
		mInitialLife=0;
		for (int y=0;y<mShip.h;y++){
			int b=mShip.getLineIndex(y);
			for (int x=0;x<mShip.w;x++)
				if (mShip.dataVisible[b+x]) mInitialLife+=1;
		}
		mCurrentLife=mInitialLife;
		updateLife();
		
		mShip.x=300-mShip.w/2;
		mShip.y=250-mShip.h/2;
		mShip.z=10;

		mShipA=new Vector3d();
		mShipV=new Vector3d();
		mShipP=new Vector3d(mShip.x,mShip.y-mZBufferYStart,mShip.z);
		
		
		try{
			ObjectInputStream levelStream = new ObjectInputStream(new BufferedInputStream(mScene.getFileURL("flighter/level"+level+".ser").openStream()));
			mLevelCuboids = (ArrayList<Cuboid>) levelStream.readObject( );
			if (mLevelCuboids==null) throw new ClassNotFoundException();
			levelStream.close(); 
		} catch (IOException e){
			mLevelCuboids=new ArrayList<Cuboid>();
			mScene.setFloaterText("zerror","Level "+level+" missing").y=mScene.height/2;
		} catch (ClassNotFoundException e){
			mLevelCuboids=new ArrayList<Cuboid>();
			mScene.setFloaterText("zerror","Level "+level+" invalid").y=mScene.height/2;
		}

		for (ScenePrimitive p: mLevelCuboids)
			mScene.addPrimitive(p);

		mLevelLength=0;
		for (Cuboid c: mLevelCuboids)
			if (c.maxx>mLevelLength) mLevelLength=c.maxx;
			
		mLevelEnd=mScene.setZSprite("levelEnd",new ZSprite());
		mLevelEnd.setToString("Level "+(level+1),mManager.getGraphics().getFontMetrics(
			//new Font("Arial Black",Font.BOLD,100)),0,15);
			new Font("Arial Black",Font.BOLD,300)),0,15);
		//mLevelEnd.rotate90R();
		mLevelEnd.x=mLevelLength+50;
		mLevelEnd.y=mZBufferYStart+(mZBufferH-mLevelEnd.h)/2;
		mLevelEnd.updateVisibilityData();
		
		mLevelScroll=0;
		mLevel=level;
		
		//mLevelCuboids.add(new Cuboid(400,530,20,150,5,15));
		//mLevelCuboids.add(new HoledCuboid(500,670,200,400,0,20, 250, 350, 5, 15));
		mManager.resumeRendering();
		
	}
		
	public void stop(){
	}
/*	public void paintFrame(){
		for (int y=0;y<mZBuffer.h;y++) {
			int newZ = 0;
			if (y<mZBufferYStart-2*ZDraw.MAXZ) newZ=ZDraw.MAXZ;
			else if (y<mZBufferYStart) newZ=ZDraw.MAXZ- (y - mZBufferYStart)/2;
			else if (y>mZBuffer.h-mZBufferYStart+2*ZDraw.MAXZ) newZ=ZDraw.MAXZ;
			else if (y>mZBuffer.h-mZBufferYStart) newZ=(y - mZBuffer.h - mZBufferYStart)/2;
			else continue;
			int b=mZBuffer.getLineIndex(y);
			for (int x=0;x<mZBuffer.w;x++) 
				mZBuffer.data[b+x]=newZ;
		}
		for (Cuboid c: mLevelCuboids)	
			c.drawTo(mZBuffer,mLevelScroll,mZBufferYStart);
	}*/
	public void calculateFrame(){
		//move user ship
		mShipV.add(mShipA);
		if (mShipV.x>10) mShipV.x=30;
		if (mShipV.y>10) mShipV.y=30;
		if (mShipV.z>1.5) mShipV.z=1.5;
		if (mShipV.x<-10) mShipV.x=-30;
		if (mShipV.y<-10) mShipV.y=-30;
		if (mShipV.z<-1) mShipV.z=-1;
		mShipV.add(mShipV.clone().abs().add(1).multiply(mShipV).multiply(-0.01));//slowing down
		if (mShipA.x==0) mShipV.x-=0.1*mShipV.x; //fast slowdown
		if (mShipA.y==0) mShipV.y-=0.1*mShipV.y; //fast slowdown
		if (mShipA.z==0) mShipV.z-=0.1*mShipV.z; //fast slowdown
		mShipP.add(mShipV);
		if (mShipP.x+mShip.w>mScene.width) {
			mShipP.x=mScene.height-mShip.w;
			mShipV.x=-0.01;
		}
		if (mShipP.y+mShip.h>mZBufferH) {
			mShipP.y=mZBufferH-mShip.h;
			mShipV.y=-0.01;
		}
		if (mShipP.z>MAXFLYZ) {	
			mShipP.z=MAXFLYZ;
			mShipV.z=0;
		}
		if (mShipP.x<ZDraw.MAXZ) {
			mShipP.x=ZDraw.MAXZ;
			mShipV.x=0.01;
		}
		if (mShipP.y<0) {
			mShipP.y=0;
			mShipV.y=0.01;
		}
		if (mShipP.z<2) {
			mShipP.z=2;
			mShipV.z=0;
		}		
		mShip.x=(int)Math.round(mShipP.x);
		mShip.y=(int)Math.round(mShipP.y)+mZBufferYStart;
		mShip.z=(int)Math.round(mShipP.z);

		mLevelEnd.x=mLevelLength+50+mLevelScroll;
		
		//scroll level
		mLevelScroll-=4;
		mScene.setCameraPosition(-mLevelScroll, -(mScene.height-mZBufferH)/2, 0);

		//calculate collisions ship/geometry
		boolean coll=false;
		for (Cuboid c: mLevelCuboids)	
			coll|=c.intersect(mShip,mLevelScroll,mZBufferYStart,true);
		if (coll) updateLife();
	}
	
	private void updateLife(){
		Floater life=mScene.getFloater("life");
		if (life==null) {
			life=mScene.setFloater("life",new Floater(ZDraw.SIRDW-ZDraw.MAXZ-10,10));
			life.x=5;
			life.y=5;
			life.z=ZDraw.MAXZ;
		}
		mCurrentLife=0;
		for (int y=0;y<mShip.h;y++){
			int b=mShip.getLineIndex(y);
			for (int x=0;x<mShip.w;x++)
				if (mShip.dataVisible[b+x]) mCurrentLife+=1;
		}
		
		int padding=1;
		int len=life.w-2*padding;
		int healthyEnd=len*(mCurrentLife-mInitialLife/2)*2/mInitialLife+padding;
		for (int x=0; x<life.w;x++){
			int b=life.getLineIndex(padding);
			int color=0xffeeeeee;
			life.data[life.getLineIndex(0)+x]=color;
			life.data[life.getLineIndex(life.h-1)+x]=color;
			if (x>=padding && x<=len)
				if (x>healthyEnd) color=0xffff0000;
				else color=0xff00ff00;
			for (int y=padding;y<life.h-padding;y++){
				life.data[b+x]=color;
				b+=life.stride;
			}
		}
	}
	
	
	public void keyPressed(KeyEvent e){
		final double acceleration=1;
		switch  (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				mShipA.y=-acceleration;
				break;
			case KeyEvent.VK_DOWN:
				mShipA.y=acceleration;
				break;
			case KeyEvent.VK_LEFT:
				mShipA.x=-acceleration;
				break;
			case KeyEvent.VK_RIGHT:
				mShipA.x=+acceleration;
				break;
			case KeyEvent.VK_SHIFT:
				mShipA.z=+0.05;
				break;
			case KeyEvent.VK_CONTROL:
				mShipA.z=-0.05;
				break;
		}
	}
	public void keyReleased(KeyEvent e){
		switch  (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				mShipA.y=0;
				break;
			case KeyEvent.VK_DOWN:
				mShipA.y=0;
				break;
			case KeyEvent.VK_LEFT:
				mShipA.x=0;
				break;
			case KeyEvent.VK_RIGHT:
				mShipA.x=0;
				break;
			case KeyEvent.VK_SHIFT:
				mShipA.z=0;
				break;
			case KeyEvent.VK_CONTROL:
				mShipA.z=0;
				break;
		}
	}
	public void keyTyped(KeyEvent e){
	}
	
	
	public String getSIRDletName(){
		return "SIRDS Flighter";
	}
	
}
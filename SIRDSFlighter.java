import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class SIRDSFlighter implements SIRDSlet	{
	public int KEY_SHIP_ACC_LEFT=KeyEvent.VK_LEFT;
	public int KEY_SHIP_ACC_RIGHT=KeyEvent.VK_RIGHT;
	public int KEY_SHIP_ACC_UP=KeyEvent.VK_UP;
	public int KEY_SHIP_ACC_DOWN=KeyEvent.VK_DOWN;
	public int KEY_SHIP_ACC_ASCEND=KeyEvent.VK_SHIFT;
	public int KEY_SHIP_ACC_DESCEND=KeyEvent.VK_CONTROL;
	public int KEY_SHIP_FIRE=KeyEvent.VK_SPACE;


	
	//technical things
	protected SIRDSAppletManager mManager;
	protected SceneManager mScene;
	protected int mZBufferYStart;
	protected final int mZBufferH=500;
	//Ship
	protected ZSprite mShip;
	private Vector3d mShipV,mShipP;
	private final int MAXFLYZ=19;//ZDraw.MAXZ-z-Shipheight
	private int mInitialLife, mCurrentLife;
	protected ArrayList<Cuboid> mShoots;
	protected long mCurTime,mLastShoot = 0;
	protected int mShootTimeout, mShootCount;
	//World
	protected ZSprite mLevelEnd;
	protected int mLevelScroll, mLevel, mLevelLength;
	protected ArrayList<Cuboid> mLevelCuboids;
	protected Map<Object,PrimitiveModifier> mLevelModifier;


	public void start(Object manager){
		mManager=(SIRDSAppletManager)manager;
		mScene=mManager.getSceneManager();

		mZBufferYStart=(mScene.height-mZBufferH)/2;

		mShootTimeout = 450;
		mShootCount = 0;
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

		mShipV=new Vector3d();
		mShipP=new Vector3d(mShip.x,mShip.y-mZBufferYStart,mShip.z);
		
		
		try{
			JSONReader json = new JSONReader();
			BufferedReader br = new BufferedReader(new FileReader(mScene.getFileURL("flighter/level"+level+".lev").getFile()));
			String temp="",temp2="";
			do {
				temp=temp+temp2;
				temp2=br.readLine();
			} while (temp2!=null);
			Object levelJSONR = json.read(temp);
			ArrayList<Object> levelJSON = (ArrayList<Object>) levelJSONR;
			br.close();
			mLevelCuboids=new ArrayList<Cuboid>();
			mLevelModifier=new HashMap<Object, PrimitiveModifier>();
			for (Object o: levelJSON)
				if (o instanceof Map) {
					Map<String, Object> m = (Map<String, Object>)o;
					if ("Cuboid".equals(m.get("type"))) {
						Cuboid c=new Cuboid();
						c.jsonDeserialize(m);
						mLevelCuboids.add(c);
						mScene.addPrimitive(c);
						if (m.containsKey("mover")) {
							PrimitiveMover pm=new PrimitiveMover(c);
							pm.jsonDeserialize(m.get("mover"));
							mScene.addPrimitiveModifier(pm);
							mLevelModifier.put(c, pm);
						}
					}
				}
			/*
			ObjectInputStream levelStream = new ObjectInputStream(new BufferedInputStream(mScene.getFileURL("flighter/level"+level+".ser").openStream()));
			mLevelCuboids = (ArrayList<Cuboid>) levelStream.readObject( );
			if (mLevelCuboids==null) throw new ClassNotFoundException();
			levelStream.close(); */
		} catch (Exception e){
			mLevelCuboids=new ArrayList<Cuboid>();
			mScene.setFloaterText("zerror","Level "+level+" missing").y=mScene.height/2;
		}

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
		
		mLevelScroll=0;
		mLevel=level;

		mShoots=new ArrayList<Cuboid>();
		
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
	public void calculateFrame(long time){
		mCurTime=time;

		if (mCurrentLife<=0){
			//DEAD
			return;
		}

		//---------------------keyinput------------------------
		final double acceleration=1;
		Vector3d mShipA=new Vector3d();
		mShipA.x-=mManager.isKeyPressed(KEY_SHIP_ACC_LEFT)?acceleration:0;
		mShipA.x+=mManager.isKeyPressed(KEY_SHIP_ACC_RIGHT)?acceleration:0;
		mShipA.y-=mManager.isKeyPressed(KEY_SHIP_ACC_UP)?acceleration:0;
		mShipA.y+=mManager.isKeyPressed(KEY_SHIP_ACC_DOWN)?acceleration:0;
		mShipA.z-=mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND)?0.05:0;
		mShipA.z+=mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND)?0.05:0;

		if (mManager.isKeyPressed(KEY_SHIP_FIRE))
			shipFire();

		//-----------------move user ship----------------------
		mShipV.add(mShipA);
		if (mShipV.x>50) mShipV.x=50;
		if (mShipV.y>30) mShipV.y=30;
		if (mShipV.z>1.5) mShipV.z=1.5;
		if (mShipV.x<-30) mShipV.x=-30;
		if (mShipV.y<-30) mShipV.y=-30;
		if (mShipV.z<-1) mShipV.z=-1;
		mShipV.add(mShipV.clone().abs().add(1).multiply(mShipV).multiply(-0.01));//slowing down
		if (mShipA.x==0) mShipV.x-=0.1*mShipV.x; //fast slowdown
		if (mShipA.y==0) mShipV.y-=0.1*mShipV.y; //fast slowdown
		if (mShipA.z==0) mShipV.z-=0.1*mShipV.z; //fast slowdown
		//mShipP.x+=1;
		mShipP.add(mShipV);
		/*if (mShipP.x+mShip.w>mScene.width) {
			mShipP.x=mScene.height-mShip.w;
			mShipV.x=-0.01;
		}
		 * */
		if (mShipP.y+mShip.h>mZBufferH) {
			mShipP.y=mZBufferH-mShip.h;
			if (mShipV.y>-0.01) mShipV.y=-0.01;
		}
		if (mShipP.z>MAXFLYZ) {	
			mShipP.z=MAXFLYZ;
			if (mShipV.z>-0.01) mShipV.z=-0.01;
		}
		if (mShipP.x< -mLevelScroll+ZDraw.MAXZ+30) { //there are maxz unreachable pixel at the left screen side
			mShipP.x=-mLevelScroll+ZDraw.MAXZ+30;
			if (mShipV.x<0.01) mShipV.x=0.01;
		}
		if (mShipP.y<0) {
			mShipP.y=0;
			if (mShipV.y<0.01) mShipV.y=0.01;
		}
		if (mShipP.z<2) {
			mShipP.z=2;
			if (mShipV.z<0.01) mShipV.z=0.01;
		}		

		mShip.x=(int)Math.round(mShipP.x);
		mShip.y=(int)Math.round(mShipP.y);
		mShip.z=(int)Math.round(mShipP.z);
//		mLevelEnd.x=mLevelLength+50+mLevelScroll;



		for (int i=mShoots.size()-1;i>=0;i--){
			Cuboid c=mShoots.get(i);
			c.maxx+=40;
			c.minx+=40;
			if (c.maxx>mScene.width+mScene.cameraX){
				mShoots.remove(i);
				mScene.removePrimitive(c);
				break;
			}
			for (Cuboid d: mLevelCuboids)
				if (c.intersect(d, -20, 0)){
					mShoots.remove(i);
					mScene.removePrimitive(c);
					break;
				}
		}



		//--------------calculate collisions ship/geometry-------------
		boolean coll=false;
		for (Cuboid c: mLevelCuboids)	
			coll|=c.intersect(mShip,0,0,true);
		if (coll) {
			updateLife();
			if (mCurrentLife<=0) return; //game end
		}


		if (mShip.x > mLevelLength + 100 + mLevelEnd.w) startLevel(mLevel+1);


		//------------------scroll level--------------------
		mLevelScroll-=4;
		if (mShip.x > mLevelLength + 50) {
			mLevelScroll-=10;
			mShipP.x+=10;
		}
		mScene.setCameraPosition(-mLevelScroll, -(mScene.height-mZBufferH)/2, 0);
	}
	
	private void updateLife(){
		if (mCurrentLife<=0) return;

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
		mCurrentLife=mCurrentLife-mInitialLife/2;

		int padding=1;
		int len=life.w-2*padding;
		int healthyEnd=len*(mCurrentLife)*2/mInitialLife+padding;
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

		if (mCurrentLife<=0) {
			//You died now
			mScene.clear();

			ZSprite mes=mScene.setZSprite("mes",new ZSprite());
			mes.setToString("YOU\nDIED",mManager.getGraphics().getFontMetrics(
			//new Font("Arial Black",Font.BOLD,100)),0,15);
			new Font("Arial Black",Font.BOLD,150)),0,15);
		//mLevelEnd.rotate90R();
			mes.x=(mScene.width-mes.w)/2;
			mes.y=(mScene.height-mes.h)/2;
		}
	}

	private void shipFire(){
		if (mCurTime - mLastShoot < mShootTimeout)
			return;
		mLastShoot=mCurTime;
		Cuboid fire=new Cuboid();
		int ox=mShip.x, oy=mShip.y, oz=mShip.z;
		//if ((mShootCount & 1) != 0) oy=mShip.y+mShip.h;
		oy=mShip.y+mShip.h/2;
		int sx=60, sy=16, sz=8;
		fire.minx=ox-sx/2;
		fire.miny=oy-sy/2;
		fire.minz=Math.max(0,oz-sz/2);
		fire.maxx=ox+sx/2;
		fire.maxy=oy+sy/2;
		fire.maxz=Math.min(ZDraw.MAXZ, oz+sz/2);
		mScene.addPrimitive(fire);
		mShoots.add(fire);
		mShootCount++;
	}
	
	public String getSIRDletName(){
		return "SIRDS Flighter";
	}
	
}
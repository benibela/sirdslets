import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Vector3d{
	public double x,y,z;
	public Vector3d(){
		x=0;
		y=0;
		z=0;
	}
	public Vector3d(double nx,double ny, double nz){
		x=nx;
		y=ny;
		z=nz;
	}
	public Vector3d(final Vector3d old){
		x=old.x;
		y=old.y;
		z=old.z;
	}
	public Vector3d abs(){
		if (x<0) x=-x;
		if (y<0) y=-y;
		if (z<0) z=-z;
		return this;
	}
	public Vector3d add(final Vector3d v){
		x+=v.x;
		y+=v.y;
		z+=v.z;
		return this;
	}
	public Vector3d add(final double f){
		x+=f;
		y+=f;
		z+=f;
		return this;
	}
	public Vector3d multiply(final double f){
		x*=f;
		y*=f;
		z*=f;
		return this;
	}
	public Vector3d multiply(final Vector3d v){
		x*=v.x;
		y*=v.y;
		z*=v.z;
		return this;
	}
	public double length(){
		return Math.sqrt(x*x+y*y+z*z);
	}
	public Vector3d clone(){
		return new Vector3d(this);
	}
}

class Cuboid implements Serializable{
	static final long serialVersionUID = -6773890043854716267L;
	int minx, maxx, miny, maxy, minz, maxz;
	int perspectiveOffset=32;
	public void setSize(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz){
		if (nmaxx<=nminx) throw new IllegalArgumentException("invalid x size: "+nminx+" - "+nmaxx);
		if (nmaxy<=nminy) throw new IllegalArgumentException("invalid y size: "+nminy+" - "+nmaxy);
		if (nmaxz<=nminz) throw new IllegalArgumentException("invalid z size: "+nminz+" - "+nmaxz);
		//System.out.println("nminz:"+nminz+" nmaxz:"+nmaxz);
		minx=nminx;
		maxx=nmaxx;
		miny=nminy;
		maxy=nmaxy;
		minz=nminz;
		maxz=nmaxz;
	}
	public Cuboid(){}
	public Cuboid(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz){
		setSize(nminx,  nmaxx, nminy, nmaxy, nminz, nmaxz);
	}
	public void drawTo(ZDraw map, int dx, int dy){
		int nminx=minx+dx;
		int nmaxx=maxx+dx;
		
		int fx=nminx;
		if (fx<0) fx=0;
		if (fx>=map.w) return; //culling
		int tx=nmaxx;
		if (tx<0) return;
		if (tx>=map.w) tx=map.w-1;
		if (tx<fx) return;
		
		int fy=miny+dy;
		if (fy<0) fy=0;
		if (fy>=map.h) return;
		int ty=maxy+dy;
		if (ty<0) return;
		if (ty>=map.h) ty=map.h-1;
		if (ty<fy) return;
		int deltaZ=maxz-minz;
		//System.out.println(deltaZ+ " "+minz+" "+maxz);
		for (int y=fy; y<=ty; y++){
			int b=map.getLineIndex(y);
			for (int x=fx;x<=tx; x++)
				if (x<nminx+perspectiveOffset) map.customPut(b+x, (deltaZ*(x-nminx))/perspectiveOffset+minz);
				else if (x>nmaxx-perspectiveOffset) map.customPut(b+x, (deltaZ*(nmaxx-x))/perspectiveOffset+minz);
				else map.customPut(b+x, maxz);
		}
	}
	public boolean intersect(ZSprite sprite, int dx, int dy, boolean removeIntersection){
		//transform in the local coordinates of the sprite
		int nminx=minx+dx-sprite.x;
		int nmaxx=maxx+dx-sprite.x;
		
		int fx=nminx;
		if (fx<0) fx=0;
		if (fx>=sprite.w) return false;
		int tx=nmaxx;
		if (tx<0) return false;
		if (tx>=sprite.w) tx=sprite.w-1;
		if (tx<fx) return false;
		
		int fy=miny+dy-sprite.y;
		if (fy<0) fy=0;
		if (fy>=sprite.h) return false;
		int ty=maxy+dy-sprite.y;
		if (ty<0) return false;
		if (ty>=sprite.h) ty=sprite.h-1;
		if (ty<fy) return false;
		int deltaZ=maxz-minz;
		//check the area (fx,tx)*(fy,ty) for equal z coordinates
		boolean result=false;
		for (int y=fy; y<=ty; y++){
			int b=sprite.getLineIndex(y);
			for (int x=fx;x<=tx; x++) {
				int sz=sprite.data[b+x]+sprite.z;
				if (sz<minz) continue;
				int myz;
				if (x<nminx+perspectiveOffset) myz=(deltaZ*(x-nminx))/perspectiveOffset+minz;
				else if (x>nmaxx-perspectiveOffset) myz=(deltaZ*(nmaxx-x))/perspectiveOffset+minz;
				else myz=maxz;
				if (sz<=myz &&  sprite.dataVisible[b+x]) {
					result=true;
					if (removeIntersection) sprite.dataVisible[b+x]=false;
					else return true;
				}
			}
		}
		return result;
	}
}

class HoledCuboid extends Cuboid{
	static final long serialVersionUID = -7398540760922297999L;
	int holeminy, holemaxy, holeminz, holemaxz;
	Cuboid upper=new Cuboid();
	Cuboid down=new Cuboid();
	Cuboid below=new Cuboid();
	Cuboid above=new Cuboid();
	
	public void setSize(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz, int nholeminy, int nholemaxy, int nholeminz, int nholemaxz){
		super.setSize(nminx,  nmaxx, nminy, nmaxy, nminz, nmaxz);
		//System.out.println("nholeminy:"+nholeminy+ " nholemaxy:"+nholemaxy+ " nholeminz:"+nholeminz +" nholemaxz:"+nholemaxz);
		int aboveOffset=perspectiveOffset+8;
		holeminy=nholeminy;
		holemaxy=nholemaxy;
		holeminz=nholeminz;
		holemaxz=nholemaxz;
		upper.setSize(nminx,  nmaxx, nminy, holeminy, nminz, nmaxz);
		down.setSize(nminx,  nmaxx, holemaxy, maxy, nminz, nmaxz);
		below.setSize(nminx,  nmaxx, nminy, nmaxy, nminz, holeminz);
		above.setSize(nminx+aboveOffset,  nmaxx-aboveOffset, holeminy, holemaxy, holemaxz, maxz);
		below.perspectiveOffset=upper.perspectiveOffset*(holeminy-miny)/(maxy-miny);
	}
	
	public HoledCuboid(){}
	public HoledCuboid(int nminx, int nmaxx, int nminy, int nmaxy, int nminz, int nmaxz, int nholeminy, int nholemaxy, int nholeminz, int nholemaxz){
		setSize(nminx,  nmaxx, nminy, nmaxy, nminz, nmaxz, nholeminy,nholemaxy,nholeminz,nholemaxz);
	}
	
	public void drawTo(ZDraw map, int dx, int dy){
		//don't draw myself
		down.drawTo(map,dx,dy);
		upper.drawTo(map,dx,dy);
		above.drawTo(map,dx,dy);
		below.drawTo(map,dx,dy);
	}

	public boolean intersect(ZSprite sprite, int dx, int dy, boolean removeIntersection){
		boolean result=false;
		result|=down.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		result|=upper.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		result|=above.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		result|=below.intersect(sprite,dx,dy,removeIntersection);
		if (result && !removeIntersection) return true;
		return result;
	}
}

public class SIRDSFlighter implements SIRDSlet, KeyListener{
	protected ZDraw mZBuffer;
	protected SIRDSAppletManager mManager;
	protected ZSprite mShip;
	private Vector3d mShipA,mShipV,mShipP;
	protected int mZBufferYStart; 
	protected final int mZBufferH=500;
	private final int MAXFLYZ=19;//ZDraw.MAXZ-z-Shipheight
	protected int mLevelScroll, mLevel;
	private int mInitialLife, mCurrentLife;
	protected ArrayList<Cuboid> mLevelCuboids;
	public void start(Object manager){
		mManager=(SIRDSAppletManager)manager;
		mManager.setDoubleBufferedZBuffer(true);
		mZBuffer=mManager.getZBuffer();
		
		mManager.addKeyListener(this);

		mZBufferYStart=(mZBuffer.h-mZBufferH)/2;
				
		startLevel(1);
	}
	protected void startLevel(int level){
		mManager.suspendRendering();
		mManager.removeFloater("zerror");
		//reset ship position
		mShip=mManager.setZSprite("ship",mManager.createZSprite("flighter/ship.png"));
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
			ObjectInputStream levelStream = new ObjectInputStream(new BufferedInputStream(mManager.getFileURL("flighter/level"+level+".ser").openStream()));
			mLevelCuboids = (ArrayList<Cuboid>) levelStream.readObject( );
			if (mLevelCuboids==null) throw new ClassNotFoundException();
			levelStream.close(); 
		} catch (IOException e){
			mLevelCuboids=new ArrayList<Cuboid>();
			mManager.setFloaterText("zerror","Level "+level+" missing").y=mZBuffer.h/2;
		} catch (ClassNotFoundException e){
			mLevelCuboids=new ArrayList<Cuboid>();
			mManager.setFloaterText("zerror","Level "+level+" invalid").y=mZBuffer.h/2;
		}
		mLevelScroll=0;
		mLevel=level;
		
		//mLevelCuboids.add(new Cuboid(400,530,20,150,5,15));
		//mLevelCuboids.add(new HoledCuboid(500,670,200,400,0,20, 250, 350, 5, 15));
		mManager.resumeRendering();
		
	}
		
	public void stop(){
	}
	public void paintFrame(){		
		mZBuffer.clear();
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
	}
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
		if (mShipP.x+mShip.w>mZBuffer.w) {
			mShipP.x=mZBuffer.w-mShip.w;
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
		
		//scroll level
		mLevelScroll-=2;
		
		//calculate collisions ship/geometry
		boolean coll=false;
		for (Cuboid c: mLevelCuboids)	
			coll|=c.intersect(mShip,mLevelScroll,mZBufferYStart,true);
		if (coll) updateLife();
	}
	
	private void updateLife(){
		Floater life=mManager.getFloater("life");
		if (life==null) {
			life=mManager.setFloater("life",new Floater(ZDraw.SIRDW-ZDraw.MAXZ-10,10));
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
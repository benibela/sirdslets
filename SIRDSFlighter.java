//import java.util.*;
import java.awt.*;
import java.awt.event.*;
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

public class SIRDSFlighter implements SIRDSlet, KeyListener{
	private ZDraw mZBuffer;
	private SIRDSAppletManager mManager;
	private ZSprite mShip;
	private Vector3d mShipA,mShipV,mShipP;
	private int mZBufferYStart; 
	private final int mZBufferH=500;
	private final int MAXFLYZ=20;
	public void start(Object manager){
		mManager=(SIRDSAppletManager)manager;
		mManager.setDoubleBufferedZBuffer(true);
		mZBuffer=mManager.getZBuffer();
		
		mManager.addKeyListener(this);
		
		mShip=mManager.setZSprite("ship",mManager.createZSprite("flighter/ship.png"));
		mShip.x=300-mShip.w/2;
		mShip.y=250-mShip.h/2;
		mShip.z=10;
		
		mShipA=new Vector3d();
		mShipV=new Vector3d();
		mShipP=new Vector3d(mShip.x,mShip.y-mZBufferYStart,mShip.z);
		
		mZBufferYStart=(mZBuffer.h-mZBufferH)/2;
		
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
	}
	public void stop(){
	}
	public void paintFrame(){	
	}
	public void calculateFrame(){
		mShipV.add(mShipA);
		if (mShipV.x>10) mShipV.x=30;
		if (mShipV.y>10) mShipV.y=30;
		if (mShipV.z>1.5) mShipV.z=1.5;
		if (mShipV.x<-10) mShipV.x=-30;
		if (mShipV.y<-10) mShipV.y=-30;
		if (mShipV.z<-1) mShipV.z=-1;
		mShipV.add(mShipV.clone().abs().add(1).multiply(mShipV).multiply(-0.01));//slowing down
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
	}
	
	
	public void keyPressed(KeyEvent e){
		switch  (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				mShipA.y=-0.6;
				break;
			case KeyEvent.VK_DOWN:
				mShipA.y=0.6;
				break;
			case KeyEvent.VK_LEFT:
				mShipA.x=-0.6;
				break;
			case KeyEvent.VK_RIGHT:
				mShipA.x=+0.6;
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
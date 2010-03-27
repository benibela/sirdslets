
//
// AbSIRDlet - A real-time SIRDS drawing applet
//
// Written by Lewey Geselowitz
//  http://www.leweyg.com
//
 
import java.awt.event.*;
import java.awt.image.*;
import java.applet.*;
import java.util.*;
import java.awt.*;
import java.net.*;

public class AbSIRDlet implements SIRDSlet
{
	private SIRDSAppletManager mManager;
	private ZSprite mZBuffer;
	private ZDraw mDrawArea;
	private int mWidth, mHeight;
	private boolean mUseSIRD = true;
	private int mCurMX, mCurMY, mCurrentZ;
	private int mLastMX, mLastMY;
	private int mDrawRadius;
	private final int mPalWidth = 40;
	private SceneManager mScene;
	
	private void DrawPallette()
	{
		int y;
		int ny;
		for (int i=0; i<ZDraw.MAXZ; i++)
		{
			y = (i*mWidth)/ZDraw.MAXZ;
			ny= ((i+1)*mWidth)/ZDraw.MAXZ;
			mZBuffer.fillRect(mWidth-mPalWidth, y,
				mPalWidth, ny-y, i);
		}
	}
	
	public void start(Object manager){
	
		mDrawRadius = 10;
		mCurrentZ = 10;
	
		mManager=(SIRDSAppletManager)manager;
		
		mScene=mManager.getSceneManager();
		


		mManager.setShowFloaterCursor(true);
		mManager.setAllowSaving(true);
		mManager.setAllowLoading(true);
		
		mWidth  = mManager.getSize().width;
		mHeight = mManager.getSize().height;
		
		mZBuffer = new ZSprite(mScene.width, mScene.height);
		mScene.addPrimitive(mZBuffer);

		mDrawArea = new ZDraw(mZBuffer.data, mWidth-mPalWidth-ZDraw.SIRDW, 
			mHeight, ZDraw.SIRDW, mZBuffer.stride );

		DrawPallette();
		
		
		Floater f=mScene.setFloaterText("position","Position: ",0xffddddcc);
		f.z=2;
		int lineHeight=f.h;
		f=mScene.setFloaterText("xpos","x: ?",0xffddddcc);
		f.y=lineHeight;
		f=mScene.setFloaterText("ypos","y: ?",0xffddddcc);
		f.y=2*lineHeight;
		f=mScene.setFloaterText("zpos","z: ?",0xffddddcc);
		f.y=3*lineHeight;
		f=mScene.setFloaterText("pen","pen-size: "+mDrawRadius,0xffddddcc);
		f.y=4*lineHeight;

		mScene.setFloater("apenmouse",new Floater(2*mDrawRadius,2*mDrawRadius));
		updatePenInformation();
		//f.ignoreHeightmap=false;
	}
	public void stop(){
		mManager.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	public void paintFrame(){
	}
	public void calculateFrame(long timeMS){

		if (mManager.isMousePressed(MouseEvent.BUTTON1)){
			if (mManager.isMousePressedChanged(MouseEvent.BUTTON1)){
				mDrawArea.mIgnoreZ=mManager.isKeyPressed(KeyEvent.VK_CONTROL);
				mDrawArea.fillCircle( mManager.getMouseX()-ZDraw.SIRDW, mManager.getMouseY(), mDrawRadius, mCurrentZ );

				if (mManager.getMouseX() + mPalWidth > mWidth)
				{
					mCurrentZ = (mManager.getMouseY() * ZDraw.MAXZ) / mHeight;
				}
			} else {
				mDrawArea.mIgnoreZ=mManager.isKeyPressed(KeyEvent.VK_CONTROL);
				mDrawArea.DrawLine( mManager.getMouseX()-ZDraw.SIRDW, mManager.getMouseY(),
					mLastMX-ZDraw.SIRDW, mLastMY, mDrawRadius, mCurrentZ);
			}
		}
		mLastMX = mManager.getMouseX();
		mLastMY = mManager.getMouseY();

		if (mCurMX!=mLastMX || mCurMY!=mLastMY){
			mCurMX=mLastMX;
			mCurMY=mLastMY;
			drawInformation();
		}

		if (mManager.isKeyPressed(KeyEvent.VK_A)){
			mCurrentZ+=1+ZDraw.MAXZ/50;
			if (mCurrentZ>ZDraw.MAXZ) mCurrentZ=ZDraw.MAXZ;
		} else if (mManager.isKeyPressed(KeyEvent.VK_S)){
			mCurrentZ-=1+ZDraw.MAXZ/50;
			if (mCurrentZ<0) mCurrentZ=0;
		} else if (mManager.isKeyPressed(KeyEvent.VK_D)){
			mDrawRadius+=1;
			updatePenInformation();
		} else if (mManager.isKeyPressed(KeyEvent.VK_F)){
			mDrawRadius-=1;
			updatePenInformation();
		}
		drawInformation();
	}
	

	private void updatePenInformation(){
		mScene.setFloaterText("pen","pen-size: "+mDrawRadius,0xffddddcc);
		Floater f=mScene.getFloater("apenmouse");
		if (2*mDrawRadius>=mZBuffer.SIRDW) return;
 		if (f.w<2*mDrawRadius || f.h<2*mDrawRadius) f.setSize(2*mDrawRadius,2*mDrawRadius);
		f.clear();
		f.fillCircle(f.w/2,f.h/2, mDrawRadius,0x88ee8800);
		f.fillCircle(f.w/2+1,f.h/2, 2,0xff000000);
	}
	private void drawInformation(){
		mScene.setFloaterText("xpos","x: "+mCurMX,0xffddddcc);
		mScene.setFloaterText("ypos", "y: "+mCurMY,0xffddddcc);
		mScene.setFloaterText("zpos","z: "+ mCurrentZ,0xffddddcc);
		mManager.setFloaterCursorZ(mCurrentZ+3);
		Floater mf=mScene.getFloater("apenmouse");
		mf.x=mCurMX-mf.w/2;
		mf.y=mCurMY-mf.h/2;
		mf.z=mCurrentZ;
	}
	
	public String getSIRDletName(){	
		return "SIRD Painter";
	}
}
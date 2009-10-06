
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

public class AbSIRDlet implements SIRDSlet, MouseListener, MouseMotionListener, KeyListener
{
	private SIRDSAppletManager mManager;
	private ZDraw mZBuffer;
	private ZDraw mDrawArea;
	private int mWidth, mHeight;
	private boolean mUseSIRD = true;
	private int mCurrentZ = 10;
	private int mLastMX, mLastMY;
	private final int mDrawRadius = 10;
	private final int mPalWidth = 40;
	
	private void DrawPallette()
	{
		int y;
		int ny;
		for (int i=0; i<ZDraw.MAXZ; i++)
		{
			y = (i*mWidth)/ZDraw.MAXZ;
			ny= ((i+1)*mWidth)/ZDraw.MAXZ;
			mZBuffer.DrawRect(mWidth-mPalWidth, y,
				mPalWidth, ny-y, i);
		}
	}
	
	public void start(Object manager){
		mManager=(SIRDSAppletManager)manager;
		
		mManager.addMouseListener(this);
		mManager.addMouseMotionListener(this);
		mManager.addKeyListener(this);
		
		mWidth  = mManager.getSize().width;
		mHeight = mManager.getSize().height;
		
		mZBuffer = mManager.getZBuffer();
		mZBuffer.Clear();
		
		mDrawArea = new ZDraw(mZBuffer.data, mWidth-mPalWidth-ZDraw.SIRDW, 
			mHeight, ZDraw.SIRDW, mZBuffer.stride );
		
		DrawPallette();
	}
	public void stop(){
		mManager.removeMouseListener(this);
		mManager.removeMouseMotionListener(this);
		mManager.removeKeyListener(this);
	}
	public void paintFrame(){
	}
	public void calculateFrame(){
	}
	
	public void mousePressed(MouseEvent e)
	{
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK)!=0)
		{
			mDrawArea.DrawCircle( e.getX()-ZDraw.SIRDW, e.getY(), 
				mDrawRadius, mCurrentZ );
		}
		mLastMX = e.getX();
		mLastMY = e.getY();
	}

	public void mouseDragged(MouseEvent e)
	{
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK)!=0)
		{
			mDrawArea.DrawLine( e.getX()-ZDraw.SIRDW, e.getY(), 
				mLastMX-ZDraw.SIRDW, mLastMY, mDrawRadius, mCurrentZ);
		}
		mLastMX = e.getX();
		mLastMY = e.getY();
	}

	public void mouseReleased(MouseEvent e)
	{
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK)!=0)
		{
			if (e.getX() + mPalWidth > mWidth)
			{
				mCurrentZ = (e.getY() * ZDraw.MAXZ) / mHeight;
			}
		}
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK)!=0)
		{
			mDrawArea.mIgnoreZ = !mDrawArea.mIgnoreZ;
		}
		//if ((e.getModifiers() & InputEvent.BUTTON3_MASK)!=0)
		//	setDisplayMode(mDisplayMode+1);
	}

	public void mouseExited(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseClicked(MouseEvent e){}
	public void mouseMoved(MouseEvent e){}

	public void keyPressed(KeyEvent e){
		if (e.getKeyCode()==KeyEvent.VK_A) {
			mCurrentZ+=1+ZDraw.MAXZ/50;
			if (mCurrentZ>ZDraw.MAXZ) mCurrentZ=ZDraw.MAXZ;
		} else if (e.getKeyCode()==KeyEvent.VK_S) {
			mCurrentZ-=1+ZDraw.MAXZ/50;
			if (mCurrentZ<0) mCurrentZ=0;
		}
	}
	public void keyReleased(KeyEvent e){
	}
	public void keyTyped(KeyEvent e){
	}

	public String getSIRDletName(){	
		return "SIRD Painter";
	}
}
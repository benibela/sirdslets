
//
// SPonglet - A SIRDS based Pong game
//
// Written by Lewey Geselowitz
//  http://www.leweyg.com
//

import java.awt.event.*;
import java.awt.image.*;
import java.applet.*;
import java.awt.*;

public class SPonglet extends Applet implements Runnable, MouseListener, MouseMotionListener
{
	private Thread mUpdateThread;
	private Image mBackBuffer;
	private ZDraw mZBuffer;
	private ZDraw mDrawArea;
	private int[] mRandData;
	private ZDraw mSIRDPixels;
	private MemoryImageSource mSIRDImage;
	private int mWidth, mHeight;
	private int mFrameNumber = 0;
	private boolean mUseSIRD = true;
	private int mCurrentZ = 10;
	private int mLastMX, mLastMY;
	private final int mDrawRadius = 10;

	private class PongGame
	{
		public ZDraw.Pnt mFrontPos;
		public ZDraw.Pnt mBackPos;
		public int mFrontWidth, mBackWidth;
		public int mFrontH, mBackH;
		public ZDraw mDrawTo;
		private ZDraw.SlantRect mRect;

		public boolean mIsActive = true;

		public long LastTime;
		public long CurDTime;

		public double BallX, BallY;
		public double BallDX, BallDY;
		public double PaddleX;
		public double BallVel;
		public final double BallVelBase = 0.5;
		public final double BallVelInc = 0.025;
		public final double BallRad = 0.2;
		public final double FrontPaddleWidth = 0.5, BackPaddleWidth = 0.8;
		public int mNumHits;

		private void BallHitPaddle()
		{
			if (BallY > 0)
			{
				BallY = 1.0;
				double b = (BallX - PaddleX);
				b /= ((FrontPaddleWidth/2.0) + BallRad);
				b *= Math.PI * 0.4;
				BallDX = Math.sin(b);
				BallDY = -Math.cos(b);
			}
			else
			{
				BallY = -1.0;
				BallDY *= -1;
			}

			BallVel += BallVelInc;
			mNumHits++;
		}

		public void Tick()
		{
			long ct = System.currentTimeMillis();
			CurDTime = ct - LastTime;
			double dt = (double)(ct - LastTime) / 1000.0;
			LastTime = ct;

			if (!mIsActive)
				return;

			BallX += BallDX*BallVel*dt;
			BallY += BallDY*BallVel*dt;

			if (BallX+BallRad >= 1.0)
			{
				BallX = 1.0-BallRad;
				BallDX *= -1.0;
			}
			if (BallX-BallRad <= -1.0)
			{
				BallX = -1.0 + BallRad;
				BallDX *= -1.0;
			}

			if (BallY >= 1.0)
			{
				if ((BallX + BallRad < PaddleX-FrontPaddleWidth/2) || 
					(BallX - BallRad > PaddleX+FrontPaddleWidth/2))
				{
					mIsActive = false;
				}
				else
					BallHitPaddle();
			}
			if (BallY <= -1.0)
			{
				if ((BallX + BallRad < PaddleX-BackPaddleWidth/2) || 
					(BallX - BallRad > PaddleX+BackPaddleWidth/2))
				{
					mIsActive = false;
				}
				else
					BallHitPaddle();
			}
		}

		public void SetPaddleX(int mx)
		{
			if (!mIsActive)
				return;

			mx -= (mDrawTo.w/2);
			double x = ((double)mx) / ((double)(mFrontWidth/2));
			double r = FrontPaddleWidth/2.0;
			if (x + r >= 1.0)
				x = 1.0 - r;
			if (x - r <= -1.0)
				x = -1.0 + r;

			PaddleX = x;
		}

		public PongGame(ZDraw to)
		{
			LastTime = System.currentTimeMillis();
			mDrawTo = to;
			mFrontPos = new ZDraw.Pnt(to.w/2, to.h-20, ZDraw.MAXZ-2 );
			mBackPos = new ZDraw.Pnt(to.w/2, 40, 2 );

			mFrontWidth = (int)(to.w * 0.8);
			mBackWidth = (int)(to.w * 0.25);

			mFrontH = (to.w - mFrontWidth)/2 - 2;
			mBackH = 10;

			mRect = new ZDraw.SlantRect();

			PaddleX = 0.0;
			NewGame();
		}

		public void Clicked()
		{
			if (mIsActive)
				return;

			NewGame();
		}

		public void NewGame()
		{
			BallVel = BallVelBase;
			BallX = 0;
			BallY = 0;
			double ang = -(Math.random()*0.5 + 0.25)*Math.PI;
			BallDX = Math.cos(ang);
			BallDY = Math.sin(ang)*-1.0;

			mNumHits = 0;
			mIsActive = true;
		}

		public void DrawBoard()
		{
			mDrawTo.Clear();

			int bx = mBackPos.x - mBackWidth/2;
			int fx = mFrontPos.x - mFrontWidth/2;

			//back paddle:
			int pw = (int)(mBackWidth*(BackPaddleWidth/2.0));
			int sx = (int)(mBackPos.x + PaddleX*(mBackWidth/2) - pw/2);
			mDrawTo.DrawRect( sx, mBackPos.y-mBackH*2, pw, mBackH*2, mBackPos.z);

			//main board
			mRect.From.set( fx, mFrontPos.y, mFrontPos.z);
			mRect.FromW = mFrontWidth;
			mRect.To.set( bx, mBackPos.y, mBackPos.z);
			mRect.ToW = mBackWidth;
			mDrawTo.DrawSlantRectX( mRect );

			//flat areas on the side
			mRect.From.set( fx - mFrontH, mFrontPos.y-2*mFrontH, mFrontPos.z);
			mRect.FromW = mFrontH+1;
			mRect.To.set( bx - mBackH, mBackPos.y-2*mBackH, mBackPos.z);
			mRect.ToW = mBackH+1;
			mDrawTo.DrawSlantRectX( mRect );

			mRect.From.set( fx + mFrontWidth, mFrontPos.y-2*mFrontH, mFrontPos.z);
			mRect.FromW = mFrontH;
			mRect.To.set( bx + mBackWidth, mBackPos.y-2*mBackH, mBackPos.z);
			mRect.ToW = mBackH;
			mDrawTo.DrawSlantRectX( mRect );

			//sides
			mRect.From.set( fx, mFrontPos.y-2*mFrontH, mFrontPos.z);
			mRect.FromW = 2*mFrontH;
			mRect.To.set( bx, mBackPos.y-2*mBackH, mBackPos.z);
			mRect.ToW = 2*mBackH;
			mDrawTo.DrawSlantRectY( mRect );

			mRect.From.set( fx + mFrontWidth, mFrontPos.y-2*mFrontH, mFrontPos.z);
			mRect.FromW = 2*mFrontH+2;
			mRect.To.set( bx + mBackWidth, mBackPos.y-2*mBackH, mBackPos.z);
			mRect.ToW = 2*mBackH+2;
			mDrawTo.DrawSlantRectY( mRect );

			//ball
			double r = (BallY+1.0)/2.0;
			int brad = (int)(BallRad*0.5* (((double)mFrontWidth)*r + ((double)mBackWidth)*(1.0-r)));
			int by = (int)(((double)mFrontPos.y)*r + ((double)mBackPos.y)*(1.0-r));
			int bz = (int)(((double)mFrontPos.z)*r + ((double)mBackPos.z)*(1.0-r));
			int blx = (int)(( ((double)mFrontWidth)*r + ((double)mBackWidth*(1.0-r)) )*0.5*BallX );
			blx += mDrawTo.w/2;
			mDrawTo.DrawCircle(blx, by-brad, brad, bz+1);

			//front paddle
			pw = (int)(mFrontWidth*(FrontPaddleWidth/2.0));
			sx = (int)(mFrontPos.x + PaddleX*(mFrontWidth/2) - pw/2);
			mDrawTo.DrawRect( sx, mFrontPos.y-mFrontH*2, pw, mFrontH*2, mFrontPos.z);

		}
	};
	private PongGame mGame;
	
	public void init()
	{
		addMouseListener(this);
		addMouseMotionListener(this);

		mWidth  = getSize().width;
		mHeight = getSize().height;

		mSIRDPixels     = new ZDraw(mWidth, mHeight);		
		
		mSIRDImage = new MemoryImageSource(mWidth, mHeight, mSIRDPixels.data, 0, mWidth);
		mSIRDImage.setAnimated(true);
		mSIRDImage.setFullBufferUpdates(true);
		mBackBuffer = createImage(mSIRDImage);
				
		mZBuffer = new ZDraw(mWidth, mHeight);
		mZBuffer.Clear();
		
		mRandData = ZDraw.GetRandSIRDData();
		
		mDrawArea = new ZDraw(mZBuffer.data, mWidth-ZDraw.SIRDW, 
			mHeight, ZDraw.SIRDW, mZBuffer.stride );

		mGame = new PongGame( mDrawArea );
		mGame.DrawBoard();
	}

	public void start()
	{
		// Create update thread
		mUpdateThread = new Thread(this);
		// Start update thread
		mUpdateThread.start();
	}

	public void stop()
	{
		// Stop update thread
		mUpdateThread = null;
	}

	public void destroy()
	{
		// Stop update thread
		mUpdateThread = null;
	}

	public void updateSIRDImage()
	{
		mGame.DrawBoard();

		if (mUseSIRD)
			mZBuffer.DrawSIRD(mSIRDPixels.data, mRandData, mFrameNumber);
		else
			mZBuffer.DrawHeightMapTo(mSIRDPixels.data);

		mSIRDImage.newPixels();

		showStatus("Hits = " + mGame.mNumHits);
	}
	
	public void run()
	{
		repaint();
		
		// While update thread is current thread
		while (Thread.currentThread() == mUpdateThread)
		{
			mGame.Tick();
			updateSIRDImage();
			mFrameNumber++;

			//double dt = ((double)mGame.CurDTime);
			//dt = 1.0/dt;
			//showStatus("fps = " + dt);
			
			try
			{
				//long delay = mGame.CurDT
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	public void update(Graphics g)
	{
		paint(g);
	}

	public void paint(Graphics g)
	{
		g.drawImage(mBackBuffer, 0, 0, this);
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
			mGame.Clicked();
		}
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK)!=0)
		{
			mDrawArea.mIgnoreZ = !mDrawArea.mIgnoreZ;
		}
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK)!=0)
		{
			mUseSIRD = !mUseSIRD;
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		mGame.SetPaddleX( e.getX() - ZDraw.SIRDW );
	}

	public void mouseExited(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseClicked(MouseEvent e){}

	public String getAppletInfo()
	{
		return "jAbSIRD Demo - by Lewey Geselowitz - lewey@leweyg.com";
	}
}
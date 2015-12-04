import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.util.Map;

public class RendererSoftware implements SIRDSRenderer{
	private Component mParent;
	private SceneManager mScene;
	private Image mBackBuffer;
	private ZDraw mZBuffer, mColorBuffer;//, mZBuffer2;
	private IntArrayImage mSIRDPixels;
	private MemoryImageSource mSIRDImage;

	private boolean mInvert, mRandomFlicker;

	private final boolean USE_COLOR_BUFFER = false;
	int mDrawMode;

	int sis1[];
	int sis2[];
	int sisTemp[];

	private int mFrameNumber;

	public void init(Component parent, SceneManager sceneManager){
		mInvert=false;
		mRandomFlicker=true;
		mDrawMode=1;

		mScene=sceneManager;
		mParent=parent;

		mSIRDPixels = new IntArrayImage(mScene.width, mScene.height);

		mSIRDImage = new MemoryImageSource(mScene.width, mScene.height, mSIRDPixels.data, 0, mScene.width);
		mSIRDImage.setAnimated(true);
		mSIRDImage.setFullBufferUpdates(true);
		mBackBuffer = parent.createImage(mSIRDImage);

		mZBuffer = new ZDraw(mScene.width, mScene.height);
		mZBuffer.clear();
		if (USE_COLOR_BUFFER) {
			mColorBuffer = new ZDraw(mScene.width, mScene.height);
			mColorBuffer.clear();
		}

		mFrameNumber=0;
	}
	public int[] enlarge(int old[], int copies){
	 	int len = old.length;
		int[] result = new int[len*copies];
		for (int c=0;c<copies;c++)
			for (int i=0;i<len;i++)
				result[c*len + i] = old[i];
		return result;
	}
	public void setSISData(int f1[], int f2[], int w, int h){
		if (w!=ZDraw.SIRDW || h<ZDraw.SIRDdefH)
			throw new IllegalArgumentException("wrong sis data size");
		if (f1 == null) return;
		sis1=f1;
		sis2=(f2==null)?f1:f2;
		if (USE_COLOR_BUFFER) {
			if (h < mScene.height) {
				sis1 = enlarge(sis1, (mScene.height + h-1) / h);
				sis2 = enlarge(sis2, (mScene.height + h-1) / h);
			}
			sisTemp = new int[Math.max(sis1.length, sis2.length)];
		}
	}
	public void setRandomMode(boolean randomFlicker){
		mRandomFlicker=randomFlicker;
	}
	public void setInversion(boolean invert){
		mInvert=invert;
	}
	public void setDrawMode(int drawMode){
		mDrawMode=drawMode;
	}
	public int getDrawMode(){
		return mDrawMode;
	}

	public void renderFrame(){
		boolean drawSIRDS=(mDrawMode == 1) && sis1!=null && sis2!=null;
		mZBuffer.clear();
		if (USE_COLOR_BUFFER) {
			mColorBuffer.clear();
			mScene.drawTo(mZBuffer, mColorBuffer, -mScene.cameraX, -mScene.cameraY);
		} else
			mScene.drawTo(mZBuffer, -mScene.cameraX, -mScene.cameraY);


		if (!drawSIRDS){
			if (mDrawMode == 0) mZBuffer.drawHeightMapTo(mSIRDPixels.data, mInvert);
			else if (mDrawMode == 3) mZBuffer.drawRainbowMapTo(mSIRDPixels.data, mInvert);
			else mZBuffer.drawAnaglyph(mSIRDPixels.data, mInvert);
		} else if (USE_COLOR_BUFFER){
			if ((mFrameNumber &1)!=0) mZBuffer.DrawColoredSIRD(mSIRDPixels.data, mColorBuffer.data, sis2, sisTemp, mRandomFlicker, mInvert);
			else mZBuffer.DrawColoredSIRD(mSIRDPixels.data, mColorBuffer.data, sis1, sisTemp, mRandomFlicker, mInvert);
		} else {
			if ((mFrameNumber &1)!=0) mZBuffer.DrawSIRD(mSIRDPixels.data, sis2, mRandomFlicker, mInvert);
			else mZBuffer.DrawSIRD(mSIRDPixels.data, sis1, mRandomFlicker, mInvert);
		}
	/*
		if (mUseZBuffer2) {
			mZBuffer.forceCopyDataFrom(0,0,mZBuffer2,0,0,mZBuffer2.w,mZBuffer2.h);

		}*/
		if (drawSIRDS) mScene.floaters.draw(mSIRDPixels);
		else mScene.floaters.drawSimple(mSIRDPixels);

		mSIRDImage.newPixels();
		
		mFrameNumber++;
	}
	public void paint(Graphics g){
		g.drawImage(mBackBuffer, 0, 0, mParent);
	}
	public Image getBackBuffer(){
		return mBackBuffer;
	}
	public void finit(){
		
	}


	ZDraw getDirectZBufferAccess(){
		return mZBuffer;
	}
}

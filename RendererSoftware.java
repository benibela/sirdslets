import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.util.Map;

public class RendererSoftware implements SIRDSRenderer{
	private Component mParent;
	private SceneManager mScene;
	private Image mBackBuffer;
	private ZDraw mZBuffer;//, mZBuffer2;
	private IntArrayImage mSIRDPixels;
	private MemoryImageSource mSIRDImage;

	private boolean mInvert, mRandomFlicker, mDrawSIRDS;

	int sis1[];
	int sis2[];

	private int mFrameNumber;

	public void init(Component parent, SceneManager sceneManager){
		mInvert=false;
		mRandomFlicker=true;
		mDrawSIRDS=true;

		mScene=sceneManager;
		mParent=parent;

		mSIRDPixels = new IntArrayImage(mScene.width, mScene.height);

		mSIRDImage = new MemoryImageSource(mScene.width, mScene.height, mSIRDPixels.data, 0, mScene.width);
		mSIRDImage.setAnimated(true);
		mSIRDImage.setFullBufferUpdates(true);
		mBackBuffer = parent.createImage(mSIRDImage);

		mZBuffer = new ZDraw(mScene.width, mScene.height);
		mZBuffer.clear();

		mFrameNumber=0;
	}
	public void setSISData(int f1[], int f2[], int w, int h){
		if (w!=ZDraw.SIRDW || h!=ZDraw.SIRDH)
			throw new IllegalArgumentException("wrong sis data size");
		sis1=f1;
		sis2=(f2==null)?f1:f2;
	}
	public void setRandomMode(boolean randomFlicker){
		mRandomFlicker=randomFlicker;
	}
	public void setInversion(boolean invert){
		mInvert=invert;
	}
	public void setDrawSIRDS(boolean drawSIRDS){
		mDrawSIRDS=drawSIRDS;
	}
	public boolean getDrawSIRDS(){
		return mDrawSIRDS;
	}


	private void renderPrimitive(ScenePrimitive p){
		p.drawTo(mZBuffer, -mScene.cameraX, -mScene.cameraY);
	}
	public void renderFrame(){
		boolean drawSIRDS=mDrawSIRDS && sis1!=null && sis2!=null;
		mZBuffer.clear();

		for (ScenePrimitive prim: mScene.primitives)
			renderPrimitive(prim);
		for (Map.Entry<String,ScenePrimitive> sprite: mScene.namedPrimitives.entrySet())
			renderPrimitive(sprite.getValue()); 

		if (!drawSIRDS){
			//heightmap mode
			mZBuffer.drawHeightMapTo(mSIRDPixels.data, mInvert);
		} else {
			if ((mFrameNumber &1)!=0) mZBuffer.DrawSIRD(mSIRDPixels.data, sis2, mRandomFlicker, mInvert);
			else mZBuffer.DrawSIRD(mSIRDPixels.data, sis1, mRandomFlicker, mInvert);
		}
	/*
		if (mUseZBuffer2) {
			mZBuffer.forceCopyDataFrom(0,0,mZBuffer2,0,0,mZBuffer2.w,mZBuffer2.h);

		}*/

		for (Floater f: mScene.floaters){
			if (drawSIRDS) f.draw(mSIRDPixels);
			else f.drawSimple(mSIRDPixels);
		}

		for (Map.Entry<String,Floater> it : mScene.namedFloaters.entrySet()){
			if (drawSIRDS) it.getValue().draw(mSIRDPixels);
			else it.getValue().drawSimple(mSIRDPixels);
		}

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

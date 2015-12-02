import java.applet.AudioClip;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;


public class SIRDSxkcd implements SIRDSlet	{
	public int KEY_SHIP_ACC_LEFT=KeyEvent.VK_LEFT;
	public int KEY_SHIP_ACC_RIGHT=KeyEvent.VK_RIGHT;
	public int KEY_SHIP_ACC_UP=KeyEvent.VK_UP;
	public int KEY_SHIP_ACC_DOWN=KeyEvent.VK_DOWN;
	/*public int KEY_SHIP_ACC_ASCEND=KeyEvent.VK_SHIFT;
	public int KEY_SHIP_ACC_DESCEND=KeyEvent.VK_CONTROL;
	public int KEY_SHIP_ACC_ASCEND2=KeyEvent.VK_A;
	public int KEY_SHIP_ACC_DESCEND2=KeyEvent.VK_S;
	public int KEY_SHIP_ACC_ASCEND3=KeyEvent.VK_X;
	public int KEY_SHIP_ACC_DESCEND3=KeyEvent.VK_Y;
	public int KEY_SHIP_ACC_DESCEND4=KeyEvent.VK_Z;
	*/

	public Vector3d levelPos;

	//technical things
	protected SIRDSAppletManager mManager;
	protected SceneManager mScene;
	protected int mZBufferYStart;
	protected final int mZBufferH=500;
	
	final static int expectedTimePerFrame = 40;
	
	protected ZSprite mHoverBoard, mEmptyTile;
    protected Vector3d mHoverBoardV, mHoverBoardP;
    private long mCurTime;
    private TileRetriever mTileRetriever;



    public void start(Object manager, int option){
		mManager=(SIRDSAppletManager)manager;
		mScene=mManager.getSceneManager();

		mZBufferYStart=(mScene.height-mZBufferH)/2;

        mHoverBoardV = new Vector3d();
        mHoverBoardP = new Vector3d(512187 - 200, -549668, 0);
        //mHoverBoardP = new Vector3d(511701, -550323, 0);

        mEmptyTile = makeEmptyTile();

        mTileRetriever = new TileRetriever(512, "http://xkcd.com/1608/");

        //System.out.println(baseURL + key.i+":"+key.j+"+s.png");
        //BufferedImage img = SceneManager.loadImage(new URL( "http://xkcd.com/1608/all-four.png"));
        mHoverBoard = mScene.createZSprite("xkcd/allfour.png");
                //new ZSprite(img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()), img.getWidth(),img.getHeight());
        mHoverBoard.w = mHoverBoard.w / 4;
        mHoverBoard.z = ZDraw.MAXZ/2;
        mScene.setPrimitive("board", mHoverBoard);
            //    readHeightMap(sprite, img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()));

        readyToGo = false;
        readyToJump = true;
    }

             /*
		mBaseShip=mScene.createZSprite("flighter/ship.png");
		mInitialLife=0;
		for (int y=0;y<mBaseShip.h;y++){
			int b=mBaseShip.getLineIndex(y);
			for (int x=0;x<mBaseShip.w;x++)
				if (mBaseShip.dataVisible[b+x]) mInitialLife+=1;
		}
		
		mRandom = new Random();

		setDifficulty(option);
		mClockSymbols = new ArrayList<Floater>(mTimeWarpPerLevel);
		for (int i=0;i<mTimeWarpPerLevel;i++){
			Floater f = mScene.createFloater("flighter/clock.png");
			f.x=7+25*i;
			f.y=20;
			f.z=ZDraw.MAXZ/2;
			mClockSymbols.add(f);
		}                                           */

	protected void init(){
		mManager.suspendRendering();
		mScene.clear();
		mScene.removeFloater("border1");
		mScene.removeFloater("border2");
		Floater border = new Floater(mScene.width,2);
		for (int i=0;i<border.data.length;i++) border.data[i] = 0xff888888;
		border.y = mZBufferYStart;
		mScene.setFloater("border1",border);
		border = border.fastClone();
		border.y = mZBufferYStart + mZBufferH;
		mScene.setFloater("border2", border);

		//mLevelCuboids.add(new Cuboid(400,530,20,150,5,15));
		//mLevelCuboids.add(new HoledCuboid(500,670,200,400,0,20, 250, 350, 5, 15));
		mManager.resumeRendering();
		
	}
		
	public void stop(){
        mTileRetriever.shutdown();
	}

    long mLastJump = 0;
    boolean readyToGo, readyToJump;

    public void calculateFrame(long time){
		long timeDelta = time - mCurTime;
		mCurTime=time;

        int primitiveId = 0;
        int focusX = mScene.cameraX - mScene.cameraX % mTileRetriever.tileSize, focusY = mScene.cameraY - mScene.cameraY % mTileRetriever.tileSize;
        for (int deltaX = 0; deltaX <= 2; deltaX++)
            for (int deltaY = -1; deltaY <= 1; deltaY++) {
                ZSprite tile = mTileRetriever.getTile( (focusX+512*deltaX), (focusY + mTileRetriever.tileSize*deltaY));
                tile.x = focusX + mTileRetriever.tileSize * deltaX;
                tile.y = focusY + mTileRetriever.tileSize * deltaY;
                mScene.setPrimitive(primitiveId, tile);
                primitiveId++;
            }
        for (int deltaX = -2; deltaX <= 2; deltaX++)
            for (int deltaY = -2; deltaY <= 2; deltaY++)
                mTileRetriever.prefetch( (mScene.cameraX+512*deltaX), (mScene.cameraY+ 512*deltaY));

		//---------------------keyinput------------------------
        //approximated jump heights: up 190, collision 50
		//physics main loop
		if (timeDelta > 200) timeDelta = 200;
        float tInSec = timeDelta / 1000.0f;

        Vector3d a=new Vector3d();
        a.x-=mManager.isKeyPressed(KEY_SHIP_ACC_LEFT) ?1000:0;
        a.x+=mManager.isKeyPressed(KEY_SHIP_ACC_RIGHT)?1000:0;
        readyToGo |= a.x != 0 && primitiveId > 1;
        if (readyToGo)
            a.y=1000; //gravity, only activated after first movement (or it falls in the ground, before the first tile is loaded)
        if (time - mLastJump > 200 && readyToJump) {
            if (mManager.isKeyPressed(KEY_SHIP_ACC_UP)) a.y -= 50000;
            mLastJump = time;
            readyToJump = false;
        }
        readyToJump |= !mManager.isKeyPressed(KEY_SHIP_ACC_UP);

        boolean collisions[] = new boolean[3];
        if (!moveBoardIfPossible(primitiveId, a, mHoverBoardV, tInSec, collisions)) {
            if (!moveBoardIfPossible(primitiveId, a.projectOnX(), mHoverBoardV.projectOnX(), tInSec, collisions)) {
                if (!moveBoardIfPossible(primitiveId, a.projectOnY(), mHoverBoardV.projectOnY(), tInSec, collisions)) {
                    mHoverBoardV = new Vector3d();

                    if (a.x != 0) {
                        a.y -= 15000;
                        if (!moveBoardIfPossible(primitiveId, a, mHoverBoardV, tInSec, collisions)) {

                        }
                    }

                }
            }

        }
        //checkCollisions(primitiveId, collisions);

        System.out.println(a + " " + mHoverBoardV + " "+tInSec);


        // (int)Math.round(mHoverBoardP.z);
		/*mShip.x=(int)Math.round(mShipData.p.x-mShip.w/2);
		mShip.y=(int)Math.round(mShipData.p.y-mShip.h/2);
		mShip.z=(int)Math.round(mShipData.p.z);*/

        mScene.cameraX = mHoverBoard.x - mScene.width / 2;
        mScene.cameraY = mHoverBoard.y - mScene.height / 2;



		/*boolean coll=false;
		for (ScenePrimitive sp: mLevelPrimitives)
			if (sp instanceof Cuboid)
				coll|=((Cuboid)sp).intersect(mShip,0,0,true);
			else if (sp instanceof ZSprite)
				coll|=mShip.intersect((ZSprite)sp,0,0,true);
			//else if (sp instanceof ZSpriteRepeater)
			//	coll|=((ZSpriteRepeater)sp).intersectReversed(mShip,0,0,true);
		int life = mShipData.life;
		if (coll) {
			if (mCurTime - mLastCollisionSound > 100) {
				mSoundCollision[(int)(Math.random()*mSoundCollision.length)].play();
				mLastCollisionSound = mCurTime;
			}
			updateLifeDamaging();
			if (mShipData.life<mMinimalRequiredLife) return; //game end
		}
		updateLifeHealing();
		if (life != mShipData.life)
			updateLifeProgressBar();

		if (mShip.x > mLevelLength + 100 + mLevelEnd.w) startLevel(mLevel+1);
          */

		//------------------scroll level--------------------
		/*if (!mInitialWait)
			mShipData.levelScroll-=2 * timeDelta / 20.0;
		if (mShipData.p.x> -mShipData.levelScroll+mScene.width - mShip.w/2 - 100) 
			mShipData.levelScroll -= 1 * timeDelta / 20.0;
		if (mShipData.p.x> -mShipData.levelScroll+mScene.width - mShip.w/2 - 60) 
			mShipData.levelScroll -= 2 * timeDelta / 20.0;
		if (mShip.x > mLevelLength + 50) {
			mShipData.levelScroll-=10 * timeDelta / 20.0;
			mShipData.p.x+=10 * timeDelta / 20.0;
		}
		mScene.setCameraPosition((int)(-mShipData.levelScroll), -(mScene.height-mZBufferH)/2, 0);*/

	}

    public void checkCollisions(int tileCount, boolean topBottomOtherOut[]){
        topBottomOtherOut[0] = false;
        topBottomOtherOut[1] = false;
        topBottomOtherOut[2] = false;
        int boundary[] = new int[4];
        for (int i=0;i<tileCount;i++) {
            ZSprite tile = (ZSprite) mScene.getPrimitive(i);
            if (tile.intersectBoundaries2D(mHoverBoard, boundary)) {
                if (IntersectionUtils.boundary2DCutThresholded(3*ZDraw.MAXZ/4-1, boundary, tile))    {
                    IntersectionUtils.boundary2DShift(boundary, tile, mHoverBoard);
                    if (boundary[1] < mHoverBoard.h / 4) topBottomOtherOut[0] = true;
                    else if (boundary[1] > 3 * mHoverBoard.h / 4) topBottomOtherOut[1] = true;
                    else topBottomOtherOut[2] = true;
                }
            }
        }
    }

    public boolean moveBoardIfPossible(int tileCount, Vector3d a, Vector3d vold, double tInSec, boolean[] collisions) {
        int oldX = mHoverBoard.x, oldY = mHoverBoard.y;
        Vector3d dragA = vold.clone().multiply(vold.length()*0.004);
        Vector3d areal = a.clone().sub(dragA).multiply(tInSec);
        Vector3d v = vold.clone().add(areal);

        int maxV = 500;
        if (v.x < -maxV) v.x = -maxV;
        if (v.x > maxV) v.x = maxV;
        if (v.y < -2000) v.y = -2000;
        if (v.y > maxV) v.y = maxV;



        Vector3d newPos = mHoverBoardP.clone().add(v.clone().multiply(tInSec));
        mHoverBoard.x = (int)Math.round(newPos.x);
        mHoverBoard.y = (int)Math.round(newPos.y);

        checkCollisions(tileCount, collisions);

        if (collisions[0] || collisions[1] || collisions[2]) {
            mHoverBoard.x = oldX;
            mHoverBoard.y = oldY;
            return false;
        } else {
            mHoverBoardP = newPos;
            mHoverBoardV = v;
            return true;
        }

    }


    //World



    void readHeightMap(ZSprite sprite, int[] from){
        int w = sprite.w; int h = sprite.h; int[] data = sprite.data;
        int MAXZ = ZSprite.MAXZ;
        int b;
        for (int y=0; y<h; y++)
        {
            b = sprite.getLineIndex(y);
            for (int x=0; x<w; x++)
            {
                int red = ((from[b+x]&0xFF0000)>>>16);
                data[b+x]=3 * MAXZ / 4 - (((from[b+x]&0xFF)+((from[b+x]&0xFF00)>>>8)+red)*MAXZ/6)/255;
                if ((red & 1) != 0) data[b+x] -= MAXZ/5; //passable
                if (data[b+x]<MAXZ / 4) data[b+x]=MAXZ / 4;
                else if (data[b+x]>3*MAXZ/4) data[b+x]=3*MAXZ/4;
            }
        }
    }

    ZSprite makeEmptyTile(){
        ZSprite EmptyTile = new ZSprite();
        EmptyTile.transparent = true;
        EmptyTile.setSize(1,1);
        EmptyTile.dataVisible[0] = false;
        return EmptyTile;
    }

    public class TileRetriever{
        int tileSize; String baseURL;
        private ExecutorService pool;
        TileRetriever(int tileSize, String baseURL) {
            this.tileSize = tileSize;
            this.baseURL = baseURL;
            pool = Executors.newFixedThreadPool(8);
        }

        protected final HashMap<Key, ZSprite> mTileCache =  new HashMap<Key, ZSprite>();
        protected ArrayDeque<Key> mTileMRU = new ArrayDeque<Key>();

        private class Key{
            int i,j;

            Key (int x, int y) {
                i = x / tileSize;
                j = y / tileSize;
            }
            @Override
            public int hashCode() {
                return ( (i + (i >> 16)) << 16) + (j + (j >> 16));
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Key)) return false;
                return ((Key)o).i == i && ((Key)o).j == j;
            }
        }
        ZSprite getTile(int x, int y) {
            final Key key = new Key(x,y);
            synchronized (mTileCache) {
                if (mTileCache.containsKey(key)) return mTileCache.get(key);
                else {
                    mTileCache.put(key, mEmptyTile);
                    pool.execute(new Runnable() {
                        @Override
                        public void run() {
                            ZSprite sprite;

                            try {
                                //System.out.println(baseURL + key.i+":"+key.j+"+s.png");
                                BufferedImage img = SceneManager.loadImage(new URL(baseURL + key.i+":"+key.j+"+s.png"));
                                if (img == null) sprite = mEmptyTile;
                                else {
                                    //System.out.println(baseURL+key.i+":"+key.j+"+s.png---"+img.getWidth());
                                    sprite=new ZSprite(img.getWidth(),img.getHeight());

                                    readHeightMap(sprite, img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()));

									//need to blur it, or there are hair thin non-passable lines around passable areas
									float factors[] = {0.25f, 0.25f};
									sprite.transformLinearHorizontal(0xffff, factors, 0.5f, factors);
									sprite.transformLinearVertical(0xffff, factors, 0.5f, factors);
                                }
                            } catch (MalformedURLException e) {
                                sprite = mEmptyTile;
                            }

                            synchronized (mTileCache) {
                                mTileCache.put(key, sprite);
                            }

                            synchronized (mTileMRU) {
                                mTileMRU.remove(key);
                                mTileMRU.addFirst(key);
                                while (mTileMRU.size() > 255) {
                                    synchronized (mTileCache) {
                                        mTileCache.remove(mTileMRU.getLast());
                                    }
                                    mTileMRU.removeLast();
                                }
                            }
                        }
                    });
                    return mEmptyTile;
                }
            }
        }
        void prefetch(int x, int y) {
            getTile(x,y);
        }
        void shutdown(){
            pool.shutdown();
        }

    }

	public String getName(){
		return Translations.getSIRDSxkcd().name();
	}
	public String getDescription(){
		return Translations.getSIRDSxkcd().desc();
	}
	public String getKeys(){
		return Translations.getInstance().SIRDSFlighterKeys();
	}
	public String[] getPossibleOptions(){
		return new String[]{};//Translations.getInstance().SIRDSFlighterVeryEasy(),Translations.getInstance().SIRDSFlighterEasy(),Translations.getInstance().SIRDSFlighterNormal(),Translations.getInstance().SIRDSFlighterHard(),Translations.getInstance().SIRDSFlighterImpossible()};
	}
	public int getDefaultOption(){
		return -1;
	}
	
}
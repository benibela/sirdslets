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
        mHoverBoardP = new Vector3d(512187, -549668, 0);
        mHoverBoardP = new Vector3d(511701, -550323, 0);

        mEmptyTile = makeEmptyTile();

        mTileRetriever = new TileRetriever(512, "http://xkcd.com/1608/");

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

    public void calculateFrame(long time){
		long timeDelta = time - mCurTime;
		mCurTime=time;
		//---------------------keyinput------------------------
		//physics main loop
		if (timeDelta > 200) timeDelta = 200;
		for (int repetition=0;repetition<timeDelta; repetition++){
			final double acceleration=1.3;
			final double accelerationz=0.2*acceleration/1.5;
			Vector3d a=new Vector3d();
			a.x-=mManager.isKeyPressed(KEY_SHIP_ACC_LEFT)?acceleration:0;
			a.x+=mManager.isKeyPressed(KEY_SHIP_ACC_RIGHT)?acceleration:0;
			a.y-=mManager.isKeyPressed(KEY_SHIP_ACC_UP)?acceleration:0;
			a.y+=mManager.isKeyPressed(KEY_SHIP_ACC_DOWN)?acceleration:0;
			//mShipA.z-=(mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND)||mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND2)||mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND3)||mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND4))?accelerationz:0;
			//mShipA.z+=(mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND)||mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND2)||mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND3))?accelerationz:0;

			a.multiply(0.04);
			
			//-----------------move user ship----------------------

			mHoverBoardV.add(a);


            mHoverBoardV.multiply(0.997);
            mHoverBoardV.z = mHoverBoardV.z * 0.997;
			if (a.z == 0) mHoverBoardV.z = 0;

            mHoverBoardP.add(mHoverBoardV.clone().multiply(0.04));/*
			if (mShipData.p.y+mShip.h/2>mZBufferH) {
				mShipData.p.y=mZBufferH-mShip.h/2;
				if (mShipData.v.y>-0.01) mShipData.v.y=-0.01;
			}
			if (mShipData.p.z>MAXFLYZ) {
				mShipData.p.z=MAXFLYZ;
				if (mShipData.v.z>-0.01) mShipData.v.z=-0.01;
			}
			if (mShipData.p.x< -mShipData.levelScroll+ZDraw.SIRDW+mShip.w/2) { //there are maxz unreachable pixel at the left screen side
				mShipData.p.x=-mShipData.levelScroll+ZDraw.SIRDW+mShip.w/2;
				if (mShipData.v.x<0.01) mShipData.v.x=0.01;
			}
			if (mShipData.p.x> -mShipData.levelScroll+mScene.width - mShip.w/2 - 28) {
				mShipData.levelScroll = (int)(mScene.width - mShipData.p.x - mShip.w/2 - 28);
			} 
			if (mShipData.p.y<mShip.h/2) {
				mShipData.p.y=mShip.h/2;
				if (mShipData.v.y<0.01) mShipData.v.y=0.01;
			}
			if (mShipData.p.z<2) {
				mShipData.p.z=2;
				if (mShipData.v.z<0.01) mShipData.v.z=0.01;
			}                                              */
		}
        int x = (int)Math.round(mHoverBoardP.x);
        int y = (int)Math.round(mHoverBoardP.y);
		/*mShip.x=(int)Math.round(mShipData.p.x-mShip.w/2);
		mShip.y=(int)Math.round(mShipData.p.y-mShip.h/2);
		mShip.z=(int)Math.round(mShipData.p.z);*/

        mScene.cameraX = x + mScene.width / 2;
        mScene.cameraY = y + mScene.height / 2;


		//--------------calculate collisions ship/geometry-------------
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
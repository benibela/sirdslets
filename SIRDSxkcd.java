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
	private ZSprite mBaseCoin;
	private SceneObjectGroup mTiles, mCoins;
	private float mCoinZ, mCoinZDir;
	private float COIN_MIN_Z, COIN_MAX_Z, COIN_SPEED = 3;



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
		mBaseCoin = mScene.createZSprite("xkcd/coin.png");
        mHoverBoard = mScene.createZSprite("xkcd/allfour.png");

		COIN_MAX_Z = ZDraw.MAXZ - mBaseCoin.maxZ();
		COIN_MIN_Z = ZDraw.MAXZ / 4 - mBaseCoin.minZ();

				System.out.println(mBaseCoin.minZ() + " " + mBaseCoin.maxZ());

		mCoinZ = (COIN_MIN_Z+COIN_MAX_Z)/2;
		mCoinZDir = 1;

		mTiles = new SceneObjectGroup();
		mCoins = new SceneObjectGroup();
		for (int i=0;i<coins.length;i+=2){
			ZSprite coin = mBaseCoin.fastClone();
			coin.moveTo(coins[i], coins[i+1], (int) mCoinZ);
			mCoins.setPrimitive(i/2, coin);
		}


		mManager.suspendRendering();
		mScene.clear();
		mScene.setPrimitive(0, mTiles);
		mScene.setPrimitive("board", mHoverBoard);
		mScene.setPrimitive("coins", mCoins);
		mManager.resumeRendering();
                //new ZSprite(img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()), img.getWidth(),img.getHeight());
        mHoverBoard.w = mHoverBoard.w / 4;
        mHoverBoard.z = ZDraw.MAXZ/2;
            //    readHeightMap(sprite, img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()));


        readyToGo = false;
        readyToJump = true;
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
                mTiles.setPrimitive(primitiveId, tile);
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

		if (mCoinZ < COIN_MIN_Z && mCoinZDir < 0 ) mCoinZDir = 1;
		else if (mCoinZ > COIN_MAX_Z && mCoinZDir > 0 ) mCoinZDir = -1;
		mCoinZ += tInSec * mCoinZDir * COIN_SPEED;

		for (ScenePrimitive prim: mCoins.primitives)
			((ZSprite)prim).z = (int) mCoinZ;

        //checkCollisions(primitiveId, collisions);

        //System.out.println(a + " " + mHoverBoardV + " "+tInSec);


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
            ZSprite tile = (ZSprite) mTiles.getPrimitive(i);
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

	//from http://1101b.com/xkcd1608/
	private int[] coins = { 537027, -560249,     525689, -560616,     526077, -560616,     526164, -559291,     531494, -559530,     531903, -559554,     532170, -560864,     555130, -562404,     503224, -552394,     503522, -551049,     542481, -560037,     542706, -559642,     550773, -560041,     553076, -567112,     553103, -567151,     553132, -567183,     553173, -565356,     553208, -567184,     553288, -567149,     553329, -567108,     554243, -565849,     554243, -565780,     557912, -558137,     526693, -560208,     526995, -559135,     527086, -559782,     528028, -561056,     528198, -560225,     527061, -557254,     483600, -551975,     483667, -551977,     553345, -563807,     519636, -549096,     522049, -553195,     490124, -554986,     552820, -560057,     520607, -549047,     521392, -549021,     538150, -550895,     560567, -549975,     528618, -549429,     553737, -550104,     529664, -558476,     556369, -556826,     523458, -549103,     548206, -561787,     548271, -561787,     542941, -562344,     475207, -553683,     475257, -553684,     475313, -553684,     475367, -553680,     518169, -560337,     519089, -561130,     519929, -559544,     525306, -561659,     535582, -561307,     535875, -562506,     535900, -563088,     534153, -559619,     508250, -567578,     551586, -563946,     552593, -563797,     486608, -554809,     516591, -560321,     517846, -559181,     517968, -559859,     518041, -561062,     548482, -549800,     546744, -559592,     567063, -550422,     567065, -550503,     567086, -550464,     567086, -550540,     567111, -550503,     567111, -550422,     567130, -550540,     567134, -550464,     567152, -550503,     567154, -550422,     539260, -562964,     540037, -562347,     540302, -562977,     540380, -562347,     558132, -563858,     507154, -568861,     541755, -563024,     542595, -562588,     542595, -562506,     518682, -551658,     519441, -552229,     530791, -558938,     531482, -558698,     479534, -554932,     501990, -549107,     481857, -554526,     482009, -554496,     539605, -558914,     542768, -564500,     543822, -563314,     544521, -564725,     544620, -563890,     512082, -549750,     512093, -549901,     512099, -550062,     512207, -549881,     512209, -550222,     512217, -549740,     512223, -549647,     512258, -549740,     512272, -550041,     512296, -549739,     512323, -549883,     512348, -550214,     512349, -549642,     512385, -549886,     512396, -549601,     512414, -550061,     512417, -549788,     549232, -565175,     549831, -563260,     550487, -564557,     477754, -554489,     522741, -551458,     497597, -551742,     525583, -549247,     547911, -560431,     492147, -553606,     556845, -567091,     523816, -560628,     524078, -560628,     524079, -560382,     552105, -568488,     527039, -549219,     528075, -549711,     537789, -558007,     539524, -559910,     540329, -560393,     483549, -549417,     485094, -549147,     551295, -565285,     551501, -566387,     551546, -566765,     552221, -566176,     523748, -561433,     523778, -561435,     523810, -561437,     547918, -555114,     548026, -556862,     541735, -559053,     541844, -557595,     542018, -558003,     542611, -557542,     541203, -563852,     541641, -563852,     542076, -563852,     545090, -561997,     545524, -562339,     546312, -562840,     546312, -562732,     546600, -562845,     546607, -562732,     513108, -560669,     513738, -559677,     549508, -558811,     549843, -558016,     550152, -558016,     528673, -559748,     529300, -560287 };

	public String getName(){
		return Translations.getSIRDSxkcd().name();
	}
	public String getDescription(){
		return Translations.getSIRDSxkcd().desc();
	}
	public String getKeys(){
		return Translations.getSIRDSxkcd().keys();
	}
	public String[] getPossibleOptions(){
		return new String[]{};//Translations.getInstance().SIRDSFlighterVeryEasy(),Translations.getInstance().SIRDSFlighterEasy(),Translations.getInstance().SIRDSFlighterNormal(),Translations.getInstance().SIRDSFlighterHard(),Translations.getInstance().SIRDSFlighterImpossible()};
	}
	public int getDefaultOption(){
		return -1;
	}
	
}
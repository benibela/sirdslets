import java.applet.AudioClip;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SIRDSFlighter implements SIRDSlet	{
	public int KEY_SHIP_ACC_LEFT=KeyEvent.VK_LEFT;
	public int KEY_SHIP_ACC_RIGHT=KeyEvent.VK_RIGHT;
	public int KEY_SHIP_ACC_UP=KeyEvent.VK_UP;
	public int KEY_SHIP_ACC_DOWN=KeyEvent.VK_DOWN;
	public int KEY_SHIP_ACC_ASCEND=KeyEvent.VK_SHIFT;
	public int KEY_SHIP_ACC_DESCEND=KeyEvent.VK_CONTROL;
	public int KEY_SHIP_ACC_ASCEND2=KeyEvent.VK_A;
	public int KEY_SHIP_ACC_DESCEND2=KeyEvent.VK_S;
	public int KEY_SHIP_ACC_ASCEND3=KeyEvent.VK_X;
	public int KEY_SHIP_ACC_DESCEND3=KeyEvent.VK_Y;
	public int KEY_SHIP_ACC_DESCEND4=KeyEvent.VK_Z;
	public int KEY_SHIP_FIRE=KeyEvent.VK_SPACE;

	private final static int DIFF_VERY_EASY = 0;
	private final static int DIFF_EASY = 1;
	private final static int DIFF_NORMAL = 2;
	private final static int DIFF_HARD = 3;
	private final static int DIFF_IMPOSSIBLE = 4;

	protected static class ShipInformation{
		public Vector3d v, p;
		public int life;
		public boolean[] damage;
		public int levelScroll;
		public void assign(ShipInformation shi){
			if (v != null) v.assign(shi.v);
			else v = new Vector3d(shi.v);
			if (p != null) p.assign(shi.p);
			else p = new Vector3d(shi.p);
			life = shi.life;
			if (damage != null && damage.length == shi.damage.length ) {
				for (int i=0;i<damage.length;i++)
					damage[i]=shi.damage[i];
			} else damage = shi.damage.clone();
			levelScroll = shi.levelScroll;
		}
	}

	//technical things
	protected SIRDSAppletManager mManager;
	protected SceneManager mScene;
	protected int mZBufferYStart;
	protected final int mZBufferH=500;
	protected boolean mInitialWait;
	protected Random mRandom;
	//Ship
	protected ZSprite mShip, mBaseShip;
	protected ShipInformation mShipData;
	protected ArrayList<ShipInformation> mShipHistory;
	protected int mShipHistoryPos;
	private final int MAXFLYZ=19;//ZDraw.MAXZ-z-Shipheight
	private int mInitialLife,  mMinimalRequiredLife; 
	private int mHealingSpeed, mTimeWarpPerLevel, mRemainingTimeWarps;
	private int mShipHistoryTravelBackPos;
	private long mTimeWarpLastClockFlicker = 0;
	private boolean mTimeWarpActive;
	private static final int mTimeWarpFrameCount = 25*3;
	protected ArrayList<Cuboid> mShoots;
	protected long mCurTime,mLastShoot = 0, mLastDied=0;
	protected int mShootTimeout, mShootCount;
	//World
	protected final static int firstLevel = 0;
	protected int mDifficulty = DIFF_NORMAL;
	public int mLastLevel = 9; //last existing level (don't forget to recompile!)
	protected ZSprite mLevelEnd;
	protected int mLevel, mLevelLength;
	protected ArrayList<ScenePrimitive> mLevelPrimitives;
	protected ArrayList<ArrayList<PrimitiveModifier>> mLevelModifier;
	protected HashMap<String, ZSprite> mImageCache = new HashMap<String, ZSprite>();
	private ArrayList<PrimitiveModifier> mSpecialModifier;
	protected ArrayList<Floater> mClockSymbols;


	private AudioClip mSoundFire;
	private AudioClip mSoundCollision[] = new AudioClip[2];
	private AudioClip mSoundSmallExplosion[] = new AudioClip[2];
	private AudioClip mSoundLargeExplosion;


	public void start(Object manager, int option){
		mManager=(SIRDSAppletManager)manager;
		mScene=mManager.getSceneManager();

		mZBufferYStart=(mScene.height-mZBufferH)/2;

		mShootTimeout = 450;
		mShootCount = 0;

		mInitialWait = true;

		mSoundFire = mManager.getAudioClip("flighter/lasersword_iespana.wav"); //all free sounds \/
		mSoundCollision[0] = mManager.getAudioClip("flighter/collision0_pacdv.wav");
		mSoundCollision[1] = mManager.getAudioClip("flighter/collision1_pacdv.wav");
		mSoundSmallExplosion[0] = mManager.getAudioClip("flighter/smallexplosion0.wav"); //public domain even
		mSoundSmallExplosion[1] = mManager.getAudioClip("flighter/smallexplosion1.wav"); //"
		mSoundLargeExplosion = mManager.getAudioClip("flighter/vehicleexplosion_diode111.wav");

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
		}

		startLevel(option >= DIFF_HARD?firstLevel+1:firstLevel); //skip first level (training) with hard
	}
	protected ScenePrimitive addSerializedObject(Map<String, Object> m){
		String type=(String)m.get("type");
		ScenePrimitive sp=null;
		if ("Cuboid".equals(type)) sp=new Cuboid();
		else if ("ZSprite".equals(type)) {
			String imageName=(String)m.get("image");
			if (!mImageCache.containsKey(imageName))
				mImageCache.put(imageName, mScene.createZSprite("flighter/"+imageName));
			sp=((ZSprite)mImageCache.get(imageName)).fastClone();
		} else if ("ZSpriteRepeater".equals(type)) {
			String imageName=(String)m.get("image");
			if (!mImageCache.containsKey(imageName))
				mImageCache.put(imageName, mScene.createZSprite("flighter/"+imageName));
			sp=new ZSpriteRepeater(((ZSprite)mImageCache.get(imageName)).fastClone());
		} else throw new IllegalArgumentException("invalid level object");
		sp.jsonDeserialize(m);
		mLevelPrimitives.add(sp);
		mScene.addPrimitive(sp);
		ArrayList<PrimitiveModifier> apm=null;
		if (m.containsKey("modifier")) {
			apm=new ArrayList<PrimitiveModifier>();
			ArrayList<Object> pmser=(ArrayList<Object>)m.get("modifier");
			for (Object p: pmser){
				Map<String, Object> n = (Map<String, Object>)p;
				String ntype = (String)n.get("type");
				PrimitiveModifier mod = mScene.deserializePrimitiveModifier(n, sp) ;
				if (mod instanceof PrimitiveMarker) mSpecialModifier.add(mod);
				mScene.addPrimitiveModifier(mod);
				apm.add(mod);
			}
		}
		mLevelModifier.add(apm);
		return sp;
	}

	protected void setDifficulty(int diff){
		mDifficulty = diff;
		switch (diff){
			case DIFF_VERY_EASY:
				mMinimalRequiredLife = mInitialLife * 30 / 100;
				mHealingSpeed = 10;
				mTimeWarpPerLevel = 2;
				break;
			case DIFF_EASY:
				mMinimalRequiredLife = mInitialLife * 40 / 100;
				mHealingSpeed = 5;
				mTimeWarpPerLevel = 1;
				break;
			case DIFF_NORMAL:
				mMinimalRequiredLife = mInitialLife * 50 / 100;
				mHealingSpeed = 1;
				mTimeWarpPerLevel = 0;
				break;
			case DIFF_HARD:
				mMinimalRequiredLife = mInitialLife * 60 / 100;
				mHealingSpeed = 0;
				mTimeWarpPerLevel = 0;
				break;
			case DIFF_IMPOSSIBLE:
				mMinimalRequiredLife = mInitialLife * 95 / 100;
				mHealingSpeed = 0;
				mTimeWarpPerLevel = 0;
				break;
		}
	}

	protected void startLevel(int level){
		if (level > mLastLevel) return;
		setDifficulty(mDifficulty);
		if (level == 6 && mDifficulty>=DIFF_NORMAL) mMinimalRequiredLife = mInitialLife*50/100; //UGLY hack: level 6 is not impossible to play if the game is too strict
		mManager.suspendRendering();
		mScene.clear();
		mScene.removeFloater("zerror");
		for (int i=0;i<mTimeWarpPerLevel;i++) {
			mScene.setFloater("clock"+i, mClockSymbols.get(i));
			mClockSymbols.get(i).visible = true;
		}
		//reset ship position
		mShip=mBaseShip.fastClone();
		mShip.dataVisible = mShip.dataVisible.clone();
		mShip=mScene.setZSprite("ship",mScene.createZSprite("flighter/ship.png"));
		mShipData = new ShipInformation();
		mShipData.life=mInitialLife;
		mShipData.damage = mShip.dataVisible;
		mShipHistory = new ArrayList<ShipInformation>();
		mShipHistoryPos = -1;
		mTimeWarpActive = false;
		
		updateLifeProgressBar();
		mRemainingTimeWarps = mTimeWarpPerLevel;
	
		mShip.x=300-mShip.w/2;
		mShip.y=mZBufferH/2-mShip.h/2;
		mShip.z=10;

		mShipData.v=new Vector3d();
		mShipData.p=new Vector3d(mShip.x+mShip.w/2,mShip.y+mShip.h/2,mShip.z);
		
		
		try{
			JSONReader json = new JSONReader();

			BufferedReader br = new BufferedReader(new InputStreamReader(mScene.getFileURL("flighter/level"+level+".lev").openStream()));
			//BufferedReader br = new BufferedReader(new FileReader(mScene.getFileURL("flighter/level"+level+".lev").getFile()));
			String temp="",temp2="";
			do {
				temp=temp+temp2;
				temp2=br.readLine();
			} while (temp2!=null);
			Object levelJSONR = json.read(temp);
			ArrayList<Object> levelJSON = (ArrayList<Object>) levelJSONR;
			br.close();
			mLevelPrimitives=new ArrayList<ScenePrimitive>();
			mLevelModifier=new ArrayList<ArrayList<PrimitiveModifier>>();
			mSpecialModifier=new ArrayList<PrimitiveModifier>();
			for (Object o: levelJSON)
				if (o instanceof Map) {
					addSerializedObject((Map<String, Object>)o);
				}
			/*
			ObjectInputStream levelStream = new ObjectInputStream(new BufferedInputStream(mScene.getFileURL("flighter/level"+level+".ser").openStream()));
			mLevelCuboids = (ArrayList<Cuboid>) levelStream.readObject( );
			if (mLevelCuboids==null) throw new ClassNotFoundException();
			levelStream.close(); */
		} catch(IllegalArgumentException e){
			mLevelPrimitives=new ArrayList<ScenePrimitive>();
			mLevelModifier=new ArrayList<ArrayList<PrimitiveModifier>>();
			mSpecialModifier=new ArrayList<PrimitiveModifier>();
			mScene.setFloaterText("zerror","error:"+e.getMessage()).y=mScene.height/2;
		} catch (Exception e){
			mLevelPrimitives=new ArrayList<ScenePrimitive>();
			mLevelModifier=new ArrayList<ArrayList<PrimitiveModifier>>();
			mSpecialModifier=new ArrayList<PrimitiveModifier>();
			mScene.setFloaterText("zerror","Level "+level+" missing").y=mScene.height/2;
		}

		mLevelLength=0;
		for (ScenePrimitive sp: mLevelPrimitives)
			if (sp instanceof  Cuboid)
				if (((Cuboid)sp).maxx>mLevelLength) mLevelLength=((Cuboid)sp).maxx;
			
		mLevelEnd=mScene.setZSprite("levelEnd",new ZSprite());
		mLevelEnd.setToString((level<mLastLevel)?("Level "+(level+1)):("Game Over"),mManager.getGraphics().getFontMetrics(
			//new Font("Arial Black",Font.BOLD,100)),0,15);
			new Font("Arial Black",Font.BOLD,300)),0,15);
		//mLevelEnd.rotate90R();
		mLevelEnd.x=mLevelLength+50;
		mLevelEnd.y=mZBufferYStart+(mZBufferH-mLevelEnd.h)/2;
		
		mShipData.levelScroll=0;
		mLevel=level;

		mShoots=new ArrayList<Cuboid>();
		
		//mLevelCuboids.add(new Cuboid(400,530,20,150,5,15));
		//mLevelCuboids.add(new HoledCuboid(500,670,200,400,0,20, 250, 350, 5, 15));
		mManager.resumeRendering();
		
	}
		
	public void stop(){
	}
/*	public void paintFrame(){
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
			c.drawTo(mZBuffer,mShipData.levelScroll,mZBufferYStart);
	}*/
	public void calculateFrame(long time){
		long timeDelta = time - mCurTime;
		mCurTime=time;


		if (mTimeWarpActive)
		{
			if (mCurTime - mTimeWarpLastClockFlicker > 300){
				Floater clock = mClockSymbols.get(mRemainingTimeWarps);
				if (clock!=null) clock.visible = !clock.visible ;
				mTimeWarpLastClockFlicker=mCurTime;
			}

			mScene.calculateFrame((int)(-timeDelta));

			if (mCurTime - mLastDied > 1000) {
				mScene.calculateFrame((int)(-timeDelta));

				mShipData.assign(mShipHistory.get(mShipHistoryPos));
				mShip.x=(int)Math.round(mShipData.p.x-mShip.w/2);
				mShip.y=(int)Math.round(mShipData.p.y-mShip.h/2);
				mShip.z=(int)Math.round(mShipData.p.z);

				mShipHistoryPos--;
				if (mShipHistoryPos<0) mShipHistoryPos=mShipHistory.size()-1;
				if (mShipHistoryTravelBackPos==mShipHistoryPos||mShipHistoryTravelBackPos<0) {
					mShipHistoryPos=-1;
					mShipHistory.clear();
					mTimeWarpActive=false;
					mScene.removeFloater("clock"+mRemainingTimeWarps);
				}
				updateLifeProgressBar();
				mScene.setCameraPosition(-mShipData.levelScroll, -(mScene.height-mZBufferH)/2, 0);
			}
			return;
		}

		if (mShipData.life<mMinimalRequiredLife && mRemainingTimeWarps > 0) {
			mRemainingTimeWarps--;			
			mShipHistoryTravelBackPos=mShipHistoryPos;
			mTimeWarpActive=true;
			return;
		}

		if (mShipData.life<mMinimalRequiredLife){
			//DEAD
			if (mManager.isKeyPressedOnce(KEY_SHIP_FIRE)
			    && mCurTime - mLastDied >= 2000)
				startLevel(mLevel);
			return;
		}

		if (mRemainingTimeWarps > 0) {
			if (mShipHistory.size() < mTimeWarpFrameCount) {
				mShipHistory.add(new ShipInformation());
				mShipHistoryPos = mShipHistoryPos + 1;
			} else mShipHistoryPos = (mShipHistoryPos+1) % mShipHistory.size();
			mShipHistory.get(mShipHistoryPos).assign(mShipData);
		}

		//---------------------keyinput------------------------
		final double acceleration=2;
		Vector3d mShipA=new Vector3d();
		mShipA.x-=mManager.isKeyPressed(KEY_SHIP_ACC_LEFT)?acceleration:0;
		mShipA.x+=mManager.isKeyPressed(KEY_SHIP_ACC_RIGHT)?acceleration:0;
		mShipA.y-=mManager.isKeyPressed(KEY_SHIP_ACC_UP)?acceleration:0;
		mShipA.y+=mManager.isKeyPressed(KEY_SHIP_ACC_DOWN)?acceleration:0;
		mShipA.z-=(mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND)||mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND2)||mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND3)||mManager.isKeyPressed(KEY_SHIP_ACC_DESCEND4))?0.25:0;
		mShipA.z+=(mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND)||mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND2)||mManager.isKeyPressed(KEY_SHIP_ACC_ASCEND3))?0.25:0;

		if (mInitialWait && mShipA.length()>0) mInitialWait = false;

		if (mManager.isKeyPressed(KEY_SHIP_FIRE) || mManager.isKeyPressedChanged(KEY_SHIP_FIRE))
			shipFire();

		for (PrimitiveModifier pm: mSpecialModifier){
			PrimitiveMarker m=((PrimitiveMarker)pm);
			Object subtype = m.properties.get("subtype");
			if ("gravitron".equals(subtype)){
				Vector3d dir = mShipData.p.clone().sub(m.prim.centerI());
				dir.z = 0; 
				double distance = dir.length();
				if (distance*distance > 500*500 + 500*500)
					continue; //ignore things too far away
				double expfac = ((Number)m.properties.get("expfactor") ).doubleValue();
				double mulfac = ((Number)m.properties.get("mulfactor") ).doubleValue();

				mShipA.sub(dir.multiply(mulfac * Math.pow(distance, - expfac - 1))); //-1 to normalize dir
			} else if ("accelerator".equals(subtype)) {
				ArrayList<Number> distances = (ArrayList<Number>)m.properties.get("distances");
				Vector3i dist;
				if (distances != null) dist = new Vector3i(distances);
				else dist = m.prim.cornerRBN().sub(m.prim.centerI());
				ArrayList<Number> acc = (ArrayList<Number>)m.properties.get("acceleration");
				if (acc == null) continue;
				Vector3i center = m.prim.centerI();
				if (Math.abs(center.x - mShipData.p.x) > dist.x) continue;
				if (Math.abs(center.y - mShipData.p.y) > dist.y) continue;
				if (Math.abs(center.z - mShipData.p.z) > dist.z) continue;
				mShipA.add(new Vector3d(acc));
			}
		}

		//-----------------move user ship----------------------
		mShipData.v.add(mShipA);
		/*if (mShipData.v.x>100) mShipData.v.x=100;
		if (mShipData.v.y>60) mShipData.v.y=60;
		if (mShipData.v.z>1.5) mShipData.v.z=1.5;
		if (mShipData.v.x<-60) mShipData.v.x=-60;
		if (mShipData.v.y<-60) mShipData.v.y=-60;
		if (mShipData.v.z<-1) mShipData.v.z=-1;*/
		/*mShipData.v.add(mShipData.v.clone().abs().add(1).multiply(mShipData.v).multiply(-0.01));//slowing down
		if (mShipA.x==0) mShipData.v.x-=0.1*mShipData.v.x; //fast slowdown
		if (mShipA.y==0) mShipData.v.y-=0.1*mShipData.v.y; //fast slowdown
		if (mShipA.z==0) mShipData.v.z-=0.1*mShipData.v.z; //fast slowdown*/
		mShipData.v.multiply(0.9);
		mShipData.v.z = mShipData.v.z * 0.9;

		//mShipData.p.x+=1;
		mShipData.p.add(mShipData.v);
		/*if (mShipData.p.x+mShip.w>mScene.width) {
			mShipData.p.x=mScene.height-mShip.w;
			mShipData.v.x=-0.01;
		}
		 * */
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
		if (mShipData.p.x> -mShipData.levelScroll+mScene.width - mShip.w/2 ) { //there are maxz unreachable pixel at the left screen side
			mShipData.levelScroll = (int)(mScene.width - mShipData.p.x - mShip.w/2);
		}
		if (mShipData.p.y<mShip.h/2) {
			mShipData.p.y=mShip.h/2;
			if (mShipData.v.y<0.01) mShipData.v.y=0.01;
		}
		if (mShipData.p.z<2) {
			mShipData.p.z=2;
			if (mShipData.v.z<0.01) mShipData.v.z=0.01;
		}		

		mShip.x=(int)Math.round(mShipData.p.x-mShip.w/2);
		mShip.y=(int)Math.round(mShipData.p.y-mShip.h/2);
		mShip.z=(int)Math.round(mShipData.p.z);
//		mLevelEnd.x=mLevelLength+50+mShipData.levelScroll;



		for (int i=mShoots.size()-1;i>=0;i--){
			Cuboid c=mShoots.get(i);
			c.maxx+=40;
			c.minx+=40;
			if (c.maxx>mScene.width+mScene.cameraX){
				mShoots.remove(i);
				mScene.removePrimitive(c);
				break;
			}
			Cuboid cWallBoundingBox = c.fastClone();
			cWallBoundingBox.minz+=3;
			cWallBoundingBox.maxz-=2;
			for (int j=0;j<mLevelPrimitives.size();j++)
				if (mLevelPrimitives.get(j) instanceof Cuboid){
					if (cWallBoundingBox.intersect(((Cuboid)mLevelPrimitives.get(j)), -20, 0)){
						mShoots.remove(i);
						mScene.removePrimitive(c);
						break;
					}
				} else if (mLevelPrimitives.get(j) instanceof ZSprite){
					if (c.intersect(((ZSprite)mLevelPrimitives.get(j)), 0, 0)){
						boolean foundMarker = false;
						for (PrimitiveModifier pm: mLevelModifier.get(j))
							if (pm instanceof PrimitiveMarker)
								foundMarker = true;
						if (foundMarker) {
							mShoots.remove(i);
							mScene.removePrimitive(c);
							break;
						}
						mShoots.remove(i);
						mScene.removePrimitive(c);
						mScene.removePrimitive(mLevelPrimitives.get(j));
						mLevelPrimitives.remove(j);
						mSoundSmallExplosion[(int)(Math.random()*mSoundSmallExplosion.length)].play();
						break;
					}
				}
		}



		//--------------calculate collisions ship/geometry-------------
		boolean coll=false;
		for (ScenePrimitive sp: mLevelPrimitives)
			if (sp instanceof Cuboid)
				coll|=((Cuboid)sp).intersect(mShip,0,0,true);
			else if (sp instanceof ZSprite)
				coll|=mShip.intersect((ZSprite)sp,0,0,true);
			//else if (sp instanceof ZSpriteRepeater)
			//	coll|=((ZSpriteRepeater)sp).intersectReversed(mShip,0,0,true);
		int life = mShipData.life;
		if (coll) {
			mSoundCollision[(int)(Math.random()*mSoundCollision.length)].play();
			updateLifeDamaging();
			if (mShipData.life<mMinimalRequiredLife) return; //game end
		}
		updateLifeHealing();
		if (life != mShipData.life)
			updateLifeProgressBar();

		if (mShip.x > mLevelLength + 100 + mLevelEnd.w) startLevel(mLevel+1);


		//------------------scroll level--------------------
		if (!mInitialWait) mShipData.levelScroll-=4;
		if (mShip.x > mLevelLength + 50) {
			mShipData.levelScroll-=10;
			mShipData.p.x+=10;
		}
		mScene.setCameraPosition(-mShipData.levelScroll, -(mScene.height-mZBufferH)/2, 0);
	}
	
	private void updateLifeDamaging(){
		if (mShipData.life<mMinimalRequiredLife) return;

		int life=0;
		for (int y=0;y<mShip.h;y++){
			int b=mShip.getLineIndex(y);
			for (int x=0;x<mShip.w;x++)
				if (mShip.dataVisible[b+x]) life+=1;
		}
		mShipData.life = life;


		if (life<mMinimalRequiredLife) {
			mSoundLargeExplosion.play();

			//if (mRemainingTimeWarps <= 0 ){
				//You died now
				//mScene.clear();

			ZSprite mes=mScene.setZSprite("mes",new ZSprite());
			mes.setToString("YOU\nDIED",mManager.getGraphics().getFontMetrics(
			//new Font("Arial Black",Font.BOLD,100)),0,15);
			new Font("Arial Black",Font.BOLD,150)),0,15);
		//mLevelEnd.rotate90R();
			mes.x=(mScene.width-mes.w)/2-1000;
			mes.y=(mScene.height-mes.h)/2;
			mScene.setCameraPosition(-1000, 0, 0);
			mLastDied = mCurTime;
			//}
			updateLifeProgressBar();
		}
	}

	private void updateLifeHealing(){
		if (mShipData.life >= mInitialLife || mShipData.life < mMinimalRequiredLife || mHealingSpeed == 0) return;

		int healablePixels = 0;
		for (int y=1;y<mShip.h-1;y++){
			int b=mShip.getLineIndex(y);
			for (int x=1;x<mShip.w-1;x++)
				if (!mShip.dataVisible[b+x] && mBaseShip.dataVisible[b+x] &&
				    (mShip.dataVisible[b+x-1] || mShip.dataVisible[b+x+1] ||
				    mShip.dataVisible[mShip.getLineIndex(y-1)+x] || mShip.dataVisible[mShip.getLineIndex(y+1)+x])) 
					healablePixels+=1;
		}

		if (healablePixels <= 0) return;


		//generate a list of pixel to heal (every healable pixel is identified with a number between 0 and healablePixels)
		int healingSpeed = mHealingSpeed;
		if (healingSpeed > healablePixels) healingSpeed = healablePixels;
		ArrayList<Integer> pixelsToHeal = new ArrayList<Integer>(healingSpeed);
		for (int i=0;i<healingSpeed;i++) pixelsToHeal.add(mRandom.nextInt(healablePixels - i));

		Collections.sort(pixelsToHeal);

		//replace the pixel indices with the distance between the pixel indices
		for (int i=healingSpeed-1;i>0;i--) pixelsToHeal.set(i, pixelsToHeal.get(i)-pixelsToHeal.get(i-1));

		int pixelToHealIndex = 0;
		int pixelToHeal = pixelsToHeal.get(0);

		//then jump over the given distance to the next pixel and heal that
		for (int y=1;y<mShip.h-1;y++){
			int b=mShip.getLineIndex(y);
			for (int x=1;x<mShip.w-1;x++)
				if (!mShip.dataVisible[b+x] && mBaseShip.dataVisible[b+x] &&
				    (mShip.dataVisible[b+x-1] || mShip.dataVisible[b+x+1] ||
				    mShip.dataVisible[mShip.getLineIndex(y-1)+x] || mShip.dataVisible[mShip.getLineIndex(y+1)+x])) {
					if (pixelToHeal == 0) {
						mShip.dataVisible[b+x] = true;
						pixelToHealIndex++;
						mShipData.life++; //increment for every single pixel, because sometimes it doesn't find all (bug? or some narrow path)
						if (pixelToHealIndex>=pixelsToHeal.size()) return;
						else pixelToHeal = pixelsToHeal.get(pixelToHeal);
					} else pixelToHeal--;
				}
		}
	}

	private void updateLifeProgressBar(){
		Floater life=mScene.getFloater("life");
		if (life==null) {
			life=mScene.setFloater("life",new Floater(ZDraw.SIRDW-ZDraw.MAXZ-10,10));
			life.x=5;
			life.y=5;
			life.z=ZDraw.MAXZ/2;
		}

		int padding=1;
		int len=life.w-2*padding;
		int healthyEnd=len*(mShipData.life - mMinimalRequiredLife)/(mInitialLife - mMinimalRequiredLife)+padding;
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

	private void shipFire(){
		if (mCurTime - mLastShoot < mShootTimeout)
			return;
		mSoundFire.play();
		mLastShoot=mCurTime;
		Cuboid fire=new Cuboid();
		int ox=mShip.x, oy=mShip.y, oz=mShip.z;
		//if ((mShootCount & 1) != 0) oy=mShip.y+mShip.h;
		oy=mShip.y+mShip.h/2;
		int sx=60, sy=16, sz=8;//if you change the height, also change the shoot collision bounding box in calculateframe
		fire.minx=ox-sx/2;
		fire.miny=oy-sy/2;
		fire.minz=Math.max(0,oz-sz/2);
		fire.maxx=ox+sx/2;
		fire.maxy=oy+sy/2;
		fire.maxz=Math.min(ZDraw.MAXZ, oz+sz/2);
		mScene.addPrimitive(fire);
		mShoots.add(fire);
		mShootCount++;
	}
	
	public String getName(){
		return Translations.getInstance().SIRDSFlighter();
	}
	public String getDescription(){
		return Translations.getInstance().SIRDSFlighterDesc();
	}
	public String getKeys(){
		return Translations.getInstance().SIRDSFlighterKeys();
	}
	public String[] getPossibleOptions(){
		return new String[]{Translations.getInstance().SIRDSFlighterVeryEasy(),Translations.getInstance().SIRDSFlighterEasy(),Translations.getInstance().SIRDSFlighterNormal(),Translations.getInstance().SIRDSFlighterHard(),Translations.getInstance().SIRDSFlighterImpossible()};
	}
	public int getDefaultOption(){
		return 2;
	}
	
}
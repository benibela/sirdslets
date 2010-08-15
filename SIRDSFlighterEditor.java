import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JOptionPane;


public class SIRDSFlighterEditor extends SIRDSFlighter{
	protected int mCurrentSelection=-1;
	private boolean mDragging;
	private int mSelectionStartrx, mSelectionStartry;
	private Vector3i mSelectionStartCenter;

	private PrimitiveMover mEditedMover;
	private ArrayList<ScenePrimitive> mEditedMoverPositions;
	private int mCurrentSelectionPosition;

	private enum EditingState {ES_NORMAL, ES_MOVER, ES_TESTING};
	private EditingState mState = EditingState.ES_NORMAL;
	private Map<String, String> mBasicElements;
	@Override
	public void start(Object manager, int option){
		super.start(manager, option);
		mLastLevel = 1000;
		
		mManager.setShowFloaterCursor(true);
		
		Floater f=mScene.setFloaterText("level","level: ?", 0xffddddcc);
		int lh=f.h;
		mScene.setFloaterText("scroll","scroll: 0", 0xffddddcc).y=lh;
		mScene.setFloaterText("cursel","cursel: -1", 0xffddddcc).y=2*lh;
		mScene.getFloater("life").visible=false;

		mEditedMoverPositions = new ArrayList<ScenePrimitive>();

		mBasicElements = new TreeMap<String, String>();
		mBasicElements.put("mine", "{\"image\":\"mine_rot.png\",\"modifier\":[{\"roipos\":[0,0,80,0,160,0,240,0,320,0,400,0,480,0,560,0,640,0],\"roisize\":[80,80],\"type\":\"PrimitiveAnimator\",\"velocity\":6.0}],\"position\":[750,10,10],\"type\":\"ZSprite\"}");
		mBasicElements.put("black hole", "{\"image\":\"blackhole.png\",\"modifier\":[{\"expfactor\":2,\"mulfactor\":40000,\"subtype\":\"gravitron\",\"type\":\"PrimitiveMarker\"}],\"position\":[1130,0,0],\"type\":\"ZSprite\"}");
		mBasicElements.put("white hole", "{\"image\":\"whitehole.png\",\"modifier\":[{\"expfactor\":2,\"mulfactor\":-40000,\"subtype\":\"gravitron\",\"type\":\"PrimitiveMarker\"}],\"position\":[3870,0,0],\"type\":\"ZSprite\"}");
		mBasicElements.put("accelerator right", "{\"image\":\"accr.png\",\"modifier\":[{\"type\":\"PrimitiveMarker\", \"subtype\":\"accelerator\", \"acceleration\":[2,0,0]}],\"type\":\"ZSpriteRepeater\", \"repeat\":[3,1], \"position\":[0,0,0]}");
		mBasicElements.put("accelerator left", "{\"image\":\"accl.png\",\"modifier\":[{\"type\":\"PrimitiveMarker\", \"subtype\":\"accelerator\", \"acceleration\":[-2,0,0]}],\"type\":\"ZSpriteRepeater\", \"repeat\":[3,1], \"position\":[0,0,0]}");
		mBasicElements.put("accelerator up", "{\"image\":\"accu.png\",\"modifier\":[{\"type\":\"PrimitiveMarker\", \"subtype\":\"accelerator\", \"acceleration\":[0,-2,0]}],\"type\":\"ZSpriteRepeater\", \"repeat\":[1,3], \"position\":[0,0,0]}");
		mBasicElements.put("accelerator down", "{\"image\":\"accd.png\",\"modifier\":[{\"type\":\"PrimitiveMarker\", \"subtype\":\"accelerator\", \"acceleration\":[0,2,0]}],\"type\":\"ZSpriteRepeater\", \"repeat\":[1,3], \"position\":[0,0,0]}");
		mBasicElements.put("accelerator far", "{\"image\":\"accf.png\",\"modifier\":[{\"type\":\"PrimitiveMarker\", \"subtype\":\"accelerator\", \"acceleration\":[0,0,-0.1]}],\"type\":\"ZSpriteRepeater\", \"repeat\":[3,3], \"position\":[0,0,0]}");
		mBasicElements.put("accelerator near", "{\"image\":\"accn.png\",\"modifier\":[{\"type\":\"PrimitiveMarker\", \"subtype\":\"accelerator\", \"acceleration\":[0,0,0.1]}],\"type\":\"ZSpriteRepeater\", \"repeat\":[3,3], \"position\":[0,0,0]}");
		startLevel(firstLevel);
	}
	@Override
	public void stop(){
		super.stop();
	}
	@Override
	public void startLevel(int level){
		if (mState == EditingState.ES_TESTING){
			setEditingState(EditingState.ES_NORMAL);
			return;
		}
		mDragging = false;
		super.startLevel(level);
		setEditingState(EditingState.ES_NORMAL);
		mScene.setFloaterText("level","level: "+mLevel, 0xffddddcc);
	}
	protected void saveLevel(){
		try{
		//System.out.println(mManager.getFileURL("flighter/level"+mLevel+".ser").toString());
			ArrayList<Object> levelSer = new ArrayList<Object>();
			for (int i=0;i<mLevelPrimitives.size();i++){
				JSONSerializable ser=mLevelPrimitives.get(i);
				Map<String, Object> map= (Map<String,Object>)ser.jsonSerialize();
				if (mLevelModifier.get(i)!=null)
					map.put("modifier", mLevelModifier.get(i));
				if (ser instanceof ZSprite )
					for (Map.Entry<String, ZSprite> e: mImageCache.entrySet())
						if (e.getValue().data==((ZSprite)ser).data){
							map.put("image",e.getKey());
							break;
						}
				if (ser instanceof ZSpriteRepeater )
					for (Map.Entry<String, ZSprite> e: mImageCache.entrySet())
						if (e.getValue().data==((ZSpriteRepeater)ser).sprite.data){
							map.put("image",e.getKey());
							break;
						}
				levelSer.add(map);
			}
			JSONWriter json = new JSONWriter();
			BufferedWriter bw = new BufferedWriter(new FileWriter(mScene.getFileURL("flighter/level"+mLevel+".lev").getFile()));
			bw.write(json.write(levelSer));
			bw.close();
			mScene.showFloaterMessage("saved level "+mLevel,SceneManager.MessageType.MESSAGE_NOTIFY);
		} catch (IOException e){
			mScene.showFloaterMessage("Couldn't save"+":"+e.toString(),SceneManager.MessageType.MESSAGE_ERROR);
		}
	}

	protected void setEditingState(EditingState newState){
		if (newState == mState) return;
		//go to normal
		switch (mState){
			case ES_MOVER:
				mState = EditingState.ES_NORMAL;
				for (ScenePrimitive c: mEditedMoverPositions)
					mScene.removePrimitive(c);
				mEditedMoverPositions.clear();
				break;
			case ES_TESTING:
				mState = EditingState.ES_NORMAL;
				startLevel(mLevel);
				break;
		}
		//go to new
		switch (newState){
			case ES_MOVER:
				editPrimitiveMover();
				break;
			case ES_TESTING:
				saveLevel();
				startLevel(mLevel);
				mState = EditingState.ES_TESTING;
				break;
		}
	}

	@Override
	public void calculateFrame(long timeMS){
		boolean ctrl=mManager.isKeyPressed(KeyEvent.VK_CONTROL);
		boolean shift=mManager.isKeyPressed(KeyEvent.VK_SHIFT);

		if (mState == EditingState.ES_TESTING){
			super.calculateFrame(timeMS);
			if (ctrl && shift && mManager.isKeyPressedOnce(KeyEvent.VK_T))
				setEditingState(EditingState.ES_NORMAL);
			return;
		}

		//no level processing

		//=================general processing===============

		//scroll/change level
		if (mManager.isKeyPressed(KeyEvent.VK_LEFT))
			if (ctrl && mLevel>=1) startLevel(mLevel-1);
			else scrollLevelDelta(+15);
		if (mManager.isKeyPressed(KeyEvent.VK_RIGHT))
			if (ctrl) startLevel(mLevel+1);
			else scrollLevelDelta(-15);
		if (mManager.isKeyPressed(KeyEvent.VK_PAGE_DOWN))
			scrollLevelDelta(-500);
		if (mManager.isKeyPressed(KeyEvent.VK_PAGE_UP))
			scrollLevelDelta(+500);
		mScene.setFloaterText("scroll","scroll: "+mShipData.levelScroll,0xffddddcc);

		//mouse
		int rx=mManager.getMouseX()+mScene.cameraX;
		int ry=mManager.getMouseY()+mScene.cameraY;


		//=============state depending==============
		switch (mState){
			case ES_NORMAL:

				//--------------keyevents--------------------------
				//final double acceleration=1;

				if (mManager.isKeyPressed(KeyEvent.VK_A))
					mManager.setFloaterCursorZ(mManager.getFloaterCursorZ()+1);

				if (mManager.isKeyPressed(KeyEvent.VK_S))
					if (ctrl&&shift) { //save
						saveLevel();
					} else //mouse control
						mManager.setFloaterCursorZ(mManager.getFloaterCursorZ()-1);

				if (ctrl && shift && mManager.isKeyPressedOnce(KeyEvent.VK_T))
					setEditingState(EditingState.ES_TESTING);

				//create
				if (mManager.isKeyPressedOnce(KeyEvent.VK_C))
					addCuboidAtCursor();
				//create image
				if (mManager.isKeyPressedOnce(KeyEvent.VK_I))
					addZSpriteAtCursor();
				//create basic element
				if (mManager.isKeyPressedOnce(KeyEvent.VK_B))
					addBasicElementAtCursor();
				//duplicate
				if (mManager.isKeyPressedOnce(KeyEvent.VK_D))
					duplicateScenePrimitive();


				//remove
				if (mManager.isKeyPressedOnce(KeyEvent.VK_R))
					removeSelectedCuboid();

				//resize
				if (mManager.isKeyPressed(KeyEvent.VK_X) ||
				    mManager.isKeyPressed(KeyEvent.VK_Y))
					resizeSelectedCuboid(mManager.isKeyPressed(KeyEvent.VK_X), mManager.isKeyPressed(KeyEvent.VK_Y), mManager.isKeyPressed(KeyEvent.VK_Z), shift, ctrl);
				if (mManager.isKeyPressedOnce(KeyEvent.VK_Z)){
					if (mLevelPrimitives.get(mCurrentSelection) instanceof Cuboid)
						resizeSelectedCuboid(false, false, mManager.isKeyPressed(KeyEvent.VK_Z), shift, ctrl);
					else if (mLevelPrimitives.get(mCurrentSelection) instanceof ZSprite)
						((ZSprite)mLevelPrimitives.get(mCurrentSelection)).z += shift?1:-1;
					else if (mLevelPrimitives.get(mCurrentSelection) instanceof ZSpriteRepeater)
						((ZSpriteRepeater)mLevelPrimitives.get(mCurrentSelection)).sprite.z += shift?1:-1;
				}
				//extended modifiers
				if (mManager.isKeyPressedOnce(KeyEvent.VK_M))
					if (!ctrl) editPrimitiveModifiers();
					else setEditingState(EditingState.ES_MOVER); 


				//-----------------mouse events-----------------------
				if (mManager.isMousePressed(MouseEvent.BUTTON1) &&
				    mManager.isMousePressedChanged(MouseEvent.BUTTON1)){
					int j=-1;
					int bestZ=-1;
					for (int i=0;i<mLevelPrimitives.size();i++){
						ScenePrimitive sp = mLevelPrimitives.get(i);
						int nz = sp.zAt(rx, ry);
						if (j==-1 || nz>bestZ){
							j=i;
							bestZ=nz;
						}
					}
					setCurrentSelection(j);
					if (j!=-1){
						mSelectionStartrx=rx;
						mSelectionStartry=ry;
						mSelectionStartCenter=mLevelPrimitives.get(mCurrentSelection).centerI();
					}
				}
				if (mManager.isMousePressed(MouseEvent.BUTTON1) && mCurrentSelection != -1) {
					mDragging |= (rx-mSelectionStartrx)*(rx-mSelectionStartrx)+
						     (ry-mSelectionStartry)*(ry-mSelectionStartry) >= 25;
					if (mDragging)
						moveSelectedObject(rx,ry,shift?1:10);
				} else mDragging = false;

				int z=-1;
				for (ScenePrimitive sp: mLevelPrimitives){
					if (sp instanceof Cuboid){
						Cuboid c=(Cuboid)sp;
						if (c.containsPoint(rx, ry))
							if (c.maxz>z)
								z = c.maxz;
					}
				}
				//int z=mZBuffer.data[mZBuffer.getLineIndex(y)+x];
				if (mCurrentSelection>=0 && mCurrentSelection<mLevelPrimitives.size() ) {
					ScenePrimitive curSel = mLevelPrimitives.get(mCurrentSelection);
					if (curSel instanceof Cuboid) {
						Cuboid c = (Cuboid)curSel;
						mScene.setFloaterText("cursel1",c.minx+":"+c.miny+":"+c.minz,0xffddddcc).y=mZBufferYStart+515;
						mScene.setFloaterText("cursel2",c.maxx+":"+c.maxy+":"+c.maxz,0xffddddcc).y=mZBufferYStart+530;
					} else if (curSel instanceof ZSprite){
						ZSprite zs = (ZSprite)curSel;
						mScene.setFloaterText("cursel1",zs.x+":"+zs.y+":"+zs.z,0xffddddcc).y=mZBufferYStart+515;
						mScene.setFloaterText("cursel2",(zs.x+zs.w)+":"+(zs.y+zs.h)+":",0xffddddcc).y=mZBufferYStart+530;
					} else if (curSel instanceof ZSpriteRepeater){
						ZSpriteRepeater r = ((ZSpriteRepeater)curSel);
						mScene.setFloaterText("cursel1",r.sprite.x+":"+r.sprite.y+":"+r.sprite.z,0xffddddcc).y=mZBufferYStart+515;
						mScene.setFloaterText("cursel2",(r.sprite.x+r.sprite.w*r.repx)+":"+(r.sprite.y+r.sprite.h*r.repy)+":",0xffddddcc).y=mZBufferYStart+530;
					}
				}
				mScene.setFloaterText("mousez:",rx+"/"+ry+"/"+z,0xffddddcc).y=mZBufferYStart+500;
				break;



			case ES_MOVER:
				if (mManager.isMousePressed(MouseEvent.BUTTON1) &&
				    mManager.isMousePressedChanged(MouseEvent.BUTTON1)){
					int j=-1;
					int bestZ=-1;
					for (int i=0;i<mEditedMoverPositions.size();i++){
						ScenePrimitive sp = mEditedMoverPositions.get(i);
						int nz = sp.zAt(rx, ry);
						if (j==-1 || nz>bestZ){
							j=i;
							bestZ=nz;
						}
					}
					mCurrentSelectionPosition = j;
					if (j!=-1){
						mSelectionStartrx=rx;
						mSelectionStartry=ry;
						mSelectionStartCenter=mEditedMoverPositions.get(mCurrentSelectionPosition).centerI();
						mDragging = false;
					}
				}

				if (mManager.isMousePressed(MouseEvent.BUTTON1) && mCurrentSelectionPosition != -1) {
					mDragging = mDragging || ((rx-mSelectionStartrx)*(rx-mSelectionStartrx)+
								 (ry-mSelectionStartry)*(ry-mSelectionStartry) >= 25);
					if (mDragging){
						Vector3i newPos = new Vector3i(mSelectionStartCenter.x+rx-mSelectionStartrx,mSelectionStartCenter.y+ry-mSelectionStartry,mSelectionStartCenter.z);
						if (!shift){
							newPos.x = ((newPos.x + 5) / 10) * 10;
							newPos.y = ((newPos.y + 5) / 10) * 10;
						}
						mEditedMoverPositions.get(mCurrentSelectionPosition).moveTo(newPos);
						mEditedMover.positions.set(mCurrentSelectionPosition, newPos);
					}

				} else mDragging = false;


				if (mManager.isKeyPressedOnce(KeyEvent.VK_Z) && mCurrentSelectionPosition!=-1){
					mEditedMover.positions.get(mCurrentSelectionPosition).z+=shift?1:-1;
					mEditedMoverPositions.get(mCurrentSelectionPosition).moveTo(mEditedMover.positions.get(mCurrentSelectionPosition));
				}


				if (mManager.isKeyPressed(KeyEvent.VK_M) && !ctrl)
					setEditingState(EditingState.ES_NORMAL);

				//create
				if (mManager.isKeyPressedOnce(KeyEvent.VK_C)){
					ScenePrimitive sp=mLevelPrimitives.get(mCurrentSelection).fastClone();
					sp.moveTo(rx, ry, 10);
					mScene.addPrimitive(sp);
					mEditedMoverPositions.add(sp);
					mEditedMover.positions.add(new Vector3i(rx, ry, 10));
					mCurrentSelectionPosition = mEditedMoverPositions.size()-1;
				}


				//remove
				if (mManager.isKeyPressedOnce(KeyEvent.VK_R) && mCurrentSelectionPosition!=-1) {
					mEditedMover.positions.remove(mCurrentSelectionPosition);
					mScene.removePrimitive(mEditedMoverPositions.get(mCurrentSelectionPosition));
					mEditedMoverPositions.remove(mCurrentSelectionPosition);
					mCurrentSelectionPosition=-1;
				}

				if (mCurrentSelectionPosition>=0 && mCurrentSelectionPosition<mEditedMover.positions.size() ) {
						mScene.setFloaterText("cursel1",mEditedMover.positions.get(mCurrentSelectionPosition).x+":"+mEditedMover.positions.get(mCurrentSelectionPosition).y+":"+mEditedMover.positions.get(mCurrentSelectionPosition).z,0xffddddcc).y=mZBufferYStart+515;
				}
				mScene.setFloaterText("mousez:",rx+"/"+ry,0xffddddcc).y=mZBufferYStart+500;
				break;
		}
	}
	

	protected void scrollLevelDelta(int delta){
		mShipData.levelScroll+=delta;
		mScene.setCameraPosition(-mShipData.levelScroll,-(mScene.height-mZBufferH)/2,0)	;
	}
	
	protected void setCurrentSelection(int currentSelection){
		mCurrentSelection=currentSelection;
		mScene.setFloaterText("cursel","cursel: "+mCurrentSelection, 0xffddddcc);
	}

	protected void moveSelectedObject(int nx, int ny, int roundTo){
		ScenePrimitive sp=mLevelPrimitives.get(mCurrentSelection);
		int nrx = mSelectionStartCenter.x+nx-mSelectionStartrx;
		int nry = mSelectionStartCenter.y+ny-mSelectionStartry;
		if (roundTo!=1){
			nrx = ((nrx + roundTo/2) / roundTo) * roundTo;
			nry = ((nry + roundTo/2) / roundTo) * roundTo;
		}
		sp.moveTo(nrx,nry,mSelectionStartCenter.z);
	}
	public void removeSelectedCuboid(){
		if (mCurrentSelection==-1) return;
		mManager.suspendRendering();
		mScene.removePrimitive(mLevelPrimitives.get(mCurrentSelection));
		for (PrimitiveModifier pm: mLevelModifier.get(mCurrentSelection))
			mScene.removePrimitiveModifier(pm);
		mLevelPrimitives.remove(mCurrentSelection);
		mLevelModifier.remove(mCurrentSelection);
		mManager.resumeRendering();
		setCurrentSelection(-1);
	}
	
	private final static int cuboidDelta = 10; 
	public void addCuboidAtCursor(){
		Floater f=mManager.getFloaterCursor();
		mManager.suspendRendering();
		int minx=f.x-mShipData.levelScroll-50; if (minx%cuboidDelta!=0) minx-=minx%cuboidDelta;
		int maxx=f.x-mShipData.levelScroll+50; if (maxx%cuboidDelta!=0) maxx-=maxx%cuboidDelta;
		int miny=Math.max(0,f.y-mZBufferYStart-100);  if (miny % cuboidDelta!=0) miny-=miny%cuboidDelta;
		int maxy=Math.min(mZBufferH,f.y-mZBufferYStart+100);if (maxy % cuboidDelta!=0) maxy-=maxy%cuboidDelta;
		int minz=Math.max(0,f.z-5);
		int maxz=Math.min(ZDraw.MAXZ,f.z+5);
		Cuboid c=new Cuboid(minx,maxx,miny,maxy,minz,maxz);
		mLevelPrimitives.add(c);
		mLevelModifier.add(new ArrayList<PrimitiveModifier>());
		mScene.addPrimitive(c);
		setCurrentSelection(mLevelPrimitives.size()-1);
		mManager.resumeRendering();
	}
	public void addZSpriteAtCursor(){
		Floater f=mManager.getFloaterCursor();
		mManager.suspendRendering();
		String imageName = (String)JOptionPane.showInputDialog(null,"Imagename:","",JOptionPane.PLAIN_MESSAGE,null,null,"");
		if (imageName==null )return;
		ZSprite base;
		if (mImageCache.containsKey(imageName)) base=mImageCache.get(imageName);
		else {
			base = mScene.createZSprite("flighter/"+imageName);
			mImageCache.put(imageName,base);
		}
		ZSprite nsp = base.fastClone();
		nsp.x=f.x+mScene.cameraX;nsp.y=f.y+mScene.cameraY;nsp.z=f.z;
		mLevelPrimitives.add(nsp);
		mLevelModifier.add(new ArrayList<PrimitiveModifier>());
		mScene.addPrimitive(nsp);
		setCurrentSelection(mLevelPrimitives.size()-1);
		mManager.resumeRendering();

	}
	public void addBasicElementAtCursor(){
		String element= (String) JOptionPane.showInputDialog(null,"Element:","",JOptionPane.PLAIN_MESSAGE,null,mBasicElements.keySet().toArray(),mBasicElements.keySet().iterator().next());
		if (element == null || element.isEmpty()) return;
		String ser = mBasicElements.get(element);
		if (ser==null) return;
		ScenePrimitive sp= addSerializedObject((Map<String, Object>)((new JSONReader()).read(ser)));
		Floater f=mManager.getFloaterCursor();
		sp.moveTo(f.x + mScene.cameraX, f.y + mScene.cameraY, 0);
		mCurrentSelection = mLevelPrimitives.indexOf(sp);
	}
	public void duplicateScenePrimitive(){
		if (mCurrentSelection==-1)return;
		ScenePrimitive c=mLevelPrimitives.get(mCurrentSelection);
		ScenePrimitive n = c.fastClone();
		mLevelPrimitives.add(n);
		if (mLevelModifier.get(mCurrentSelection)==null)
			mLevelModifier.add(null);
		else{
			ArrayList<PrimitiveModifier> list=new ArrayList<PrimitiveModifier>();
			for (PrimitiveModifier pm: (ArrayList<PrimitiveModifier>) mLevelModifier.get(mCurrentSelection)){
				PrimitiveModifier pm2=pm.clone(n);
				list.add(pm2);
				mScene.addPrimitiveModifier(pm2);
			}
			mLevelModifier.add(list);
		}
		mScene.addPrimitive(n);
		mCurrentSelection = mLevelPrimitives.size()-1;

	}
	public void resizeSelectedCuboid(boolean resizeX, boolean resizeY, boolean resizeZ, boolean enlarge, boolean minimum){
		if (mCurrentSelection==-1) return;
		ScenePrimitive sp = mLevelPrimitives.get(mCurrentSelection);
		if (sp instanceof ZSpriteRepeater){
			ZSpriteRepeater r = (ZSpriteRepeater)sp;
			if (resizeX && enlarge) r.repx++;
			else if (resizeX && !enlarge) r.repx--;
			else if (resizeY && enlarge) r.repy++;
			else if (resizeY && !enlarge) r.repy--;
		}

		if (!(sp instanceof Cuboid)) return;
		Cuboid c = (Cuboid)sp;
		int inc=enlarge?1:-1;

		//3d
		if (resizeZ){
			if (minimum) c.minz=Math.min(c.maxz,Math.max(0,c.minz+inc));
			else c.maxz=Math.max(c.minz,Math.min(ZDraw.MAXZ,c.maxz+inc));
		}
		//1-2d
		inc*=cuboidDelta;
		if (resizeX)
			if (minimum) c.minx+=inc;
			else c.maxx+=inc;
		if (resizeY)
			if (minimum) c.miny=Math.min(c.maxy,Math.max(0,c.miny+inc));
			else c.maxy=Math.max(c.miny,Math.min(mZBufferH,c.maxy+inc));

	}

	public void editPrimitiveModifiers(){
		if (mCurrentSelection==-1) return;
		ArrayList<PrimitiveModifier> apm = mLevelModifier.get(mCurrentSelection);
		String s = (String)JOptionPane.showInputDialog(null,"Modifiers:","",JOptionPane.PLAIN_MESSAGE,null,null,apm!=null?(new JSONWriter()).write(apm):"");
		if (s==null)
			return;
		if (s.equals("")) {
			if (apm!=null){
				for (PrimitiveModifier mod: apm)
					mScene.removePrimitiveModifier(mod);
				mLevelModifier.set(mCurrentSelection, null);
			}
		} else {
			float moverTime=0;
			if (apm==null) apm=new ArrayList<PrimitiveModifier>();
			else {
				for (PrimitiveModifier mod: apm){
					if (mod instanceof PrimitiveMover)
						moverTime=((PrimitiveMover)mod).time;
					mScene.removePrimitiveModifier(mod);
				}
				apm.clear();
			}
			ArrayList<Map<String, Object>> temp=(ArrayList<Map<String, Object>>)((new JSONReader()).read(s));
			for (Map<String, Object> map: temp){
				PrimitiveModifier pm=mScene.deserializePrimitiveModifier(map, mLevelPrimitives.get(mCurrentSelection));
				if (pm instanceof PrimitiveMover)
					((PrimitiveMover)pm).time = moverTime;
				mScene.addPrimitiveModifier(pm);
				apm.add(pm);
			}

			mLevelModifier.set(mCurrentSelection, apm);
		}
	}


	public void editPrimitiveMover(){
		if (mCurrentSelection==-1) return;
		if (mLevelModifier.get(mCurrentSelection)==null){
			mLevelModifier.set(mCurrentSelection, new ArrayList<PrimitiveModifier>());
			mEditedMover=new PrimitiveMover(mLevelPrimitives.get(mCurrentSelection));
			mLevelModifier.get(mCurrentSelection).add(mEditedMover);
			mScene.addPrimitiveModifier(mEditedMover);
		} else {
			mEditedMover=null;
			for (PrimitiveModifier m: mLevelModifier.get(mCurrentSelection))
				if (m instanceof PrimitiveMover){
					mEditedMover=(PrimitiveMover)(m);
					break;
				}
			if (mEditedMover==null){
				mEditedMover=new PrimitiveMover(mLevelPrimitives.get(mCurrentSelection));
				mLevelModifier.get(mCurrentSelection).add(mEditedMover);
				mScene.addPrimitiveModifier(mEditedMover);
			}
		}
		for (Vector3i pos: mEditedMover.positions){
			ScenePrimitive sp = mLevelPrimitives.get(mCurrentSelection).fastClone();
			sp.moveTo(pos);
			mEditedMoverPositions.add(sp);
			mScene.addPrimitive(sp);
		}
		mState = EditingState.ES_MOVER;
	}

	@Override
	public String getName(){
		return Translations.getInstance().SIRDSFlighterEditor();
	}
	@Override
	public String getDescription(){
		return Translations.getInstance().SIRDSFlighterEditorDesc();
			
	}
	@Override
	public String getKeys(){
		return Translations.getInstance().SIRDSFlighterEditorKeys();
	}
}
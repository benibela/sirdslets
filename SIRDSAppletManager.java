//
// SIRDS Applet Manager 
//
// By Benito van der Zander - http://www.benibela.de
// Based on AbSIRDlet of Lewey Geselowitz - http://www.leweyg.com

import java.awt.event.*;
import java.awt.image.*;
import java.applet.*;
import java.util.*;
import java.awt.*;
import java.io.*; 
import javax.imageio.*; 
import java.net.*;

class TimedFloater extends Floater{
	int timetolive;  //time until removal in ms
}
		
public class SIRDSAppletManager extends Applet implements Runnable,  KeyListener, MouseMotionListener, MouseListener
{	
	private Thread mUpdateThread;
	//private int[] mRandData1, mRandData2;
	private SIRDSRenderer renderer;
	private SceneManager scene=new SceneManager();
//	private int mWidth, mHeight;
	private int mFrameNumber = 0;
	private boolean mUseSIRD = true;
	private boolean mInvert = false; //false: parallel, true: cross eyes
	private SIRDSAppletManager self; //=this, but also usable in anonymous sub classes
	
	private SIRDSlet curSIRDSlet=null;
	private ArrayList<SIRDSlet> sirdslets=new ArrayList<SIRDSlet>();

	private boolean mAllowLoading=false;
	private boolean mAllowSaving=false;
	private boolean mShowFloaterCursor=false;
//	private boolean mFloaterCursorZ=false;
	
	private java.util.concurrent.locks.Lock renderLock=new java.util.concurrent.locks.ReentrantLock();
			
	//sird id: (.*)\.png => load (\1).png
	//          .*random.* => random (checks for "color" and "strid")
	//          color (black,gray) => single color
	//			else => "" => (in 1 => heightmap, in 2 => = 1)
	//see initSIRDChoice
	private int str2Color(String color){
		if ("white".equals(color)) return 0xFFffffff;
		if ("gray".equals(color)) return 0xFF888888;
		if ("black".equals(color)) return 0xFF000000;
		if ("red".equals(color)) return 0xFFff0000;
		if ("green".equals(color)) return 0xFF00ff00;
		if ("blue".equals(color)) return 0xFF0000ff;
		if ("yellow".equals(color)) return 0xFFffff00;
		if ("violet".equals(color)) return 0xFFff00ff;
		if ("cyan".equals(color)) return 0xFF00ffff;
		return 0;
	}
	private int[] singleSIRDData(String id){
		if (id.endsWith(".png")) 
			return ZDraw.GetImageSIRDData(scene.loadImage(id));
		if (id.contains("random")){
			int c = str2Color(id.substring(0,id.indexOf(" ")));
			return ZDraw.GetRandSIRDData(id.contains("color"), id.contains("strid"), c!=0?c:0xffffffff);
		}
		if (id.equals("black")||id.equals("gray")) {
			int[] temp=new int[ZDraw.SIRDW*ZDraw.SIRDH];
			Arrays.fill(temp,id.equals("black")?0xff000000:0xff888888);
			return temp;
		}
		return null;
	}
	String mFrame1SIRDId, mFrame2SIRDId;
	private void setFrameSIRD(String sird1, String sird2){
		mFrame1SIRDId=sird1;
		mFrame2SIRDId=sird2;
		renderer.setSISData(singleSIRDData(sird1), singleSIRDData(sird2), ZDraw.SIRDW, ZDraw.SIRDH);
		frame1SIRDChoice.select(sird1);
		frame2SIRDChoice.select(sird2);
	}
	Label fpsInfo;
	Panel startPanel;
	Panel optionPanel;
	Panel infoPanel;
	Choice frame1SIRDChoice,frame2SIRDChoice;
	private void initSIRDChoice(Choice c){
		c.add("white random");
		c.add("red random");
		c.add("green random");
		c.add("blue random");
		c.add("yellow random");
		c.add("violet random");
		c.add("cyan random");
		c.add("colored random");
		c.add("white random strides");
		c.add("colored random strides");
		c.add("gold.png");
		c.add("tiger.png");
		c.add("squares.png");
		c.add("black");
		c.add("gray");
	}
	private void initGUI(){		
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		infoPanel=new Panel();
		infoPanel.setLayout(new FlowLayout());
		fpsInfo=new Label("<<<<<<<<<<<<<<<<<<<<<<<<");
		infoPanel.add(fpsInfo);
		gbc.anchor = GridBagConstraints.NORTHEAST;
		gbc.weightx = 0.1;
		gbc.weighty = 0.1;
		add(infoPanel, gbc);

		
		startPanel=new Panel();
		startPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		startPanel.setLayout(new GridLayout(0,1));
		
		
		Label title=new Label("Available SIRDSlets:");
		title.setFont(getFont().deriveFont(Font.BOLD));
		startPanel.add(title);
		
		for (int i=0;i< sirdslets.size();i++){
			Button b = new Button(sirdslets.get(i).getSIRDletName());
			final SIRDSlet temp=sirdslets.get(i);
			b.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				startSIRDSlet(temp);
			}});
			startPanel.add(b);
		}

		

		optionPanel=new Panel();
		optionPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		optionPanel.setLayout(new GridLayout(0,2));

		title=new Label("Options:");
		title.setFont(getFont().deriveFont(Font.BOLD));
		optionPanel.add(title);
		optionPanel.add(new Label(""));
		
		frame1SIRDChoice = new Choice();
		frame1SIRDChoice.add("!height-map");
		initSIRDChoice(frame1SIRDChoice);
		frame1SIRDChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				setFrameSIRD((String)e.getItem(),mFrame2SIRDId);
			}
		});
		optionPanel.add(new Label("first frame:"));
		optionPanel.add(frame1SIRDChoice);

		frame2SIRDChoice = new Choice();
		frame2SIRDChoice.add("!same as above");
		frame2SIRDChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				setFrameSIRD(mFrame1SIRDId,(String)e.getItem());
			}
		});
		initSIRDChoice(frame2SIRDChoice);
		optionPanel.add(new Label("second frame:"));
		optionPanel.add(frame2SIRDChoice);

		/*final TextField secondsPerFrame=new TextField();
		optionPanel.add(new Label("seconds/frame:"));
		secondsPerFrame.addTextListener(new TextListener(){
			public  void textValueChanged(TextEvent e){
				try{
					timePerFrame=Integer.parseInt(secondsPerFrame.getText());
				} catch (NumberFormatException ex){
				}
				//javax.swing.JOptionPane.showMessageDialog(null, e.paramString(), "Test Titel", javax.swing.JOptionPane.OK_CANCEL_OPTION);
			}});
		optionPanel.add(secondsPerFrame);*/

		optionPanel.add(new Label("invert (cross eye)"));
		Checkbox inverter=new Checkbox();
		inverter.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				renderer.setInversion(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(inverter);

		optionPanel.add(new Label("use random offset:"));
		Checkbox randomOffset=new Checkbox();
		randomOffset.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				renderer.setRandomMode(e.getStateChange()==ItemEvent.SELECTED);
			}});
		randomOffset.setState(true);
		optionPanel.add(randomOffset);

		optionPanel.add(new Label("show performance:"));
		Checkbox showFPS=new Checkbox();
		showFPS.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				infoPanel.setVisible(e.getStateChange()==ItemEvent.SELECTED);
				infoPanel.doLayout();
				doLayout();
				infoPanel.doLayout();
			}});
		optionPanel.add(showFPS);

		final Button freeze=new Button("freeze");
		freeze.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				if ("freeze".equals(freeze.getLabel())){
					freeze.setLabel("continue");
					self.suspendRendering();
				} else {
					freeze.setLabel("freeze");
					self.resumeRendering();
				}
			}});
		optionPanel.add(freeze);
		Button close=new Button("close");
		close.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				startPanel.setVisible(false);
				optionPanel.setVisible(false);
				self.requestFocus();
			}});
		optionPanel.add(close);
		
		
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridy=1;
		gbc.weighty=0.5;
		add(startPanel, gbc);
		gbc.gridy=2;
		add(optionPanel, gbc);

		doLayout();
		infoPanel.setVisible(false);		
	}
	
	@Override
	public void init()
	{
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		self=this;

		initGUI();

		scene.width=getSize().width;
		scene.height=getSize().height;
		scene.setBaseURL(getCodeBase());

		renderer=new RendererSoftware();
		renderer.init(this,scene);
		
		timePerFrame=40; 

		setFrameSIRD("squares.png","");
		
			}

	
	@Override
	public void start()
	{
		// Create update thread
		mUpdateThread = new Thread(this);
		// Start update thread
		mUpdateThread.start();
	}

	@Override
	public void stop()
	{
		// Stop update thread
		mUpdateThread = null;
	}

	@Override
	public void destroy()
	{
		// Stop update thread
		mUpdateThread = null;
	}

	private void startSIRDSlet(SIRDSlet let){
		suspendRendering();
		scene.setBaseFontMetric(getGraphics().getFontMetrics(getFont()));
		if (curSIRDSlet!=null) {
			if (curSIRDSlet instanceof KeyListener) removeKeyListener((KeyListener)curSIRDSlet);
			if (curSIRDSlet instanceof MouseListener) removeMouseListener((MouseListener)curSIRDSlet);
			if (curSIRDSlet instanceof MouseMotionListener) removeMouseMotionListener((MouseMotionListener)curSIRDSlet);
			curSIRDSlet.stop();
			curSIRDSlet=null;
		}
		scene.clear();
		startPanel.setVisible(false);
		optionPanel.setVisible(false);
		requestFocus();
		//reset to default
		setShowFloaterCursor(false);
		setAllowSaving(false);
		setAllowSaving(false);
		let.start(this);
		curSIRDSlet=let;
		resumeRendering();
	}
	
	public void suspendRendering(){
		renderLock.lock();
	}
	public void resumeRendering(){
		renderLock.unlock();
	}
	
	private int timePerFrame;
	public void run()
	{
		repaint();
		
		// While update thread is current thread
		//int curTimePerFrameRate=0;
		//int lastFrameTime=System.currentTimeMillis();
		int conSlow=0;
		while (Thread.currentThread() == mUpdateThread)
		{
			long drawstart = System.currentTimeMillis();
			suspendRendering();
			scene.calculateFrame(timePerFrame);
			if (curSIRDSlet!=null) curSIRDSlet.calculateFrame(drawstart);
			renderer.renderFrame();
			resumeRendering();
			mFrameNumber++;

			try
			{
				long s=timePerFrame - (System.currentTimeMillis()-drawstart);
				
				if (s<=0) {
					//drawing to slow
					conSlow+=1;
					if (conSlow>10) timePerFrame++;
					Thread.sleep(1);
				} else {
					Thread.sleep(s);
					conSlow=0;
				}
				long realtime=System.currentTimeMillis()-drawstart;
				if (realtime<=0) realtime=1;
				fpsInfo.setText("FPS:"+(1000/realtime)+" SPF:"+realtime);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	@Override
	public void update(Graphics g)
	{
		paint(g);
	}

	@Override
	public void paint(Graphics g)
	{
		renderer.paint(g);
	}


	@Override
	public String getAppletInfo()
	{
		return "jAbSIRD Demo - by Lewey Geselowitz - lewey@leweyg.com";
	}
	
	public void registerSIRDSlet(SIRDSlet sirdslet){
		sirdslets.add(sirdslet);
	}
	
	
	public void setAllowSaving(boolean allowSaving){
		mAllowSaving=allowSaving;
	}
	public void setAllowLoading(boolean allowLoading){
		mAllowLoading=allowLoading;
	}
	
	public void setShowFloaterCursor(boolean showMouse){
		if (mShowFloaterCursor==showMouse) return;
		mShowFloaterCursor=showMouse;
		if (mShowFloaterCursor) {
			scene.setFloater("~floaterCursor",scene.createFloater("mouse.png"));
			try{
				Cursor c = getToolkit().createCustomCursor(
					new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
					new Point(1, 1), "Custom Cursor");
				setCursor(c);//new Cursor(Cursor.CUSTOM_CURSOR ));//we draw our own	
			}catch (java.lang.IndexOutOfBoundsException e){
				System.out.println("Couldn't set cursor (error catched):"+e);
			}
		} else {
			scene.removeFloater("~floaterCursor");
		}
	}

	public void setFloaterCursorZ(int mouseZ){
		if (!mShowFloaterCursor) return;
		Floater f = scene.getFloater("~floaterCursor");
		//don't check, cursor movements in higher level could be useful
//		if (mouseZ<0) mouseZ=0;
//		if (mouseZ>ZDraw.MAXZ) mouseZ=ZDraw.MAXZ;
		f.z=mouseZ;
	}
	public int getFloaterCursorZ(){
		if (!mShowFloaterCursor) return -1;
		Floater f = scene.getFloater("~floaterCursor");
		if (f==null) return -1;
		return f.z;
	}
	public Floater getFloaterCursor(){
		if (!mShowFloaterCursor) return null;
		return scene.getFloater("~floaterCursor");
	}
	
	public SceneManager getSceneManager() {
		return scene;
	}


	//event cache to prevent thread problems
	//(event listener are not called in the render thread, so it can't be
	//passed directly to the sirdlet)
	//This implementation is not thread-safe, therefore it could lose input
	//events on slow computers, but an lost input event is not really problematic
	//Frameratedrops due to event flooding and synchronization would be worse
	private int mMouseX, mMouseY;
	private boolean keyState[]=new boolean[256];
	private int keyStateJustChanged[]=new int[256];
	private boolean mouseState[]=new boolean[MouseEvent.BUTTON3+1];
	private int mouseStateJustChanged[]=new int[MouseEvent.BUTTON3+1];


	public void keyPressed(KeyEvent e){
		if (e.getKeyCode()==KeyEvent.VK_ESCAPE) {
			requestFocus();
			optionPanel.setVisible(!optionPanel.isVisible());
			startPanel.setVisible(optionPanel.isVisible());
		} else if (e.getKeyCode()==KeyEvent.VK_O && (e.getModifiers() & InputEvent.CTRL_MASK)!=0 && mAllowLoading) {
			javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
			int returnVal = chooser.showOpenDialog(this);
			if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
				int w=scene.width, h=scene.height;
				try {
				BufferedImage img = ImageIO.read(chooser.getSelectedFile());
				int [] temp= new int[w*h];
				if (img.getWidth()>=w && img.getHeight()>=h) {
					temp=img.getRGB(0,0,w,h,null, 0, img.getWidth());
				} else {
					ZDraw zb=((RendererSoftware)(renderer)).getDirectZBufferAccess();
					zb.drawHeightMapTo(temp);
					int minwidth=Math.min(w,img.getWidth());
					int minheight=Math.min(h,img.getHeight());
					int []temp2=img.getRGB(0,0,minwidth,minheight,null, 0, img.getWidth());
					int indentx=(w-minwidth)/2;
					int indenty=(h-minheight)/2;
					for (int y=0;y<minheight;y++){
						int b1=zb.getLineIndex(indenty+y);
						int b2=y*minwidth;
						for (int x=0;x<minwidth;x++)
							temp[b1+x+indentx]=temp2[b2+x];
					}
				}
				((RendererSoftware)(renderer)).getDirectZBufferAccess().readHeightMapFrom(temp);
				} catch (IOException ex){
				}
			}
		}else if (e.getKeyCode()==KeyEvent.VK_S && (e.getModifiers() & InputEvent.CTRL_MASK)!=0 && mAllowSaving) {
			javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
			//chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("png", "png"));
			int returnVal = chooser.showSaveDialog(this);
			if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
				try {
					int w=scene.width; int h=scene.height;
					int [] temp= new int[w*h];
					((RendererSoftware)(renderer)).getDirectZBufferAccess().drawHeightMapTo(temp);
					BufferedImage img=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
					img.setRGB(0,0,w,h, temp, 0, img.getWidth());
					ImageIO.write(img, "png", chooser.getSelectedFile());
				} catch (IOException ex){
				}
			}
		} else if (e.getKeyCode()==KeyEvent.VK_ENTER){
			renderer.setDrawSIRDS(!renderer.getDrawSIRDS());
		}

		if (e.getKeyCode()>=0 && e.getKeyCode()<keyState.length){
			keyState[e.getKeyCode()]=true;
			keyStateJustChanged[e.getKeyCode()]=2;
		}

	}
	public void keyReleased(KeyEvent e){
		if (e.getKeyCode()>=0 && e.getKeyCode()<keyState.length){
			keyState[e.getKeyCode()]=false;
			keyStateJustChanged[e.getKeyCode()]=2;
		}
	}
	public void keyTyped(KeyEvent e){
	}


	public void mouseMoved(MouseEvent e)
	{
		mouseDragged(e);
	}

	public void mouseDragged(MouseEvent e)
	{
		mMouseX=e.getX();
		mMouseY=e.getY();

		if (!mShowFloaterCursor) return;
		Floater f = scene.getFloater("~floaterCursor");
		f.x=e.getX();
		f.y=e.getY();
	}

	public void mouseClicked(MouseEvent me) {
	}

	public void mousePressed(MouseEvent me) {
		mMouseX=me.getX();
		mMouseY=me.getY();
		if (me.getButton()>=0 && me.getButton()<mouseState.length){
			mouseState[me.getButton()] = true;
			mouseStateJustChanged[me.getButton()] = 2;
		}
	}

	public void mouseReleased(MouseEvent me) {
		mMouseX=me.getX();
		mMouseY=me.getY();
		if (me.getButton()>=0 && me.getButton()<mouseState.length){
			mouseState[me.getButton()] = false;
			mouseStateJustChanged[me.getButton()] = 2;
		}
	}

	public void mouseEntered(MouseEvent me) {
	}

	public void mouseExited(MouseEvent me) {
	}




	public int getMouseX(){
		return mMouseX;
	}
	public int getMouseY(){
		return mMouseY;
	}
	//these getters have side-effects (c-like)
	//You can write isKeyPressedChanged() returns if the value of the previous isKeyPressed
	//call is different than the value of the pre-previous isKeyPressed call
	public boolean isKeyPressed(int vk){
		if (vk<0 || vk>keyState.length) return false;
		keyStateJustChanged[vk]--;
		return keyState[vk];
	}
	public boolean isMousePressed(int mb){
		if (mb<0 || mb>mouseState.length) return false;
		mouseStateJustChanged[mb]--;
		return mouseState[mb];
	}
	public boolean isKeyPressedChanged(int vk){
		if (vk<0 || vk>keyState.length) return false;
		return keyStateJustChanged[vk]>=1;
	}
	public boolean isMousePressedChanged(int mb){
		if (mb<0 || mb>mouseState.length) return false;
		return mouseStateJustChanged[mb]>=1;
	}
}

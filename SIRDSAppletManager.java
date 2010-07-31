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
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;

class TimedFloater extends Floater{
	int timetolive;  //time until removal in ms
}
		
public class SIRDSAppletManager extends JApplet implements Runnable,  KeyListener, MouseMotionListener, MouseListener
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

	private boolean mPauseScene=true;
	private boolean mAllowLoading=false;
	private boolean mAllowSaving=false;
	private boolean mShowFloaterCursor=false;
	public boolean mSoundEnabled=true;
//	private boolean mFloaterCursorZ=false;
	
	private java.util.concurrent.locks.Lock renderLock=new java.util.concurrent.locks.ReentrantLock();

	private Image mGuiDoubleBuffer;
	private boolean mShowInfos;
	private String mFPS;
	private boolean mNewMenuVisibility;
	private int mMenuAnimation;

	class AudioClipWrapper implements AudioClip{
		private AudioClip mRealClip;

		public AudioClipWrapper(AudioClip realClip){
			mRealClip = realClip;
		}

		public void play() {
			if (mSoundEnabled) mRealClip.play();
		}

		public void loop() {
			if (mSoundEnabled) mRealClip.loop();
		}

		public void stop() {
			mRealClip.stop();
		}

	}

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
		frame1SIRDChoice.setSelectedItem(sird1);
		frame2SIRDChoice.setSelectedItem(sird2);
	}
	JPanel startPanel;
	JPanel optionPanel;
	JComboBox frame1SIRDChoice,frame2SIRDChoice;
	private void initSIRDChoice(JComboBox c){
		c.addItem("white random");
		c.addItem("red random");
		c.addItem("green random");
		c.addItem("blue random");
		c.addItem("yellow random");
		c.addItem("violet random");
		c.addItem("cyan random");
		c.addItem("colored random");
		c.addItem("white random strides");
		c.addItem("colored random strides");
		c.addItem("gold.png");
		c.addItem("tiger.png");
		c.addItem("squares.png");
		c.addItem("black");
		c.addItem("gray");
	}
	private void initGUI(){
		mNewMenuVisibility=true;
		setFocusable(true);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		startPanel=new JPanel();
		startPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		startPanel.setLayout(new GridBagLayout());
		
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JLabel title=new JLabel("Available SIRDSlets:");
		title.setFont(getFont().deriveFont(Font.BOLD));
		startPanel.add(title,gbc);

		gbc.gridx=1;
		JButton copyright=new JButton("\u00a9\u0338");
		copyright.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
			JOptionPane.showMessageDialog(null, "Written by\n" +
				"        Benito van der Zander\n" +
				"        www.benibela.de\n"+
				"        benito@benibela.de\n\n" +
				"Algorithm created by\n" +
				"        Lewey Geselowitz\n" +
				"        www.leweyg.com" +
				"\n\n" +
				"Sounds from:\n" +
				"        amazingsounds.iespana.es\n" +
				"        www.pacdv.com/sound\n" +
				"        diode111", "Copyleft", JOptionPane.INFORMATION_MESSAGE);
		}});
		startPanel.add(copyright, gbc);
	
		//gbc.fill = GridBagConstraints.NONE;
		for (int i=0;i< sirdslets.size();i++){
			gbc.gridy=i+1;
			gbc.gridx=0;
			JButton b = new JButton(sirdslets.get(i).getName());
			final SIRDSlet temp=sirdslets.get(i);
			b.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				startSIRDSlet(temp);
			}});
			startPanel.add(b,gbc);
			
			
			JButton helpButton = new JButton("?");
			helpButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				JOptionPane.showMessageDialog(null, temp.getDescription(), "Help", JOptionPane.INFORMATION_MESSAGE);
			}});
			gbc.gridx=1;
			startPanel.add(helpButton,gbc);
		}
		startPanel.setOpaque(true);
		startPanel.doLayout();
		

		optionPanel=new JPanel();
		optionPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		optionPanel.setLayout(new GridLayout(0,2));

		title=new JLabel("Options:");
		title.setFont(getFont().deriveFont(Font.BOLD));
		optionPanel.add(title);
		optionPanel.add(new JLabel(" "));

		frame1SIRDChoice = new JComboBox();
		frame1SIRDChoice.addItem("!height-map");
		initSIRDChoice(frame1SIRDChoice);
		frame1SIRDChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				setFrameSIRD((String)e.getItem(),mFrame2SIRDId);
			}
		});
		optionPanel.add(new JLabel("first frame:"));
		optionPanel.add(frame1SIRDChoice);

		frame2SIRDChoice = new JComboBox();
		frame2SIRDChoice.addItem("!same as above");
		frame2SIRDChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				setFrameSIRD(mFrame1SIRDId,(String)e.getItem());
			}
		});
		initSIRDChoice(frame2SIRDChoice);
		optionPanel.add(new JLabel("second frame:"));
		optionPanel.add(frame2SIRDChoice);

		/*final TextField secondsPerFrame=new TextField();
		optionPanel.add(new JLabel("seconds/frame:"));
		secondsPerFrame.addTextListener(new TextListener(){
			public  void textValueChanged(TextEvent e){
				try{
					timePerFrame=Integer.parseInt(secondsPerFrame.getText());
				} catch (NumberFormatException ex){
				}
				//javax.swing.JOptionPane.showMessageDialog(null, e.paramString(), "Test Titel", javax.swing.JOptionPane.OK_CANCEL_OPTION);
			}});
		optionPanel.add(secondsPerFrame);*/

		optionPanel.add(new JLabel("invert (cross eye)"));
		JCheckBox inverter=new JCheckBox();
		inverter.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				renderer.setInversion(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(inverter);

		optionPanel.add(new JLabel("use random offset:"));
		JCheckBox randomOffset=new JCheckBox();
		randomOffset.setSelected(true);
		randomOffset.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (renderer!=null)
					renderer.setRandomMode(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(randomOffset);

		optionPanel.add(new JLabel("show performance:"));
		JCheckBox showFPS=new JCheckBox();
		showFPS.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				mShowInfos = e.getStateChange()==ItemEvent.SELECTED;
			}});
		optionPanel.add(showFPS);

		optionPanel.add(new JLabel("sound:"));
		JCheckBox sound=new JCheckBox();
		sound.setSelected(true);
		sound.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				self.mSoundEnabled = e.getStateChange() == ItemEvent.SELECTED;
			}});
		optionPanel.add(sound);

		final JButton freeze=new JButton("freeze");
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
		JButton close=new JButton("close menu");
		close.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				mPauseScene=false;
				mNewMenuVisibility = false;
				self.requestFocus();
			}});
		optionPanel.add(close);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridy=1;
		gbc.weightx=0.1;
		gbc.weighty=0.5;
		getContentPane().add(startPanel, gbc);
		gbc.gridy=2;
		getContentPane().add(optionPanel, gbc);

		getContentPane().doLayout();
		doLayout();
	}
	
	@Override
	public void init()
	{
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		self=this;

		((JPanel)getContentPane()).setDoubleBuffered(false);
		getRootPane().setDoubleBuffered(false); //disable swing double buffer, because the sirds are already double (triple, quadro??) buffered
		
		initGUI();

		scene.width=getSize().width;
		scene.height=getSize().height;
		scene.setBaseURL(getCodeBase());


		mGuiDoubleBuffer = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(scene.width, scene.height);


		ZSprite logo = scene.setZSprite("logo",scene.createZSprite("logo.png"));
		logo.x=(scene.width-logo.w)/2+30;
		logo.y=(scene.height-logo.h)/2-30;
		logo.z=0;

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
		mPauseScene=false;
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
		long totalTime=0;
		while (Thread.currentThread() == mUpdateThread)
		{
			long drawstart = System.currentTimeMillis();
			suspendRendering();
			if  (!mPauseScene){
				totalTime+=timePerFrame;
				scene.calculateFrame(timePerFrame);
				if (curSIRDSlet!=null) curSIRDSlet.calculateFrame(totalTime);
			}
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
				mFPS = ("FPS:"+(1000/realtime)+" SPF:"+realtime);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	@Override
	public void update(Graphics g)
	{
		//super.update(g);
		//renderer.paint(getContentPane().getGraphics());
	}

	@Override
	public void paint(Graphics g)
	{
		if (mNewMenuVisibility || optionPanel.isVisible() ) {
			if (mNewMenuVisibility) {
				if (!optionPanel.isVisible()){
					optionPanel.setVisible(true);
					startPanel.setVisible(true);
				}
				mMenuAnimation -= 15;
				if (mMenuAnimation < 0)
					mMenuAnimation = 0;
			} else {
				mMenuAnimation += 15;
				if (mMenuAnimation > 500) {
					startPanel.setVisible(false);
					optionPanel.setVisible(false);
					repaint(); //avoid flicker (setVisible(false) shows the panel before it hides it)
				}
			}
		}
		if (optionPanel.isVisible()) {
			Graphics g2 = mGuiDoubleBuffer.getGraphics();
			g2.drawImage(renderer.getBackBuffer(), 0, 0, this);

			g2.translate(optionPanel.getX() - mMenuAnimation, optionPanel.getY());
			optionPanel.paint(g2);
			g2.translate(-optionPanel.getX() + mMenuAnimation, -optionPanel.getY());

			g2.translate(startPanel.getX() + mMenuAnimation, startPanel.getY());
			startPanel.paint(g2);
			g2.translate(-startPanel.getX() - mMenuAnimation, -startPanel.getY());


			if (mShowInfos)
				g2.drawString(mFPS, 15, 25);
			
			g.drawImage(mGuiDoubleBuffer, 0, 0, this);
		} else {
			g.drawImage(renderer.getBackBuffer(), 0, 0, this);
			if (mShowInfos)
				g.drawString(mFPS, 15, 25);
		}
	}
	/*
	@Override
	public void paintComponent(Graphics g){
		JPanel x;
		x.get
		Graphics g2 = renderer.getBackBuffer().getGraphics();


		g2.translate(optionPanel.getX(), optionPanel.getY());
		optionPanel.paint(g2);
		g.drawImage(renderer.getBackBuffer(), 0, 0, this);
	}*/


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
			mNewMenuVisibility = !mNewMenuVisibility;
			getContentPane().doLayout();
			repaint();
			mPauseScene=mNewMenuVisibility;
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
		//set focus to the current window because it is not
		//automatically given if there is no awt widget 
		requestFocus();
		requestFocusInWindow();
		requestFocus();

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
	public boolean isKeyPressedOnce(int vk){
		return isKeyPressed(vk) && isKeyPressedChanged(vk);
	}



	//============Sound================
	public AudioClip getAudioClip(String filename){
//		System.out.println(scene.getFileURL(filename));
		return new AudioClipWrapper(newAudioClip(scene.getFileURL(filename)));
	}
}

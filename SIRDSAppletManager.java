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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


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
	
	private SIRDSlet curSIRDSlet=null, selectedSIRDSlet=null;
	private int mSelectedOption = 0;
	private ArrayList<JRadioButton> mOptions = new ArrayList<JRadioButton>();
	private ArrayList<SIRDSlet> sirdslets=new ArrayList<SIRDSlet>();

	private boolean mPauseScene=true;
	private boolean mAllowLoading=false;
	private boolean mAllowSaving=false;
	private boolean mShowFloaterCursor=false;
	public boolean mSoundEnabled=true;
//	private boolean mFloaterCursorZ=false;
	
	private java.util.concurrent.locks.Lock renderLock=new java.util.concurrent.locks.ReentrantLock();
	private boolean  renderLockLocked = false;

	private Image mGuiDoubleBuffer;
	private boolean mShowInfos;
	private int mShowInfoCount;
	private String mFPS;

	JPanel startPanel;
	JPanel optionPanel;
	JPanel mSirdletStartPanel;
	JTextArea mSirdsletKeys;
	JLabel mSirdsletDescription;
	JButton mSirdsletStart;
	JComboBox frame1SIRDChoice,frame2SIRDChoice;

	private Translations mTranslator;

	private AudioThread mAudioLoop;

	class AudioClipWrapper implements AudioClip{
		private AudioClip mRealClip;

		public AudioClipWrapper(AudioClip realClip){
			mRealClip = realClip;
		}

		public void play() {
			if (mSoundEnabled) mAudioLoop.play(mRealClip); //mRealClip.play();
		}

		public void loop() {
			if (mSoundEnabled) mRealClip.loop();
		}

		public void stop() {
			mRealClip.stop();
		}

	}

	static class AudioThread implements Runnable{
		private final Deque<AudioClip> clips = new LinkedList<AudioClip>();
		public void play(AudioClip clip){
			synchronized (clips) {
				clips.add(clip);
			}
			synchronized(this) { notify(); }
		}
		public void run() {
			try{
			while (true){
				synchronized(this) { wait(); }
				synchronized (clips){
					for (AudioClip clip: clips) clip.play();
					clips.clear();
				}

			}
			} catch (InterruptedException e){
				System.out.println("sound thread failed");
			}
		}
	}

	//sird id: (.*)\.png => load (\1).png
	//          .*random.* => random (checks for "color" and "strid")
	//          color (black,gray) => single color
	//			else => "" => (in 1 => heightmap, in 2 => = 1)
	//see initSIRDChoice
	private int str2Color(String color){
		if (mTranslator.white().equals(color)) return 0xFFffffff;
		if (mTranslator.gray().equals(color)) return 0xFF888888;
		if (mTranslator.black().equals(color)) return 0xFF000000;
		if (mTranslator.red().equals(color)) return 0xFFff0000;
		if (mTranslator.green().equals(color)) return 0xFF00ff00;
		if (mTranslator.blue().equals(color)) return 0xFF0000ff;
		if (mTranslator.yellow().equals(color)) return 0xFFffff00;
		if (mTranslator.violet().equals(color)) return 0xFFff00ff;
		if (mTranslator.cyan().equals(color)) return 0xFF00ffff;
		return 0;
	}
	private int[] singleSIRDData(String id){
		if (id.endsWith(".png")) 
			return ZDraw.GetImageSIRDData(scene.loadImage(id));
		if (id.contains(mTranslator.random())){
			int c = str2Color(id.substring(0,id.indexOf(" ")));
			return ZDraw.GetRandSIRDData(id.contains(mTranslator.colored()), id.contains(mTranslator.stripes()), c!=0?c:0xffffffff);
		}
		if (id.equals(mTranslator.black())||id.equals(mTranslator.gray())) {
			int[] temp=new int[ZDraw.SIRDW*ZDraw.SIRDH];
			Arrays.fill(temp,id.equals(mTranslator.black())?0xff000000:0xff888888);
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

	private void initSIRDChoice(JComboBox c){
		c.addItem(mTranslator.white() + " "+mTranslator.random());
		c.addItem(mTranslator.red() + " "+ mTranslator.random());
		c.addItem(mTranslator.green() + " "+ mTranslator.random());
		c.addItem(mTranslator.blue() + " "+ mTranslator.random());
		c.addItem(mTranslator.yellow() + " "+ mTranslator.random());
		c.addItem(mTranslator.violet() + " "+ mTranslator.random());
		c.addItem(mTranslator.cyan() + " "+ mTranslator.random());
		c.addItem(mTranslator.colored() + " "+ mTranslator.random());
		c.addItem(mTranslator.white() + " "+ mTranslator.random()+ " "+mTranslator.stripes());
		c.addItem(mTranslator.colored() + " "+ mTranslator.random()+ " "+mTranslator.stripes());
		c.addItem("gold.png");
		c.addItem("tiger.png");
		c.addItem("squares.png");
		c.addItem(mTranslator.black());
		c.addItem(mTranslator.gray());
	}
	private void initGUI(){
		setFocusable(true);
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		startPanel=new JPanel();
		startPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(0, 0, 180), 3));
		startPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		startPanel.setLayout(new GridBagLayout());
		startPanel.setBackground(Color.BLUE);

		gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 2, 2,2);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JLabel title=new JLabel(mTranslator.AvailableSIRDSlets());
		title.setFont(getFont().deriveFont(Font.BOLD,22));
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		//title.setOpaque(true);
		startPanel.add(title,gbc);

		gbc.gridx=1;
		/*JButton copyright=new JButton("\u00a9\u0338");
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
		startPanel.add(copyright, gbc);*/
	
		//gbc.fill = GridBagConstraints.NONE;
		for (int i=0;i< sirdslets.size();i++){
			gbc.gridy=i+1;
			gbc.gridx=0;
			JButton b = new JButton(sirdslets.get(i).getName());
			final SIRDSlet temp=sirdslets.get(i);
			b.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				//startSIRDSlet(temp);
				selectSIRDSlet(temp);

			}});
			startPanel.add(b,gbc);
			
			/*
			JButton helpButton = new JButton("?");
			helpButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				JOptionPane.showMessageDialog(null, temp.getDescription(), "Help", JOptionPane.INFORMATION_MESSAGE);
			}});
			gbc.gridx=1;
			startPanel.add(helpButton,gbc);*/
		}
		startPanel.setOpaque(true);
		startPanel.doLayout();
		

		optionPanel=new JPanel();
		optionPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(0,190,190), 3));
		Color optionPanelLabelColor = Color.BLACK;
		optionPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		GridLayout lay = new GridLayout(0,2);
		lay.setHgap(1); lay.setVgap(1);
		optionPanel.setLayout(lay);
		optionPanel.setBackground(Color.CYAN);
		
		title=new JLabel(mTranslator.Options());
		title.setFont(getFont().deriveFont(Font.BOLD,16));
		title.setForeground(optionPanelLabelColor);
		optionPanel.add(title);
		optionPanel.add(new JLabel(" "));

		frame1SIRDChoice = new JComboBox();
		frame1SIRDChoice.setLightWeightPopupEnabled(false);
		frame1SIRDChoice.addItem("!"+mTranslator.heightmap());
		initSIRDChoice(frame1SIRDChoice);
		frame1SIRDChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				setFrameSIRD((String)e.getItem(),mFrame2SIRDId);
			}
		});
		JLabel label = new JLabel(mTranslator.firstFrame());
		label.setForeground(optionPanelLabelColor);
		optionPanel.add(label);
		optionPanel.add(frame1SIRDChoice);

		frame2SIRDChoice = new JComboBox();
		frame2SIRDChoice.setLightWeightPopupEnabled(false);
		frame2SIRDChoice.addItem("!"+mTranslator.sameAsAbove());
		frame2SIRDChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				setFrameSIRD(mFrame1SIRDId,(String)e.getItem());
			}
		});
		initSIRDChoice(frame2SIRDChoice);
		label = new JLabel(mTranslator.secondFrame());
		label.setForeground(optionPanelLabelColor);
		optionPanel.add(label);
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

		label = new JLabel(mTranslator.invertCE());
		label.setForeground(optionPanelLabelColor);
		optionPanel.add(label);
		JCheckBox inverter=new JCheckBox();
		inverter.setOpaque(false);
		inverter.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				renderer.setInversion(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(inverter);

		label = new JLabel(mTranslator.useRandomOffset());
		label.setForeground(optionPanelLabelColor);
		optionPanel.add(label);
		JCheckBox randomOffset=new JCheckBox();
		randomOffset.setOpaque(false);
		randomOffset.setSelected(true);
		randomOffset.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (renderer!=null)
					renderer.setRandomMode(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(randomOffset);

		label = new JLabel(mTranslator.setFrameRate());
		label.setForeground(optionPanelLabelColor);
		optionPanel.add(label);
		/*JCheckBox showFPS=new JCheckBox();
		showFPS.setOpaque(false);
		showFPS.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				mShowInfos = e.getStateChange()==ItemEvent.SELECTED;
			}});
		optionPanel.add(showFPS);*/
		final JSpinner fps = new JSpinner();
		fps.setValue(1000/timePerFrame);
		fps.setOpaque(false);
		fps.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				timePerFrame = 1000/(Integer)(fps.getValue());
				mShowInfos = true;
				mShowInfoCount = 500;
			}
		});
		optionPanel.add(fps);

		label = new JLabel(mTranslator.sound());
		label.setForeground(optionPanelLabelColor);
		optionPanel.add(label);
		JCheckBox sound=new JCheckBox();
		sound.setOpaque(false);
		sound.setSelected(true);
		sound.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				self.mSoundEnabled = e.getStateChange() == ItemEvent.SELECTED;
			}});
		optionPanel.add(sound);

		final JButton freeze=new JButton(mTranslator.freeze());
		freeze.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				if (mTranslator.freeze().equals(freeze.getLabel())){
					freeze.setLabel(mTranslator.continu());
					self.suspendRendering();
					renderLockLocked = true;
				} else {
					freeze.setLabel(mTranslator.freeze());
					self.resumeRendering();
					renderLockLocked = false;
				}
			}});
		optionPanel.add(freeze);
		JButton close=new JButton(mTranslator.closeMenu());
		close.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				animatedSetMenuVisible(false);
			}});
		optionPanel.add(close);


		mSirdletStartPanel = new JPanel();
		mSirdletStartPanel.setBorder(javax.swing.BorderFactory.createLineBorder(Color.ORANGE, 3));
		mSirdletStartPanel.setBackground(Color.YELLOW);

		mSirdsletDescription = new JLabel("desc");
		mSirdsletDescription.setForeground(Color.BLACK);
		mSirdsletKeys = new JTextArea();
		mSirdsletKeys.setOpaque(false);
		//mSirdsletKeys.setBackground(mSirdsletDescription.getBackground());
		//mSirdsletKeys.setForeground(mSirdsletDescription.getForeground());
		mSirdsletKeys.setForeground(Color.BLACK);
		mSirdsletKeys.setEditable(false);
		mSirdsletKeys.setTabSize(6);
		mSirdsletStart = new JButton(mTranslator.start());
		mSirdsletStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startSIRDSlet(selectedSIRDSlet);
			}
		});
		mSirdletStartPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		mSirdletStartPanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridy = 0;
		gbc.insets.top = 3;
		gbc.insets.bottom = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.5;
		mSirdletStartPanel.add(mSirdsletDescription,gbc);
		gbc.gridy = 1;
		gbc.weightx = 0.7;
		mSirdletStartPanel.add(mSirdsletKeys,gbc);
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridy = 3;
		mSirdletStartPanel.add(mSirdsletStart,gbc);


		mSirdletStartPanel.setVisible(false);

		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridy=1;
		gbc.weightx=0.1;
		gbc.weighty=0.5;
		getContentPane().add(startPanel, gbc);
		gbc.gridy=2;
		getContentPane().add(mSirdletStartPanel, gbc);
		gbc.gridy=3;
		getContentPane().add(optionPanel, gbc);

		getContentPane().doLayout();
		doLayout();
	}
	
	@Override
	public void init()
	{
		mTranslator = Translations.getInstance();
		timePerFrame=20;

		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		self=this;

		((JPanel)getContentPane()).setDoubleBuffered(false);
		getRootPane().setDoubleBuffered(false); //disable swing double buffer, because the sirds are already double (triple, quadro??) buffered
		
		initGUI();
		mAudioLoop = new AudioThread();
		(new Thread(mAudioLoop)).start();

		scene.width=getSize().width;
		scene.height=getSize().height;
		//scene.setBaseURL(getCodeBase());


		mGuiDoubleBuffer = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(scene.width, scene.height);


		ZSprite logo = scene.setZSprite("logo",scene.createZSprite("logo.png"));
		logo.x=(scene.width-logo.w)/2+30;
		logo.y=(scene.height-logo.h)/2-30;
		logo.z=0;

		renderer=new RendererSoftware();
		renderer.init(this,scene);
		

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

	private void selectSIRDSlet(SIRDSlet let){
		 selectedSIRDSlet = let;
		 mSirdsletDescription.setText("<html>"+let.getDescription().replace("\n", "<br>")+"</html>");
		 String keys = let.getKeys()+"\n"+mTranslator.enterSwitches();
		 Pattern p = Pattern.compile("^[^\t]*\t",Pattern.MULTILINE);
		 Matcher m = p.matcher(keys);
		 int max = 0;
		 while (m.find()) max = Math.max(max, m.group().length());
		 mSirdsletKeys.setTabSize(max/2+5);
		 mSirdsletKeys.setText(keys); //there is a swing bug that setting the tab size doesn't update the size

		 String [] options = selectedSIRDSlet.getPossibleOptions();
		 GridBagConstraints gbc = new GridBagConstraints();
		 gbc.gridy = 2;
		 gbc.gridx = 0;
		 gbc.weightx = 0.5;
		 for (JRadioButton cb: mOptions)
			 mSirdletStartPanel.remove(cb);
		 mOptions.clear();
		 ButtonGroup b = new ButtonGroup();
		 int id = 0;
		 for (String s: options) {
			JRadioButton cb = new JRadioButton(s);
			cb.setOpaque(false);
			final int wtfid = id;
			if (id == selectedSIRDSlet.getDefaultOption())
				 cb.setSelected(true);
			cb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					mSelectedOption = wtfid;
				}
			});
			b.add(cb);
			mOptions.add(cb);
			mSirdletStartPanel.add(cb, gbc);
			gbc.gridx += 1;
			id += 1;
		 }
		 mSelectedOption = selectedSIRDSlet.getDefaultOption();
		/*gbc.gridwidth=1;
		gbc.gridy = 2;
		gbc.gridx = 0;
		mSirdletStartPanel.add(new JCheckBox("very easy"), gbc);
		gbc.gridx = 1;
		mSirdletStartPanel.add(new JCheckBox("easy"), gbc);
		gbc.gridx = 2;
		mSirdletStartPanel.add(new JCheckBox("normal"), gbc);
		gbc.gridx = 3;
		mSirdletStartPanel.add(new JCheckBox("hard"), gbc);
		gbc.gridx = 4;
		mSirdletStartPanel.add(new JCheckBox("impossible"), gbc);*/

		 mSirdletStartPanel.doLayout();
		 animationDoLayout();
		 animatedSetVisible(mSirdletStartPanel, true);
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
		animatedSetVisible(startPanel, false);
		animatedSetVisible(mSirdletStartPanel, false);
		animatedSetVisible(optionPanel, false);
		requestFocus();
		//reset to default
		setShowFloaterCursor(false);
		setAllowSaving(false);
		setAllowSaving(false);
		let.start(this, mSelectedOption);
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
		long realTimeDelta = timePerFrame;

		AccurateTiming timing = new AccurateTiming();

		while (Thread.currentThread() == mUpdateThread)
		{
			//System.out.println(realTimeDelta);
			long drawstart = timing.getTiming();
			suspendRendering();
			if  (!mPauseScene){
				totalTime+=timePerFrame;
				scene.calculateFrame(realTimeDelta);
				if (curSIRDSlet!=null) curSIRDSlet.calculateFrame(drawstart);
			}
			renderer.renderFrame();
			resumeRendering();
			mFrameNumber++;
			
			long s=timePerFrame - (timing.getTiming()-drawstart);
			//System.out.println("sleep:"+s);
			if (s<=0) {
				//drawing to slow
				conSlow+=1;
				if (conSlow>10) timePerFrame++;
			} else {
				conSlow=0;
				timing.threadSleep(s);
			}
			long realtime=timing.getTiming()-drawstart;
			if (realtime<=0) realtime=1;
			mFPS = ("FPS:"+(1000/realtime)+" SPF:"+realtime);

			realTimeDelta = timing.getTiming() - drawstart;
		}
	}

	@Override
	public void update(Graphics g)
	{
		//super.update(g);
		//renderer.paint(getContentPane().getGraphics());
	}

	private int frameLogId = 0;

	@Override
	public void paint(Graphics g)
	{
		if (!renderLockLocked) {
			boolean visibilityChanged = false;
			for (Map.Entry<Component, ComponentAnimation> e: mComponentAnimations.entrySet()){
				ComponentAnimation anim = e.getValue();
				if (anim.destX != anim.x || anim.destY != anim.y) {
					if (anim.x < anim.destX) anim.x = Math.min(anim.destX, anim.x + 20);
					else if (anim.x > anim.destX) anim.x = Math.max(anim.destX, anim.x - 20);
					if (anim.y < anim.destY) anim.y = Math.min(anim.destY, anim.y + 10);
					else if (anim.y > anim.destY) anim.y = Math.max(anim.destY, anim.y - 10);
					if (anim.x == anim.destX && anim.y == anim.destY){
						e.getKey().setVisible(anim.visible);
						visibilityChanged = true;
					}
				}
			}
			if (visibilityChanged) animationDoLayout();
		}
		if (optionPanel.isVisible() || mSirdletStartPanel.isVisible()) {
			Graphics g2 = mGuiDoubleBuffer.getGraphics();
			g2.drawImage(renderer.getBackBuffer(), 0, 0, this);

			for (Component c: getContentPane().getComponents()){
				int x; int y;
				if (!c.isVisible()) continue;
				if (mComponentAnimations.containsKey(c)) {
					x = mComponentAnimations.get(c).x;
					y = mComponentAnimations.get(c).y;
				} else {
					x = c.getX();
					y = c.getY();
				}
				g2.translate(x,y);
				c.paint(g2);
				g2.translate(-x,-y);
			}

			if (mShowInfos) {
				g2.drawString(mFPS, 15, 25);
			}
			
			g.drawImage(mGuiDoubleBuffer, 0, 0, this);
		} else {
			g.drawImage(renderer.getBackBuffer(), 0, 0, this);
			if (mShowInfos){
				g.drawString(mFPS, 15, 25);
			}
		}

		if (mShowInfos) {
			mShowInfoCount-=1;
			if (mShowInfoCount<=0) mShowInfos=false;
		}
/*			Graphics g2 = mGuiDoubleBuffer.getGraphics();
			g2.drawImage(renderer.getBackBuffer(), 0, 0, this);
		}
		try{
			ImageIO.write((BufferedImage)mGuiDoubleBuffer, "png", new File("/tmp/sirdslets/framelog"+frameLogId+".png"));
		} catch (IOException e){}
		frameLogId++;*/
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
		return "SIRDS by Benito van der Zander";
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


	private static class ComponentAnimation {
		int x, y;
		int destX, destY;
		boolean visible;
		int progress;
	}
	private Map<Component, ComponentAnimation> mComponentAnimations = new HashMap<Component, ComponentAnimation>();

	void animatedSetVisible(Component c, boolean vis){
		ComponentAnimation anim = mComponentAnimations.get(c);
		if (anim == null && vis == c.isVisible())
			return;
		if (anim != null && vis == anim.visible)
			return;
		c.setVisible(true);
		getContentPane().doLayout();
		if (anim == null) {
			anim = new ComponentAnimation();
			anim.x = c.getX();
			if (c==mSirdletStartPanel) anim.x += 400;
			anim.y = c.getY();
			anim.progress = 0;
			anim.visible = c.isVisible();
			mComponentAnimations.put(c, anim);
		}

		anim.visible = vis;
		if (vis) {
			anim.destX = c.getX();
			anim.destY = c.getY();
		} else {
			anim.destX = c.getX();
			anim.destY = c.getY();
			if (c==startPanel) anim.destY -= 200;
			else if (c==optionPanel) anim.destY += 300;	
			else if (c==mSirdletStartPanel) anim.destX = 600;
		}
	}

	boolean animatedIsVisible(Component c){
		ComponentAnimation anim = mComponentAnimations.get(c);
		if (anim == null) return c.isVisible();
		return anim.visible;
	}

	void animatedSetMenuVisible(boolean vis){
		requestFocus();
		animatedSetVisible(mSirdletStartPanel, false);
		animatedSetVisible(startPanel, vis);
		animatedSetVisible(optionPanel, vis);
		getContentPane().doLayout();
		repaint();
		mPauseScene=vis;
	}

	private void animationDoLayout() {
		getContentPane().doLayout();
		for (Map.Entry<Component, ComponentAnimation> e: mComponentAnimations.entrySet()){
			if (e.getValue().visible) { //update position after layout change
				e.getValue().destX = e.getKey().getX();
				e.getValue().destY = e.getKey().getY();
			}
		}
		repaint();
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
			animatedSetMenuVisible(!animatedIsVisible(startPanel));
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
			renderer.setDrawMode((renderer.getDrawMode()+1)%3);
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

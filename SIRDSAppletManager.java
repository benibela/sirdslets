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
import java.net.*;

		
public class SIRDSAppletManager extends Applet implements Runnable,  KeyListener
{
	private Thread mUpdateThread;
	private Image mBackBuffer;
	private ZDraw mZBuffer;
	private int[] mRandData1, mRandData2;
	private ZDraw mSIRDPixels;
	private MemoryImageSource mSIRDImage;
	private int mWidth, mHeight;
	private int mFrameNumber = 0;
	private boolean mUseSIRD = true;
	private SIRDSAppletManager self; //=this, but also usable in anonymous sub classes
	
	private ArrayList<SIRDSlet> sirdslets=new ArrayList<SIRDSlet>();
		
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
			return ZDraw.GetImageSIRDData(getCodeBase(), id); 
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
		mRandData1=singleSIRDData(sird1);
		mUseSIRD=mRandData1!=null;
		mRandData2=singleSIRDData(sird2);
		if (mRandData2==null) mRandData2=mRandData1;
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
		startPanel.setLayout(new GridLayout(0,1));
		
		
		Label title=new Label("Available SIRDSlets:");
		title.setFont(getFont().deriveFont(Font.BOLD));
		startPanel.add(title);
		
		for (int i=0;i< sirdslets.size();i++){
			Button b = new Button(sirdslets.get(i).getSIRDletName());
			final SIRDSlet temp=sirdslets.get(i);
			b.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				startPanel.setVisible(false);
				optionPanel.setVisible(false);
				self.requestFocus();
				temp.start(self);
			}});
			startPanel.add(b);
		}

		

		optionPanel=new Panel();
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

		final TextField secondsPerFrame=new TextField();
		optionPanel.add(new Label("seconds/frame:"));
		secondsPerFrame.addTextListener(new TextListener(){
			public  void textValueChanged(TextEvent e){
				try{
					timePerFrame=Integer.parseInt(secondsPerFrame.getText());
				} catch (NumberFormatException ex){
				}
				//javax.swing.JOptionPane.showMessageDialog(null, e.paramString(), "Test Titel", javax.swing.JOptionPane.OK_CANCEL_OPTION);
			}});
		optionPanel.add(secondsPerFrame);

		optionPanel.add(new Label("show performance:"));
		Checkbox showFPS=new Checkbox();
		showFPS.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				infoPanel.setVisible(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(showFPS);

		optionPanel.add(new Label(""));
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
	
	public void init()
	{
		addKeyListener(this);
		
		self=this;

		initGUI();
		
		mWidth  = getSize().width;
		mHeight = getSize().height;

		mSIRDPixels     = new ZDraw(mWidth, mHeight);		
		
		mSIRDImage = new MemoryImageSource(mWidth, mHeight, mSIRDPixels.data, 0, mWidth);
		mSIRDImage.setAnimated(true);
		mSIRDImage.setFullBufferUpdates(true);
		mBackBuffer = createImage(mSIRDImage);
				
		mZBuffer = new ZDraw(mWidth, mHeight);
		mZBuffer.Clear();
		
		//mRandData = ZDraw.GetRandSIRDData();
		setFrameSIRD("squares.png","");
		
		timePerFrame=40; 
		
	}

	
	public void start()
	{
		// Create update thread
		mUpdateThread = new Thread(this);
		// Start update thread
		mUpdateThread.start();
	}

	public void stop()
	{
		// Stop update thread
		mUpdateThread = null;
	}

	public void destroy()
	{
		// Stop update thread
		mUpdateThread = null;
	}

	public void updateSIRDImage()
	{
		if (!mUseSIRD) mZBuffer.DrawHeightMapTo(mSIRDPixels.data);
		else if ((mFrameNumber &1)!=0) mZBuffer.DrawSIRD(mSIRDPixels.data, mRandData2, mFrameNumber);
		else mZBuffer.DrawSIRD(mSIRDPixels.data, mRandData1, mFrameNumber);
		
		mSIRDImage.newPixels();
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
			updateSIRDImage();
			mFrameNumber++;
			getToolkit().sync();
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

	public void update(Graphics g)
	{
		paint(g);
	}

	public void paint(Graphics g)
	{
		g.drawImage(mBackBuffer, 0, 0, this);
	}

	public void keyPressed(KeyEvent e){
		if (e.getKeyCode()==KeyEvent.VK_ESCAPE) {
			requestFocus();
			optionPanel.setVisible(!optionPanel.isVisible());
			startPanel.setVisible(optionPanel.isVisible());
		}
	}
	public void keyReleased(KeyEvent e){
	}
	public void keyTyped(KeyEvent e){
	}

	public String getAppletInfo()
	{
		return "jAbSIRD Demo - by Lewey Geselowitz - lewey@leweyg.com";
	}
	
	public void registerSIRDSlet(SIRDSlet sirdslet){
		sirdslets.add(sirdslet);
	}
	
	public ZDraw getZBuffer(){
		return mZBuffer;
	}
}
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

		
public class SIRDSAppletManager extends Applet implements Runnable,  KeyListener
{
	private Thread mUpdateThread;
	private Image mBackBuffer;
	private ZDraw mZBuffer, mZBuffer2;
	private int[] mRandData1, mRandData2;
	private ZDraw mSIRDPixels;
	private MemoryImageSource mSIRDImage;
	private int mWidth, mHeight;
	private int mFrameNumber = 0;
	private boolean mUseSIRD = true;
	private SIRDSAppletManager self; //=this, but also usable in anonymous sub classes
	
	private SIRDSlet curSIRDSlet=null;
	private ArrayList<SIRDSlet> sirdslets=new ArrayList<SIRDSlet>();

	private boolean mAllowLoading=false;
	private boolean mAllowSaving=false;
	private boolean mUseZBuffer2=false;
	private boolean mRandomOffset=false;
	
	protected ArrayList<Floater> floaters = new ArrayList<Floater>();
	protected TreeMap<String,Floater> namedFloaters = new TreeMap<String,Floater>();
	protected TreeMap<String,ZSprite> namedSprites = new TreeMap<String,ZSprite>();
		
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
			return ZDraw.GetImageSIRDData(loadImage(id)); 
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

		optionPanel.add(new Label("use random offset:"));
		Checkbox randomOffset=new Checkbox();
		randomOffset.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				self.mRandomOffset=(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(randomOffset);

		optionPanel.add(new Label("show performance:"));
		Checkbox showFPS=new Checkbox();
		showFPS.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				infoPanel.setVisible(e.getStateChange()==ItemEvent.SELECTED);
			}});
		optionPanel.add(showFPS);

		final Button freeze=new Button("freeze");
		freeze.addActionListener(new ActionListener(){ 
			public void actionPerformed(ActionEvent e){
				if (freeze.getLabel()=="freeze"){
					freeze.setLabel("continue");
					self.mUpdateThread.suspend();
				} else {
					freeze.setLabel("freeze");
					self.mUpdateThread.resume();
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
		mZBuffer.clear();
		
		timePerFrame=40; 

		//mRandData = ZDraw.GetRandSIRDData();
		setFrameSIRD("squares.png","");
		
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

	private void startSIRDSlet(SIRDSlet let){
		self.mUpdateThread.suspend();
		if (curSIRDSlet!=null) {
			curSIRDSlet.stop();
			curSIRDSlet=null;
		}
		mZBuffer.clear();
		startPanel.setVisible(false);
		optionPanel.setVisible(false);
		self.requestFocus();
		self.floaters.clear();
		self.namedFloaters.clear();
		self.namedSprites.clear();
		let.start(self);
		curSIRDSlet=let;
		self.mUpdateThread.resume();
	}
	
	public void updateSIRDImage()
	{
		if (curSIRDSlet!=null)
			curSIRDSlet.paintFrame();
		if (mUseZBuffer2) {
			mZBuffer.forceCopyDataFrom(0,0,mZBuffer2,0,0,mZBuffer2.w,mZBuffer2.h);
			
			for (Map.Entry<String,ZSprite> sprite: namedSprites.entrySet())
				sprite.getValue().drawTo(mZBuffer);
		}
	
		if (!mUseSIRD) mZBuffer.drawHeightMapTo(mSIRDPixels.data);
		else if ((mFrameNumber &1)!=0) mZBuffer.DrawSIRD(mSIRDPixels.data, mRandData2, mRandomOffset);
		else mZBuffer.DrawSIRD(mSIRDPixels.data, mRandData1, mRandomOffset);
		
		for (Floater f: floaters){	
			f.draw(mSIRDPixels.data, mZBuffer);
		}
		
		Iterator<Map.Entry<String,Floater>> it = namedFloaters.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,Floater> pairs = it.next();
			pairs.getValue().draw(mSIRDPixels.data, mZBuffer);
		}

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
			if (curSIRDSlet!=null) curSIRDSlet.calculateFrame();
			updateSIRDImage();
			mFrameNumber++;
			//floaters.get(0).z=(floaters.get(0).z+1)%ZDraw.MAXZ;
			//floaters.get(0).x=(floaters.get(0).x+1)%mWidth;
			//floaters.get(0).y=(floaters.get(0).y+1)%mHeight;
			//repaint();
//			getToolkit().sync();
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
		} else if (e.getKeyCode()==KeyEvent.VK_O && (e.getModifiers() & InputEvent.CTRL_MASK)!=0 && mAllowLoading) {
			javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
			int returnVal = chooser.showOpenDialog(this);
			if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
				try {
				BufferedImage img = ImageIO.read(chooser.getSelectedFile());
				int [] temp= new int[mWidth*mHeight]; 
				if (img.getWidth()>=mWidth && img.getHeight()>=mWidth) {
					temp=img.getRGB(0,0,mWidth,mHeight,null, 0, img.getWidth());
				} else {
					mZBuffer.drawHeightMapTo(temp);
					int minwidth=Math.min(mWidth,img.getWidth());
					int minheight=Math.min(mHeight,img.getHeight());
					int []temp2=img.getRGB(0,0,minwidth,minheight,null, 0, img.getWidth());
					int indentx=(mWidth-minwidth)/2;
					int indenty=(mHeight-minheight)/2;
					for (int y=0;y<minheight;y++){
						int b1=mZBuffer.getLineIndex(indenty+y);
						int b2=y*minwidth;
						for (int x=0;x<minwidth;x++)
							temp[b1+x+indentx]=temp2[b2+x];
					}
				}
				mZBuffer.readHeightMapFrom(temp);
				} catch (IOException ex){
				}
			}
		}else if (e.getKeyCode()==KeyEvent.VK_S && (e.getModifiers() & InputEvent.CTRL_MASK)!=0 && mAllowSaving) {
			javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
			//chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("png", "png"));
			int returnVal = chooser.showSaveDialog(this);
			if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
				try {
				int [] temp= new int[mWidth*mHeight]; 
				mZBuffer.drawHeightMapTo(temp);
				BufferedImage img=new BufferedImage(mWidth,mHeight,BufferedImage.TYPE_INT_ARGB);
				img.setRGB(0,0,mWidth,mHeight, temp, 0, img.getWidth());
				ImageIO.write(img, "png", chooser.getSelectedFile());
				} catch (IOException ex){
				}
			}
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
	
	
	public void setAllowSaving(boolean allowSaving){
		mAllowSaving=allowSaving;
	}
	public void setAllowLoading(boolean allowLoading){
		mAllowLoading=allowLoading;
	}
	public void setDoubleBufferedZBuffer(boolean useZBuffer2){
		mUseZBuffer2=useZBuffer2;
		if (!mUseZBuffer2) mZBuffer2=null;
		else mZBuffer2=new ZDraw(mZBuffer.w,mZBuffer.h);
	}
	
	//Some management functions
	
	public ZDraw getZBuffer(){
		if (!mUseZBuffer2) return mZBuffer;
		else  return mZBuffer2;
	}
	/*public ZDraw getZBuffer2(){
		return mZBuffer2;
	}*/
	
	public Floater getFloater(int id){
		if (id<0 || id>=floaters.size()) return null;
		return floaters.get(id);
	}
	public Floater setFloater(int id, Floater f){
		if (id>=floaters.size()) floaters.add(f);
		else floaters.set(id,f);
		return f;
	}
	public Floater setFloaterText(int id, String text){
		Floater newText=createTextFloater(text);
		Floater old=getFloater(id);
		if (old==null) return setFloater(id, newText);
		else old.setToFloater(newText);
		return old;
	}
	
	public Floater getFloater(String id){
		return namedFloaters.get(id);
	}
	public Floater setFloater(String id, Floater f){
		namedFloaters.put(id,f);
		return f;
	}
	public Floater setFloaterText(String id, String text){
		Floater newText=createTextFloater(text);
		Floater old=getFloater(id);
		if (old==null) return setFloater(id, newText);
		else old.setToFloater(newText);
		return old;
	}

	public ZSprite getZSprite(String id){
		return namedSprites.get(id);
	}
	public ZSprite setZSprite(String id, ZSprite s){
		namedSprites.put(id,s);
		return s;
	}
	
	//Graphic Utility Functions
	public int[] loadImageData(String name){
		BufferedImage img = loadImage(name);
		if (img==null) return null;
		return img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth());
	}
	public BufferedImage loadImage(String name){
		return loadImage(getCodeBase(), name);
	}
	public static BufferedImage loadImage(URL baseUrl, String name){
		try {
			return loadImage(new URL(baseUrl,name));
		} catch (java.net.MalformedURLException e){
		}
		return null;
	}
	public static BufferedImage loadImage(URL url){
		BufferedImage img = null;
		try {
			//img = ImageIO.read(new File(fileName));
			img = ImageIO.read(url);
			//URL url = new URL(getCodeBase(), "strawberry.jpg");
		} catch (IOException e) {
		}
		return img;
	}



	public Floater createFloater(String name){
		Floater floater=new Floater();
		BufferedImage img=loadImage(name);
		if (img==null) throw new IllegalArgumentException("couldn't find image: "+name);
		floater.data=img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth());
		floater.w=img.getWidth();
		floater.h=img.getHeight();
		return floater;
	}
	public Floater createTextFloater(String text){
		return createTextFloater(text,Color.BLACK);
	}
	public Floater createTextFloater(String text, Color c){
		Floater floater=new Floater();
		floater.setToString(text,getGraphics().getFontMetrics(getFont()), c);
		return floater;
	}

	
	
	public ZSprite createZSprite(String fileName){
		BufferedImage img=loadImage(fileName);
		if (img==null) throw new IllegalArgumentException("couldn't find image: "+fileName);
		ZSprite sprite=new ZSprite(img.getRGB(0,0,img.getWidth(),img.getHeight(), null, 0, img.getWidth()),
									img.getWidth(),
									img.getHeight());
		return sprite;
	}
}

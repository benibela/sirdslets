import java.util.*;
import java.awt.*; //images
import java.awt.image.*; 
public class IntArrayImage{
	public int[] data;
	public int w, h;
	public int start=0;
	public int stride;

	public IntArrayImage(){
	}
	public IntArrayImage(int nw, int nh){
		setSize(nw,nh);
	}
	public IntArrayImage(BufferedImage img){
		setToImageARGB(img);
	}
	
	public int getLineIndex(int y)
	{
		return (start + (y*stride));
	}
	public int getIndex(int x, int y)
	{
		return (start + x + (y*stride));
	}

	public void setSize(int nw, int nh){
		w=nw;
		h=nh;
		data=new int[w*h];
		stride=w;
	}

	public void setROI(int x, int y, int width, int height){
		start = 0; //reset to default origin
		start = getIndex(x, y);
		w = width;
		h = height;
	}

	public void clear()
	{
		int b;
		for (int y=0; y<h; y++)
		{
			b = getLineIndex(y);
			for (int x=0; x<w; x++)
				data[b+x] = 0;
		}
	}
	
	public void assignNoCopy(IntArrayImage f){
		data = f.data;
		w=f.w;
		h=f.h;
		start=f.start;
		stride=f.stride;
	}
	
	public boolean inBounds(int x, int y)
	{
		return !((x < 0) || (x >= w) 
			|| (y < 0) || (y >= h));
	}
	
	public boolean inYBounds(int y){
		return !((y < 0) || (y>=h));
	}

	public boolean inXBounds(int x){
		return !((x < 0) || (x>=w));
	}
	
	void customPut(int pos, int value){
		data[pos]=value;
	}

	final void forcePut(int pos, int value){
		data[pos]=value;
	}
		
	public void copyDataFrom(int tx, int ty, IntArrayImage from, int fx, int fy, int w, int h){
		for (int y=0;y<h;y++){
			if (!from.inYBounds(fy+y)) continue;
			if (!inYBounds(ty+y)) continue;
			int bf = from.getLineIndex(fy+y)+fx;
			int bt = getLineIndex(ty+y)+tx;
			
			for (int x=0;x<w;x++)
				if (inXBounds(tx+x) && from.inXBounds(fx+x))
					customPut(bt+x, from.data[bf+x]);
		}
	}

	public void forceCopyDataFrom(int tx, int ty, IntArrayImage from, int fx, int fy, int w, int h){
		int minx=0;
		if (tx+minx<0) minx=-tx;
		if (fx+minx<0) minx=-fx;
		int maxx=w;
		if (tx+maxx>w) maxx=w-tx;
		if (fx+maxx>from.w) maxx=from.w-fx;
		if (maxx<minx) return; //not visible
		for (int y=0;y<h;y++){
			if (!from.inYBounds(fy+y)) continue;
			if (!inYBounds(ty+y)) continue;
			int bf = from.getLineIndex(fy+y)+fx;
			int bt = getLineIndex(ty+y)+tx;
			
			for (int x=minx;x<maxx;x++)
				forcePut(bt+x, from.data[bf+x]);
		}
	}

	public void forceDraw(int tx, int ty, IntArrayImage from){
		copyDataFrom(tx, ty, from, 0, 0, from.w, from.h);
	}
	public void draw(int tx, int ty, IntArrayImage from){
		forceCopyDataFrom(tx, ty, from, 0, 0, from.w, from.h);
	}
	
	public void enlarge(int addLeft, int addTop, int addRight, int addBottom, int fillValue){
		IntArrayImage old=new IntArrayImage();
		old.assignNoCopy(this);
		setSize(w+addLeft+addRight, h+addTop+addBottom);
		//fill new areas
		fillRect(0,0,old.w+addLeft+addRight,addTop,fillValue); //top
		fillRect(0,old.h+addTop,old.w+addLeft+addRight,addBottom, fillValue); //bottom
		fillRect(0,0,addLeft,old.h+addTop+addBottom, fillValue); //left
		fillRect(old.w+addLeft,0,addRight,old.h+addTop+addBottom,fillValue); //right
		forceDraw(addLeft,addTop,old);
	}
	public void enlarge(int add, int fillValue){
		enlarge(add,add,add,add,fillValue);
	}
	
	public void rotate90R(){
		IntArrayImage old=new IntArrayImage();
		old.assignNoCopy(this);
		setSize(h,w);
		for (int y=0;y<h;y++){
			int b=getLineIndex(y);
			for (int x=0;x<w;x++)
				data[b+x] = old.data[old.getLineIndex(w-1-x)+y];
		}
	}
	
	public void rotate180(){
		//TODO:rotate180
		rotate90R();rotate90R();
	}
	public void rotate90L(){
		//TODO:rotate180
		rotate180();rotate90R();
	}
	
	//simple draw
	public void fillCircle(int x, int y, int r, int value){
		int fx=Math.max(0,x-r);
		int tx=Math.min(w-1,x+r);
		int fy=Math.max(0,y-r);
		int ty=Math.min(h-1,y+r);
		int rsqr=r*r;
		for (int cy=fy;cy<=ty;cy++) {
			int b=getLineIndex(cy);
			for (int cx=fx;cx<=tx;cx++)
				if ((x-cx)*(x-cx)+(y-cy)*(y-cy)<=rsqr) 
					customPut(b+cx,value);
		}
	}

	public void fillRect(int x, int y, int wd, int ht, int value)
	{
		int b;
		for (int dy=0; dy<ht; dy++)
		{
			b = getIndex(x, y+dy);
			for (int dx=0; dx<wd; dx++)
			{
				data[b+dx] = value;
			}
		}
	}
	
	
	
	//convert/extended draw	
	public void setToImageARGB(BufferedImage img){
		setSize(img.getWidth(), img.getHeight());
		data=img.getRGB(0,0,w,h, data, 0, w);
		assert (data.length == w*h);
	}

	public void setToStringARGB(String text, FontMetrics fontMetric, Color c){
		int lineHeight=fontMetric.getMaxDescent()+fontMetric.getMaxAscent()+fontMetric.getLeading();// getHeight();

		String[] lines=text.split("\n");
		w=0;
		for (int i=0;i<lines.length;i++)
			w=Math.max(w,fontMetric.stringWidth(lines[i]));
		h=lines.length*lineHeight;
		
		BufferedImage temp=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		Graphics g = temp.getGraphics();
		g.setFont(fontMetric.getFont());
		g.setColor(c);
		for (int i=0;i<lines.length;i++)
			g.drawString(lines[i],0,-fontMetric.getMaxDescent()+(i+1)*lineHeight);
		setToImageARGB(temp);
	}

	//why, why is there no real unsigned int?????
	public static final long unsignedIntToLong(int i){
		long high=i >>> 16;
		long low = i & 0xffff;
		return (high << 16) | low;
	}

	//todo: why does it ignore the factor[0] values?
	public void transformLinearHorizontal(int mask, float[] factorsLeft, float factorMid, float[] factorsRight){
		int sizeL=factorsLeft.length-1;
		int sizeR=factorsRight.length-1;
		float oldData[]=new float[w+sizeL+sizeR];
		
		for (int y=0;y<h;y++){
			int b = getLineIndex(y);
			//border
			for (int x=0;x<sizeL;x++)
				oldData[x] = unsignedIntToLong(data[b] & mask);
			for (int x=w;x<w+sizeR;x++)
				oldData[x] = unsignedIntToLong(data[b+w-1] & mask);
			//actual line
			for (int x=0;x<w;x++) 	
				oldData[x+sizeL] = unsignedIntToLong(data[b+x] & mask);	
			//transform
			for (int x=0;x<w;x++){
				//calculate new value
				float n=oldData[x+sizeL] * factorMid;
				for (int k=-sizeL;k<0;k++)
					n += oldData[x+k+sizeL] * factorsLeft[-k];
				for (int k=1;k<=sizeR;k++)
					n += oldData[x+k+sizeL] * factorsRight[k];
				//store
				data[b+x] = (data[b+x] & ~mask) | (((int) n) & mask);
			}
		}
	}

	public void transformLinearVertical(int mask, float[] factorsTop, float factorMid, float[] factorsBottom){
		int sizeT=factorsTop.length-1;
		int sizeB=factorsBottom.length-1;
		float oldData[]=new float[h+sizeT+sizeB];
		
		for (int x=0;x<w;x++){
			//border
			for (int y=0;y<sizeT;y++)
				oldData[y] = unsignedIntToLong(data[getLineIndex(0)+x] & mask);
			for (int y=h;y<h+sizeB;y++)
				oldData[y] = unsignedIntToLong(data[getLineIndex(h-1)+x] & mask);
			//actual line
			for (int y=0;y<h;y++) 	
				oldData[y+sizeT] = unsignedIntToLong(data[getLineIndex(y)+x] & mask);	
			//transform
			for (int y=0;y<h;y++){
				//calculate new value
				float n=oldData[y+sizeT] * factorMid;
				for (int k=-sizeT;k<0;k++)
					n += oldData[y+k+sizeT] * factorsTop[-k];
				for (int k=1;k<=sizeB;k++)
					n += oldData[y+k+sizeT] * factorsBottom[k];
				//store
				data[getLineIndex(y)+x] = (data[getLineIndex(y)+x] & ~mask) | (((int) n) & mask);
			}
		}
	}

	
	public void blur(int sigma, int mask){
		//Gauss: f(x) = e^(-(x/sigma)^2)/(sigma*Math.sqrt(2*Math.PI))
		float gauss[]=new float[3*sigma];
		for (int x=1;x<=3*sigma;x++) 
			gauss[x-1]=(float)Math.exp(-(x*x/(sigma*sigma)))/(float)(sigma*Math.sqrt(2*Math.PI));
		transformLinearHorizontal(mask, gauss, 1.0f/(float)(sigma*Math.sqrt(2*Math.PI)), gauss);
		transformLinearVertical(mask, gauss, 1.0f/(float)(sigma*Math.sqrt(2*Math.PI)), gauss);
		//System.out.print(gauss[x]+", ");
		//}System.out.println();
	}
}
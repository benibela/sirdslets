import java.awt.*; //images
import java.awt.image.*; //images

public class Floater extends IntArrayImage{
	public int x,y,z;
	public boolean visible=true;//, alphaTransparent; 
	public boolean ignoreHeightmap=true;

	Floater(){
		setSize(0, 0);
	}
	Floater(int nw, int nh){
		setSize(nw, nh);
	}
	
	//set ignoreHeighmap to false to draw only the pixel of the floater which are actually
	//visible => problem, SIRDS can't be partially visible=>you normally can still see the floater on 
	//one (only one) eye
	public void draw(int[] to, ZDraw hmap){
		if (!visible) 
			return;
		if (data.length!=w*h)
			throw new IllegalArgumentException("invalid floater data");
		int SIRDW=ZDraw.SIRDW;
		if (w>SIRDW)
			throw new IllegalArgumentException("floater width to large");
		for (int cy=0;cy<h;cy++){
			if (y+cy<0) continue;
			if (y+cy>=hmap.h) break;
			int b=hmap.getLineIndex(y+cy);
			int bd=cy*w;
			int offset=(x+z) % (SIRDW -z);
			int cx=-offset;
			while(cx+offset<hmap.w){
				//to[b+cx] = data[bd+cx];
				int o=to[b+cx+offset];
				int ncx = (SIRDW+cx) % (SIRDW-z);
				if (ncx>=w) {
					cx=cx+(SIRDW-z)-ncx;
					continue;
				}
				int n=data[bd+ncx];
				int alpha=n>>>24;
				int malpha=0xff - alpha;
				if (ignoreHeightmap || hmap.data[b+cx+offset]<=z)
				to[b+cx+offset] = 0xff000000 
						   | (((malpha * ((o>>>16) & 0xff) + alpha*((n>>>16) & 0xff)) / 0xff) << 16)
						   | (((malpha * ((o>>>8)  & 0xff) + alpha*((n>>> 8) & 0xff)) / 0xff) << 8)
						   |  ((malpha * (o       & 0xff) + alpha*(n       & 0xff)) / 0xff);
				cx++;
			}
		}
	}
	
	//set to methods only change the data, not the position 	
	public void setToImage(BufferedImage img){
		w=img.getWidth();
		h=img.getHeight();
		data=img.getRGB(0,0,w,h, null, 0, img.getWidth());
	}
	
	public void setToString(String text, FontMetrics fontMetric, Color c){
		w=fontMetric.stringWidth(text);
		h=fontMetric.getMaxDescent()+fontMetric.getMaxAscent()+fontMetric.getLeading();// getHeight();
		BufferedImage temp=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		Graphics g = temp.getGraphics();
		g.setFont(fontMetric.getFont());
		g.setColor(c);
		g.drawString(text,0,h-fontMetric.getMaxDescent());
		setToImage(temp);
	}
	
	public void setToFloater(Floater f){
		data = f.data;
		w=f.w;
		h=f.h;
	}
	
	public void mergeColor(int o){
		for (int i=0;i<data.length;i++){
			int n=data[i];
			int alpha=n >>> 24;
			int malpha=0xff - alpha;
			data[i] = 0xff000000 
				| (((malpha * ((o>>>16) & 0xff) + alpha*((n>>>16) & 0xff)) / 0xff) << 16)
				| (((malpha * ((o>>>8)  & 0xff) + alpha*((n>>> 8) & 0xff)) / 0xff) << 8)
				|  ((malpha * (o       & 0xff) + alpha*(n       & 0xff)) / 0xff);
		}
	}

	public void clear(){	
		for (int i=0;i<w*h;i++)
			data[i]=0;
	}
}

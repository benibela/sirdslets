import java.awt.*; //images
import java.awt.image.*; //images

public class Floater extends IntArrayImage implements FloatingObject {
	public int x,y,z;
	public boolean visible=true;//, alphaTransparent; 
	public boolean ignoreHeightmap=true;

	Floater(){
		setSize(0, 0);
	}
	Floater(int nw, int nh){
		setSize(nw, nh);
	}

	public Floater fastClone(){
		Floater f = new Floater();
		f.x = x;
		f.y = y;
		f.z = z;
		f.visible = visible;
		f.ignoreHeightmap = ignoreHeightmap;
		f.data=data;
		f.w = w;
		f.h = h;
		f.start = start;
		f.stride = stride;
		return f;
	}

	//set ignoreHeighmap to false to draw only the pixel of the floater which are actually
	//visible => problem, SIRDS can't be partially visible=>you normally can still see the floater on 
	//one (only one) eye
	public void draw(IntArrayImage output){
		if (!visible) 
			return;
		if (data.length!=w*h)
			throw new IllegalArgumentException("invalid floater data");
		int SIRDW=ZDraw.SIRDW;
		//if (w>SIRDW)
		//	throw new IllegalArgumentException("floater width to large");
		for (int cy=0;cy<h;cy++){
			if (y+cy<0) continue;
			if (y+cy>=output.h) break;
			int b=output.getLineIndex(y+cy);
			int bd=getLineIndex(cy);
			int offset=(x+z) % (SIRDW -z);
			int cx=-offset;
			while(cx+offset<output.w){
				//to[b+cx] = data[bd+cx];
				int o=output.data[b+cx+offset];
				int ncx = (SIRDW+cx) % (SIRDW-z);
				if (ncx>=w) {
					cx=cx+(SIRDW-z)-ncx;
					continue;
				}
				int n=data[bd+ncx];
				int alpha=n>>>24;
				int malpha=0xff - alpha;
				if (ignoreHeightmap || output.data[b+cx+offset]<=z)
				output.data[b+cx+offset] = 0xff000000 
						   | (((malpha * ((o>>>16) & 0xff) + alpha*((n>>>16) & 0xff)) / 0xff) << 16)
						   | (((malpha * ((o>>>8)  & 0xff) + alpha*((n>>> 8) & 0xff)) / 0xff) << 8)
						   |  ((malpha * (o       & 0xff) + alpha*(n       & 0xff)) / 0xff);
				cx++;
			}
		}
	}

	public void drawSimple(IntArrayImage output){
		if (!visible) return;
		if (data.length!=w*h)
			throw new IllegalArgumentException("invalid floater data");
		for (int cy=0;cy<h;cy++){
			if (y+cy<0) continue;
			if (y+cy>=output.h) break;
			int b=output.getLineIndex(y+cy);
			int bd=getLineIndex(cy);
			for (int cx=0;cx<w; cx++){		
				//to[b+cx] = data[bd+cx];
				if (cx+x>=output.w) break;
				int o=output.data[b+cx+x];
				int n=data[bd+cx];
				int alpha=n>>>24;
				int malpha=0xff - alpha;

				output.data[b+cx+x] = 0xff000000 
						   | (((malpha * ((o>>>16) & 0xff) + alpha*((n>>>16) & 0xff)) / 0xff) << 16)
						   | (((malpha * ((o>>>8)  & 0xff) + alpha*((n>>> 8) & 0xff)) / 0xff) << 8)
						   |  ((malpha * (o       & 0xff) + alpha*(n       & 0xff)) / 0xff);
			}
		}
	}

	
	//set to methods only change the data, not the position 	
	
	//override to force max width
	public void setToImageARGB(BufferedImage img){
		int nw=img.getWidth();
		if (nw>ZDraw.SIRDW) nw = ZDraw.SIRDW; //prevent exception
		setSize(nw, img.getHeight());
		data=img.getRGB(0,0,nw,h, data, 0, nw);
	}
			
	public void setToString(String text, FontMetrics fontMetric, Color c){
		setToStringARGB(text,fontMetric,c);
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
	
	public void blur(int size){
		//blur color components independently
		blur(size,  0x000000ff);
		blur(size,  0x0000ff00);
		blur(size,  0x00ff0000);
		blur((size+1)/2,0xff000000);
	}
	
	public void setToImage(BufferedImage img){
		setToImageARGB(img);
	}
}

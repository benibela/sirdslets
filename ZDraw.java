
//
// ZDraw - a simple height image class
//
//  This class represents a height map, with related z-rendering
//  tools. It can render the map as a SIRDS image or a grey-scale 
//  height map.
//
// Written by Lewey Geselowitz
//  http://www.leweyg.com
//   


// mirror for leweyg: http://www.lewcid.com/download/SIRD/index.html

import java.util.*;
import java.awt.image.*; //images
import java.io.*; //images
import javax.imageio.*; //images
import java.net.*;

public class ZDraw
{
	public int[] data;
	public int w, h;
	public int start, stride;
	public boolean mIgnoreZ = false;

	public static class Pnt
	{
		public int x, y, z;

		public Pnt() {};
		public Pnt(int X, int Y, int Z)
		{
			x = X; y = Y; z=Z;
		}

		void set(int X, int Y, int Z)
		{
			x = X; y = Y; z=Z;
		}
	}

	public static class SlantRect
	{
		public Pnt From, To;
		public int FromW, ToW;

		public SlantRect()
		{
			From = new Pnt();
			To = new Pnt();
		};
	}

	public void DrawSlantRectY(SlantRect r)
	{
		Pnt f=r.From, t=r.To;
		int fw=r.FromW, tw=r.ToW;
		if (f.x > t.x)
		{
			Pnt tp = f;
			f = t;
			t = tp;

			int tt = fw;
			fw = tw;
			tw = tt;
		}

		int y, w, b, z;
		int dx = t.x - f.x;
		int dy = t.y - f.y;
		int dz = t.z - f.z;
		int dw = tw - fw;
		for (int x=f.x; x<=t.x; x++)
		{
			y = (((x - f.x)*dy)/dx) + f.y;
			w = (((x - f.x)*dw)/dx) + fw;
			z = (((x - f.x)*dz)/dx) + f.z;
			b = GetIndex(x, y);
			for (int i=0; i<w; i++)
			{
				data[b+i*stride] = z;
			}
		}
	}

	public void DrawSlantRectX(SlantRect r)
	{
		Pnt f=r.From, t=r.To;
		int fw=r.FromW, tw=r.ToW;
		if (f.y > t.y)
		{
			Pnt tp = f;
			f = t;
			t = tp;

			int tt = fw;
			fw = tw;
			tw = tt;
		}

		int x, w, b, z;
		int dx = t.x - f.x;
		int dy = t.y - f.y;
		int dz = t.z - f.z;
		int dw = tw - fw;
		for (int y=f.y; y<=t.y; y++)
		{
			x = (((y - f.y)*dx)/dy) + f.x;
			w = (((y - f.y)*dw)/dy) + fw;
			z = (((y - f.y)*dz)/dy) + f.z;
			b = GetLineIndex(y) + x;
			for (int i=0; i<w; i++)
			{
				data[b+i] = z;
			}
		}
	}
		
	public int GetIndex(int x, int y)
	{
		return (start + x + (y*stride));
	}
	
	public int GetLineIndex(int y)
	{
		return (start + (y*stride));
	}

	public ZDraw(int[] pxls, int width, int height, int strt, int strid)
	{
		w = width;
		h = height;
		start = strt;
		stride = strid;
		data = pxls;
	}
	
	public ZDraw(int width, int height)
	{
		w = width;
		h = height;
		start = 0;
		stride = w;
		data = new int[w*h];
	}
	
	public final static int MAXZ = 20;
	public final static int SIRDW = 80;
	public final static int SIRDH = 50;
	
	public static int[] GetRandSIRDData(){
		return GetRandSIRDData(true,false,0xffffffff);
	}
	
	public static int[] GetRandSIRDData(boolean color, boolean slices, int mask)
	{
		int[] ans = new int[SIRDW*SIRDH];
		Random r = new Random();
		int d = (int)(Math.random()*255);
		for (int i=0; i<ans.length; i++)
		{
			int v;
			if (!slices) {
				v = (int)(Math.random()*255);
			} else {
				v = (((i%SIRDW)*128 / SIRDW) << 1);
				if (v==0) d = (int)(Math.random()*255);
				v ^= d;
//				v |= (v<<8) | (v << 16);
			}
			if (color) v |= ((int)(Math.random()*255)<<8) | ((int)(Math.random()*255) << 16);
			else {
				int ad = v;// ((v >> 1) + (v >> 2));
				v |= (ad<<8) | (ad << 16);
				v &= mask;
			}
			ans[i] = v;
		}
		return ans;
	}
	
	public static int[] GetImageSIRDData(URL baseUrl, String name){
		try {
			return GetImageSIRDData(new URL(baseUrl,name));
		} catch (java.net.MalformedURLException e){
		}
		return GetRandSIRDData();
	}
	public static int[] GetImageSIRDData(URL url){
		BufferedImage img = null;
		try {
			//img = ImageIO.read(new File(fileName));
			img = ImageIO.read(url);
			//URL url = new URL(getCodeBase(), "strawberry.jpg");
		} catch (IOException e) {
		}
		
		if (img==null || img.getWidth()<SIRDW || img.getHeight() < SIRDH) return GetRandSIRDData();
		int [] data=new int[SIRDW*SIRDH];
		img.getRGB(0, 0, SIRDW, SIRDH, data, 0, img.getWidth());
		if (data.length!=SIRDW*SIRDH) return  GetRandSIRDData();
		return data;
	}
	
	//draw a SIRDS image of this height map into 'to'
	//NOTE: 'start' must be 0, 'stride' must equal w, and this
	//should be exactly the same as 'to'
	public void DrawSIRD(int[] to, int[] randdata, int randoffset)
	{
		int rlen = randdata.length;
		//int roff =  randoffset % 4;//Math.abs(randdata[randoffset%rlen])%rlen;
		int roff =  (int)(Math.random()*SIRDW);//10-(10-randoffset) % 20;//Math.abs(randdata[randoffset%rlen])%rlen;
		int rvoff =   (int)(Math.random()*SIRDH);//randoffset % 4;//Math.abs(randdata[randoffset%rlen])%rlen;
		roff=Math.abs(roff)%rlen;
		rvoff=Math.abs(rvoff)%rlen;
		roff=0;rvoff=0;
		int b, x, rb;
		for (int y=0; y<h; y++)
		{
			rb = ((y+rvoff)%SIRDH)*SIRDW + roff;
			b = GetLineIndex(y);
			//if (b+w>rlen) b-=rlen;
			for (x=0; x<SIRDW; x++)
			{
				to[b+x] = 0xff000000 + randdata[(x+rb)%rlen];
			}
			for (x=SIRDW; x<w; x++)
			{
				to[b+x] = to[b+x-SIRDW+data[b+x]];
			}
		}
	}
	
	public void DrawHeightMapTo(int[] to)
	{
		int b;
		for (int y=0; y<h; y++)
		{
			b = GetLineIndex(y);
			for (int x=0; x<w; x++)
			{
				int v = data[b+x] << 2;
				v = 0xff000000 | v | (v<<8) | (v<<16);
				to[b+x] = v;
			}
		}
	}
	
	public boolean InBounds(int x, int y)
	{
		return !((x < 0) || (x >= w) 
			|| (y < 0) || (y >= h));
	}
	
	private void SafePut(int at, int z)
	{
		if (mIgnoreZ || (data[at] < z))
			data[at] = z;
	}
	
	private void DrawThinLine(int x1, int y1, int x2, int y2, int z)
	{
		int sx=(x2-x1), sy=(y2-y1);
		int dx=1, dy=1;
		if (sx < 0)
			dx=-1;
		if (sy < 0)
			dy=-1;
		sx*=dx;
		sy*=dy;

		int i=0, x=x1, y=y1;
		
		int b = start;
		
		x2+=dx;
		y2+=dy;

		if (sx > sy)
		{
			for (x=x1; x!=x2; x+=dx)
			{
				if (InBounds(x,y))
				{
					SafePut(start+x+(y*stride), z);
				}

				i += sy;
				if (i >= sx)
				{
					y += dy;
					i-=sx;
				}
			}
		}
		else
		{
			for (y=y1; y!=y2; y+=dy)
			{
				if (InBounds(x,y))
				{
					SafePut(start+x+(y*stride), z);
				}

				i += sx;
				if (i >= sy)
				{
					x += dx;
					i -= sy;
				}
			}
		}
	}
	
	public void DrawLine(int x1, int y1, int x2, int y2, int r, int z)
	{
		for (int i=-r; i<=r; i++)
		{
			DrawThinLine(x1+i, y1, x2+i, y2, z);
			DrawThinLine(x1, y1+i, x2, y2+i, z);
		}
		DrawCircle(x1, y1, r, z);
		DrawCircle(x2, y2, r, z);
	}
	
	public void DrawCircle(int x, int y, int r, int z)
	{
		int cr = r*r, cy;
		int b;
		for (int dy=-r; dy<=r; dy++)
		{
			b = GetLineIndex(y+dy);
			cy = dy*dy;
			for (int dx=-r; dx<=r; dx++)
			{
				if ( (dx*dx) + cy <= cr)
				{
					if (InBounds(x+dx, y+dy))
						SafePut(b+x+dx,z);
				}
			}
		}
	}
	
	public void DrawRect(int x, int y, int wd, int ht, int z)
	{
		int b;
		for (int dy=0; dy<ht; dy++)
		{
			b = GetIndex(x, y+dy);
			for (int dx=0; dx<wd; dx++)
			{
				data[b+dx] = z;
			}
		}
	}
	
	public void Clear()
	{
		int b;
		for (int y=0; y<h; y++)
		{
			b = GetLineIndex(y);
			for (int x=0; x<w; x++)
			{
				data[b+x] = 0;
			}
		}
	}
}
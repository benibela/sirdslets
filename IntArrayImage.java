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
	
	public int getLineIndex(int y)
	{
		return (start + (y*stride));
	}

	public void setSize(int nw, int nh){
		w=nw;
		h=nh;
		data=new int[w*h];
		stride=w;
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
	
	public void assign(IntArrayImage f){
		data = f.data;
		w=f.w;
		h=f.h;
		start=f.start;
		stride=f.stride;
	}
	
	public boolean InBounds(int x, int y)
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
}
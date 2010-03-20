public class ZSprite extends ZDraw implements ScenePrimitive{
	//transparent ZDraw
	int x,y,z;
	boolean transparent; //transparent <=> dataVisible!=null (should be)
	boolean dataVisible[];
	
	
	public ZSprite(){}
	//reads a heightmap from the given array and normalize it
	public ZSprite(int[] heightMap, int w, int h){
		transparent=true;
		setSize(w,h);
		readHeightMapFrom(heightMap);
		normalizeZ();
	}
	
	public ZSprite(int width, int height)
	{
		transparent=false;
		setSize(width, height);
	}
	
	public void setSize(int nw, int nh){	
		super.setSize(nw,nh);
		if (transparent)
			dataVisible=new boolean[start+stride*nh];
	}	
	
	//subtracts the z value of the first invisible point from every z-vaue
	public void normalizeZ(){
		int baseZ=0;
		int b=getLineIndex(0);
		for (int x=0;x<w;x++)
			if (transparent && !dataVisible[b+x]) {
				baseZ=data[b+x];
				break;
			}
		normalizeZ(baseZ);
	}
	
	//subtracts baseZ from every z value
	public void normalizeZ(int baseZ){
		if (baseZ==0) return;
		for (int y=0; y<h; y++)
		{
			int b = getLineIndex(y);
			for (int x=0; x<w; x++)
				data[b+x]-=baseZ;
		}
	}
	
	public void readHeightMapFrom(int[] from){
		assert transparent;
		//read height data
		super.readHeightMapFrom(from);
		//set inverse alpha channel
		for (int y=0; y<h; y++)
		{
			int b = getLineIndex(y);
			for (int x=0; x<w; x++)
				dataVisible[b+x]=((from[b+x]&0xff000000)>>>24) > 0x80;
		}
	}
	
	public void drawTo(ZDraw zbuffer, int xo, int yo){
	//	System.out.println("print to: " +x+"/"+y+" size: "+w+":"+h);
		int rx=x+xo;
		int ry=y+yo;
		for (int cy=0; cy<h; cy++)
		{
			int b = getLineIndex(cy);
			int bo = zbuffer.getLineIndex(cy+ry)+rx;
			for (int cx=0; cx<w; cx++)
				if ((!transparent || dataVisible[b+cx]) && zbuffer.inBounds(rx+cx,ry+cy)){
					zbuffer.customPut(bo+cx,data[b+cx]+z);
//					System.out.println("!");
				}
		}
	}

}
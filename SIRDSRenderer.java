
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

public interface SIRDSRenderer{
	void init(Component parent, SceneManager scene);
	void setSISData(int f1[], int f2[], int w, int h);
	void setRandomMode(boolean randomFlicker);
	void setInversion(boolean invert);
	void setDrawMode(int drawMode); //0: height map 1: sirds 2: anaglyph
	int getDrawMode();

	void renderFrame();
	void paint(Graphics g);
	Image getBackBuffer();


	void finit();
}

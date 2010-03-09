
import java.awt.Component;
import java.awt.Graphics;

public interface SIRDSRenderer{
	void init(Component parent, SceneManager scene);
	void setSISData(int f1[], int f2[], int w, int h);
	void setRandomMode(boolean randomFlicker);
	void setInversion(boolean invert);
	void setDrawSIRDS(boolean drawSIRDS);
	boolean getDrawSIRDS();

	void renderFrame();
	void paint(Graphics g);


	void finit();
}

public interface SIRDSlet{
	void start(Object manager);
	void stop();
	void paintFrame();
	void calculateFrame();
	
	String getSIRDletName();
}
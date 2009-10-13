public interface SIRDSlet{
	public void start(Object manager);
	public void stop();
	public void paintFrame();
	public void calculateFrame();
	
	public String getSIRDletName();
}
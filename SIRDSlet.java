public interface SIRDSlet{
	public void start(Object manager);
	public void stop();
	public void calculateFrame();
	
	public String getSIRDletName();
}
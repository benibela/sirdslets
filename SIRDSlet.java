public interface SIRDSlet{
	public void start(Object manager);
	public void stop();
	public void calculateFrame(long timeMS);
	
	public String getName();
	public String getDescription();
	public String getKeys();
}
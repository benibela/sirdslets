public interface SIRDSlet{
	public void start(Object manager, int option);
	public void stop();
	public void calculateFrame(long timeMS);
	
	public String getName();
	public String getDescription();
	public String getKeys();

	public String[] getPossibleOptions();
	public int getDefaultOption();
}
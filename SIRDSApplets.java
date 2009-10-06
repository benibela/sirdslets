//
// SIRDS Applet Manager 
//
// By Benito van der Zander - http://www.benibela.de
// Based on AbSIRDlet of Lewey Geselowitz - http://www.leweyg.com

import java.awt.event.*;
import java.awt.image.*;
import java.applet.*;
import java.util.*;
import java.awt.*;
import java.net.*;

public class SIRDSApplets extends SIRDSAppletManager 
{	
	public void init()
	{
		registerSIRDSlet(new AbSIRDlet());
		super.init();
	}
}
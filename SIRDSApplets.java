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
	@Override
	public void init()
	{
		if (Locale.getDefault() == Locale.GERMAN || Locale.getDefault() == Locale.GERMANY )
			Translations.setInstance(new Translations_DE());
		registerSIRDSlet(new SIRDSFlighter());
		registerSIRDSlet(new AbSIRDlet());
		registerSIRDSlet(new SIRDSFlighterEditor());
		super.init();
	}
	
	
	//applet stand alone execution	
	static class MockAppletStub implements AppletStub{
		String path,documentPath;
		public MockAppletStub(String baseFileName){
			documentPath=baseFileName;
			path=documentPath.substring(0,documentPath.lastIndexOf("/")+1);
		}
		public void appletResize(int width, int height){
			return;
		}
		public AppletContext getAppletContext(){
			return null;
		}
		
        public URL getCodeBase(){
			try{
				return new URL(path);
			} catch (MalformedURLException e){
				return null;
			}
		}
		public URL getDocumentBase(){
			try{
				return new URL(documentPath);
			} catch (MalformedURLException e){
				return null;
			}
		}
		public String getParameter(String name){
			return "";
		}
		public boolean isActive(){
			return true;
		}
	}
	
	public static void main(String args[]){
		Window w = new Frame();
		w.addWindowListener(new WindowAdapter(){
			public void windowClosing (WindowEvent event)
			{ 
				System.exit(0);
			}
		});

		SIRDSApplets sa=new SIRDSApplets();
		sa.setSize(620,640);
		w.setSize(620,640);
		w.add(sa);
		MockAppletStub stub=new MockAppletStub(sa.getClass().getClassLoader().getResource("SIRDSApplets.class").toString());
		sa.setStub(stub);
		System.out.println(stub.getCodeBase().toString());//Class().getClassLoader().getResource("SIRDSApplets.class").toString());
		sa.setFont(new Font("Dialog",0,11));
		sa.init();
		sa.start();
		w.setVisible(true);
		sa.run();
	}
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author benito
 */
public class Translations {
	public static Translations getInstance(){
		return mInstance==null?new Translations():mInstance;
	}
	public static void setInstance(Translations instance){
		mInstance = instance;
	}
	private static Translations mInstance = null;
	public String random(){return "random";}
	public String colored(){ return "colored";}
	public String stripes(){ return "stripes";}
	public String white(){ return "white"; }
	public String gray(){ return "gray"; }
	public String black(){ return "black"; }
	public String red(){ return "red";}
	public String green(){ return "green";}
	public String blue(){ return "blue";}
	public String yellow(){ return "yellow";}
	public String violet(){ return "violet";}
	public String cyan(){ return "cyan";}

	public String AvailableSIRDSlets(){return "Available SIRDSlets:";}
	public String Options(){return "Options:";}
	public String heightmap(){return "height-map";}
	public String firstFrame(){return "First frame:";}
	public String sameAsAbove(){return "same as above";}
	public String secondFrame(){return "Second frame:";}
	public String invertCE(){return "Invert (cross eye)";}
	public String useRandomOffset(){return "Use random offset:";}
	public String showPerformance(){return "Show performance:";}
	public String setFrameRate(){return "Frames/Second:";}
	public String sound(){return "Sound:";}
	public String freeze(){return "freeze";}
	public String continu(){return "continue";}
	public String closeMenu(){return "close menu";}
	public String start(){return "start";}

	public String enterSwitches(){return "Enter\tSwitch between heightmap/SIRDS/anaglyph";}

//module text
	public String SIRDSFlighter(){return "SIRDS Flighter";}
	public String SIRDSFlighterDesc(){
		return "Side scroller where you fly a space ship through a parkour\n\n" +
			"Don't collide with anything, destroy mines and stay away from black and white holes.";
	}
	public String SIRDSFlighterKeys(){
		return "Arrows\tMovement in the xy-plane\n" +
			"Shift/Control/a/s/x/y/z\tMovement along the z-axis (near/far)\n" +
			"Space\tFire";
	}

	public String SIRDSFlighterEditor(){return "SIRDS Flighter (Editor)";}
	public String SIRDSFlighterEditorDesc(){
		return "Level editor of the SIRDS Flighter\n" +
			"It need to save the levels to be useful, so it only works if you download the game and run it locally\n\n";
	}
	public String SIRDSFlighterEditorKeys(){
		return "ctrl+shift+t/s\ttest/save\n" +
			"left/right\tslow scroll\n" +
			"page up/down\tfast scroll\n" +
			"a/s\tcursor on z-axis\n" +
			"c/i/b\tcreate cuboid/image/element\n" +
			"d/r\tduplicate/remove\n" +
			"[ctrl]+[shift]+x/y/z\tedit cuboid\n" +
			"[ctrl]+m\tedit mover/modifier";
	}

	public String SIRDSFlighterVeryEasy(){
		return "very easy";
	}
	public String SIRDSFlighterEasy(){
		return "easy";
	}
	public String SIRDSFlighterNormal(){
		return "normal";
	}
	public String SIRDSFlighterHard(){
		return "hard";
	}
	public String SIRDSFlighterImpossible(){
		return "impossible";
	}

	public String AbSIRDS(){
		return "SIRD Painter";
	}
	public String AbSIRDSDesc(){
		return "This lets you paint your own SIRDS\n\n";
	}
	public String AbSIRDSKeys(){
		return  "a,s\tnear/far\n" +
			"d,f\tdrawing radius\n" +
			"mouse\tdraw/select height";
	}
}


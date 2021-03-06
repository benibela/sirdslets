/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author benito
 */
public class Translations_DE extends Translations{
	Translations_DE(){
		mSIRDSXkcd = new SIRDSxkcd();
	}
	public String random(){return "zufällig";}
	public String colored(){ return "bunt";}
	public String stripes(){ return "stripes";}
	public String confetti(){ return "Konfetti"; }
	public String white(){ return "weiss"; }
	public String gray(){ return "grau"; }
	public String black(){ return "schwarz"; }
	public String red(){ return "rot";}
	public String green(){ return "grün";}
	public String blue(){ return "blau";}
	public String yellow(){ return "gelb";}
	public String violet(){ return "violett";}
	public String cyan(){ return "cyan";}
	
	public String AvailableSIRDSlets(){return "Vorhandene SIRDSlets:";}
	public String Options(){return "Optionen:";}
	public String heightmap(){return "height-map";}
	public String firstFrame(){return "Erster Frame:";}
	public String sameAsAbove(){return "wie oben";}
	public String secondFrame(){return "Zweiter Frame:";}
	public String invertCE(){return "Invertiert (Kreuzblick)";}
	public String useRandomOffset(){return "Zufällige Verschiebungen:";}
	public String showPerformance(){return "Framerate:";}
	public String setFrameRate(){return "Framerate:";}
	public String sound(){return "Sound:";}
	public String freeze(){return "Totstellen";}
	public String continu(){return "Weiter";}
	public String closeMenu(){return "Menü schließen";}
	public String start(){return "START";}

	public String enterSwitches(){return "Enter\tWechsel zwischen Heightmap/SIRDS/Anaglyph";}

//module text
	public String SIRDSFlighter(){return "SIRDS Flighter";}
	public String SIRDSFlighterDesc(){
		return "Sidescroller, in dem man ein Raumschiff zwischen Hindernissen hindurchsteuert.\n\n" +
			"Sei auf der Hut vor Wänden, Minen, schwarzen Löchern und ihrem Gegenstück den weißen Löchern. ";
	}
	public String SIRDSFlighterKeys(){
		return "Pfeiltasten\tBewegung in der xy-Ebene\n" +
			"Shift/Control/a/s/x/y/z\tBewegung entlang der z-Achse (nah/fern)\n" +
			"Leertaste\tSchieße";
	}

	public String SIRDSFlighterEditor(){return "SIRDS Flighter (Editor)";}
	public String SIRDSFlighterEditorDesc(){
		return "Leveleditor des SIRDS Flighter\n" +
			"Er muss die Level speichern können, weshalb er nur funktioniert, wenn die SIRDSlets heruntergeladen wurden und lokal ausgeführt werden.\n\n";
	}
	public String SIRDSFlighterEditorKeys(){
		return "ctrl+shift+t/s\ttest/speichern\n" +
			"left/right\tlangsam scrollen\n" +
			"page up/down\tschnell scrollen\n" +
			"a/s\tnah/fern cursor\n" +
			"c/i/b\tneues Objekt\n" +
			"d/r\tverdoppeln/löschen\n" +
			"[ctrl]+[shift]+x/y/z\tWürfel skalieren\n" +
			"[ctrl]+m\tBewegungspfade/modifizierer setzen";
	}

	public String SIRDSFlighterVeryEasy(){
		return "sehr einfach";
	}
	public String SIRDSFlighterEasy(){
		return "einfach";
	}
	public String SIRDSFlighterNormal(){
		return "normal";
	}
	public String SIRDSFlighterHard(){
		return "schwer";
	}
	public String SIRDSFlighterImpossible(){
		return "unmöglich";
	}

	public String AbSIRDS(){
		return "SIRDS Malprogramm";
	}
	public String AbSIRDSDesc(){
		return "Hier kannst du dein eigenes SIRDS malen\n\n";
	}
	public String AbSIRDSKeys(){
		return  "a,s\tnah/fern\n" +
			"d,f\tPinselstärke\n" +
			"mouse\tmalen/Abstand auswählen";
	}

	public static class SIRDSxkcd extends Translations.SIRDSxkcd {
		public String name() { return "SIRDSifiziertes XKCD"; }
		public String desc() { return "Zeigt XKCDs Welten als SIRD."; }
		public String keys() { return "Pfeiltasten\tBewegt das Hoverboard."; }

	}

}


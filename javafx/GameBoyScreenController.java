package javafx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import core.JavaBoy;
import javafx.fxml.Initializable;

public class GameBoyScreenController implements Initializable {

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		/*
		 * Load INI preferences
		 */
		try {
			Ini ini = new Ini(new File("JavaBoy.ini"));
			Preferences prefs = new IniPreferences(ini);
			System.out.println("Hola " + prefs.node("hola").get("cadena", null));
		} catch (IOException e) {
			System.out.println("No hay preferencias");
		}
		
	}

}

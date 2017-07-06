package javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GameBoyScreen extends Application {
	
	/*
	 * This file only runs the JavaFX screen.
	 */
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("GBScreen.fxml"));
		Scene scene = new Scene(root);
		
		primaryStage.setTitle("JavaBoy NEO");
		primaryStage.setScene(scene);
		
		setWindowSize(primaryStage, 2);
		
		primaryStage.show();
	}
	
	/*
	 * Size of the screen must fit width GameBoy resolution.
	 */
	public void setWindowSize(Stage stage, int mag) {
		stage.setWidth(175 * mag + 20);
		stage.setHeight(174 * mag + 20);
	}
}

package jlinkgui;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class Main extends Application {
    @Override
    public void start(Stage mainStage) {
    	Controler.stage = mainStage;
    	Parent root = null;
		try {
			URL url = getClass().getResource("/seen.fxml");
			root = FXMLLoader.load(url);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
        
    	mainStage.setTitle("Jlink gui");
    	mainStage.setScene(new Scene(root));
    	mainStage.show();
    }
}

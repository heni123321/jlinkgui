package jlinkgui;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage mainStage) {
        Controler.stage = mainStage;
        try {
            URL url = getClass().getResource("/seen.fxml");
            Parent root = FXMLLoader.load(url);
            mainStage.setTitle("Jlink gui");
            mainStage.setScene(new Scene(root));
            mainStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

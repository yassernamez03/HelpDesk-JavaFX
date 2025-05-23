package com.helpdesk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/chat_view.fxml"));

        // Set minimum width/height to make it mobile-like
        primaryStage.setMinWidth(380);
        primaryStage.setMinHeight(650);
        // Remove window decoration (title bar)
        primaryStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        Scene scene = new Scene(root, 800, 650);
        scene.getStylesheets().addAll(
            getClass().getResource("/css/mobile-light.css").toExternalForm(),
            getClass().getResource("/css/library-light.css").toExternalForm(),
            getClass().getResource("/css/kb-light.css").toExternalForm(),
            getClass().getResource("/css/nav-light.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

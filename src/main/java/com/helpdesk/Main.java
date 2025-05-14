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
        primaryStage.setTitle("IT Support Helpdesk");

        Scene scene = new Scene(root, 800, 650);
        scene.getStylesheets().add(getClass().getResource("/css/mobile-light.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
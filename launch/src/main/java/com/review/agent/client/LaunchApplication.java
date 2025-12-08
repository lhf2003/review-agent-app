package com.review.agent.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LaunchApplication extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com.review.agent.client/main-view.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setTitle("Review Agent");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

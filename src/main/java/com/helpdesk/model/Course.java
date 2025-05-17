package com.helpdesk.model;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class Course {
    private String title;
    private String description;
    private String imageUrl;

    public Course(String title, String description, String imageUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public VBox createCourseCard() {
        VBox courseCard = new VBox();
        courseCard.getStyleClass().add("course-card");

        // Create and setup image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);
        try {
            imageView.setImage(new Image(getClass().getResource(imageUrl).toExternalForm()));
        } catch (Exception e) {
            System.err.println("Failed to load image: " + imageUrl);
        }

        // Create course info container
        VBox infoContainer = new VBox();
        infoContainer.getStyleClass().add("course-info");
        infoContainer.setSpacing(8);

        // Create title label
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("course-title");
        titleLabel.setWrapText(true);

        // Create description label
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("course-description");
        descLabel.setWrapText(true);

        // Create action button
        Button startButton = new Button("Start Course");
        startButton.getStyleClass().add("course-button");

        // Add all elements to info container
        infoContainer.getChildren().addAll(titleLabel, descLabel, startButton);

        // Add components to card
        courseCard.getChildren().addAll(imageView, infoContainer);

        return courseCard;
    }
}

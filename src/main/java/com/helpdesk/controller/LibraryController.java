package com.helpdesk.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import com.helpdesk.model.Course;

public class LibraryController {
    @FXML
    private TextField searchField;

    @FXML
    private FlowPane coursesContainer;

    @FXML
    private VBox libraryView;

    private List<Course> courses;
    private Preferences preferences;    @FXML
    public void initialize() {
        preferences = Preferences.userNodeForPackage(LibraryController.class);
        
        // Initialize courses list
        courses = new ArrayList<>();
        loadCourses();

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCourses(newValue);
        });
        
        // Listen for theme changes
        setupThemeChangeListener();
        
        // Apply current theme after the scene is set
        libraryView.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                applyCurrentTheme();
            }
        });
        
        // Apply theme if scene is already set
        if (libraryView.getScene() != null) {
            applyCurrentTheme();
        }
    }
    
    private void setupThemeChangeListener() {
        // Create a timer to periodically check for theme changes
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(500), e -> {
                // Check if theme has changed by comparing timestamps
                long lastThemeChange = preferences.getLong("themeChangeTimestamp", 0);
                long lastKnownChange = preferences.getLong("lastKnownThemeChange", 0);
                
                if (lastThemeChange > lastKnownChange) {
                    preferences.putLong("lastKnownThemeChange", lastThemeChange);
                    applyCurrentTheme();
                }
            })
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
    
    public void applyCurrentTheme() {
        String theme = preferences.get("theme", "light");
        String mobileCssPath = theme.equals("dark") ? "/css/mobile-dark.css" : "/css/mobile-light.css";
        String libraryCssPath = theme.equals("dark") ? "/css/library-dark.css" : "/css/library-light.css";
        String kbCssPath = theme.equals("dark") ? "/css/kb-dark.css" : "/css/kb-light.css";
        String navCssPath = theme.equals("dark") ? "/css/nav-dark.css" : "/css/nav-light.css";

        Scene scene = libraryView.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            
            // Add CSS files in the correct order to prevent conflicts
            try {
                scene.getStylesheets().addAll(
                    getClass().getResource(mobileCssPath).toExternalForm(),
                    getClass().getResource(libraryCssPath).toExternalForm(),
                    getClass().getResource(kbCssPath).toExternalForm(),
                    getClass().getResource(navCssPath).toExternalForm()
                );
            } catch (Exception e) {
                System.err.println("Failed to load CSS for library theme: " + e.getMessage());
            }
            
            // Refresh the course display to ensure proper styling
            refreshCourseDisplay();
        }
    }
    
    private void refreshCourseDisplay() {
        // Re-display courses to apply new styling
        String currentSearchText = searchField.getText();
        if (currentSearchText == null || currentSearchText.isEmpty()) {
            displayCourses(courses);
        } else {
            filterCourses(currentSearchText);
        }
    }

    private void loadCourses() {
        // Add demo courses (these would typically come from a database)
        addDemoCourses();
        displayCourses(courses);
    }

    private void addDemoCourses() {
        courses.add(new Course(
            "Network Troubleshooting Basics",
            "Learn the fundamentals of diagnosing and fixing common network issues",
            "/images/network-troubleshooting.jpg"
        ));
        
        courses.add(new Course(
            "Windows Security Essentials",
            "Master essential Windows security practices and malware protection",
            "/images/windows-security.jpg"
        ));
        
        courses.add(new Course(
            "Software Installation Guide",
            "Step-by-step guides for installing and configuring common software",
            "/images/software-installation.jpg"
        ));
    }

    private void filterCourses(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            displayCourses(courses);
            return;
        }

        List<Course> filteredCourses = courses.stream()
            .filter(course -> 
                course.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                course.getDescription().toLowerCase().contains(searchText.toLowerCase()))
            .collect(java.util.stream.Collectors.toList());

        displayCourses(filteredCourses);
    }

    private void displayCourses(List<Course> coursesToDisplay) {
        coursesContainer.getChildren().clear();
        coursesToDisplay.forEach(course -> {
            VBox courseCard = course.createCourseCard();
            coursesContainer.getChildren().add(courseCard);
        });
    }
}

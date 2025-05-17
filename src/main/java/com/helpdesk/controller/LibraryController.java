package com.helpdesk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.ArrayList;
import com.helpdesk.model.Course;

public class LibraryController {
    @FXML
    private TextField searchField;

    @FXML
    private FlowPane coursesContainer;

    @FXML
    private VBox libraryView;

    private List<Course> courses;

    @FXML
    public void initialize() {
        // Initialize courses list
        courses = new ArrayList<>();
        loadCourses();

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCourses(newValue);
        });
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

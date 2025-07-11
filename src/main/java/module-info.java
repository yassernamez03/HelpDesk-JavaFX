module com.helpdesk {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;
    requires javafx.swing;

    requires java.sql;
    requires java.prefs;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome; // Add this line
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    requires com.google.gson;
    requires okhttp3;
    requires org.xerial.sqlitejdbc;
    requires flexmark;
    requires flexmark.util.ast;
    requires flexmark.util.data;
    requires flexmark.util.misc;

    // No longer using vosk or JNA for speech recognition
    // Using OpenAI's Whisper API through HTTP requests instead

    opens com.helpdesk to javafx.fxml;
    exports com.helpdesk;
    exports com.helpdesk.controller;
    opens com.helpdesk.controller to javafx.fxml;
}

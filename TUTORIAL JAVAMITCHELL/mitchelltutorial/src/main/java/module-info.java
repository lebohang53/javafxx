module com.example.mitchelltutorial {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens com.example.mitchelltutorial to javafx.fxml;
    exports com.example.mitchelltutorial;
}
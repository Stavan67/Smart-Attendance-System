module io.itpl.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.bytedeco.opencv;
    requires mysql.connector.j;
    requires org.bytedeco.javacpp;
    requires org.bytedeco.openblas;
    requires org.bytedeco.javacv;
    requires javafx.swing;

    opens io.itpl.ui to javafx.fxml;
    exports io.itpl.ui;
    exports io.itpl.database;
    exports io.itpl.facerecognition;
}
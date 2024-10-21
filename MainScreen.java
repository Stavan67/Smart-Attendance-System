package io.itpl.ui;

import io.itpl.facerecognition.FaceRecognitionTrainer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainScreen extends Application {
    private Label statusLabel;
    private Stage primaryStage;
    private Scene mainScene;
    private boolean wasMaximized;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Online Attendance System");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        VBox centerBox = createCenterBox();
        mainLayout.setCenter(centerBox);

        statusLabel = new Label();
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        statusLabel.setStyle("-fx-text-fill: #4a4a4a;");
        mainLayout.setBottom(statusLabel);
        BorderPane.setAlignment(statusLabel, Pos.CENTER);
        BorderPane.setMargin(statusLabel, new Insets(20, 0, 0, 0));

        mainScene = new Scene(mainLayout);
        primaryStage.setScene(mainScene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        wasMaximized = primaryStage.isMaximized();

        primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            wasMaximized = newVal;
        });
    }

    private VBox createCenterBox() {
        VBox centerBox = new VBox(30);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(600);

        Label titleLabel = new Label("Online Attendance System");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 36));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        Button registerButton = createStyledButton("Register New User");
        Button attendanceButton = createStyledButton("Mark Attendance");
        Button trainModelButton = createStyledButton("Train Face Recognition Model");
        Button retrainModelButton = createStyledButton("Retrain Face Recognition Model");

        registerButton.setOnAction(e -> showRegisterUserScreen());
        attendanceButton.setOnAction(e -> showAttendanceScreen());
        trainModelButton.setOnAction(e -> trainModel());
        retrainModelButton.setOnAction(e -> retrainModel());

        centerBox.getChildren().addAll(titleLabel, registerButton, attendanceButton, trainModelButton, retrainModelButton);
        return centerBox;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("System", FontWeight.NORMAL, 18));
        button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
        button.setPrefWidth(400);
        button.setPrefHeight(60);
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;"));
        return button;
    }

    private void showRegisterUserScreen() {
        RegisterUserScreen registerUserScreen = new RegisterUserScreen(primaryStage);
        registerUserScreen.show();
        restoreWindowState();
    }

    private void showAttendanceScreen() {
        AttendanceScreen attendanceScreen = new AttendanceScreen(primaryStage);
        attendanceScreen.show();
        restoreWindowState();
    }

    private void trainModel() {
        FaceRecognitionTrainer trainer = new FaceRecognitionTrainer();
        trainer.loadModel();
        trainer.trainModel();
        updateStatus("Model Trained Successfully");
    }

    private void retrainModel() {
        FaceRecognitionTrainer trainer = new FaceRecognitionTrainer();
        trainer.trainModel();
        updateStatus("Model Re-Trained Successfully");
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void restoreWindowState() {
        if (wasMaximized) {
            primaryStage.setMaximized(true);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package io.itpl.ui;

import io.itpl.database.DatabaseConnection;
import io.itpl.facerecognition.FaceRecognitionTrainer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegisterUserScreen
{
    private TextField enrollmentField;
    private TextField nameField;
    private Button captureButton;
    private Button registerButton;
    private Label statusLabel;
    private ImageView cameraView;
    private List<String> capturedImagePaths;
    private VideoCapture capture;
    private boolean isCapturing = false;
    private GridPane grid;
    private Stage primaryStage;
    public static final String BASE_IMAGE_PATH = "captured_images";
    private static final int REQUIRED_IMAGES = 5;
    private Button backButton;

    public RegisterUserScreen(Stage primaryStage)
    {
        this.primaryStage = primaryStage;
        capturedImagePaths = new ArrayList<>();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        this.grid = new GridPane();
    }
    public void show()
    {
        primaryStage.setTitle("Register New User");

        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        enrollmentField = new TextField();
        enrollmentField.setPromptText("Enrollment Number");
        nameField = new TextField();
        nameField.setPromptText("Name");

        captureButton = new Button("Capture Face Data");
        registerButton = new Button("Register");
        backButton = new Button("Back to Main Screen");
        statusLabel = new Label();

        cameraView = new ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);

        grid.add(new Label("Enrollment Number:"), 0, 0);
        grid.add(enrollmentField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(cameraView, 0, 2, 2, 1);
        grid.add(captureButton, 0, 3);
        grid.add(registerButton, 1, 3);
        grid.add(backButton, 0, 8);
        grid.add(statusLabel, 0, 4, 2, 1);

        captureButton.setOnAction(e -> captureImage());
        registerButton.setOnAction(e -> registerUser());
        backButton.setOnAction(e -> returnToMainScreen());

        Scene scene = new Scene(grid, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        startCamera();
    }

    private void startCamera()
    {
        capture = new VideoCapture(0);
        Thread cameraThread = new Thread(this::updateCameraView);
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void updateCameraView()
    {
        Mat frame = new Mat();
        while (capture.isOpened())
        {
            capture.read(frame);
            if (!frame.empty())
            {
                Image image = mat2Image(frame);
                Platform.runLater(() -> cameraView.setImage(image));
            }
            try
            {
                Thread.sleep(33);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void captureImage()
    {
        String enrollmentNumber = enrollmentField.getText();
        if (enrollmentNumber.isEmpty())
        {
            statusLabel.setText("Please enter an enrollment number before capturing images.");
            return;
        }

        if (capturedImagePaths.size() >= REQUIRED_IMAGES)
        {
            statusLabel.setText("You've already captured " + REQUIRED_IMAGES + " images. You can now register.");
            return;
        }

        Mat frame = new Mat();
        if (capture.read(frame))
        {
            String dirPath = BASE_IMAGE_PATH + File.separator + enrollmentNumber;
            File dir = new File(dirPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            String filename = dirPath + File.separator + "image_" + (capturedImagePaths.size() + 1) + ".png";
            Imgcodecs.imwrite(filename, frame);
            capturedImagePaths.add(filename);

            int remaining = REQUIRED_IMAGES - capturedImagePaths.size();
            statusLabel.setText("Image " + capturedImagePaths.size() + " captured. " +
                    (remaining > 0 ? remaining + " more to go." : "You can now register."));

            if (capturedImagePaths.size() == REQUIRED_IMAGES)
            {
                captureButton.setDisable(true);
            }
        }
    }

    private Image mat2Image(Mat frame)
    {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private void registerUser()
    {
        String enrollmentNumber = enrollmentField.getText();
        String name = nameField.getText();

        if (enrollmentNumber.isEmpty() || name.isEmpty() || capturedImagePaths.size() < REQUIRED_IMAGES)
        {
            statusLabel.setText("Please fill all fields and capture " + REQUIRED_IMAGES + " face images.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection())
        {
            String sql = "INSERT INTO users (enrollmentNumber, name) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql))
            {
                pstmt.setString(1, enrollmentNumber);
                pstmt.setString(2, name);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0)
                {
                    statusLabel.setText("User registered successfully.");
                    FaceRecognitionTrainer trainer = new FaceRecognitionTrainer();
                    trainer.loadModel();
                    trainer.updateModel(enrollmentNumber);
                }
                else
                {
                    statusLabel.setText("Failed to register user.");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void returnToMainScreen()
    {
        if (capture != null)
        {
            capture.release();
        }
        MainScreen mainScreen = new MainScreen();
        mainScreen.start(primaryStage);
        if (primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    public static List<String> getImagePathsForUser(String enrollmentNumber)
    {
        List<String> imagePaths = new ArrayList<>();
        String userImageDir = BASE_IMAGE_PATH + File.separator + enrollmentNumber;
        File dir = new File(userImageDir);
        if (dir.exists() && dir.isDirectory())
        {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null)
            {
                for (File file : files)
                {
                    imagePaths.add(file.getAbsolutePath());
                }
            }
        }
        return imagePaths;
    }
}
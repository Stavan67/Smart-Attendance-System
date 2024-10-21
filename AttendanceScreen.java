package io.itpl.ui;

import io.itpl.database.DatabaseConnection;
import io.itpl.facerecognition.FaceRecognitionTrainer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import javafx.scene.layout.GridPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.embed.swing.SwingFXUtils;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class AttendanceScreen {
    private OpenCVFrameGrabber capture;
    private ImageView cameraView;
    private FaceRecognitionTrainer faceRecognizer;
    private String selectedSubject;
    private Label statusLabel;
    private BorderPane mainLayout;
    private VBox subjectSelectionLayout;
    private VBox attendanceLayout;
    private CascadeClassifier faceDetector;
    private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    private Map<String, Map<String, LocalDateTime>> lastAttendanceTime = new HashMap<>();
    private static final int PREDICTION_BUFFER_SIZE = 50;
    private static final double CONFIDENCE_THRESHOLD = 200.0;
    private java.util.Queue<FaceRecognitionTrainer.RecognitionResult> predictionBuffer = new LinkedList<>();
    private GridPane grid;
    private Button nextButton;
    private Button rescanButton;
    private Button registerButton;
    private AtomicBoolean isScanning = new AtomicBoolean(false);
    private Thread cameraThread;
    private Stage primaryStage;
    private Button backButton;
    private boolean attendanceMarked = false;
    public AttendanceScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        faceRecognizer = new FaceRecognitionTrainer();
        faceRecognizer.loadModel();
        String cascadePath = faceRecognizer.getPathToXml("haarcascade_frontalface_default.xml");
        faceDetector = new CascadeClassifier(cascadePath);

        initializeLayouts();
    }

    private void initializeLayouts() {
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        subjectSelectionLayout = createSubjectSelectionLayout();
        attendanceLayout = createAttendanceLayout();

        mainLayout.setCenter(subjectSelectionLayout);
    }

    private VBox createSubjectSelectionLayout() {
        VBox layout = new VBox(30);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setMaxWidth(600);


        Label selectSubjectLabel = new Label("Select a subject to mark attendance:");
        selectSubjectLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Button aiButton = createStyledButton("AI");
        Button erpButton = createStyledButton("ERP");
        Button backButton = createStyledButton("Back to Main Menu");

        aiButton.setOnAction(e -> showAttendanceLayout("AI"));
        erpButton.setOnAction(e -> showAttendanceLayout("ERP"));
        backButton.setOnAction(e -> returnToMainScreen());

        layout.getChildren().addAll(selectSubjectLabel, aiButton, erpButton, backButton);
        return layout;
    }

    private VBox createAttendanceLayout() {
        VBox layout = new VBox(30);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        cameraView = new ImageView();
        cameraView.setFitWidth(500);
        cameraView.setFitHeight(580);
        cameraView.setPreserveRatio(true);

        statusLabel = new Label("Initializing camera...");
        statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        nextButton = createStyledButton("Next");
        rescanButton = createStyledButton("Rescan");
        registerButton = createStyledButton("Haven't registered yet?");
        backButton = createStyledButton("Back to Subject Selection");

        nextButton.setOnAction(e -> startScanning());
        rescanButton.setOnAction(e -> resetAttendance());
        registerButton.setOnAction(e -> goToRegisterScreen());
        backButton.setOnAction(e -> returnToSubjectSelection());

        buttonBox.getChildren().addAll(nextButton, rescanButton, registerButton, backButton);

        layout.getChildren().addAll(cameraView, statusLabel, buttonBox);
        return layout;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-font-size: 16px; -fx-min-width: 200px; -fx-min-height: 40px;");
        return button;
    }

    public void show() {
        Scene scene = new Scene(mainLayout);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Attendance Management System");
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void showAttendanceLayout(String subject) {
        selectedSubject = subject;
        mainLayout.setCenter(attendanceLayout);
        initializeCamera();
    }

    private void returnToSubjectSelection() {
        stopCamera();
        mainLayout.setCenter(subjectSelectionLayout);
    }

    private void initializeCamera() {
        try {
            if (capture == null) {
                capture = new OpenCVFrameGrabber(0);
                capture.start();
            }
            showCameraPreview();
            startScanning();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
            updateStatus("Error starting camera: " + e.getMessage());
        }
    }

    private void showCameraPreview() {
        if (cameraThread != null && cameraThread.isAlive()) {
            return;
        }

        cameraThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Frame frame = capture.grab();
                    if (frame != null) {
                        Mat mat = converterToMat.convert(frame);
                        Image image = mat2Image(mat);
                        Platform.runLater(() -> cameraView.setImage(image));

                        if (isScanning.get()) {
                            processFrame(mat);
                        }

                        mat.release();
                    }
                    Thread.sleep(33);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> updateStatus("Camera error: " + e.getMessage()));
            }
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void startScanning() {
        attendanceMarked = false;
        isScanning.set(true);
        updateStatus("Scanning... Please stand in front of the camera.");
        nextButton.setDisable(true);
    }

    private void processFrame(Mat mat) {
        if (attendanceMarked || !isScanning.get()) {
            return;
        }

        Mat grayMat = new Mat();
        cvtColor(mat, grayMat, COLOR_BGR2GRAY);

        RectVector faceDetections = new RectVector();
        faceDetector.detectMultiScale(grayMat, faceDetections);

        if (faceDetections.empty()) {
            Platform.runLater(() -> updateStatus("No face detected. Please stand in front of the camera."));
            resetPredictionBuffer();
            return;
        }

        Rect rect = faceDetections.get(0);

        if (rect.width() < 50 || rect.height() < 50) {
            Platform.runLater(() -> updateStatus("Face too small to recognize. Please move closer."));
            resetPredictionBuffer();
            return;
        }

        Mat face = new Mat(grayMat, rect);
        FaceRecognitionTrainer.RecognitionResult result = faceRecognizer.recognizeFace(face);

        addPrediction(result);

        if (predictionBuffer.size() >= PREDICTION_BUFFER_SIZE) {
            processRecognitionResults();
        } else {
            Platform.runLater(() -> updateStatus("Analyzing... Please keep your face in view. (" + predictionBuffer.size() + "/" + PREDICTION_BUFFER_SIZE + ")"));
        }

        grayMat.release();
    }

    private void resetPredictionBuffer() {
        predictionBuffer.clear();
    }

    private void addPrediction(FaceRecognitionTrainer.RecognitionResult result) {
        predictionBuffer.offer(result);
        if (predictionBuffer.size() > PREDICTION_BUFFER_SIZE) {
            predictionBuffer.poll();
        }
    }

    private void processRecognitionResults() {
        Map<String, Integer> labelCounts = new HashMap<>();
        double totalConfidence = 0;

        for (FaceRecognitionTrainer.RecognitionResult result : predictionBuffer) {
            labelCounts.put(result.enrollmentNumber, labelCounts.getOrDefault(result.enrollmentNumber, 0) + 1);
            totalConfidence += result.confidence;
        }

        double averageConfidence = totalConfidence / predictionBuffer.size();

        String mostFrequentLabel = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : labelCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequentLabel = entry.getKey();
            }
        }

        final double recognitionThreshold = PREDICTION_BUFFER_SIZE * 0.6;
        final String finalMostFrequentLabel = mostFrequentLabel;
        final int finalMaxCount = maxCount;

        Platform.runLater(() -> {
            if (finalMaxCount >= recognitionThreshold) {
                if (averageConfidence < CONFIDENCE_THRESHOLD) {
                    if ("Unknown".equals(finalMostFrequentLabel) || "0".equals(finalMostFrequentLabel)) {
                        updateStatus("Face not recognized. Please register or try again.");
                        showButtons(true);
                        nextButton.setDisable(true);
                    } else {
                        if (!attendanceMarked) {
                            markAttendance(finalMostFrequentLabel);
                            attendanceMarked = true;
                            isScanning.set(false);
                        } else {
                            updateStatus("Attendance already marked for " + finalMostFrequentLabel);
                        }
                    }
                } else {
                    updateStatus("Face not recognized with sufficient confidence. Please try again.");
                    showButtons(true);
                    nextButton.setDisable(true);
                }
            } else {
                updateStatus("Face not recognized consistently. Please try again.");
                showButtons(true);
                nextButton.setDisable(true);
            }
        });

        resetPredictionBuffer();
    }

    private void showButtons(boolean show) {
        Platform.runLater(() -> {
            rescanButton.setVisible(show);
            registerButton.setVisible(show);
        });
    }
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private Image mat2Image(Mat frame) {
        try {
            Java2DFrameConverter converter = new Java2DFrameConverter();
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
            Frame javaFrame = converterToMat.convert(frame);
            return SwingFXUtils.toFXImage(converter.getBufferedImage(javaFrame, 1.0), null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void markAttendance(String enrollmentNumber) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT * FROM attendance WHERE enrollmentNumber = ? AND date = ? AND subject = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, enrollmentNumber);
                checkStmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                checkStmt.setString(3, selectedSubject);

                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    Platform.runLater(() -> {
                        updateStatus("Attendance already marked for " + enrollmentNumber + " today in " + selectedSubject);
                        nextButton.setDisable(false);
                        isScanning.set(false);
                        attendanceMarked = true;
                    });
                    return;
                }
            }

            String sql = "INSERT INTO attendance (enrollmentNumber, date, time, subject, status) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, enrollmentNumber);
                pstmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
                pstmt.setTime(3, java.sql.Time.valueOf(LocalTime.now()));
                pstmt.setString(4, selectedSubject);
                pstmt.setString(5, "Present");

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    Platform.runLater(() -> {
                        updateStatus("Attendance marked for " + enrollmentNumber + " in " + selectedSubject);
                        nextButton.setDisable(false);
                    });
                    Map<String, LocalDateTime> subjectAttendanceTimes = lastAttendanceTime.getOrDefault(enrollmentNumber, new HashMap<>());
                    subjectAttendanceTimes.put(selectedSubject, LocalDateTime.now());
                    lastAttendanceTime.put(enrollmentNumber, subjectAttendanceTimes);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> updateStatus("Error marking attendance: " + e.getMessage()));
        }
    }

    private void resetAttendance() {
        attendanceMarked = false;
        isScanning.set(true);
        nextButton.setDisable(true);
        updateStatus("Rescanning... Please stand in front of the camera.");
        resetPredictionBuffer();
    }

    private void returnToMainScreen() {
        stopCamera();
        MainScreen mainScreen = new MainScreen();
        mainScreen.start(primaryStage);
        if (primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    private void goToRegisterScreen() {
        stopCamera();
        RegisterUserScreen registerScreen = new RegisterUserScreen(primaryStage);
        registerScreen.show();
    }

    private void stopCamera() {
        isScanning.set(false);
        if (cameraThread != null) {
            cameraThread.interrupt();
            try {
                cameraThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (capture != null) {
            try {
                capture.stop();
                capture.release();
                capture = null;
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
    }
}
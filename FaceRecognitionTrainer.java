package io.itpl.facerecognition;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import io.itpl.ui.RegisterUserScreen;

public class FaceRecognitionTrainer
{
    private LBPHFaceRecognizer faceRecognizer;
    private CascadeClassifier faceDetector;
    private Map<Integer, String> labelMap;
    private static final String MODEL_FILE = "trained_model.yml";
    private static final String LABEL_MAP_FILE = "label_map.txt";

    public FaceRecognitionTrainer()
    {
        faceRecognizer = LBPHFaceRecognizer.create();
        labelMap = new HashMap<>();
        loadFaceDetector();
    }

    private void loadFaceDetector()
    {
        String cascadePath = getPathToXml("haarcascade_frontalface_default.xml");
        if (cascadePath != null)
        {
            faceDetector = new CascadeClassifier(cascadePath);
            if (faceDetector.empty())
            {
                System.err.println("Failed to load face detector cascade classifier.");
                loadClassifierFromStream("haarcascade_frontalface_default.xml");
            }
        }
        else
        {
            System.err.println("Failed to get path to cascade XML file.");
            loadClassifierFromStream("haarcascade_frontalface_default.xml");
        }
    }

    private void loadClassifierFromStream(String resourceName)
    {
        try (InputStream is = getClass().getResourceAsStream("/" + resourceName))
        {
            if (is == null)
            {
                System.err.println("Could not find resource: " + resourceName);
                return;
            }
            File tempFile = File.createTempFile("cascade", ".xml");
            tempFile.deleteOnExit();
            try (FileOutputStream os = new FileOutputStream(tempFile))
            {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1)
                {
                    os.write(buffer, 0, bytesRead);
                }
            }
            faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());
            System.out.println("Classifier loaded from stream. Empty: " + faceDetector.empty());
        }
        catch (IOException e)
        {
            System.err.println("Error loading classifier from stream: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getPathToXml(String xmlFilename)
    {
        System.out.println("Attempting to load XML file: " + xmlFilename);

        File file = new File("src/main/resources/" + xmlFilename);
        if (file.exists())
        {
            System.out.println("Found XML file as a file: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        }

        URL resource = getClass().getResource("/" + xmlFilename);
        if (resource != null)
        {
            System.out.println("Found XML file as a resource: " + resource.getPath());
            try
            {
                File resourceFile = Paths.get(resource.toURI()).toFile();
                System.out.println("Resource file path: " + resourceFile.getAbsolutePath());
                return resourceFile.getAbsolutePath();
            }
            catch (URISyntaxException e)
            {
                System.err.println("Error converting resource URL to file: " + e.getMessage());
            }
        }

        file = new File(xmlFilename);
        if (file.exists())
        {
            System.out.println("Found XML file in current directory: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        }

        System.err.println("Failed to find XML file: " + xmlFilename);
        return null;
    }

    public void trainModel()
    {
        MatVector images = new MatVector();
        Mat labels = new Mat();

        File baseDir = new File(RegisterUserScreen.BASE_IMAGE_PATH);
        File[] userDirs = baseDir.listFiles(File::isDirectory);

        int label = 0;
        int totalImages = 0;
        int totalFaces = 0;
        if (userDirs != null)
        {
            for (File userDir : userDirs)
            {
                String enrollmentNumber = userDir.getName();
                List<String> imagePaths = RegisterUserScreen.getImagePathsForUser(enrollmentNumber);

                System.out.println("Processing user: " + enrollmentNumber);
                System.out.println("Number of images found: " + imagePaths.size());

                int userFaces = 0;
                for (String imagePath : imagePaths) {
                    Mat image = imread(imagePath, IMREAD_GRAYSCALE);
                    RectVector faceDetections = new RectVector();
                    faceDetector.detectMultiScale(image, faceDetections,
                            1.1, 3, 0,
                            new Size(30, 30), new Size(image.cols(), image.rows()));

                    System.out.println("Image: " + imagePath);
                    System.out.println("Faces detected: " + faceDetections.size());

                    if (faceDetections.size() > 0)
                    {
                        Rect largestFace = getLargestFace(faceDetections);
                        Mat face = new Mat(image, largestFace);
                        Mat resizedFace = new Mat();
                        resize(face, resizedFace, new Size(100, 100));
                        images.push_back(resizedFace);
                        labels.push_back(new Mat(new int[]{label}));
                        userFaces++;
                        totalFaces++;
                    }
                    else
                    {
                        System.out.println("No face detected in image: " + imagePath);
                    }
                    totalImages++;
                }
                System.out.println("Total faces detected for user " + enrollmentNumber + ": " + userFaces);
                labelMap.put(label, enrollmentNumber);
                label++;
            }
        }

        if (images.empty())
        {
            System.err.println("No images found for training. Please check the image directories.");
            return;
        }

        faceRecognizer.train(images, labels);
        saveModel();
        System.out.println("Model trained successfully.");
        System.out.println("Total images processed: " + totalImages);
        System.out.println("Total faces detected and used for training: " + totalFaces);
    }

    private Rect getLargestFace(RectVector faceDetections)
    {
        Rect largestFace = new Rect();
        double largestArea = 0;
        for (long i = 0; i < faceDetections.size(); i++)
        {
            Rect face = faceDetections.get(i);
            double area = face.area();
            if (area > largestArea) {
                largestArea = area;
                largestFace = face;
            }
        }
        return largestFace;
    }

    public void updateModel(String newUserEnrollmentNumber)
    {
        MatVector images = new MatVector();
        Mat labels = new Mat();

        int newLabel = labelMap.size();
        List<String> imagePaths = RegisterUserScreen.getImagePathsForUser(newUserEnrollmentNumber);

        for (String imagePath : imagePaths) {
            Mat image = imread(imagePath, IMREAD_GRAYSCALE);
            RectVector faceDetections = new RectVector();
            faceDetector.detectMultiScale(image, faceDetections);

            for (long i = 0; i < faceDetections.size(); i++)
            {
                Rect rect = faceDetections.get(i);
                Mat face = new Mat(image, rect);
                Mat resizedFace = new Mat();
                resize(face, resizedFace, new Size(100, 100));
                images.push_back(resizedFace);
                labels.push_back(new Mat(new int[]{newLabel}));
            }
        }

        faceRecognizer.update(images, labels);
        labelMap.put(newLabel, newUserEnrollmentNumber);
        saveModel();
        System.out.println("Model updated successfully for user: " + newUserEnrollmentNumber);
    }

    private void saveModel()
    {
        faceRecognizer.save(MODEL_FILE);
        saveLabelMap();
        System.out.println("Model and label map saved successfully.");
    }

    private void saveLabelMap()
    {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LABEL_MAP_FILE)))
        {
            for (Map.Entry<Integer, String> entry : labelMap.entrySet())
            {
                writer.println(entry.getKey() + "," + entry.getValue());
            }
        }
        catch (IOException e)
        {
            System.err.println("Error saving label map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadModel()
    {
        File modelFile = new File(MODEL_FILE);
        if (!modelFile.exists())
        {
            System.out.println("Model file not found. A new model will be created when training.");
            return;
        }
        try
        {
            faceRecognizer.read(MODEL_FILE);
            System.out.println("Model loaded successfully.");
            loadLabelMap();
        }
        catch (Exception e)
        {
            System.err.println("Error loading model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLabelMap()
    {
        labelMap.clear();
        Path path = Paths.get(LABEL_MAP_FILE);
        if (!Files.exists(path))
        {
            System.out.println("Label map file not found. It will be created when training.");
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split(",");
                if (parts.length == 2)
                {
                    int label = Integer.parseInt(parts[0]);
                    String enrollmentNumber = parts[1];
                    labelMap.put(label, enrollmentNumber);
                }
            }
            System.out.println("Label map loaded successfully.");
        }
        catch (IOException e)
        {
            System.err.println("Error loading label map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public RecognitionResult recognizeFace(Mat face)
    {
        if (face.empty())
        {
            System.err.println("Error: Face image is empty");
            return new RecognitionResult("Unknown", Double.MAX_VALUE);
        }

        Mat grayFace = new Mat();
        if (face.channels() > 1)
        {
            cvtColor(face, grayFace, COLOR_BGR2GRAY);
        } else
        {
            grayFace = face;
        }

        Mat resizedFace = new Mat();
        resize(grayFace, resizedFace, new Size(100, 100));

        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        try
        {
            faceRecognizer.predict(resizedFace, label, confidence);
        }
        catch (Exception e)
        {
            System.err.println("Error during face recognition: " + e.getMessage());
            e.printStackTrace();
            return new RecognitionResult("Unknown", Double.MAX_VALUE);
        }

        int predictedLabel = label.get();
        double predictionConfidence = confidence.get();

        System.out.println("Predicted Label: " + predictedLabel + ", Confidence: " + predictionConfidence);

        if (labelMap.containsKey(predictedLabel))
        {
            return new RecognitionResult(labelMap.get(predictedLabel), predictionConfidence);
        }
        else
        {
            return new RecognitionResult("Unknown", Double.MAX_VALUE);
        }
    }

    public static class RecognitionResult
    {
        public final String enrollmentNumber;
        public final double confidence;

        public RecognitionResult(String enrollmentNumber, double confidence)
        {
            this.enrollmentNumber = enrollmentNumber;
            this.confidence = confidence;
        }
    }
}
package io.itpl;

import io.itpl.ui.MainScreen;
import org.opencv.core.Core;

public class Main
{
    static
    {
        try
        {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV loaded successfully");
        }
        catch (UnsatisfiedLinkError e)
        {
            System.err.println("Failed to load OpenCV library");
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        MainScreen.main(args);
    }
}
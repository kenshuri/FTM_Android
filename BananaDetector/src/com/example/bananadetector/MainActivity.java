package com.example.bananadetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * Activit� principale de l'application
 * <p> Cette activit� est appel�e au lancement de l'application. Son layout est
 * <i>activity_main.xml</i> qui se trouve dans le dossier /res/layout.
 * 
 * @author Alexis Sciau
 *
 */

public class MainActivity extends Activity implements CvCameraViewListener2 {
	/**
	 * TAG utilis� pour l'affichage d'informations lors du debug
	 */
    private static final String TAG = "Banana Detector";

    /**
     * Vue de la cam�ra
     */
    private CameraBridgeViewBase mOpenCvCameraView;
    
    /**
     * Fichier contenant les informations du classifieur
     */
    private File                 mCascadeFile;
    
    /**
     * Le classifieur charg�
     */
    private CascadeClassifier    mCascadeClassifier;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    
                    try {
                        // On charge le fichier XML contenant les donn�es du classifieur (on l'a ajout� au dossier res/raw)
                        InputStream is = getResources().openRawResource(R.raw.banana);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "banana.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mCascadeClassifier.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mCascadeClassifier = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /**
     * Constructeur par d�faut de l'activit�
     */
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Appel�e lorque l'activit� est cr��e
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);       
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    /**
     * Appel�e lorsque l'activit� est mise en pause (elle n'est plus au premier plan)
     */
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * Appel�e lorsque l'activit� revient au premier plan
     */
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    /**
     * Appel�e lorsque l'activit� est d�truite
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * M�thode qui doit �tre pr�sente, mais elle n'est pas utile
     */
    public void onCameraViewStarted(int width, int height) {
    }
    
    /**
     * M�thode qui doit �tre pr�sente, mais elle n'est pas utile
     */
    public void onCameraViewStopped() {
    }

    /**
     * Appel�e � chaque nouvelle prise de vue par la cam�ra.
     * 
     * D�tecte � chaque nouvelle prise de vue s'il y a une banane ou pas
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	//On va essayer de d�tecter la pr�sence d'une banane pour chaque nouvelle image
    	//capt�e par le t�l�phone
    	Mat Grey = inputFrame.gray(); //Image prise par la cam�ra
    	MatOfRect bananas = new MatOfRect();
    	Size minSize = new Size(30,20);
    	Size maxSize = new Size(150,100);
    	mCascadeClassifier.detectMultiScale(Grey, bananas, 1.1, 10, 0,minSize,maxSize);
    	if (bananas.rows()>0){
    		Log.i(TAG, "Nombre de bananes d�tect�es : " + bananas.rows());
    		Log.i(TAG, "Largeur de la banane : " + bananas.toList().get(0).width);
    		Log.i(TAG, "Hauteur de la banane : " + bananas.toList().get(0).height);
    	}
    	
        return inputFrame.rgba();
    }
}

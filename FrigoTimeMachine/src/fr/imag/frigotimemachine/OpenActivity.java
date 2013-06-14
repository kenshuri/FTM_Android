package fr.imag.frigotimemachine;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import fr.imag.frigotimemachine.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Deuxième activité de l'application.
 * <p> Elle est appelée par <i>MainActivity</i>. Son layout est <i>open_surface_view.xml</i> 
 * qui se trouve dans le dossier /res/layout. Elle peut être lancée pour deux objectifs différents :
 * <ul>
 * <li> Soit pour détecter l'évènement <b>"La porte est stable"</b></li>
 * <li> Soit pour détecter l'évènement <b>"La porte est fermée"</b></li>
 * </ul>
 * </p>
 * @author Alexis Sciau
 *
 */
public class OpenActivity extends Activity implements CvCameraViewListener2{
	/**
	 * Tag utilisé pour l'affichage d'infomation lors du débuggage
	 */
	private static final String TAG = "OpenActivity";
	
	/**
	 * Vu de l'activité
	 */
    private CameraBridgeViewBase mOpenCvCameraView;
    
    /**
     * Matrice buffer servant a stocker la dernière image 
     */
    private Mat buffer = null;
    
    /**
     * Contient les normes calculées dans <b>onCameraFrame</b>
     * 
     * @see OpenActivity#onCameraFrame(CvCameraViewFrame)
     */
    private List<Double> n = new ArrayList<Double>();
    /**
     * Compteur du nombre d'image observées
     * 
     * @see OpenActivity#onCameraFrame(CvCameraViewFrame)
     */
    private int compteur = 0;
    
    /**
     * Vrai si la porte du frigo est stable, faux sinon
     */
    private boolean stable = false;
    
    /**
     * Vrai si la porte du frigo est fermée, faux sinon
     */
    private boolean fermee = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
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
     * Créateur par défaut de l'activité OpenActivity
     */
    public OpenActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Appelée lorsque l'activité est créée */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        compteur = 0;
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.open_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.open_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }
    
    /**
     * Appelée lorsque l'activité est mise en pause (l'activité n'est plus en premier plan)
     */
    @Override
    public void onPause()
    {
        super.onPause();
        //genereFichier();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    /**
     * Appelée lorsque l'activité revient au premier plan
     */
    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }
    
    /**
     * Appelée lors de la destruction de l'activité
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }
    
    /**
     * Appelée à chaque nouvelle image captée par la caméra
     * 
     * <p> Le comportement de cette fonction est différent en fonction
     * de la manière dont a été appelée <i>OpenActivity</i> :
     * <ul>
     * <li> Si l'extra de clé Choix vaut 0, la fonction va chercher a détectée
     * l'événement "La porte est fermée" </li>
     * <li> Si l'extra de clé Choix vaut 1, la fonction va chercher a détectée
     * l'événement "La porte est stable" </li>
     * </ul>
     * </p>
     * 
     * @param inputFrame Image captée par la caméra
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat current = inputFrame.rgba();
    	Intent mIntent = getIntent();
    	int choix = mIntent.getIntExtra(MainActivity.Choix,2);
    	if (choix == 0){
    		//Une photo a été prise
    		//On va rechercher l'événement : le flux vidéo représente des images noires
    		Scalar scalaireN = new Scalar(0x00,0x00,0x00,0xFF);
    		Mat noir = new Mat(current.size(),current.type(),scalaireN);
    		//Comparaison avec une image noire, résultat stocké dans une matrice diffNoir
    		Mat diffNoir = new Mat(current.size(),current.type());
    		Core.absdiff(current, noir, diffNoir);
    		Double normeDiffNoir = new Double(Core.norm(diffNoir));
    		n.add(normeDiffNoir);
    		compteur++;		//Compteur du nombre d'images prises
    		if (compteur > 11) {
    			//S'il y a suffisamment d'images déjà prises, on vérifie que la porte est fermée
    			fermee = true;
    			int i =0;
    			while (fermee && i<10) {
    				//La porte est fermee si sur les dix dernières photos prises, la différence
    				//entre une image noire et l'image current n'est pas trop grande.
    				if (n.get(compteur-1-i) > 4500) {
    					fermee = false;
    				}
    				i++;
    				}
    			if (fermee){
    				//Remise à 0 du compteur s'il doit être réutilisé pour une nouvelle photo
					//De même pour le tableau n
    				compteur = 0;
    				n.clear();
					finish();
    			}
    		}
    	} else if (choix == 1){
    		//Aucune photo n'a encore été prise
    		Mat diffBuffer = new Mat(current.size(),current.type());
    		if (buffer == null) {
    			buffer = new Mat(current.size(),current.type());
    			buffer = current.clone();
    		} else {
    			Core.absdiff(current, buffer, diffBuffer);
    			Double normeDiffBuffer = new Double(Core.norm(diffBuffer));
    			n.add(normeDiffBuffer);
    			compteur++;		//Compteur du nombre d'images prises
    			if (compteur > 11) {
    				//S'il y a suffisamment d'images déjà prises, on vérifie que la porte est stable
    				stable = true;
    				int i = 0;
    				while (stable && i<10) {
    					//On est stable si sur les dix dernières prises, la différence entre
    					//l'image current est l'image stockée n'est pas trop grande
    					if(n.get(compteur-1-i)>4500){
    						stable = false;
    					}
    					i++;
    				}
    				if (stable){
    					//Remise à 0 du compteur s'il doit être réutilisé pour une nouvelle photo
    					//De même pour le tableau n
    					compteur = 0;
    					n.clear();
    					finish();
    				}
    			}
    			buffer = current.clone();
    		}
    	}
    	return inputFrame.rgba();
    }
    
    /**
     * Appelée lorsque l'activité est fermée
     * 
     * <p>Le comportement de cette fonction est différent en fonction
     * de la manière dont a été appelée <i>OpenActivity</i> :
     * <ul>
     * <li> Si l'extra de clé Choix vaut 0, on va fournir à l'activité appelante un extra
     * de clé Choix dont la valeur vaut 0, ce qui indiquera que la porte est fermée</li>
     * <li> Si l'extra de clé Choix vaut 1, on va fournir à l'activité appelante un extra
     * de clé Choix dont la valeur vaut 1, ce qui indiquera que la porte est stable </li>
     * </ul>
     * </p>
     */
    @Override
    public void finish() {
      // Création d'un intent à renvoyer à l'activité appelante
      Intent result = new Intent();
      Intent mIntent = getIntent();
      int choix = mIntent.getIntExtra(MainActivity.Choix, -1);
      if (choix == 0) {
    	  //La porte est fermée
			result.putExtra(MainActivity.Choix, 0);
			setResult(RESULT_OK, result);
      } else if (choix == 1){
    	  //La porte est stable
			result.putExtra(MainActivity.Choix, 1);
			setResult(RESULT_OK, result);
      }
      setResult(RESULT_OK, result);
      super.finish();
    } 
    
    /**
     * Méthode interne de debug. Permet de générer un fichier contenant
     * le contenu du tableau n
     */
    void genereFichier() {
		// Génération du fichier de données
		 File FileDir = getDir();

		    if (!FileDir.exists() && !FileDir.mkdirs()) {

		      Toast.makeText(this, "Can't create directory to save image.",Toast.LENGTH_LONG).show();
		      return;

		    }

		    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
		    String date = dateFormat.format(new Date());
		    String nameDataFile = "Norme_" + date + ".txt";

		    String filename = FileDir.getPath() + File.separator + nameDataFile;

		    File dataFile = new File(filename);
		    
		    try {
		      FileWriter fx = new FileWriter(filename);
		      for (Double m : n) {
		    	  fx.write(m.doubleValue() + "\r\n");
		      }
		      fx.close();
		      Toast.makeText(this, "New File saved:" + nameDataFile,
		          Toast.LENGTH_LONG).show();
		    } catch (Exception error) {
		      Toast.makeText(this, "File could not be saved.",
		          Toast.LENGTH_LONG).show();
		    }
	}
	
    /**
     * Méthode interne qui renvoie un dossier de destination
     * 
     * @return Le dossier de destination
     */
	private File getDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		return new File(sdDir, "FrigoTimeMachine");
	}
}

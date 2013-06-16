package fr.imag.frigotimemachinev2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * <b> Deuxième activité de l'application.</b> 
 * <p> Elle est appelée par <i>MainActivity</i>. Son layout est <i>open_surface_view.xml</i> 
 * qui se trouve dans le dossier /res/layout.</p>
 * 
 * @author Alexis Sciau
 *
 */
public class OpenActivity extends Activity implements CvCameraViewListener2{
	/**
	 * Tag utilisé pour l'affichage d'infomation lors du débuggage
	 */
	private static final String TAG = "OpenActivity";
	
	/**
	 * Vue de l'activité
	 */
    private CameraBridgeViewBase mOpenCvCameraView;
    
    /**
     * Matrice buffer servant a stocker la dernière image prise
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
    
    /**
     * Fichier contenant les informations du classifieur (reconnaisance d'objet)
     */
    private File                 mCascadeFile;
    
    /**
     * Le classifieur chargé (reconnaisance d'objet)
     */
    private CascadeClassifier    mCascadeClassifier;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //Ce qui suit concerne la reconnaissance d'objet, à commenter si non souhaité
                    try {
                        // On charge le fichier XML contenant les données du classifieur (on l'a ajouté au dossier res/raw)
                        InputStream is = getResources().openRawResource(R.raw.banana);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
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
                    //Fin de la partie sur la reconnaissance d'image
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
     * Constructeur par défaut de l'activité
     */
    public OpenActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** 
     * Appelée lorque l'activité est créée 
     * */
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
     * Appelée lorsque l'activité est mise en pause (elle n'est plus au premier plan)
     */
    @Override
    public void onPause()
    {
        super.onPause();
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
     * Appelée lorsque l'activité est détruite
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /**
     * Méthode qui doit être présente, mais elle n'est pas utile
     */
    public void onCameraViewStarted(int width, int height) {
    }

    /**
     * Méthode qui doit être présente, mais elle n'est pas utile
     */
    public void onCameraViewStopped() {
    }

    /**
     * Appelée à chaque nouvelle prise de vue par la caméra.
     * 
     * <p>Son comportement sera différent suivant ce que l'on cherche à faire :
     * <ul>
     * <li>Si la porte n'est pas stable, on cherche alors à détecter l'événement
     * porte stable pour pouvoir prendre une photo.</li>
     * <li>Si la porte est stable mais pas fermée, cela signifie que l'on a déjà
     * pris une photo du contenu du frigo et on attend que la porte soit fermée pour revenir
     * dans l'état initial. </li>
     * </ul>
     * </p>
     * 
     * @param inputFrame Image captée par la caméra
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat current = inputFrame.rgba();
    	if (stable && !fermee){
    		//Une photo a été prise
    		//On va rechercher l'événement : le flux vidéo représente des images noires
    		Scalar scalaireN = new Scalar(0x00,0x00,0x00,0xFF);
    		Mat noir = new Mat(current.size(),current.type(),scalaireN);
    		//noir est une matrice noire
    		//Comparaison avec une image noire, résultat stocké dans une matrice diffNoir
    		Mat diffNoir = new Mat(current.size(),current.type());
    		Core.absdiff(current, noir, diffNoir); 
    		Double normeDiffNoir = new Double(Core.norm(diffNoir)); //Calclule de la norme de cette matrice
    		n.add(normeDiffNoir); //Ajout de cette norme dans un conteneur
    		compteur++;		//Compteur du nombre d'images prises
    		if (compteur > 11) {
    			//S'il y a suffisamment d'images déjà prises, on vérifie que la porte est fermée
    			fermee = true; 
    			int i =0;
    			while (fermee && i<10) {
    				//La porte est fermee si sur les dix dernières photos prises, la différence
    				//entre une image noire et l'image current n'est pas trop grande.
    				if (n.get(compteur-1-i) > 4500) {
    					fermee = false; //Si cette différence est trop grande, on considère que la porte n'est pas fermée
    				}
    				i++;
    				} //Si elle n'a jamais été trop grande, la porte est effectivement fermée
    			if (fermee){
    				//Remise à 0 du compteur s'il doit être réutilisé pour une nouvelle photo
					//De même pour le tableau n
    				compteur = 0;
    				n.clear();
					finish(); //Retour sur l'activité principale qui attend une ouverture du frigo.
    			}
    		}
    	} else if (!stable){
    		//Aucune photo n'a encore été prise
    		//On va rechercher l'événement : l'image est stable
    		if (buffer == null) { //Première image reçue, il faut créer une matrice buffer qui contiendra l'image précédente
    			buffer = new Mat(current.size(),current.type());
    			buffer = current.clone();
    		} else { //C'est au moins la deuxième image reçue
    			//Comparaison entre l'image précédente et l'image courante, résultat stocké dans une matrice diffBuffer
    			Mat diffBuffer = new Mat(current.size(),current.type());
    			Core.absdiff(current, buffer, diffBuffer);
    			Double normeDiffBuffer = new Double(Core.norm(diffBuffer)); //Calcul de la norme de cette matrice
    			n.add(normeDiffBuffer); //Ajout de cette norme dans un conteneur
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
    					Log.i(TAG,"Prise de la photo");
    					//Si l'image est stable, il faut vérifier tout d'abord que la porte n'est pas fermée.
    					//(on effectue ici le même traîtement que pour une détection de porte fermée)
    					Scalar scalaireN = new Scalar(0x00,0x00,0x00,0xFF);
    					Mat noir = new Mat(current.size(),current.type(),scalaireN);
    					Mat diffNoir = new Mat(current.size(),current.type());
    					Core.absdiff(current, noir, diffNoir);
    					Double normeDiffNoir = new Double(Core.norm(diffNoir));
    					if (normeDiffNoir > 4500){
    						//Si la porte n'est pas fermée, on va sauvegarder l'image avant de l'envoyer
    						File pictureFileDir = getDir();
    						SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
    						String date = dateFormat.format(new Date());
    						String photoFile = "PictureCV_" + date + ".jpg"; //Nom du fichier
    						String filename = pictureFileDir.getPath() + File.separator + photoFile; 
    						//On doit convertir les couleurs avant de sauvegarder l'image.
    						//La description de la fonction cvtColor explique pourquoi
    						Imgproc.cvtColor(current, current, Imgproc.COLOR_BGR2RGB); 
    						Highgui.imwrite(filename, current); //Sauvegarde
    						Log.i(TAG,"Photo sauvegardée");
    						//Remise à 0 du compteur s'il doit être réutilisé pour une nouvelle photo
    						//De même pour le tableau n
    						compteur = 0;
    						n.clear();
    						
    						
    						//Tentative de reconnaissance d'image
    						//On va essayer de détecter la présence d'une banane pour chaque nouvelle image
    				    	//captée par le téléphone
    				    	Mat Grey = inputFrame.gray(); //Image prise par la caméra
    				    	MatOfRect bananas = new MatOfRect();
    				    	Size minSize = new Size(30,20);
    				    	Size maxSize = new Size(150,100);
    				    	Log.i(TAG, "Tentative de détection de banane");
    				    	mCascadeClassifier.detectMultiScale(Grey, bananas, 1.1, 0, 10,minSize,maxSize);
    				    	if (bananas.rows()>0){
    				    		Log.i(TAG, "Nombre de bananes détectées : " + bananas.rows());
    				    	}
    						envoiPhoto(filename, bananas.rows()); //Envoi de la photo avec les données de reconnaissance
    						//Fin de la reconnaissance de l'image
    						
    						
    						//envoiPhoto(filename); //Envoi de la photo sans les données de reconnaissance
    						
    					} else {
    						//Cas où a porte est fermée
    						//Remise à 0 du compteur s'il doit être réutilisé pour une nouvelle photo
    						//De même pour le tableau n
    						compteur = 0;
    						n.clear();
    						finish();
    					}
    				}
    			}
    			buffer = current.clone();
    		}
    	}
    	return inputFrame.rgba();
    }
    
    /**
     * Envoie une photo vers la plateforme web
     * 
     * @param filename Nom de la photo à envoyer
     */
    void envoiPhoto(String filename){
    	Log.i(TAG, "Envoie de la photo"); 	
    	File pictureFile = new File(filename); //On récupère le fichier image
    	// Initialisation du client HTTP
    	HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		/* Création de la requête POST. On lui donne en adresse l'adresse du serveur
		suivi de /upload. Le serveur mis en place pendant le projet attend
		une requête de ce type */
		HttpPost postRequest = new HttpPost("http://192.168.43.8:9001/upload");
		try {
			// Création de l'entité qui sera associée à la requête
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			//On lui ajoute les champs "picture" et "email"
			// !!Attention, les noms "picture" et "email" ont leur importance, c'est ce
			//qu'attend le serveur
			entity.addPart("picture", new FileBody(((File)pictureFile), "image/jpeg"));
			entity.addPart("email", new StringBody("emilie.paillous@gmail.com", "text/plain", Charset.forName("UTF-8")));
			postRequest.setEntity(entity); //Exécution de la requête
			String response = EntityUtils.toString(httpClient.execute(postRequest).getEntity(),"UTF-8");
			Log.i(TAG,"Requete exécutée");
		} catch (IOException e) {
			Log.i(TAG,"L'exécution de la requête lance une exception car : " + e.toString());
		}
		Log.i(TAG,"Sortie envoiPhoto"); 	
    }
    
    /**
     * Envoie une photo vers la plateforme web, avec le nombre de banane détectées
     * 
     * @param filename Nom de la photo à envoyer
     * @param nbBanana Nombre de bananes détectées
     */
    void envoiPhoto(String filename, int nbBanana){
    	Log.i(TAG, "Envoie de la photo"); 	
    	File pictureFile = new File(filename); //On récupère le fichier image
    	// Initialisation du client HTTP
    	HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		/* Création de la requête POST. On lui donne en adresse l'adresse du serveur
		suivi de /upload. Le serveur mis en place pendant le projet attend
		une requête de ce type */
		HttpPost postRequest = new HttpPost("http://192.168.43.8:9001/upload");
		try {
			// Création de l'entité qui sera associée à la requête
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			//On lui ajoute les champs "picture" et "email"
			// !!Attention, les noms "picture" et "email" ont leur importance, c'est ce
			//qu'attend le serveur
			entity.addPart("picture", new FileBody(((File)pictureFile), "image/jpeg"));
			entity.addPart("email", new StringBody("emilie.paillous@gmail.com", "text/plain", Charset.forName("UTF-8")));
			entity.addPart("nbBanana", new StringBody(""+nbBanana, "text/plain", Charset.forName("UTF-8")));
			postRequest.setEntity(entity); //Exécution de la requête
			String response = EntityUtils.toString(httpClient.execute(postRequest).getEntity(),"UTF-8");
			Log.i(TAG,"Requete exécutée");
		} catch (IOException e) {
			Log.i(TAG,"L'exécution de la requête lance une exception car : " + e.toString());
		}
		Log.i(TAG,"Sortie envoiPhoto"); 	
    }
    
    /**
     * Méthode interne de debug. Permet de générer un fichier contenant
     * le contenu du tableau n
     */
    void genereFichier() {
		// Génération du fichier de données des normes (pour être analyser par la suite)
		 File FileDir = getDir();
		    if (!FileDir.exists() && !FileDir.mkdirs()) {
		      Toast.makeText(this, "Can't create directory to save image.",Toast.LENGTH_LONG).show();
		      return;
		    }
		    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss.SS");
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

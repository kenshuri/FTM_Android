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
 * <b> Deuxi�me activit� de l'application.</b> 
 * <p> Elle est appel�e par <i>MainActivity</i>. Son layout est <i>open_surface_view.xml</i> 
 * qui se trouve dans le dossier /res/layout.</p>
 * 
 * @author Alexis Sciau
 *
 */
public class OpenActivity extends Activity implements CvCameraViewListener2{
	/**
	 * Tag utilis� pour l'affichage d'infomation lors du d�buggage
	 */
	private static final String TAG = "OpenActivity";
	
	/**
	 * Vue de l'activit�
	 */
    private CameraBridgeViewBase mOpenCvCameraView;
    
    /**
     * Matrice buffer servant a stocker la derni�re image prise
     */
    private Mat buffer = null;
    
    /**
     * Contient les normes calcul�es dans <b>onCameraFrame</b>
     * 
     * @see OpenActivity#onCameraFrame(CvCameraViewFrame)
     */
    private List<Double> n = new ArrayList<Double>();
    /**
     * Compteur du nombre d'image observ�es
     * 
     */
    private int compteur = 0;
    
    /**
     * Vrai si la porte du frigo est stable, faux sinon
     */
    private boolean stable = false;
    
    /**
     * Vrai si la porte du frigo est ferm�e, faux sinon
     */
    private boolean fermee = false;
    
    /**
     * Fichier contenant les informations du classifieur (reconnaisance d'objet)
     */
    private File                 mCascadeFile;
    
    /**
     * Le classifieur charg� (reconnaisance d'objet)
     */
    private CascadeClassifier    mCascadeClassifier;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //Ce qui suit concerne la reconnaissance d'objet, � commenter si non souhait�
                    try {
                        // On charge le fichier XML contenant les donn�es du classifieur (on l'a ajout� au dossier res/raw)
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
     * Constructeur par d�faut de l'activit�
     */
    public OpenActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** 
     * Appel�e lorque l'activit� est cr��e 
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
     * <p>Son comportement sera diff�rent suivant ce que l'on cherche � faire :
     * <ul>
     * <li>Si la porte n'est pas stable, on cherche alors � d�tecter l'�v�nement
     * porte stable pour pouvoir prendre une photo.</li>
     * <li>Si la porte est stable mais pas ferm�e, cela signifie que l'on a d�j�
     * pris une photo du contenu du frigo et on attend que la porte soit ferm�e pour revenir
     * dans l'�tat initial. </li>
     * </ul>
     * </p>
     * 
     * @param inputFrame Image capt�e par la cam�ra
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	Mat current = inputFrame.rgba();
    	if (stable && !fermee){
    		//Une photo a �t� prise
    		//On va rechercher l'�v�nement : le flux vid�o repr�sente des images noires
    		Scalar scalaireN = new Scalar(0x00,0x00,0x00,0xFF);
    		Mat noir = new Mat(current.size(),current.type(),scalaireN);
    		//noir est une matrice noire
    		//Comparaison avec une image noire, r�sultat stock� dans une matrice diffNoir
    		Mat diffNoir = new Mat(current.size(),current.type());
    		Core.absdiff(current, noir, diffNoir); 
    		Double normeDiffNoir = new Double(Core.norm(diffNoir)); //Calclule de la norme de cette matrice
    		n.add(normeDiffNoir); //Ajout de cette norme dans un conteneur
    		compteur++;		//Compteur du nombre d'images prises
    		if (compteur > 11) {
    			//S'il y a suffisamment d'images d�j� prises, on v�rifie que la porte est ferm�e
    			fermee = true; 
    			int i =0;
    			while (fermee && i<10) {
    				//La porte est fermee si sur les dix derni�res photos prises, la diff�rence
    				//entre une image noire et l'image current n'est pas trop grande.
    				if (n.get(compteur-1-i) > 4500) {
    					fermee = false; //Si cette diff�rence est trop grande, on consid�re que la porte n'est pas ferm�e
    				}
    				i++;
    				} //Si elle n'a jamais �t� trop grande, la porte est effectivement ferm�e
    			if (fermee){
    				//Remise � 0 du compteur s'il doit �tre r�utilis� pour une nouvelle photo
					//De m�me pour le tableau n
    				compteur = 0;
    				n.clear();
					finish(); //Retour sur l'activit� principale qui attend une ouverture du frigo.
    			}
    		}
    	} else if (!stable){
    		//Aucune photo n'a encore �t� prise
    		//On va rechercher l'�v�nement : l'image est stable
    		if (buffer == null) { //Premi�re image re�ue, il faut cr�er une matrice buffer qui contiendra l'image pr�c�dente
    			buffer = new Mat(current.size(),current.type());
    			buffer = current.clone();
    		} else { //C'est au moins la deuxi�me image re�ue
    			//Comparaison entre l'image pr�c�dente et l'image courante, r�sultat stock� dans une matrice diffBuffer
    			Mat diffBuffer = new Mat(current.size(),current.type());
    			Core.absdiff(current, buffer, diffBuffer);
    			Double normeDiffBuffer = new Double(Core.norm(diffBuffer)); //Calcul de la norme de cette matrice
    			n.add(normeDiffBuffer); //Ajout de cette norme dans un conteneur
    			compteur++;		//Compteur du nombre d'images prises
    			if (compteur > 11) {
    				//S'il y a suffisamment d'images d�j� prises, on v�rifie que la porte est stable
    				stable = true;
    				int i = 0;
    				while (stable && i<10) {
    					//On est stable si sur les dix derni�res prises, la diff�rence entre
    					//l'image current est l'image stock�e n'est pas trop grande
    					if(n.get(compteur-1-i)>4500){
    						stable = false;
    					}
    					i++;
    				}
    				if (stable){
    					Log.i(TAG,"Prise de la photo");
    					//Si l'image est stable, il faut v�rifier tout d'abord que la porte n'est pas ferm�e.
    					//(on effectue ici le m�me tra�tement que pour une d�tection de porte ferm�e)
    					Scalar scalaireN = new Scalar(0x00,0x00,0x00,0xFF);
    					Mat noir = new Mat(current.size(),current.type(),scalaireN);
    					Mat diffNoir = new Mat(current.size(),current.type());
    					Core.absdiff(current, noir, diffNoir);
    					Double normeDiffNoir = new Double(Core.norm(diffNoir));
    					if (normeDiffNoir > 4500){
    						//Si la porte n'est pas ferm�e, on va sauvegarder l'image avant de l'envoyer
    						File pictureFileDir = getDir();
    						SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
    						String date = dateFormat.format(new Date());
    						String photoFile = "PictureCV_" + date + ".jpg"; //Nom du fichier
    						String filename = pictureFileDir.getPath() + File.separator + photoFile; 
    						//On doit convertir les couleurs avant de sauvegarder l'image.
    						//La description de la fonction cvtColor explique pourquoi
    						Imgproc.cvtColor(current, current, Imgproc.COLOR_BGR2RGB); 
    						Highgui.imwrite(filename, current); //Sauvegarde
    						Log.i(TAG,"Photo sauvegard�e");
    						//Remise � 0 du compteur s'il doit �tre r�utilis� pour une nouvelle photo
    						//De m�me pour le tableau n
    						compteur = 0;
    						n.clear();
    						
    						
    						//Tentative de reconnaissance d'image
    						//On va essayer de d�tecter la pr�sence d'une banane pour chaque nouvelle image
    				    	//capt�e par le t�l�phone
    				    	Mat Grey = inputFrame.gray(); //Image prise par la cam�ra
    				    	MatOfRect bananas = new MatOfRect();
    				    	Size minSize = new Size(30,20);
    				    	Size maxSize = new Size(150,100);
    				    	Log.i(TAG, "Tentative de d�tection de banane");
    				    	mCascadeClassifier.detectMultiScale(Grey, bananas, 1.1, 0, 10,minSize,maxSize);
    				    	if (bananas.rows()>0){
    				    		Log.i(TAG, "Nombre de bananes d�tect�es : " + bananas.rows());
    				    	}
    						envoiPhoto(filename, bananas.rows()); //Envoi de la photo avec les donn�es de reconnaissance
    						//Fin de la reconnaissance de l'image
    						
    						
    						//envoiPhoto(filename); //Envoi de la photo sans les donn�es de reconnaissance
    						
    					} else {
    						//Cas o� a porte est ferm�e
    						//Remise � 0 du compteur s'il doit �tre r�utilis� pour une nouvelle photo
    						//De m�me pour le tableau n
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
     * @param filename Nom de la photo � envoyer
     */
    void envoiPhoto(String filename){
    	Log.i(TAG, "Envoie de la photo"); 	
    	File pictureFile = new File(filename); //On r�cup�re le fichier image
    	// Initialisation du client HTTP
    	HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		/* Cr�ation de la requ�te POST. On lui donne en adresse l'adresse du serveur
		suivi de /upload. Le serveur mis en place pendant le projet attend
		une requ�te de ce type */
		HttpPost postRequest = new HttpPost("http://192.168.43.8:9001/upload");
		try {
			// Cr�ation de l'entit� qui sera associ�e � la requ�te
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			//On lui ajoute les champs "picture" et "email"
			// !!Attention, les noms "picture" et "email" ont leur importance, c'est ce
			//qu'attend le serveur
			entity.addPart("picture", new FileBody(((File)pictureFile), "image/jpeg"));
			entity.addPart("email", new StringBody("emilie.paillous@gmail.com", "text/plain", Charset.forName("UTF-8")));
			postRequest.setEntity(entity); //Ex�cution de la requ�te
			String response = EntityUtils.toString(httpClient.execute(postRequest).getEntity(),"UTF-8");
			Log.i(TAG,"Requete ex�cut�e");
		} catch (IOException e) {
			Log.i(TAG,"L'ex�cution de la requ�te lance une exception car : " + e.toString());
		}
		Log.i(TAG,"Sortie envoiPhoto"); 	
    }
    
    /**
     * Envoie une photo vers la plateforme web, avec le nombre de banane d�tect�es
     * 
     * @param filename Nom de la photo � envoyer
     * @param nbBanana Nombre de bananes d�tect�es
     */
    void envoiPhoto(String filename, int nbBanana){
    	Log.i(TAG, "Envoie de la photo"); 	
    	File pictureFile = new File(filename); //On r�cup�re le fichier image
    	// Initialisation du client HTTP
    	HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		/* Cr�ation de la requ�te POST. On lui donne en adresse l'adresse du serveur
		suivi de /upload. Le serveur mis en place pendant le projet attend
		une requ�te de ce type */
		HttpPost postRequest = new HttpPost("http://192.168.43.8:9001/upload");
		try {
			// Cr�ation de l'entit� qui sera associ�e � la requ�te
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			//On lui ajoute les champs "picture" et "email"
			// !!Attention, les noms "picture" et "email" ont leur importance, c'est ce
			//qu'attend le serveur
			entity.addPart("picture", new FileBody(((File)pictureFile), "image/jpeg"));
			entity.addPart("email", new StringBody("emilie.paillous@gmail.com", "text/plain", Charset.forName("UTF-8")));
			entity.addPart("nbBanana", new StringBody(""+nbBanana, "text/plain", Charset.forName("UTF-8")));
			postRequest.setEntity(entity); //Ex�cution de la requ�te
			String response = EntityUtils.toString(httpClient.execute(postRequest).getEntity(),"UTF-8");
			Log.i(TAG,"Requete ex�cut�e");
		} catch (IOException e) {
			Log.i(TAG,"L'ex�cution de la requ�te lance une exception car : " + e.toString());
		}
		Log.i(TAG,"Sortie envoiPhoto"); 	
    }
    
    /**
     * M�thode interne de debug. Permet de g�n�rer un fichier contenant
     * le contenu du tableau n
     */
    void genereFichier() {
		// G�n�ration du fichier de donn�es des normes (pour �tre analyser par la suite)
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
     * M�thode interne qui renvoie un dossier de destination
     * 
     * @return Le dossier de destination
     */
	private File getDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		return new File(sdDir, "FrigoTimeMachine");
	}
}

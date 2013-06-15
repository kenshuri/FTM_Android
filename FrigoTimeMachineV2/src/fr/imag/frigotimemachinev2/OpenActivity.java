package fr.imag.frigotimemachinev2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
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
        //genereFichier();
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
     * pris une photo du contenu du frigo et on attend que la porte soir ferm�e pour revenir
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
    		//Comparaison avec une image noire, r�sultat stock� dans une matrice diffNoir
    		Mat diffNoir = new Mat(current.size(),current.type());
    		Core.absdiff(current, noir, diffNoir);
    		Double normeDiffNoir = new Double(Core.norm(diffNoir));
    		n.add(normeDiffNoir);
    		compteur++;		//Compteur du nombre d'images prises
    		if (compteur > 11) {
    			//S'il y a suffisamment d'images d�j� prises, on v�rifie que la porte est ferm�e
    			fermee = true;
    			int i =0;
    			while (fermee && i<10) {
    				//La porte est fermee si sur les dix derni�res photos prises, la diff�rence
    				//entre une image noire et l'image current n'est pas trop grande.
    				if (n.get(compteur-1-i) > 4500) {
    					fermee = false;
    				}
    				i++;
    				}
    			if (fermee){
    				//Remise � 0 du compteur s'il doit �tre r�utilis� pour une nouvelle photo
					//De m�me pour le tableau n
    				compteur = 0;
    				n.clear();
					finish();
    			}
    		}
    	} else if (!stable){
    		//Aucune photo n'a encore �t� prise
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
    					Scalar scalaireN = new Scalar(0x00,0x00,0x00,0xFF);
    					Mat noir = new Mat(current.size(),current.type(),scalaireN);
    					//Comparaison avec une image noire, r�sultat stock� dans une matrice diffNoir
    					Mat diffNoir = new Mat(current.size(),current.type());
    					Core.absdiff(current, noir, diffNoir);
    					Double normeDiffNoir = new Double(Core.norm(diffNoir));
    					if (normeDiffNoir > 4500){
    						File pictureFileDir = getDir();
    						SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
    						String date = dateFormat.format(new Date());
    						String photoFile = "PictureCV_" + date + ".jpg";
    						String filename = pictureFileDir.getPath() + File.separator + photoFile;
    						//On doit convertir les couleurs avant de sauvegarder l'image.
    						//La description de la fonction cvtColor explique pourquoi
    						Imgproc.cvtColor(current, current, Imgproc.COLOR_BGR2RGB); 
    						Highgui.imwrite(filename, current);
    						Log.i(TAG,"Photo sauvegard�e");
    						//Remise � 0 du compteur s'il doit �tre r�utilis� pour une nouvelle photo
    						//De m�me pour le tableau n
    						compteur = 0;
    						n.clear();
    						envoiPhoto(filename);
    					} else {
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
    	Bitmap bitmap = BitmapFactory.decodeFile(filename);
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	bitmap.compress(CompressFormat.JPEG, 60, bos);
    	byte[] data = bos.toByteArray();
    	
    	File pictureFileDir = getDir();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
		String date = dateFormat.format(new Date());
		String photoFile = "PictureSend_" + date + ".jpg";
		String fileName = pictureFileDir.getPath() + File.separator + photoFile;
    	File pictureFile = new File(fileName);
    	
    	try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);
			fos.close();
		} catch (Exception error) {
			Log.d(TAG, "File" + filename + "not saved: "
					+ error.getMessage());
		}
    	
    	HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		HttpPost postRequest = new HttpPost("http://192.168.43.8:9001/upload");
		try {
		MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		entity.addPart("picture", new FileBody(((File)pictureFile), "image/jpeg"));
		entity.addPart("email", new StringBody("emilie.paillous@gmail.com", "text/plain", Charset.forName("UTF-8")));
		postRequest.setEntity(entity);
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
		// G�n�ration du fichier de donn�es
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
     * M�thode interne qui renvoie un dossier de destination
     * 
     * @return Le dossier de destination
     */
	private File getDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		return new File(sdDir, "FrigoTimeMachine");
	}
}

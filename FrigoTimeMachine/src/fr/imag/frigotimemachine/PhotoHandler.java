package fr.imag.frigotimemachine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

/**
 * Classe gérant la capture d'une photo et son envoie vers la plateforme web
 * 
 * @author Alexis Sciau
 *
 */
public class PhotoHandler implements PictureCallback {

	private final Context context;

	public PhotoHandler(Context context) {
		this.context = context;
	}
	
	/**
	 * Appelée lorsque la photo est prise
	 * 
	 * @param data Le contenu de l'image en bytes
	 * @param camera Instance de la caméra ayant pris la photo
	 */
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		//On construit dans un premier temps le nom du fichier image
		File pictureFileDir = getDir();
		if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
			Log.d("", "Can't create directory to save image.");
			Toast.makeText(context, "Can't create directory to save image.",Toast.LENGTH_LONG).show();
			return;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
		String date = dateFormat.format(new Date());
		String photoFile = "Picture_" + date + ".jpg";
		String filename = pictureFileDir.getPath() + File.separator + photoFile;
		File pictureFile = new File(filename);
		//Puis on stocke le contenu dans le fichier
		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);
			fos.close();
			Toast.makeText(context, "Nouvelle image : " + filename,
					Toast.LENGTH_LONG).show();
		} catch (Exception error) {
			Log.d("", "File" + filename + "not saved: "
					+ error.getMessage());
		}
		
		//On envoie la photo
		envoiPhoto(filename);
		
		//On informe l'activité principale que la photo est maintenant envoyée
		MainActivity.photoEnvoyee = true;
	}

	private File getDir() {
		File sdDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		return new File(sdDir, "FrigoTimeMachine");
	}
	
	/**
     * Envoie une photo vers la plateforme web
     * 
     * @param filename Nom de la photo à envoyer
     */
    void envoiPhoto(String filename){
    	String TAG = "envoiPhoto";
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
	
} 


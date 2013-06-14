package fr.imag.frigotimemachine;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
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
		//On construit dans un premier le nom du fichier image
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
		
		//On informe l'activité principale que la photo est maintenant envoyée
		MainActivity.photoEnvoyee = true;
	}

	private File getDir() {
		File sdDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		return new File(sdDir, "FrigoTimeMachine");
	}
	
} 


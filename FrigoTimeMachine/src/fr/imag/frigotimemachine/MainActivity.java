package fr.imag.frigotimemachine;

import java.io.File;

import fr.imag.frigotimemachine.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

/**
 * Activité principale de l'application.
 * <p> Cette activité est appelée au lancement de l'application. Son layout est
 * <i>activity_main.xml</i> qui se trouve dans le dossier /res/layout. Cette activité
 * provoque le lancement d'une nouvelle activité <i>OpenActivity</i>. <i>OpenActivity</i>
 * sera lancée de deux manières différentes suivant les cas : 
 * <ul>
 * <li>Soit <i>MainActivity</i> a détecté une ouverture de la porte et lance <i>OpenActivity</i>
 * pour que cette dernière détecte l'événement <b>"La porte du frigo est stable"</b>
 * <li>Soit <i>MainActivity</i> vient de prendre une photo et lance <i>OpenActivity</i>
 * pour que cette dernière détecte l'événement <b>"La porte du frigo est fermée"</b>
 * </ul>
 * </p>
 * 
 * @see OpenActivity
 * 
 * @author Alexis Sciau
 *
 */
public class MainActivity extends Activity implements SensorEventListener{
	/**
	 * Manager qui gère les capteurs. Il ne gèrera ici que l'accéléromètre
	 * 
	 * @see android.hardware.SensorManager
	 */
	private SensorManager mSensorManager;
	/**
	 * L'accéléromètre du téléphone
	 */
	private Sensor mAccel;
	/**
	 * L'appareil photo du téléphone
	 */
	private Camera camera;
	/**
	 * Entier qui correspond à l'identifiant de l'appareil photo
	 */
	private int cameraId = 0;
	/**
	 * Vrai si une photo a été prise, faux si elle doit l'être
	 */
	public static boolean photoPrise = false;
	/**
	 * Vrai si la photo prise est sauvegardée, faux si elle doit l'être
	 */
	public static boolean photoEnvoyee = false;
	/**
	 * Clé de l'extra qui sera modifié avant d'être donné à la seconde activité. La 
	 * valeur de cet extra est <b>1</b> si une photo a été prise, <b>0</b> si
	 * elle doit l'être.
	 * 
	 * @see MainActivity#onSensorChanged(SensorEvent)
	 * @see MainActivity#onActivityResult(int, int, Intent)
	 * @see OpenActivity#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
	 */
	public final static String Choix = "Contient 1 si une photo doit etre prise, 0 sinon";
	
	/**
	 * Appelée lorsque l'activité est créée
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	/**
	 * Appelée lors du retour de l'activité <i>OpenActivity</i>
	 * 
	 * 
	 * <p>Lors du retour de l'activité <i>OpenActivity</i>, deux options sont possibles :
	 * <ul>
	 * <li>Soit <i>OpenActivity</i> nous informe que "La porte du frigo est stable",
	 * auquel cas l'extra qui a la clé "Choix" contient 1 et une photo doit être prise </li>
	 * <li>Soit <i>OpenActivity</i> nous informe que "La porte du frigo est fermée",
	 * auquel cas l'extra qui a la clé "Choix" contient 0 et on attend maintenant
	 * l'ouverture de la porte.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param requestCode
	 * 			Code de l'activité dont on revient (ici toujours 0)
	 * @param resultCode
	 * 			Code permmettant de savoir si <i>OpenActivity</i> s'est bien terminée
	 * @param data
	 * 			Contient les informations que <i>OpenActivity</i> veut communiquer
	 * 			à <i>MainActivity</i>
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			// On vérifie que l'opération s'est bien déroulée
			if (resultCode == RESULT_OK) {
				if(data.getIntExtra(this.Choix,-1)==1) {
					//On revient de la seconde activité qui nous informe que la porte est stable
					Toast.makeText(this, "C'est stable, on va prendre une photo", Toast.LENGTH_SHORT).show();
					//Initialisation de la camera
					cameraId = findFrontFacingCamera();
					if (cameraId < 0) {
						Log.i("Open Activity","Pas de caméra");
					} else {
						camera = Camera.open(cameraId);
					}
					camera.takePicture(null, null,new PhotoHandler(getApplicationContext()));
					photoPrise=true;
				} else if (data.getIntExtra(this.Choix, -1)==0){
					//On revient de la seconde activité qui nous informe que la porte est fermée
					Toast.makeText(this, "La porte est fermée", Toast.LENGTH_SHORT).show();
					photoPrise = false;
					photoEnvoyee = false;
				} 
			} else {
				Toast.makeText(this, "Retour de la seconde activité incorrect", Toast.LENGTH_SHORT).show();
			}
		} 
	}
	
	/**
	 * Appelée lorque l'activité est mise en pause (l'activité n'est plus au premier plan)
	 */
	@Override
	protected void onPause() {
		/*On doit libérer la caméra lorque l'activité est mise en pause
		 * pour qu'elle puisse être utilisée ailleurs
		 */
		if (camera != null) {
			camera.release();
			camera = null;
		}
		/*
		 * On arrête aussi d'écouter l'accéléromètre
		 */
		mSensorManager.unregisterListener(this);
		super.onPause();
	}
	
	/**
	 * Appelée lorsque l'activité revient au premier plan
	 */
	protected void onResume() {
		/* Ce qu'en dit Google dans le cas de l'accéléromètre :
		 * «  Ce n'est pas nécessaire d'avoir les évènements des capteurs à un rythme trop rapide.
		 * En utilisant un rythme moins rapide (SENSOR_DELAY_UI), nous obtenons un filtre
		 * automatique de bas-niveau qui "extrait" la gravité  de l'accélération.
		 * Un autre bénéfice étant que l'on utilise moins d'énergie et de CPU. »
		 */
		super.onResume();
		//On écoute l'accéléromètre
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
		//On affiche la vue
		setContentView(R.layout.activity_main);
	}
	
	/**
	 * Méthode qui doit être obligatoirement implémentée pour une classe qui implémente
	 * <i>SensorEventListener</i>. Elle n'a pas d'utilité ici.
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	/**
	 * Appelée à chaque fois que l'accéléromètre détecte un mouvement
	 * 
	 * 
	 * <p>Deux options possibles : 
	 * <ul>
	 * <li>Si le mouvement correspond à une ouverture de porte et qu'une photo
	 * doit être prise, on lance l'activité <i>OpenActivity</i> avec l'extra 
	 * "Choix" qui vaut 1. Cela lui indique qu'elle devra détécter l'événement :
	 *  "La porte est stable" </li>
	 * <li> Si une photo a été prise et sauvegardée, on lance l'activité <i>OpenActivity</i> avec l'extra 
	 * "Choix" qui vaut 0. Cela lui indique qu'elle devra détécter l'événement :
	 *  "La porte est fermée" </li>
	 *  </ul>
	 *  </p>
	 *  
	 *  @param event Contient les informations sur le mouvement enregistré par 
	 * l'accéléromètre
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values[0] > 0.75 && !photoPrise) {
			//Correspond à une ouverture de porte
	        Intent secondeActivite = new Intent(MainActivity.this, OpenActivity.class);
	        secondeActivite.putExtra(Choix, 1);
	        startActivityForResult(secondeActivite, 0);
		}
		if (photoPrise && photoEnvoyee) {
			Intent secondeActivite = new Intent(MainActivity.this, OpenActivity.class);
			secondeActivite.putExtra(Choix, 0);
			startActivityForResult(secondeActivite, 0);
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
    
	/**
	 * Méthode interne qui trouve l'identifiant de l'appareil photo
	 * 
	 * @return L'identifiant de l'appareil photo
	 */
    private int findFrontFacingCamera() {
		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				Log.d("", "Camera found");
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}

}

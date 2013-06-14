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
 * Activit� principale de l'application.
 * <p> Cette activit� est appel�e au lancement de l'application. Son layout est
 * <i>activity_main.xml</i> qui se trouve dans le dossier /res/layout. Cette activit�
 * provoque le lancement d'une nouvelle activit� <i>OpenActivity</i>. <i>OpenActivity</i>
 * sera lanc�e de deux mani�res diff�rentes suivant les cas : 
 * <ul>
 * <li>Soit <i>MainActivity</i> a d�tect� une ouverture de la porte et lance <i>OpenActivity</i>
 * pour que cette derni�re d�tecte l'�v�nement <b>"La porte du frigo est stable"</b>
 * <li>Soit <i>MainActivity</i> vient de prendre une photo et lance <i>OpenActivity</i>
 * pour que cette derni�re d�tecte l'�v�nement <b>"La porte du frigo est ferm�e"</b>
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
	 * Manager qui g�re les capteurs. Il ne g�rera ici que l'acc�l�rom�tre
	 * 
	 * @see android.hardware.SensorManager
	 */
	private SensorManager mSensorManager;
	/**
	 * L'acc�l�rom�tre du t�l�phone
	 */
	private Sensor mAccel;
	/**
	 * L'appareil photo du t�l�phone
	 */
	private Camera camera;
	/**
	 * Entier qui correspond � l'identifiant de l'appareil photo
	 */
	private int cameraId = 0;
	/**
	 * Vrai si une photo a �t� prise, faux si elle doit l'�tre
	 */
	public static boolean photoPrise = false;
	/**
	 * Vrai si la photo prise est sauvegard�e, faux si elle doit l'�tre
	 */
	public static boolean photoEnvoyee = false;
	/**
	 * Cl� de l'extra qui sera modifi� avant d'�tre donn� � la seconde activit�. La 
	 * valeur de cet extra est <b>1</b> si une photo a �t� prise, <b>0</b> si
	 * elle doit l'�tre.
	 * 
	 * @see MainActivity#onSensorChanged(SensorEvent)
	 * @see MainActivity#onActivityResult(int, int, Intent)
	 * @see OpenActivity#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
	 */
	public final static String Choix = "Contient 1 si une photo doit etre prise, 0 sinon";
	
	/**
	 * Appel�e lorsque l'activit� est cr��e
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	/**
	 * Appel�e lors du retour de l'activit� <i>OpenActivity</i>
	 * 
	 * 
	 * <p>Lors du retour de l'activit� <i>OpenActivity</i>, deux options sont possibles :
	 * <ul>
	 * <li>Soit <i>OpenActivity</i> nous informe que "La porte du frigo est stable",
	 * auquel cas l'extra qui a la cl� "Choix" contient 1 et une photo doit �tre prise </li>
	 * <li>Soit <i>OpenActivity</i> nous informe que "La porte du frigo est ferm�e",
	 * auquel cas l'extra qui a la cl� "Choix" contient 0 et on attend maintenant
	 * l'ouverture de la porte.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param requestCode
	 * 			Code de l'activit� dont on revient (ici toujours 0)
	 * @param resultCode
	 * 			Code permmettant de savoir si <i>OpenActivity</i> s'est bien termin�e
	 * @param data
	 * 			Contient les informations que <i>OpenActivity</i> veut communiquer
	 * 			� <i>MainActivity</i>
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			// On v�rifie que l'op�ration s'est bien d�roul�e
			if (resultCode == RESULT_OK) {
				if(data.getIntExtra(this.Choix,-1)==1) {
					//On revient de la seconde activit� qui nous informe que la porte est stable
					Toast.makeText(this, "C'est stable, on va prendre une photo", Toast.LENGTH_SHORT).show();
					//Initialisation de la camera
					cameraId = findFrontFacingCamera();
					if (cameraId < 0) {
						Log.i("Open Activity","Pas de cam�ra");
					} else {
						camera = Camera.open(cameraId);
					}
					camera.takePicture(null, null,new PhotoHandler(getApplicationContext()));
					photoPrise=true;
				} else if (data.getIntExtra(this.Choix, -1)==0){
					//On revient de la seconde activit� qui nous informe que la porte est ferm�e
					Toast.makeText(this, "La porte est ferm�e", Toast.LENGTH_SHORT).show();
					photoPrise = false;
					photoEnvoyee = false;
				} 
			} else {
				Toast.makeText(this, "Retour de la seconde activit� incorrect", Toast.LENGTH_SHORT).show();
			}
		} 
	}
	
	/**
	 * Appel�e lorque l'activit� est mise en pause (l'activit� n'est plus au premier plan)
	 */
	@Override
	protected void onPause() {
		/*On doit lib�rer la cam�ra lorque l'activit� est mise en pause
		 * pour qu'elle puisse �tre utilis�e ailleurs
		 */
		if (camera != null) {
			camera.release();
			camera = null;
		}
		/*
		 * On arr�te aussi d'�couter l'acc�l�rom�tre
		 */
		mSensorManager.unregisterListener(this);
		super.onPause();
	}
	
	/**
	 * Appel�e lorsque l'activit� revient au premier plan
	 */
	protected void onResume() {
		/* Ce qu'en dit Google dans le cas de l'acc�l�rom�tre :
		 * �  Ce n'est pas n�cessaire d'avoir les �v�nements des capteurs � un rythme trop rapide.
		 * En utilisant un rythme moins rapide (SENSOR_DELAY_UI), nous obtenons un filtre
		 * automatique de bas-niveau qui "extrait" la gravit�  de l'acc�l�ration.
		 * Un autre b�n�fice �tant que l'on utilise moins d'�nergie et de CPU. �
		 */
		super.onResume();
		//On �coute l'acc�l�rom�tre
		mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
		//On affiche la vue
		setContentView(R.layout.activity_main);
	}
	
	/**
	 * M�thode qui doit �tre obligatoirement impl�ment�e pour une classe qui impl�mente
	 * <i>SensorEventListener</i>. Elle n'a pas d'utilit� ici.
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	/**
	 * Appel�e � chaque fois que l'acc�l�rom�tre d�tecte un mouvement
	 * 
	 * 
	 * <p>Deux options possibles : 
	 * <ul>
	 * <li>Si le mouvement correspond � une ouverture de porte et qu'une photo
	 * doit �tre prise, on lance l'activit� <i>OpenActivity</i> avec l'extra 
	 * "Choix" qui vaut 1. Cela lui indique qu'elle devra d�t�cter l'�v�nement :
	 *  "La porte est stable" </li>
	 * <li> Si une photo a �t� prise et sauvegard�e, on lance l'activit� <i>OpenActivity</i> avec l'extra 
	 * "Choix" qui vaut 0. Cela lui indique qu'elle devra d�t�cter l'�v�nement :
	 *  "La porte est ferm�e" </li>
	 *  </ul>
	 *  </p>
	 *  
	 *  @param event Contient les informations sur le mouvement enregistr� par 
	 * l'acc�l�rom�tre
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values[0] > 0.75 && !photoPrise) {
			//Correspond � une ouverture de porte
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
	 * M�thode interne qui renvoie un dossier de destination
	 * 
	 * @return Le dossier de destination
	 */
	private File getDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		return new File(sdDir, "FrigoTimeMachine");
	}
    
	/**
	 * M�thode interne qui trouve l'identifiant de l'appareil photo
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

package fr.imag.frigotimemachinev2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

/**
 * <b> Activité lancée lors du démarrage de l'application.</b>
 * <p> Son layout est <i>activity_main.xml</i> qui se trouve dans le dossier /res/layout. 
 * Cette activité détecte grâce à un accéléromètre l'ouverture de la porte du frigo
 * et lance l'activité <i>OpenActivity</i> à ce moment là.
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
	
	public final static String fileName = "Nom du fichir image";
	
	/**
	 * Appelée lors de la création de l'activité
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	/**
	 * Appelée lorque l'activité est mise en pause (l'activité n'est plus au premier plan)
	 */
	@Override
	protected void onPause() {
		/*
		 * On arrête d'écouter l'accéléromètre
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
	 * Appelée à chaque fois que l'accéléromètre détecte un mouvement. Elle doit
	 * détecter l'ouverture de la porte du frigo en lancer l'activité 
	 * <i>OpenActivity</i> à ce moment.
	 * 
	 * @param event Contient les informations sur le mouvement enregistré par 
	 * l'accéléromètre
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values[0] > 0.75) {
			//Correspond à une ouverture de porte
	        Intent secondeActivite = new Intent(MainActivity.this, OpenActivity.class);
	        startActivity(secondeActivite);
		}
	}
}


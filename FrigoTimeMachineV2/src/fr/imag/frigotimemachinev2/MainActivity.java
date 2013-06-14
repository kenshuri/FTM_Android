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
 * <b> Activit� lanc�e lors du d�marrage de l'application.</b>
 * <p> Son layout est <i>activity_main.xml</i> qui se trouve dans le dossier /res/layout. 
 * Cette activit� d�tecte gr�ce � un acc�l�rom�tre l'ouverture de la porte du frigo
 * et lance l'activit� <i>OpenActivity</i> � ce moment l�.
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
	
	public final static String fileName = "Nom du fichir image";
	
	/**
	 * Appel�e lors de la cr�ation de l'activit�
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}
	
	/**
	 * Appel�e lorque l'activit� est mise en pause (l'activit� n'est plus au premier plan)
	 */
	@Override
	protected void onPause() {
		/*
		 * On arr�te d'�couter l'acc�l�rom�tre
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
	 * Appel�e � chaque fois que l'acc�l�rom�tre d�tecte un mouvement. Elle doit
	 * d�tecter l'ouverture de la porte du frigo en lancer l'activit� 
	 * <i>OpenActivity</i> � ce moment.
	 * 
	 * @param event Contient les informations sur le mouvement enregistr� par 
	 * l'acc�l�rom�tre
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.values[0] > 0.75) {
			//Correspond � une ouverture de porte
	        Intent secondeActivite = new Intent(MainActivity.this, OpenActivity.class);
	        startActivity(secondeActivite);
		}
	}
}


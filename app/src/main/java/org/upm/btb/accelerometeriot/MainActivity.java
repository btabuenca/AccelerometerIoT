package org.upm.btb.accelerometeriot;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity implements SensorEventListener {

	private static final String TAG = "btb";

	private SensorManager sensorManager;
	private Sensor accelerometer;

	private float lastX, lastY, lastZ;

	private float deltaX = 0;
	private float deltaY = 0;
	private float deltaZ = 0;

	private float vibrateThreshold = 0;
	public Vibrator v;

	private TextView currentX, currentY, currentZ, currentIncl;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initializeActuators();
		initializeSensors();

	}

	/**
	 *  Initialize actuators
	 */
	public void initializeActuators() {
		currentX = (TextView) findViewById(R.id.currentX);
		currentY = (TextView) findViewById(R.id.currentY);
		currentZ = (TextView) findViewById(R.id.currentZ);
		currentIncl = (TextView) findViewById(R.id.currentIncl);

		// Initialize vibration
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

	}

	/**
	 * Initialize sensors
	 */
	public void initializeSensors() {

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			Log.e(TAG, "Success! we have an accelerometer");

			accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			vibrateThreshold = accelerometer.getMaximumRange() / 2;

		} else {
			// failed, we dont have an accelerometer!
			Log.e(TAG, "Failed. Unfortunately we do not have an accelerometer");
		}

	}

	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	//onPause() unregister the accelerometer to stop listening to events
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

			//
			// Acceleromter
			//

			// clean current values
			displayCleanValues();

			// display the current x,y,z accelerometer values
			displayCurrentValues();

			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			// get the change of the x,y,z values of the accelerometer
			deltaX = Math.abs(lastX - x);
			deltaY = Math.abs(lastY - y);
			deltaZ = Math.abs(lastZ - z);

			// if variation is below 2, it is just plain noise. Discard it!
			if (deltaX < 2) deltaX = 0;
			if (deltaY < 2) deltaY = 0;
			if (deltaZ < 2) deltaZ = 0;

			// set the last know values of x,y,z
			lastX = x;
			lastY = y;
			lastZ = z;


			// push values into IoT platform
			pushIoTPlaform(deltaX, deltaY, deltaZ);


			//
			// Calculate inclination
			//

			double inclination = Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
			currentIncl.setText(inclination + "º");

		}


		vibrate();

	}

	// if the change in the accelerometer value is big enough, then vibrate!
	// our threshold is MaxValue/2
	public void vibrate() {
		if ((deltaX > vibrateThreshold) || (deltaY > vibrateThreshold) || (deltaZ > vibrateThreshold)) {
			v.vibrate(50);
		}
	}

	public void displayCleanValues() {
		currentX.setText("0.0");
		currentY.setText("0.0");
		currentZ.setText("0.0");
	}

	// display the current x,y,z accelerometer values
	public void displayCurrentValues() {
		currentX.setText(Float.toString(deltaX));
		currentY.setText(Float.toString(deltaY));
		currentZ.setText(Float.toString(deltaZ));
	}


	/**
	 * Push JSON notification via http to IoT platform: Thingsboard
	 * @param fx
	 * @param fy
	 * @param fz
	 */
	public void pushIoTPlaform(float fx, float fy, float fz){

		if ((fx > vibrateThreshold) || (fz > vibrateThreshold) || (fz > vibrateThreshold)) {

			// Push thingsboard telemetry
			JSONObject jsonObject = new JSONObject();
			//jsonObject.put("amplitude", mSensor.getAmplitude());
			try {
				jsonObject.put("x", fx);
				jsonObject.put("y", fy);
				jsonObject.put("z", fz);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			Log.e(TAG, "Pushing telemetry ["+jsonObject.toString()+"]");

			new RequestManagerThingsboardAsyncTask().execute(jsonObject);
		}
	}

}
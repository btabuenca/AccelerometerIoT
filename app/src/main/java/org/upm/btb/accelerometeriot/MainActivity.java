package org.upm.btb.accelerometeriot;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements SensorEventListener {

	private static final String TAG = "btb";

	private SensorManager sensorManager;
	private Sensor accelerometer;

	private float lastX, lastY, lastZ;
	private float deltaX = 0, deltaY = 0, deltaZ = 0;

	private float vibrateThreshold = 0;
	public Vibrator v;
	private TextView currentX, currentY, currentZ, currentIncl;

	private ExecutorService executorService = Executors.newSingleThreadExecutor();
	private Handler mainHandler = new Handler(Looper.getMainLooper());

	private String telemetryUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Inicializamos las vistas y sensores
		initializeActuators();
		initializeSensors();

		// Cargar la URL desde config.properties
		ConfigReader configReader = new ConfigReader(this);
		telemetryUrl = configReader.getProperty("url");
		if (telemetryUrl == null) {
			Log.e(TAG, "URL de telemetría no encontrada en config.properties.");
		}
	}

	public void initializeActuators() {
		currentX = findViewById(R.id.currentX);
		currentY = findViewById(R.id.currentY);
		currentZ = findViewById(R.id.currentZ);
		currentIncl = findViewById(R.id.currentIncl);

		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
	}

	public void initializeSensors() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			vibrateThreshold = accelerometer.getMaximumRange() / 2;
		} else {
			Log.e(TAG, "Failed. Unfortunately we do not have an accelerometer");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			displayCleanValues();
			displayCurrentValues();

			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			deltaX = Math.abs(lastX - x);
			deltaY = Math.abs(lastY - y);
			deltaZ = Math.abs(lastZ - z);

			if (deltaX < 2) deltaX = 0;
			if (deltaY < 2) deltaY = 0;
			if (deltaZ < 2) deltaZ = 0;

			lastX = x;
			lastY = y;
			lastZ = z;

			// Enviar telemetría
			pushAcceleromIoTPlaform(deltaX, deltaY, deltaZ);

			double inclinationX = Math.toDegrees(Math.atan2(x, Math.sqrt(y * y + z * z)));
			double inclinationY = Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));
			currentIncl.setText(String.format(" X: %.2f°  Y: %.2f°", inclinationX, inclinationY));
		}

		vibrate();
	}

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

	public void displayCurrentValues() {
		currentX.setText(Float.toString(deltaX));
		currentY.setText(Float.toString(deltaY));
		currentZ.setText(Float.toString(deltaZ));
	}

	public void pushAcceleromIoTPlaform(float fx, float fy, float fz) {
		if ((fx > vibrateThreshold) || (fz > vibrateThreshold)) {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("x", fx);
				jsonObject.put("y", fy);
				jsonObject.put("z", fz);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			Log.e(TAG, "Pushing telemetry [" + jsonObject.toString() + "]");

			// Ejecutar la solicitud en el hilo de fondo
			executorService.submit(() -> {
				String response = NetworkUtils.sendTelemetryData(jsonObject, telemetryUrl);
				// Si necesitas actualizar la UI después de recibir la respuesta:
				mainHandler.post(() -> {
					Log.d(TAG, "Response from server: " + response);
				});
			});
		}
	}
}

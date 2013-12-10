package ar.com.fclad.datasender;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

public class DrawActivity extends Activity implements SensorEventListener {
	
	private SensorManager sensorManager;
	float timestampGyro;
	private static final float NS2S = 1.0f / 1000000000.0f;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(new DrawView(this));
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
	}
	
	@Override
	protected void onPause() {
		sensorManager.unregisterListener(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
		        SensorManager.SENSOR_DELAY_GAME);
		super.onResume();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			float dT = 0;
			if(timestampGyro!=0){
				dT = (event.timestamp-timestampGyro)*NS2S;
			}
			timestampGyro = event.timestamp;

			String str = dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2];
			Intent sendmsg = new Intent(this,BluetoothServerService.class);
			sendmsg.putExtra(BluetoothServerService.COMMAND, BluetoothServerService.SENDMSG);
			sendmsg.putExtra(BluetoothServerService.ORIGIN, "G");
			sendmsg.putExtra(BluetoothServerService.MSG, str);
			startService(sendmsg);
			
		}	
		
	}
	
	
}

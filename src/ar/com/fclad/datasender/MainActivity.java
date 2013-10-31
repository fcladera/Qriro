package ar.com.fclad.datasender;

import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener{
	
	private SensorManager sensorManager;
	private int i = 0;
	private float accumulated;
	
	private TextView accelSensorValues, gyroSensorValues;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		
		accelSensorValues = (TextView) findViewById(R.id.AccelSensorValue);
		gyroSensorValues = (TextView) findViewById(R.id.GyroSensorValue);
		
		
		
		TextView sensorInfo = (TextView) findViewById(R.id.availableSensors);
		
		sensorInfo.setText("GYRO\n");
		List<Sensor> deviceGyro= sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
		for(i=0;i<deviceGyro.size();i++){
			sensorInfo.append(deviceGyro.get(i).getName()+"\t");
			sensorInfo.append(Float.valueOf(deviceGyro.get(i).getResolution()).toString());
			sensorInfo.append("\n");
		}
		
		sensorInfo.append("ACCEL\n");
		List<Sensor> deviceAccel= sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		for(i=0;i<deviceAccel.size();i++){
			sensorInfo.append(deviceAccel.get(i).getName()+"\t");
			sensorInfo.append(Float.valueOf(deviceAccel.get(i).getResolution()).toString());
			sensorInfo.append("\n");
		}
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
			getAccelerometer(event);
		}
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			getGyro(event);
		}
		
	}
	
	private void getAccelerometer(SensorEvent event){
		float[] values = event.values;
		
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		long actualTime = System.currentTimeMillis();
		
		String accelString ="x: "+Float.valueOf(x).toString()+"\n"+
							"y: "+Float.valueOf(y).toString()+"\n"+
							"z: "+Float.valueOf(z).toString();
		accelSensorValues.setText(accelString);
				
	}
	
	private void getGyro(SensorEvent event){
		
		float[] values = event.values;
		
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		long actualTime = System.currentTimeMillis();
		
		String gyroString = "alpha: "+Float.valueOf(x).toString()+"\n"+
							"beta: "+Float.valueOf(y).toString()+"\n"+
							"gamma: "+Float.valueOf(z).toString();
		gyroSensorValues.setText(gyroString);
		
		
	}

	 @Override
	  protected void onResume() {
	    super.onResume();
	    // register this class as a listener for the orientation and
	    // accelerometer sensors
	    
	    sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
		        SensorManager.SENSOR_DELAY_FASTEST);
	    sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
		        SensorManager.SENSOR_DELAY_FASTEST);

	  }

	  @Override
	  protected void onPause() {
	    // unregister listener
	    super.onPause();
	    sensorManager.unregisterListener(this);
	  }

}

package ar.com.fclad.datasender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

// Resources used to write this code:
//
// TCP CLIENT
// http://examples.javacodegeeks.com/android/core/socket-core/android-socket-example/
// Creative Commons Attribution-ShareAlike 3.0 Unported License
//
// SENSORS
// https://developer.android.com/guide/topics/sensors/sensors_motion.html


public class MainActivity extends Activity implements SensorEventListener{
	
	private SensorManager sensorManager;
	private int i = 0;
	private boolean testingSensors;
	
	private TextView accelSensorValues, gyroSensorValues;
	
	// Server parameters
	private String server = "192.168.2.122";
	private int port = 7777;
	private Socket socket;
	private boolean isSending;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// SENSORS
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		accelSensorValues = (TextView) findViewById(R.id.AccelSensorValue);
		gyroSensorValues = (TextView) findViewById(R.id.GyroSensorValue);
		
		TextView sensorInfo = (TextView) findViewById(R.id.availableSensors);
		
		testingSensors = ((ToggleButton)findViewById(R.id.testSensors)).isChecked();
		
		sensorInfo.setText("GYRO\n");
		Sensor deviceGyro= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorInfo.append(deviceGyro.getName()+"\n");
		sensorInfo.append(Float.valueOf(deviceGyro.getResolution()).toString());
		sensorInfo.append("\n");
		
		sensorInfo.append("ACCEL\n");
		Sensor deviceAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		sensorInfo.append(deviceAccel.getName()+"\n");
		sensorInfo.append(Float.valueOf(deviceAccel.getResolution()).toString());
		sensorInfo.append("\n");
		
		// SOCKET
		new Thread(new ClientThread()).start();
		
		
		
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

	    // connect to a socket that must be running in the server
	    
	   
	}

	@Override
	protected void onPause() {
		// unregister listener
		super.onPause();
		sensorManager.unregisterListener(this);
	    
	}
	
	class ClientThread implements Runnable {

		@Override
		public void run() {
			
			try {
				InetAddress serverAddr = InetAddress.getByName(server);

				socket = new Socket(serverAddr, port);

			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}
	
	public void onToggleClicked(View view){	
		switch (view.getId()){
		case R.id.testSensors:
			Log.w("MainActivity","Toggled testSensor button");
			testingSensors = ((ToggleButton) view).isChecked();
		return;
		}
	}
	
	public void onClick(View view){
		switch(view.getId()){
		case R.id.connect:
			if(isSending)
				isSending = false;
			else
				isSending = true;
			Log.d("MainActivity", "Is sending "+isSending);
			return;
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
			if(testingSensors){
				showAccelValues(event.values);
			}
			if(isSending){
				try {
					String str = "A:"+System.currentTimeMillis()+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println(str);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			if(testingSensors){
				showGyroValues(event.values);
			}
			if(isSending){
				try {
					String str = "G:"+System.currentTimeMillis()+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
					out.println(str);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}
		
	private void showAccelValues(float[] values){
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		//long actualTime = System.currentTimeMillis();
		
		
		String accelString ="x: "+Float.valueOf(x).toString()+"\n"+
							"y: "+Float.valueOf(y).toString()+"\n"+
							"z: "+Float.valueOf(z).toString();
		accelSensorValues.setText(accelString);
	}
	
	private void showGyroValues(float[] values){
		
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		//long actualTime = System.currentTimeMillis();
		
		String gyroString = "alpha: "+Float.valueOf(x).toString()+"\n"+
							"beta: "+Float.valueOf(y).toString()+"\n"+
							"gamma: "+Float.valueOf(z).toString();
		gyroSensorValues.setText(gyroString);
		
		
	}

	 

}

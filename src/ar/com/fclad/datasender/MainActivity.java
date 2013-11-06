package ar.com.fclad.datasender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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
	private boolean testingSensors;
	
	private TextView accelSensorValues, gyroSensorValues;
	
	// Server parameters
	private static final String localServer = "10.0.0.1";
	private static final String remoteServer = "192.168.2.122";
	private int port = 7777;
	private static final int timeout = 1000;
	private Socket socket;
	PrintWriter writer = null;
	private boolean isSending = false;
	private boolean isConnected = false;
	
	// UI elements
	private ToggleButton sendData;
	private Button connectRemote;
	private Button connectLocal;

	
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
		
		// UI
		
		sendData = (ToggleButton) findViewById(R.id.sendData);
		connectLocal = (Button) findViewById(R.id.connect_Local);
		connectRemote = (Button) findViewById(R.id.connect_Remote);
		
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
		if(socket!=null){
			disconnect();
		}
	    
	}
	
	
	
	public void onToggleClicked(View view){	
		switch (view.getId()){
		case R.id.testSensors:
			Log.w("MainActivity","Toggled testSensor button");
			testingSensors = ((ToggleButton) view).isChecked();
		return;
		case R.id.sendData:
			isSending = ((ToggleButton) view).isChecked();
			if(isSending){
				try {
					 writer = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(socket.getOutputStream())),
							true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else{
				writer = null;
			}
				
			Log.d("MainActivity", "Is sending "+isSending);
			return;
		}
	}
	
	public void onClick(View view){
		if(!isConnected){
			switch(view.getId()){
			case R.id.connect_Remote:
				Log.d("mainActivity", "Connection to remote asked");
				new Connect().execute(remoteServer);
				return;
			case R.id.connect_Local:
				Log.d("mainActivity", "Connection to local asked");
				new Connect().execute(localServer);
				return;
			}
		}
		else{
			disconnect();
		}
		
	}
	
	private void disconnect(){
		// disconnect socket
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendData.setEnabled(false);
		connectRemote.setEnabled(true);
		connectLocal.setEnabled(true);
		isConnected = false;
		socket = null;
	}

	private class Connect extends AsyncTask<String, Void, String>{
		/**
		 * Tries to connect in background to the socket.
		 * If succed, UI is changed allowing to send data
		 * If fails, show toast
		 */

		@Override
		protected String doInBackground(String... params) {
				//Toast.makeText(getApplicationContext(), "Connecting to server "+params[0].toString(), Toast.LENGTH_SHORT).show();
				InetSocketAddress serverAddr = new InetSocketAddress(params[0], port);
				socket = new Socket();
				try {
					socket.connect(serverAddr, timeout);
					isConnected = true;
				} catch (IOException e) {
					//e.printStackTrace();
					Log.w("Socket","Connection error");
					isConnected = false;
				}
				return params[0];
		}
		
		protected void onPostExecute(String result){
			if(isConnected){
				Toast.makeText(getApplicationContext(), "Successfully connected", Toast.LENGTH_SHORT).show();
				if(result==localServer){
					connectRemote.setEnabled(false);
				}
				else{
					
					connectLocal.setEnabled(false);
				}
				sendData.setEnabled(true);
			}
			else{
				Toast.makeText(getApplicationContext(), "Error connecting to "+result, Toast.LENGTH_SHORT).show();
			}
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

				String str = "A:"+(int)System.currentTimeMillis()+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
				
				writer.println(str);
				writer.flush();
			}
			
		}
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			if(testingSensors){
				showGyroValues(event.values);
			}
			if(isSending){
				
				String str = "G:"+(int)System.currentTimeMillis()+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
				writer.println(str);
				writer.flush();
				
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

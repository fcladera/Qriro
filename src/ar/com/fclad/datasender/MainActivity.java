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
import android.widget.CheckBox;
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
	private boolean isRotationVectorActivated = false;
	private boolean testingSensors;
	private float timestampGyro;
	private float timestampAccel;
	private float timestampRotationVector;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private long code;

	
	private TextView accelSensorValues, gyroSensorValues, rotationVectorSensorValues;
	
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
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// SENSORS
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		accelSensorValues = (TextView) findViewById(R.id.AccelSensorValue);
		gyroSensorValues = (TextView) findViewById(R.id.GyroSensorValue);
		rotationVectorSensorValues = (TextView) findViewById(R.id.RotationVectorSensorValue);
		
		
		TextView sensorInfo = (TextView) findViewById(R.id.availableSensors);
		
		testingSensors = ((ToggleButton)findViewById(R.id.testSensors)).isChecked();
		
		sensorInfo.setText("GYRO:\t");
		Sensor deviceGyro= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorInfo.append(deviceGyro.getName()+"\n");
		//sensorInfo.append(Float.valueOf(deviceGyro.getResolution()).toString());
		//sensorInfo.append("\n");
		
		sensorInfo.append("ACCEL:\t");
		Sensor deviceAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		sensorInfo.append(deviceAccel.getName()+"\n");
		//sensorInfo.append(Float.valueOf(deviceAccel.getResolution()).toString());
		//sensorInfo.append("\n");
		
		sensorInfo.append("ROTATION VECT:\t");
		Sensor deviceRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		sensorInfo.append(deviceRotationVector.getName()+"\n");
		
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
	    
	    int sensorRate = SensorManager.SENSOR_DELAY_GAME;

	    sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
		        sensorRate);
	    sensorManager.registerListener(this,
		        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
		        sensorRate);
	    sensorManager.registerListener(this, 
	    		sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
	    		sensorRate);
	    // connect to a socket that must be running in the server
	    
	   
	}

	@Override
	protected void onPause() {
		// unregister listener
		super.onPause();
		sensorManager.unregisterListener(this);
		if(socket!=null){
			if(isSending){
				destroyWriterStream();
				isSending = false;
				sendData.setChecked(false);
				
			}
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
				createWriterStream();
			}
			else{
				destroyWriterStream();
			}
				
			Log.d("MainActivity", "Is sending "+isSending);
			return;
		case R.id.RotationVectActivated:
			isRotationVectorActivated = ((CheckBox) view).isChecked();
			Log.d("MainActivity","Toggled Rotation vector sensor");
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
			if(isSending){
				sendData.setChecked(false);
				isSending=false;
			}
		}
		
	}
	
	private void disconnect(){
		// disconnect socket
		try {
			socket.close();
		} catch (IOException e) {
			Log.e("MainActivity", "Error on disconnect");
			//e.printStackTrace();
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
					Log.e("Socket","Connection error");
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
	
	private void createWriterStream(){
		try {
			 writer = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())),
					true);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("MainActivity", "Error on createWriterStream");
		}
	}
	
	private void destroyWriterStream(){
		writer = null;
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
	
	
	// TODO: arreglar bug raro con el timestamp y el dt que es siempre nulo! 
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
			float dT = 0;
			if(timestampAccel!=0){
				dT =  (event.timestamp-timestampAccel)*NS2S;
			}
			timestampAccel = event.timestamp;
			
			if(testingSensors){
				showAccelValues(event.values,dT);
			}
			if(isSending){
	
					String str = "A:"+code+":"+dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
					code++;
					writer.println(str);
					writer.flush();
				}
				
			}
			
		if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
			float dT = 0;
			if(timestampGyro!=0){
				dT = (event.timestamp-timestampGyro)*NS2S;
			}
			timestampGyro = event.timestamp;

			if(testingSensors){
				showGyroValues(event.values,dT);
			}
			if(isSending){
				String str = "G:"+code+":"+dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
				code++;
				writer.println(str);
				writer.flush();
			}
			
		}
	
		if((event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)&&isRotationVectorActivated){
			float dT = 0;
			if(timestampRotationVector!=0){
				dT = (event.timestamp-timestampRotationVector)*NS2S;
			}
			if(testingSensors){
				showRotationVectorValues(event.values, dT);
			}
			if(isSending){
				String str = "R:"+code+":"+dT+":"+event.values[0]+":"+event.values[1]+":"+event.values[2]+";";
				code++;
				writer.println(str);
				writer.flush();
			}
		}
		
	}
		
	private void showAccelValues(float[] values, float dT){
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		//long actualTime = System.currentTimeMillis();
		
		
		String accelString =
							"x:\t"+String.format("%.4f", x)+"\t\t\t\t\t"+
							"y:\t"+String.format("%.4f", y)+"\n"+
							"z:\t"+String.format("%.4f", z)+"\t\t\t\t\t"+
							"timeAccel:\t"+String.format("%.4f", dT);
		accelSensorValues.setText(accelString);
	}
	
	private void showGyroValues(float[] values, float dT){
		
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		//long actualTime = System.currentTimeMillis();
		
		String gyroString = 
							"alpha: \t"+String.format("%.4f", x)+"\t\t"+
							"beta:\t"+String.format("%.4f", y)+"\n"+
							"gamma:\t"+String.format("%.4f", z)+"\t\t"+
							"timeGyro:\t"+String.format("%.4f", dT);
		gyroSensorValues.setText(gyroString);
		
		
	}
	
	private void showRotationVectorValues(float[] values, float dT){
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		//long actualTime = System.currentTimeMillis();
		
		
		String rotationVectorString =
							"x:\t"+String.format("%.4f", x)+"\t\t\t\t\t"+
							"y:\t"+String.format("%.4f", y)+"\n"+
							"z:\t"+String.format("%.4f", z)+"\t\t\t\t\t"+
							"timeRot:\t"+String.format("%.4f", dT);
		rotationVectorSensorValues.setText(rotationVectorString);
	}

	 

}

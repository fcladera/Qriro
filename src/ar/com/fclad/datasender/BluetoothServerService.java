package ar.com.fclad.datasender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class BluetoothServerService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private static final String TAG = "BluetoothServerService";
	
	// Name for SDP
	private static final String NAME = "DataSender";
	
	// UUID of the application
	private static final UUID MY_UUID = 
			UUID.fromString("2fa7beb1-6acf-4703-8b7e-dcbc14f07b89"); 

	// Connection state
	private int state = -1;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	
	// commands that can be sent to BluetoothServerService 
	public static final int LISTEN = 1;
	public static final int STOP_LISTEN = 2;
	public static final int GETSTATUS = 3;
	public static final int SENDMSG = 10;
	
	// Strings needed to send/receive messages
	public static final String NOTIFICATION = "ar.com.fclad.datasender";
	public static final String COMMAND = "command";
	public static final String MSG  = "msg";
	public static final String ORIGIN = "origin";
	public static final String STATUS = "status"; 
    
    // Rcv Buffer
    private static final int BUFFER_SIZE = 1024;
    
    
    // Service variables
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private long code;
    
	public BluetoothServerService() {
		Log.d(TAG,"Constructor");
		
	}
	
		
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
	    }
	    @Override
	    public void handleMessage(Message msg) {
	    	switch(msg.getData().getInt(COMMAND)){
	    	
	    	case LISTEN:
	    		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    		state = STATE_NONE;
	    		connectedThread = null;
	    		if(connectedThread!=null){
	    			connectedThread.cancel();
	    			connectedThread = null;
	    		}
	    		//server = msg.getData().getString(SERVER);
    			//port = msg.getData().getInt(PORT);
    			
    			if(acceptThread == null){
    				acceptThread = new AcceptThread();
    				acceptThread.start();
    				state = STATE_LISTEN;
    			}
    			//Log.d(TAG, "Listening bluetooth socket");
	    		break;
	    	
	    	case STOP_LISTEN:
	    		
    			Log.d("TCPclienService","Disconnecting from server");
    			if(connectedThread!=null)
    				connectedThread.cancel();
    			if(acceptThread!=null)
    				acceptThread.cancel();
	    			
	    		
	    		stopSelf();	// Destroy service on disconnection
	    			
	    		break;
	    		
	    	case SENDMSG:
	    		if(state == STATE_CONNECTED){
	    			ConnectedThread r;
	    			synchronized (this) {
						r = connectedThread;
					}
	    			String line = msg.getData().getString(ORIGIN)+":"+code+":";
		    		line += msg.getData().getString(MSG)+";";
		    		//Log.d(TAG,"Asked to send "+line);
		    		code++;
		    		r.write(line.getBytes());
	    		}
	    		else{
	    			Log.e(TAG,"Trying to send a message without connection!");
	    		}
	    		
	    		return;
	    		
	    	case GETSTATUS:
	    		Intent intent = new Intent(NOTIFICATION);
	    		intent.putExtra(STATUS, state);
	    		sendBroadcast(intent);
	    	default:
	    		Log.w(TAG,"Erroneous code");
	    		break;
	    	
	    	}
	    }
	}
	
	private class AcceptThread extends Thread{
		private final BluetoothServerSocket bluetoothServerSocket;
		public AcceptThread() {
			BluetoothServerSocket tmp= null;
			try {
				tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "Error creating listen socket");
				stopSelf();
			}
			bluetoothServerSocket = tmp;
			Log.d(TAG,"Created ServerSocket");
		}
		
		public void run(){
			BluetoothSocket bluetoothSocket = null;
			while (true) {
				try {
					bluetoothSocket = bluetoothServerSocket.accept();
				} catch (IOException e) {
					break;
				}
				
				if(bluetoothSocket != null){
					synchronized (BluetoothServerService.this) {
						try {
							// manage connection
							// TODO do it better, read BluetoothChat
							state = STATE_CONNECTING;
							connectedThread = new ConnectedThread(bluetoothSocket);
							connectedThread.start();
							Log.d(TAG,"Created connectedThread");
							bluetoothServerSocket.close();
						} catch (IOException e) {
							Log.e(TAG,"Error closing socket after connection");
							stopSelf();
						}
						break;
					}
					
				}
			}
		}
		
		public void cancel(){
			try {
				bluetoothServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG,"Error closing serverSocket in cancel - AcceptThread");
			}
		}
	}
	
	private class ConnectedThread extends Thread{
		private final BluetoothSocket bluetoothSocket;
		private final InputStream inputStream;
		private final OutputStream outputStream;
		
		public ConnectedThread(BluetoothSocket socket) {
			bluetoothSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try {
				tmpIn = bluetoothSocket.getInputStream();
				tmpOut = bluetoothSocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "Error creating input/output streams");
				stopSelf();
			}
			inputStream = tmpIn;
			outputStream = tmpOut;
			state = STATE_CONNECTED;
			Log.d(TAG,"Created Input/output streams");
		}
		
		public void run(){
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			
			while(true){
				try{
					bytesRead = inputStream.read(buffer);
				}
				catch(IOException e){
					Log.e(TAG, "Error reading input stream, disconnected");
				}
			}
		}
		
		public void write(byte[] bytes){
			try {
				outputStream.write(bytes);
			} catch (IOException e) {
				Log.e(TAG, "Error writing to output stream");
			}
		}
		public void cancel(){
			try {
				bluetoothSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "Error closing BluetoothSocket in cancel - ConnectedThread");
			}
		}
		
	}
	
	/*private class Connect extends AsyncTask<String, Void, String>{
		
		private BluetoothSocket bluetoothSocket;

		@Override
		protected String doInBackground(String... params) {
				//Toast.makeText(getApplicationContext(), "Connecting to server "+params[0].toString(), Toast.LENGTH_SHORT).show();
				//InetSocketAddress serverAddr = new InetSocketAddress(params[0], port);
				//socket = new Socket();
				try {
					tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
					socket.connect(serverAddr, timeout);
					socket.setTcpNoDelay(true);
					isConnected = true;
					Intent intent = new Intent(NOTIFICATION); 
					intent.putExtra(STATUS, CONNECTED);
	    			sendBroadcast(intent);
				} catch (IOException e) {
					//e.printStackTrace();
					Log.e("Socket","Connection error");
					isConnected = false;
					Intent intent = new Intent(NOTIFICATION);
					intent.putExtra(STATUS, DISCONNECTED);
					sendBroadcast(intent);
					
					stopSelf();
				}
				return params[0];
		}
		
		protected void onPostExecute(String result){
			if(isConnected){
				Toast.makeText(getApplicationContext(), "Successfully connected to "+result, Toast.LENGTH_SHORT).show();
				createWriterStream();
			}
			else{
				Toast.makeText(getApplicationContext(), "Error connecting to "+result, Toast.LENGTH_SHORT).show();
				stopSelf();
			}
		}
		
	}
	*/
	
	
	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not implemented");
	}
	public void onCreate(){
		HandlerThread thread = new HandlerThread("BluetoothServerThread");
		thread.start();
		Log.d(TAG,"Service created");
		mServiceLooper = thread.getLooper();
	    mServiceHandler = new ServiceHandler(mServiceLooper);
	    
	    
	}
	
	 @Override
	 public int onStartCommand(Intent intent, int flags, int startId) {
	    //Log.d(TAG,"Service started");
	    Message msg = mServiceHandler.obtainMessage();
	    msg.arg1 = startId;
	    msg.setData(intent.getExtras());
	    mServiceHandler.sendMessage(msg);

	    return Service.START_NOT_STICKY;
	  }
	 
	 public void onDestroy(){
		 Log.w(TAG,"Service destroyed");
	 }
	


}

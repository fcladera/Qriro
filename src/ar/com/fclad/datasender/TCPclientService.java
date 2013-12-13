package ar.com.fclad.datasender;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class TCPclientService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	 
	
	
	public static final String NOTIFICATION = "ar.com.fclad.datasender";
	
	public static final String COMMAND = "command";
	public static final String SERVER = "server";
	public static final String PORT = "port";
	public static final String MSG  = "msg";
	public static final String ORIGIN = "origin";
	
	// commands that can be sent to TCPclientService 
	// through Intent.putExtra(TCPclientService.COMMAND, ...)
	public static final int CONNECT = 1;
	public static final int DISCONNECT = 0;
	public static final int GETSTATUS = 2;
	public static final int SENDMSG = 10;
	
	// Status response
	public static final String STATUS = "status"; 
	public static final int CONNECTED = 1;
	public static final int DISCONNECTED = 0; 

	
	private Socket socket;
	PrintWriter writer = null;
	private boolean isConnected = false;
	
	private String server = null;
	private int port = -1;
	
	private static final int timeout = 2000;

	private long msgId;
	
	
	public TCPclientService() {
		
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
	    }
	    @Override
	    public void handleMessage(Message msg) {
	    	switch(msg.getData().getInt(COMMAND)){
	    	case CONNECT:
	    		if(!isConnected){
	    			server = msg.getData().getString(SERVER);
	    			port = msg.getData().getInt(PORT);
	    			Log.d("TCPclienService","Connecting with server "+server+" port "+port);
	    			new Connect().execute(server);
	    			
	    		}
	    		break;
	    	case DISCONNECT:
	    		if(isConnected){
	    			Log.d("TCPclienService","Disconnecting from server");
	    			disconnect();
	    			
	    		}
	    		stopSelf();	// Destroy service on disconnection
	    			
	    		break;
	    	case SENDMSG:
	    		String line = msg.getData().getString(ORIGIN)+":"+msgId+":";
	    		line += msg.getData().getString(MSG)+";";
	    		msgId++;
	    		writer.flush();
	    		writer.println(line);
	    		return;
	    		
	    	case GETSTATUS:
	    		Intent intent = new Intent(NOTIFICATION);
	    		if(isConnected)
	    			intent.putExtra(STATUS, CONNECTED);
	    		else
	    			intent.putExtra(STATUS, DISCONNECTED);
	    		sendBroadcast(intent);
	    	default:
	    		break;
	    	
	    	}
	    }
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
	
	private void disconnect(){
		// disconnect socket
		try {
			socket.close();
			destroyWriterStream();
		} catch (IOException e) {
			Log.e("MainActivity", "Error on disconnect");
			stopSelf();
			//e.printStackTrace();
		}
		isConnected = false;
		socket = null;
		Intent intent = new Intent(NOTIFICATION);
		intent.putExtra(STATUS, DISCONNECTED);
		sendBroadcast(intent);
	}

	
	public void onCreate() {
		HandlerThread thread = new HandlerThread("TCPclientThread");
		thread.start();
		Log.w("TCPclientService","Service created");
		mServiceLooper = thread.getLooper();
	    mServiceHandler = new ServiceHandler(mServiceLooper);
	    

	}
	
	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	 @Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
	    //Log.w("TCPclientService","Service started");
	    Message msg = mServiceHandler.obtainMessage();
	    msg.arg1 = startId;
	    msg.setData(intent.getExtras());
	    mServiceHandler.sendMessage(msg);
	    
	    
	    
	    return Service.START_NOT_STICKY;
	  }
	 
	 public void onDestroy(){
		 Log.w("TCPclientService","Service destroyed");
	 }

	 
}

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
	public static final String TAG = "TCPclientService";
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	 
	public static final String NOTIFICATION = "ar.com.fclad.datasender";
	
	public static final String COMMAND = "command";
	public static final String SERVER = "server";
	public static final String PORT = "port";
	public static final String MSG  = "msg";
	public static final String ORIGIN = "origin";
	public static final String STATUS = "status"; 
	
	// commands that can be sent to TCPclientService 
	// through Intent.putExtra(TCPclientService.COMMAND, ...)
	public static final int CONNECT = 1;
	public static final int DISCONNECT = 0;
	public static final int GETSTATUS = 2;
	public static final int SENDMSG = 10;
	
	// State response
	public static final int STATE_CONNECTED = 2;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_NONE = 0; 
	private static int state;

	
	private Socket socket;
	private PrintWriter writer = null;
	
	private String server = null;
	private int port = -1;
	
	private static final int timeout = 1000;

	private long msgId;
	
	
	public TCPclientService() {
		state = STATE_NONE; //setStatus is not used to avoid double answer if service is not created
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
	    }
	    @Override
	    public void handleMessage(Message msg) {
	    	switch(msg.getData().getInt(COMMAND)){
	    	case CONNECT:
	    		if(state==STATE_NONE){
	    			server = msg.getData().getString(SERVER);
	    			port = msg.getData().getInt(PORT);
	    			Log.d(TAG,"Connecting with server "+server+" port "+port);
	    			new Connect().execute(server);
	    			
	    		}
	    		break;
	    	case DISCONNECT:
	    		if((state==STATE_CONNECTED)||(state==STATE_CONNECTING)){
	    			Log.d(TAG,"Disconnecting from server");
	    			disconnect();	
	    		}
	    			
	    		break;
	    	case SENDMSG:
	    		String line = msg.getData().getString(ORIGIN)+":"+msgId+":";
	    		line += msg.getData().getString(MSG)+";";
	    		msgId++;
	    		writer.flush();
	    		writer.println(line);
	    		return;
	    		
	    	case GETSTATUS:
	    		notifyState();
	    	default:
	    		break;
	    	
	    	}
	    }
	}
	
	private synchronized void setState(int s) {
        Log.d(TAG, "State:\t" + state + " -> " + s);
		state = s;
	}
	
	public synchronized int getState() {
	    return state;
	}
	
	public void notifyState(){
		Intent intent = new Intent(NOTIFICATION); 
		intent.putExtra("TAG", TAG);
		intent.putExtra(STATUS, getState());
		sendBroadcast(intent);
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
					setState(STATE_CONNECTING);
				} catch (IOException e) {
					//e.printStackTrace();
					Log.e(TAG,"Connection error");
					setState(STATE_NONE);
				}
				return params[0]+":"+port;
		}
		
		protected void onPostExecute(String result){
			if(state==STATE_CONNECTING){
				notifyState();
				Toast.makeText(getApplicationContext(), "Connecting to "+result, Toast.LENGTH_SHORT).show();
				createWriterStream();
			}
			else{
				Toast.makeText(getApplicationContext(), "Error connecting to "+result, Toast.LENGTH_SHORT).show();
				disconnect();
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
			Log.e(TAG, "Error on createWriterStream");
			disconnect();
		}
		setState(STATE_CONNECTED);
		notifyState();
		
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
			Log.e(TAG, "Socket was not created?");
			//e.printStackTrace();
		}
		socket = null;
		setState(STATE_NONE);
		notifyState();
		stopSelf();
	}

	
	public void onCreate() {
		HandlerThread thread = new HandlerThread("TCPclientThread");
		thread.start();
		Log.w(TAG,"Service created");
		mServiceLooper = thread.getLooper();
	    mServiceHandler = new ServiceHandler(mServiceLooper);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
	 @Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
	    //Log.w(TAG,"Service started");
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

package ar.com.fclad.processingEclipse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import processing.core.PApplet;
import processing.core.PMatrix3D;

class CommunicationThread extends Thread{
	private OutputStreamWriter outToServer = null;
	private BufferedReader inFromServer = null;
	private InputStreamReader inputStreamReader = null;
	private Socket clientSocket;
	
	private static final int RUNNING = 1;
	private static final int STOPPED = 0;
	
	private int state;
	
	private PApplet parent;
	
	// Commands from dataProcessor
	private int command = 0;
	public static final int COMMAND_DOUBLE_TAP = 1024+1;
	public static final int COMMAND_THREAD_START = 2048+1;
	public static final int COMMAND_CALIBRATION_FINISHED = 2048+2;
	
	private float[][] rotationTranslationMatrix ={	{1f,0f,0f,0f},
													{0f,1f,0f,0f},
													{0f,0f,1f,0f},
													{0f,0f,0f,1f}};
	
	public CommunicationThread(PApplet p) {
		this.parent = p;
		state = STOPPED;
		InetAddress ipAddress = null;
		try {
			ipAddress = InetAddress.getByName("localhost");
		} catch (UnknownHostException e1) {
			System.err.println("Error finding hostname");
			e1.printStackTrace();
		}
		clientSocket = new Socket();
		InetSocketAddress toAddr = new InetSocketAddress(ipAddress,7778);
		try{
			clientSocket.connect(toAddr);
		}catch (IOException e){
			e.printStackTrace();
			System.err.println("Error connecting to server!");
			try {
				clientSocket.close();
			} catch (IOException e1) {
				System.err.println("Error closing socket");
				e1.printStackTrace();
			}
			stopServer();
		}
		
		try {
			outToServer = new OutputStreamWriter(clientSocket.getOutputStream());
		} catch (IOException e) {
			System.err.println("Error binding to output stream");
			e.printStackTrace();
		}
				   
		try {
			inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
			inFromServer = new BufferedReader(inputStreamReader);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		state = RUNNING;

	}
	
	@Override
	public void run() {
		while(state==RUNNING){
			
			try {
				while(!inFromServer.ready()){
					Thread.sleep(14);
					//System.out.println("Client loop "+System.currentTimeMillis());
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				String dataIn= inFromServer.readLine();
				while(dataIn!=null){
				    //println(dataIn);
				    String[] list = PApplet.splitTokens(dataIn,":;");

				    // Get matrix from server and store in rotationTranslationMatrix
				    if(list[0].equals("MAT")){
				    	//println("Processing matrix");
					    int i=0;
					    float[][] rotMatrix = new float[4][4];
					    for(String str : list){
						    //print(i+" : "+str+"\n");
						    if(i!=0){
						    	int p=i-1;
						        rotMatrix[p/4][p%4] = Float.parseFloat(str);
						        //println(p/4+" "+p%4);
					        }
					        i++;
					        if(i>16) break;
					         
				      	}
				      	setRotationMatrix(rotMatrix);
				    }
				    
				
				    else if(list[0].equals("COM")){
				    	setCommand(Integer.parseInt(list[1]));
				    	System.out.println("Command from server "+command);
				    	break;
				    }
				    else{
				    	System.err.println("Erroneous message from the server:"+list[0]);
				    	stopServer();
				    }
				//System.out.println("FROM SERVER: " + dataIn);
				    dataIn = inFromServer.readLine();
				}
			}
		catch (IOException e) {
			System.err.println("Error on reading");
			e.printStackTrace();
			}
		}
	}
	
	public void write(String msg){
		try {
			outToServer.write(msg);
			outToServer.flush();
		} catch (IOException e) {
			System.err.println("Error on write");
			e.printStackTrace();
			stopServer();
		}
	}
	
	public void stopServer(){
		state = STOPPED;
		parent.exit();
	}
	
	private void setRotationMatrix(  float[][] newRotMat){
	    rotationTranslationMatrix = newRotMat;                          
	}
	
	public synchronized void setCommand(int c){
		command = c;
	}
	
	public void askNewRotationMatrix(){
		if(state==RUNNING)
			write("GETMAT");
	}
	
	public int getCommand(){
		return command;
	}
	
	public PMatrix3D getRotationTranslationMatrix(){
		 PMatrix3D matrix = new PMatrix3D(	rotationTranslationMatrix[0][0],
											rotationTranslationMatrix[0][1],
											rotationTranslationMatrix[0][2],
											rotationTranslationMatrix[0][3],
											rotationTranslationMatrix[1][0],
											rotationTranslationMatrix[1][1],
											rotationTranslationMatrix[1][2],
											rotationTranslationMatrix[1][3],
											rotationTranslationMatrix[2][0],
											rotationTranslationMatrix[2][1],
						                 	rotationTranslationMatrix[2][2],
						                 	rotationTranslationMatrix[2][3],
						                 	rotationTranslationMatrix[3][0],
						                 	rotationTranslationMatrix[3][1],
						                 	rotationTranslationMatrix[3][2],
						                 	rotationTranslationMatrix[3][3]);
		return matrix;
	}
	
	
	public PMatrix3D getRotationWithoutTranslationMatrix(PMatrix3D originalMatrix){
		 PMatrix3D matrix = new PMatrix3D(	rotationTranslationMatrix[0][0],
					rotationTranslationMatrix[0][1],
					rotationTranslationMatrix[0][2],
					originalMatrix.m03,
					rotationTranslationMatrix[1][0],
					rotationTranslationMatrix[1][1],
					rotationTranslationMatrix[1][2],
					originalMatrix.m13,
					rotationTranslationMatrix[2][0],
					rotationTranslationMatrix[2][1],
	              	rotationTranslationMatrix[2][2],
	              	originalMatrix.m23,
	              	originalMatrix.m30,
	              	originalMatrix.m31,
	              	originalMatrix.m32,
	              	originalMatrix.m33);
		 
		 return matrix;
	}
	
}
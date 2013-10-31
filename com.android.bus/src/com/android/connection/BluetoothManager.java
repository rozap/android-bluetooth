package com.android.connection;

import java.util.ArrayList;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothManager {
	
	
	public static final String TAG = "BluetoothManager";

    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;	
    public static final int PACKET_READ = 6;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "linvor";
    public static final String TOAST = "toast";
    
    private Context mContext;
    private BluetoothSerialService mService;
    private ArrayList<PacketHandler> packetHandlers;
    
    private BluetoothManager(Context c) {
    	mContext = c;
    	packetHandlers = new ArrayList<PacketHandler>();
    	mService = new BluetoothSerialService(mContext, mHandlerBT);
    	mService.start();
    	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> devices = adapter.getBondedDevices();
    	for(BluetoothDevice d : devices) {
    		Log.d(TAG, "DEVICE: " + d.getName());
    		if(d.getName().equals(DEVICE_NAME)) {
    			mService.connect(d);
    		}
    	}
    	

    	
    }
    
    
    private static BluetoothManager instance;
    public static BluetoothManager getInstance(Context c) {
    	if(instance == null) {
    		instance = new BluetoothManager(c);
    	}
    	return instance;
    }
    
    
    private String snippets = "";
    
    public void addSnippet(String snippet) {
    	snippets = snippets + snippet;
    }
    
    public void pushMessage() {
    	if(snippets.length() > 2) {
        	sendMessage(snippets);
        	snippets = "";
    	}
    	
    	
    }

    public void sendTo(int addr, char v1, char v2, char v3) {
    	char[] seq = new char[4];
		seq[0] = (char)addr;
		seq[1] = v1;
		seq[2] = v2;
		seq[3] = v3;
		sendMessage(new String(seq));
    }

    
    private void sendMessage(String msg) {
    	msg = "{" + sanitizeMessage(msg) + "}";
    	Log.d("Sending...", msg);
    	mService.write(msg.getBytes());
    }
    
    private String sanitizeMessage(String msg) {
    	char[] arr = msg.toCharArray();
    	for(int i = 0; i < arr.length; i++) {
    		if(arr[i] == '{') {
    			arr[i] = '|';
    		} else if(arr[i] == '}') {
    			arr[i] = '|';
    		}
    	}
    	return new String(arr);
    }
    
    public void registerHandler(PacketHandler handler) {
    	packetHandlers.add(handler);
    }
    
    public void unregisterHandler(PacketHandler handler) {
    	int i = 0;
    	for(PacketHandler h : packetHandlers) {
    		if(h == handler) {
    			break;
    		}
    		i++;
    	}
    	packetHandlers.remove(i);
    }
	

	
	// The Handler that gets information back from the BluetoothService
    private final Handler mHandlerBT = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {      
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	Log.d(TAG, "State connecting");
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                case BluetoothSerialService.STATE_NONE:

                    break;
                }
                break;
            case MESSAGE_WRITE:
                
                break;
             
            case MESSAGE_READ:
                       
                
                
                break;
            
               
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                break;
            case MESSAGE_TOAST:
                break;
            case PACKET_READ:
            	String packet = (String)msg.obj;
            	for(PacketHandler handler : packetHandlers) {
            		handler.onPacket(packet);
            	}
            	break;
            }
        }
    };    

}

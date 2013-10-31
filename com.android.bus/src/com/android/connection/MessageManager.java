package com.android.connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import android.util.Log;

public class MessageManager implements PacketHandler {
	
	public static final int MAX_HANDLERS = 16;
	private BluetoothManager bluetooth;
	private HashMap<Integer, ArrayList<MessageHandler>> messageHandlers;
	
	public MessageManager(BluetoothManager bluetooth) {
		this.bluetooth = bluetooth;
		bluetooth.registerHandler(this);
		messageHandlers = new HashMap<Integer, ArrayList<MessageHandler>>();
	}

	@Override
	public void onPacket(String packet) {
		StringTokenizer tokenizer = new StringTokenizer(packet.substring(1, packet.length()-1), ",");
		int address = 0;
		while(tokenizer.hasMoreElements()) {
			String message = (String)tokenizer.nextElement();
			if(address < MAX_HANDLERS && messageHandlers.get(address) != null) {
				ArrayList<MessageHandler> handlers = messageHandlers.get(address);
				for(MessageHandler h : handlers) {
					h.onMessage(address, message);
				}
			}
			address++;
		}
	}
	
	
	
	
	
	public void registerHandler(int address, MessageHandler handler) {
		ArrayList<MessageHandler> handlers = messageHandlers.get(address);
		if(handlers == null) {
			messageHandlers.put(address, new ArrayList<MessageHandler>());
		}
		messageHandlers.get(address).add(handler);
	}
	
	public void unregisterHandler(int address, String key) {
		ArrayList<MessageHandler> handlers = messageHandlers.get(address);
		if(handlers != null) {
			Iterator<MessageHandler> it = handlers.iterator();
			while(it.hasNext()) {
				MessageHandler h = it.next();
				if(h.getKey().equals(key)) {
					messageHandlers.get(address).remove(h);
				}
			}
		}
	}

}

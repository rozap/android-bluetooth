package com.android.connection;

public interface MessageHandler {
	
	
	public void onMessage(int address, String message);
	public String getKey();

}

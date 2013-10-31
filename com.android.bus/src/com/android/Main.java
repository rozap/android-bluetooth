package com.android;


import java.text.DecimalFormat;

import com.android.connection.BluetoothManager;
import com.android.connection.MessageHandler;
import com.android.connection.MessageManager;
import com.android.connection.PacketHandler;

import android.R.anim;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CheckBox;
import android.widget.TextView;


public class Main extends Activity {
	
	//Input addresses
	private static final int I_ADDR_BATTERY1_VOLTAGE = 0;
	private static final int I_ADDR_BATTERY2_VOLTAGE = 1;
	private static final int I_ADDR_CHT_TEMP = 2;
	private static final int I_ADDR_RPM = 3;
	//Output addresses
	private static final int O_ADDR_HEAT = 126;

	
	private BluetoothManager bluetoothManager;
	private static String TAG = "MAIN_ACTIVITY";
	private MessageManager messageManager;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		bluetoothManager = BluetoothManager.getInstance(this);
		messageManager = new MessageManager(bluetoothManager);
		setupButtons();
		
		
		
	}
	
	@Override
	 public void onWindowFocusChanged(boolean hasFocus) {
	  // TODO Auto-generated method stub
	  super.onWindowFocusChanged(hasFocus);
	  setupBatteryMonitors();
	 }
	
	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		super.onConfigurationChanged(config);
		Log.d("Main", "Screen rotated");

	}
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflate = getMenuInflater();
		inflate.inflate(R.menu.main_activity_menu, menu);
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.control_lights:
			Intent lightControl = new Intent(this, LightControl.class);
			startActivity(lightControl);
			break;
		}
		return true;
	}
	
	private void setupButtons() {
		setupHeat();
	}
	
	private void setupHeat() {
		CheckBox sw = (CheckBox)findViewById(R.id.heat_switch);
		sw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					char v = (char)(isChecked? 1 : 0);
					bluetoothManager.sendTo(O_ADDR_HEAT, v, v, v);
				
				
			}
		});
	}
	
	

	
	private void setupBatteryMonitors() {
		messageManager.registerHandler(I_ADDR_BATTERY1_VOLTAGE, new Battery1VoltageMonitor());
		messageManager.registerHandler(I_ADDR_BATTERY2_VOLTAGE, new Battery2VoltageMonitor());
		messageManager.registerHandler(I_ADDR_CHT_TEMP, new CHTMonitor());
		messageManager.registerHandler(I_ADDR_RPM, new RPMMonitor());
	}
	
	
	
	
	private class Battery1VoltageMonitor implements MessageHandler {
		
		private TextView batteryText;
		private View batteryBar;
		private int width;
		
		public Battery1VoltageMonitor() {
			batteryText = (TextView)findViewById(R.id.battery1_text);
			batteryBar = (View)findViewById(R.id.battery1_bar);
			width = findViewById(R.id.battery1_row).getWidth();
			
		}

		@Override
		public void onMessage(int address, String message) {
			double batVal = -1;
			try {
				batVal = Double.parseDouble(message) / (double)1000;
				DecimalFormat df = new DecimalFormat("##.##");
				batteryText.setText(df.format(batVal) + "v");
				
				LayoutParams params = batteryBar.getLayoutParams();
				params.width = (int)((batVal / 20) * width);
				batteryBar.setLayoutParams(params);
				batteryBar.setBackgroundColor(getColor(batVal));
			} catch(NumberFormatException e) {
				//??
			}
			
		}

		@Override
		public String getKey() {
			return "BATTERY1_MONITOR";
		}
		
		private int getColor(double val) {
			if(val < 12.2) {
				return getResources().getColor(R.color.holo_red_light);
			} else if(val > 13.8) {
				return getResources().getColor(R.color.holo_orange_light);
			} else {
				return getResources().getColor(R.color.holo_green_light);
			}
		}
		
	}
	
	private class Battery2VoltageMonitor implements MessageHandler {
		private TextView batteryText;
		private View batteryBar;
		private int width;
		
		public Battery2VoltageMonitor() {
			batteryText = (TextView)findViewById(R.id.battery2_text);
			batteryBar = (View)findViewById(R.id.battery2_bar);
			width = findViewById(R.id.battery2_row).getWidth();
			
		}

		@Override
		public void onMessage(int address, String message) {
			double batVal = -1;
			try {
				batVal = Double.parseDouble(message) / (double)1000;
				DecimalFormat df = new DecimalFormat("##.##");
				batteryText.setText(df.format(batVal) + "v");
				
				LayoutParams params = batteryBar.getLayoutParams();
				params.width = (int)((batVal / 20) * width);
				batteryBar.setLayoutParams(params);
				batteryBar.setBackgroundColor(getColor(batVal));
			} catch(NumberFormatException e) {
				//??
			}
			
		}
		
		private int getColor(double val) {
			if(val < 12.2) {
				return getResources().getColor(R.color.holo_red_light);
			} else if(val > 13.8) {
				return getResources().getColor(R.color.holo_orange_light);
			} else {
				return getResources().getColor(R.color.holo_green_light);
			}
		}

		@Override
		public String getKey() {
			return "BATTERY2_MONITOR";
		}
	}
	

	private class RPMMonitor implements MessageHandler {
		
		private TextView text;
		private View bar;
		private int width;
		
		public RPMMonitor() {
			text = (TextView)findViewById(R.id.rpm_text);
			bar = (View)findViewById(R.id.rpm_bar);
			width = findViewById(R.id.rpm_row).getWidth();
		}

		@Override
		public void onMessage(int address, String message) {
			
			
			try {
				Integer rpmVal = Integer.parseInt(message);
				text.setText(rpmVal + " rpm");
				LayoutParams params = bar.getLayoutParams();
				params.width = (int)(((double)rpmVal / (double)6000) * width);
				bar.setLayoutParams(params);
				bar.setBackgroundColor(getColor(rpmVal));
			} catch(NumberFormatException e) {
				//??
			}
		}

		@Override
		public String getKey() {
			return "RPM_MONITOR";
		}
		
		private int getColor(double val) {
			if(val < 12.2) {
				return getResources().getColor(R.color.holo_red_light);
			} else if(val > 13.8) {
				return getResources().getColor(R.color.holo_orange_light);
			} else {
				return getResources().getColor(R.color.holo_green_light);
			}
		}
		
	}
	
	private class CHTMonitor implements MessageHandler {

		private TextView text;
		private View bar;
		private int width;
		
		public CHTMonitor() {
			text = (TextView)findViewById(R.id.cht_text);
			bar = (View)findViewById(R.id.cht_bar);
			width = findViewById(R.id.cht_row).getWidth();
		}
		
		@Override
		public void onMessage(int address, String message) {
			try {
				Integer chtVal = Integer.parseInt(message);
				text.setText(chtVal + " f");
				LayoutParams params = bar.getLayoutParams();
				params.width = (int)(((double)chtVal / (double)500) * width);
				bar.setLayoutParams(params);
				bar.setBackgroundColor(getColor(chtVal));	
			}catch(NumberFormatException e) {
				
			}

		}

		@Override
		public String getKey() {
			return "CHT_MONITOR";
		}
		
		
		private int getColor(double val) {
			if(val < 12.2) {
				return getResources().getColor(R.color.holo_red_light);
			} else if(val > 13.8) {
				return getResources().getColor(R.color.holo_orange_light);
			} else {
				return getResources().getColor(R.color.holo_green_light);
			}
		}
		
	}
	
	
}
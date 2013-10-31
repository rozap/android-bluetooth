package com.android;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.widget.TextView;
import com.android.connection.BluetoothManager;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class LightControl extends Activity implements OnSeekBarChangeListener {

	private BluetoothManager bluetoothManager;

	private SeekBar redSlider;
	private SeekBar greenSlider;
	private SeekBar blueSlider;
	private SeekBar whiteSlider;
	private boolean isLocked = false;
	private long startTracking;


    private IndividualManager individualManager;
    private Random randomManager;

	private static final int[] whiteLeds = {0, 3, 7, 10, 13, 19, 25, 27, 38, 50, 52, 54, 55, 56, 59};
	private static final int[] redLeds = {1, 4, 9, 11, 14, 21, 22, 23, 28, 32, 42, 45, 49, 60};
	private static final int[] greenLeds = {6, 12, 17, 18, 30, 31, 33, 36, 37, 39, 41, 46, 53, 58};
	private static final int[] blueLeds = {2, 5, 8, 15, 16, 20, 24, 26, 29, 34, 35, 40, 43, 44, 47, 48, 51, 57};


    private class Fade {
        public int duration;
        public long started;
        public int val;
        public Fade(int duration, long started, int val) {
            this.duration = duration;
            this.started = started;
            this.val = val;
        }
    }
	private HashMap<Integer, Fade> fadeState = new HashMap<Integer, Fade>();
	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		bluetoothManager = BluetoothManager.getInstance(this);
		setContentView(R.layout.light_control);
		setupListeners();
		zeroLEDS();
	}

	@Override
	public void onPause() {
		super.onPause();
		if(randomManager != null) {
			randomManager.stop();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if(randomManager != null) {
			randomManager.start();
		}
	}

	private void setupListeners() {
		redSlider = (SeekBar)findViewById(R.id.red_seek_bar);
		greenSlider = (SeekBar)findViewById(R.id.green_seek_bar);
		blueSlider = (SeekBar)findViewById(R.id.blue_seek_bar);
		whiteSlider = (SeekBar)findViewById(R.id.white_seek_bar);

		redSlider.setOnSeekBarChangeListener(this);
		greenSlider.setOnSeekBarChangeListener(this);
		blueSlider.setOnSeekBarChangeListener(this);
		whiteSlider.setOnSeekBarChangeListener(this);

		redSlider.setMax(127);
		greenSlider.setMax(127);
		blueSlider.setMax(127);
		whiteSlider.setMax(127);



		setupRandom();
        setupIndividual();
	}

    private void setupIndividual() {
        Button ind = (Button)findViewById(R.id.individual);
        ind.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(individualManager == null) {
                    individualManager = new IndividualManager();
                    individualManager.start();
                } else {
                    individualManager.stop();
                    individualManager = null;
                }

            }
        });
    }




	private void setupRandom() {
        final TextView rateTv = (TextView)findViewById(R.id.random_rate_display);
		final SeekBar rate = (SeekBar)findViewById(R.id.random_rate);
        Button random = (Button)findViewById(R.id.random);
        random.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(randomManager != null) {
                    randomManager.stop();
                    randomManager = null;
                    findViewById(R.id.random_wrap).setVisibility(View.GONE);
                } else {
                    randomManager = new Random(whiteLeds, 4000);
                    randomManager.start();
                    findViewById(R.id.random_wrap).setVisibility(View.VISIBLE);
                }

            }
        });
		rate.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(randomManager != null) {
					randomManager.setRate(seekBar.getProgress());
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				rateTv.setText("Fade randomly at rate between " + progress + " and " + (progress*2));

			}
		});
	}


	private void levelTimeout(final int timeout, final int[] leds, final int val, final int duration) {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setLEDsTo(leds, val, duration);
			}
		});
		t.start();
	}



	//Methods for handling the seek bar change
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	}

	private void zeroLEDS() {
		redSlider.setProgress(0);
		blueSlider.setProgress(0);
		greenSlider.setProgress(0);
		whiteSlider.setProgress(0);

		levelTimeout(0, whiteLeds, 0, 0);
		levelTimeout(400, redLeds, 0, 0);
		levelTimeout(800, greenLeds, 0, 0);
		levelTimeout(1200, blueLeds, 0, 0);
	}

	private void queueLeds(int[] leds, int val, int duration) {
		synchronized (fadeState) {
			for(int led : leds) {

				char[] seq = new char[4];
				seq[0] = (char)led;
				seq[1] = (char)Math.min((duration/100), 127);
                Fade current = fadeState.get(led);
				fadeState.put(led, new Fade(duration, System.currentTimeMillis()+500, val));
				seq[2] = (char)(current == null? 0 : current.val);
				seq[3] = (char)val;
				bluetoothManager.addSnippet(new String(seq));
			}
		}
	}


	private void setLEDsTo(int[] leds, int val, int duration) {
		queueLeds(leds, val, duration);
		bluetoothManager.pushMessage();
	}

	private boolean isFading(int led) {
		return (System.currentTimeMillis() - fadeState.get(led).started)  < fadeState.get(led).duration;
	}



	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		startTracking = System.currentTimeMillis();

	}


	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int duration = (int)(System.currentTimeMillis() - startTracking);
		if(seekBar.getId() == R.id.red_seek_bar || isLocked) {
			setLEDsTo(redLeds, seekBar.getProgress(), duration);
		}
		if(seekBar.getId() == R.id.green_seek_bar || isLocked) {
			setLEDsTo(greenLeds, seekBar.getProgress(), duration);
		}
		if(seekBar.getId() == R.id.blue_seek_bar || isLocked) {
			setLEDsTo(blueLeds, seekBar.getProgress(), duration);
		}
		if(seekBar.getId() == R.id.white_seek_bar || isLocked) {
			setLEDsTo(whiteLeds, seekBar.getProgress(), duration);
		}
	}




	private class Random {
		private int[] leds;
		private int rate;
		private Thread runner;
        private boolean running;
		public Random(int[] leds, final int rate) {
			this.rate = rate;
			

			runner = new Thread(new Runnable() {

				@Override
				public void run() {
					while(Random.this.running) {
                        boolean changed = false;
						synchronized(fadeState) {
							Iterator<Entry<Integer, Fade>> it = fadeState.entrySet().iterator();
							while(it.hasNext()) {
								Entry<Integer, Fade> e = it.next();
								if(!isFading(e.getKey())) {
									int val = e.getValue().val > 0? 0 : 127;
									int duration = (int) Math.max(Random.this.rate, (Math.random()*Random.this.rate*2));
									queueLeds(new int[] {(int)e.getKey()}, val, duration);
                                    changed = true;
								}
							}
						}

                        try {
                            Thread.sleep(400);
                            Log.d("Push", "PushMessage");
                            if(changed) {
                                bluetoothManager.pushMessage();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
					}

				}
			});
		}

		public void setRate(int r) {
			this.rate = r;
		}

		public void start() {
            this.running = true;
            try {
                runner.start();
            } catch(IllegalThreadStateException e) {

            }
		}

		public void stop() {
            this.running = false;
		}

	}


    public class IndividualManager {
        private final TextView stateTv;
        private final SeekBar addrSeek;
        private final SeekBar lvlSeek;

        public IndividualManager() {
            stateTv = (TextView)findViewById(R.id.individual_display);
            addrSeek = (SeekBar)findViewById(R.id.individual_addr);
            lvlSeek = (SeekBar)findViewById(R.id.individual_level);

        }

        public void start() {
            findViewById(R.id.individual_wrap).setVisibility(View.VISIBLE);



            addrSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    stateTv.setText(displayMessage(addrSeek.getProgress(), lvlSeek.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    send();
                }
            });

            lvlSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    stateTv.setText(displayMessage(addrSeek.getProgress(), lvlSeek.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    send();
                }
            });
        }

        private void send() {
            setLEDsTo(new int[] {addrSeek.getProgress()}, lvlSeek.getProgress(), 100);
        }

        private String displayMessage(int addr, int seek) {
            return "Set " + addr + " to " + seek;
        }

        public void stop() {
            findViewById(R.id.individual_wrap).setVisibility(View.GONE);
        }





    }





}

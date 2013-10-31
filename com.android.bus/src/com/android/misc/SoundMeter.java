package com.android.misc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.media.MediaRecorder;
import android.util.Log;


public class SoundMeter {
        static final private double EMA_FILTER = 0.6;

        private MediaRecorder mRecorder = null;
        private double mEMA = 0.0;
        private HashMap<String, LevelHandler> handlers;

        public void start() {
                if (mRecorder == null) {
                        mRecorder = new MediaRecorder();
                        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mRecorder.setOutputFile("/dev/null"); 
                    try {
						mRecorder.prepare();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    mRecorder.start();
                    mEMA = 0.0;
                }
                handlers = new HashMap<String, LevelHandler>();
        }
        
        private Thread levelChecker;
        public void registerLevelHandler(String key, LevelHandler handler) {
        	handlers.put(key,  handler);
        	
        	if(levelChecker == null) {
            	levelChecker = new Thread(new Runnable() {
    				
    				@Override
    				public void run() {
    					while(true) {
    						double amp = SoundMeter.this.getAmplitude();
    						Iterator<Entry<String, LevelHandler>> it = handlers.entrySet().iterator();
    						while(it.hasNext()) {
    							Entry<String, LevelHandler> e = it.next();
    							e.getValue().onLevel(amp);
    						}
    						try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
    					}
    					
    				}
    			});
            	levelChecker.start();	
        	}

        }
        
        public void stop() {
                if (mRecorder != null) {
                        mRecorder.stop();       
                        mRecorder.release();
                        mRecorder = null;
                }
                if(levelChecker != null) {
                	levelChecker.stop();
                }
        }
        
        public double getAmplitude() {
                if (mRecorder != null)
                        return  (mRecorder.getMaxAmplitude()/2700.0);
                else
                        return 0;

        }

        public double getAmplitudeEMA() {
                double amp = getAmplitude();
                mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
                return mEMA;
        }
}
package com.shellware.genesisconnect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.shellware.genesisconnect.Enums;

public class MainActivity extends Activity implements OnTouchListener, BusDataListener {

	private final static String APP_NAME = "GenesisConnect";
//	private static final int LONG_PAUSE = 250;

	private static Context context;
	private static SharedPreferences prefs;
	private static PowerManager.WakeLock wakeLock;
	
	private CanBusTripleService canBusTripleService;
	private ServiceConnection canBusTripleServiceConnection;

	private final Handler registerFadeHandler = new Handler();
	private final Handler audioControlsFadeHandler = new Handler();

    //settings dialog
    private Dialog settingsDialog;

	// register layout
	private LinearLayout registerLayout;
	private ImageView registerImage;
	private TextView registerText;
	
	// sound control layout
	private RelativeLayout audioControlsLayout;
	private SeekBar bassSeek;
	private SeekBar midRangeSeek;
	private SeekBar trebleSeek;
	private SeekBar faderSeek;
	private SeekBar balanceSeek;
	
	private boolean startingUp = true;
	private boolean shuttingDown = false;
	private boolean paused = false;
	
	private static long lastHiPriMsg = 0;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(APP_NAME, "onCreate");
        
        context = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        setContentView(R.layout.activity_main);
        
        findViewById(R.id.mainLayout).setOnTouchListener(this);
        
        registerLayout = (LinearLayout) findViewById(R.id.registerLayout);
        registerImage = (ImageView) findViewById(R.id.registerImage);
        registerText = (TextView) findViewById(R.id.registerText);
        
        audioControlsLayout = (RelativeLayout) findViewById(R.id.audioControlsLayout);
        bassSeek = (SeekBar) findViewById(R.id.bassSeek);
        midRangeSeek = (SeekBar) findViewById(R.id.midSeek);
        trebleSeek = (SeekBar) findViewById(R.id.trebleSeek);
        faderSeek = (SeekBar) findViewById(R.id.fadeSeek);
        balanceSeek = (SeekBar) findViewById(R.id.balanceSeek);

        // initialize our settings dialog
        settingsDialog = new Dialog(context, android.R.style.Theme_Holo_Dialog);

        settingsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        settingsDialog.setContentView(R.layout.settings_dialog);
        settingsDialog.setCancelable(false);
        settingsDialog.setCanceledOnTouchOutside(false);
        settingsDialog.setTitle(R.string.settings_title);
        
		canBusTripleServiceConnection = new ServiceConnection() {
		    public void onServiceConnected(ComponentName className, IBinder service) {
		        canBusTripleService = ((CanBusTripleService.ServiceBinder) service).getService();
		        
		    	Intent intent = new Intent(CanBusTripleService.ACTION_UI_ACTIVE);
		    	intent.setPackage(context.getPackageName());
		    	startService(intent);	

		    	canBusTripleService.getBusData().addBusDataListener((BusDataListener) context);
		    	Log.d(APP_NAME, "onServiceConnected");
		    }

		    public void onServiceDisconnected(ComponentName className) {
		        canBusTripleService = null;
		        Log.d(APP_NAME, "onServiceDisconnected");
		    }
		};
		
	    bindService(new Intent(this, CanBusTripleService.class), canBusTripleServiceConnection, Context.BIND_AUTO_CREATE);	
    }

	@Override
	protected void onResume() {
		super.onResume();
		
        if (shuttingDown) return;

    	paused = false;

    	if (startingUp) {
        	startingUp = false;
        	
            ChangeLog cl = new ChangeLog(this);
            
            if (cl.firstRun()) {
                cl.getLogDialog().show();
            } else {
            	moveTaskToBack(true);
            }
        }

		Log.d(APP_NAME, "onResume");
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		paused = true;
		Log.d(APP_NAME, "onPause");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

        registerFadeHandler.removeCallbacks(RegisterFadeRunnable);
        audioControlsFadeHandler.removeCallbacks(audioControlsFadeRunnable);

        canBusTripleService.getBusData().removeBusDataListener((MainActivity) context);
    	unbindService(canBusTripleServiceConnection);

        Log.d(APP_NAME, "onDestroy");
	}
	
	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
        Log.d(APP_NAME, "onBackPressed");
	}
	
	@Override
	protected void onNewIntent(Intent intent) {

    	if ("shutdown".equals(intent.getAction())) {
    		Log.i(APP_NAME, "received shutdown intent");
    		shuttingDown = true;
    		
        	Intent i = new Intent(context, ShutdownReceiver.class);
        	sendBroadcast(i); 
        		
    		finish();
    	}

		if ("settings".equals(intent.getAction())) {
            if (!settingsDialog.isShowing()) showSettingsDialog();
		}
	}

    private void showSettingsDialog() {
        final EditText cbtEditText = (EditText) settingsDialog.findViewById(R.id.cbtEditText);
        final CheckBox chargeCheckBox = (CheckBox) settingsDialog.findViewById(R.id.chargeCheckBox);
        final CheckBox btCheckBox = (CheckBox) settingsDialog.findViewById(R.id.btCheckBox);
        final ExpandableListView devicesList = (ExpandableListView) settingsDialog.findViewById(R.id.btListView);

        cbtEditText.setText(prefs.getString("cbt_name", getResources().getString(R.string.cbt_name)));
        chargeCheckBox.setChecked(prefs.getBoolean("connect_on_charge", false));
        btCheckBox.setChecked(prefs.getBoolean("connect_on_bt_connect", false));

        ArrayList<String> listDataHeader = new ArrayList<String>();
        HashMap<String, List<String>> listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataHeader.add("Paired Devices");

        // Adding child data
        List<String> devices = new ArrayList<String>();

        try {
            for (BluetoothDevice device : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                devices.add(device.getName());
            }
        } catch (Exception ex) {
            devices.add("No Bluetooth Adapter");
        }

        listDataChild.put(listDataHeader.get(0), devices);

        final ExpandableListAdapter listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        listAdapter.setChecksList(prefs.getString("bt_devices", ""));

        devicesList.setAdapter(listAdapter);

        Button saveButton = (Button) settingsDialog.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.dismiss();

                final SharedPreferences.Editor edit = prefs.edit();

                edit.putString("cbt_name", cbtEditText.getText().toString());
                edit.putBoolean("connect_on_charge", chargeCheckBox.isChecked());
                edit.putBoolean("connect_on_bt_connect", btCheckBox.isChecked());

                final StringBuilder checks = new StringBuilder(128);

                for (CheckBox check : listAdapter.getChecks()) {
                    if (check.isChecked()) {
                        checks.append(check.getText());
                        checks.append("|");
                    }
                }

                // remove our trailing | and save our pref
                if (checks.length() > 0) {
                    checks.setLength(checks.length() - 1);
                    edit.putString("bt_devices", checks.toString());
                } else {
                    edit.remove("bt_devices");
                }

                edit.apply();

                Intent intent = new Intent(CanBusTripleService.ACTION_SETTINGS_CHANGED);
                intent.setPackage(context.getPackageName());
                startService(intent);

                moveTaskToBack(true);
            }
        });

        Button cancelButton = (Button) settingsDialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.dismiss();
                moveTaskToBack(true);
            }
        });

        settingsDialog.show();
    }


	@Override
	public boolean onTouch(View v, MotionEvent event) {	
	    switch (event.getAction()) {
		    case MotionEvent.ACTION_DOWN:
				moveTaskToBack(true);
				break;
		    case MotionEvent.ACTION_UP:
		        v.performClick();
		        break;
		    default:
		        break;
	    }
		return true;
	}
	

	@Override
	public void onBusDataChanged(Enums.BusDataFields field, Object value) {

		int drawable = 0;

		String text = "";
        final String band = canBusTripleService.getBusData().getRadioBand();
        String station = "";

        switch (field) {
            case AIRFLOW:
                if (lastHiPriMsg + 1000 > System.currentTimeMillis()) return;

                final Enums.Airflow airflow = (Enums.Airflow) value;

                switch (airflow) {
                    case AUTO_FRESH_AIR:
                        drawable = R.drawable.freshair;
                        break;
                    case AUTO_RECIRCULATE:
                        drawable = R.drawable.recirculate;
                        break;
                    case FRESH_AIR:
                        drawable = R.drawable.freshair;
                        break;
                    case RECIRCULATE:
                        drawable = R.drawable.recirculate;
                        break;
                    default:
                        break;
                }

                setAndShowRegister(drawable, airflow.toString());
                break;

            case AUDIO_SOURCE:
                final Enums.AudioSource source = (Enums.AudioSource) value;

                if (source != Enums.AudioSource.AUDIO_SETUP && audioControlsLayout.getVisibility() == View.VISIBLE) {
                    audioControlsFadeHandler.removeCallbacks(audioControlsFadeRunnable);
                    audioControlsFadeHandler.post(audioControlsFadeRunnable);
                    return;
                }

                switch (source) {
                    case BLUETOOTH:
                        drawable = R.drawable.bluetooth;
                        break;
                    case RADIO:
                        drawable = R.drawable.radio;
                        text = canBusTripleService.getBusData().getRadioBand();
                        break;
                    case USB:
                        drawable = R.drawable.usb;
                        break;
                    case XM:
                        drawable = R.drawable.xm;
                        break;

                    case AUDIO_SETUP:
                        showAudioControls();
                        return;

                    case BT_PAIRING:
                        drawable = R.drawable.bluetooth;
                        text = "BT PAIRING";
                        break;
                    case CLOCK_SETUP:
                        drawable = R.drawable.clock;
                        text = "CLOCK SETUP";
                        break;
                    default:
                        drawable = R.drawable.cd;
                        break;
                }

                setAndShowRegister(drawable, text);
                break;

            case BLUETOOTH_CONNECTED:
                boolean btConnected = (Boolean) value;
                text = String.format(getResources().getString(R.string.bt_device_status), btConnected ? getResources().getString(R.string.connected) : getResources().getString(R.string.disconnected));
                setAndShowRegister(R.drawable.bluetooth, text);
                break;

            case COMPRESSOR_ON:
                if (lastHiPriMsg + 1000 > System.currentTimeMillis()) return;
                setAndShowRegister(R.drawable.compressor, (Boolean) value ? getResources().getString(R.string.on) : getResources().getString(R.string.off));
                break;

            case DISPLAY_BUTTON_PRESSED:
//				setAndShowRegister(R.drawable.clock, canBusTripleService.getBusData().getTimeOfDay());
                setAndShowRegister(R.drawable.thermometer, String.format("%s\u00B0 Outside", canBusTripleService.getBusData().getOutsideTemperature()));
                Intent i = new Intent("com.android.music.musicservicecommand");
                i.putExtra("command", "next");
                sendBroadcast(i);
                break;

            case MUTED:
                boolean muted = (Boolean) value;
                setAndShowRegister(muted ? R.drawable.mute : R.drawable.volume, "");
                break;

            case OUTSIDE_TEMPERATURE:
//				setAndShowRegister(R.drawable.thermometer, String.format("%d\nOutside", (Integer) value));
                break;

            case RADIO_POWERED_ON:
                setAndShowRegister(R.drawable.radio, (Boolean) value ? "ON" : "OFF");
                break;

            case THERMOSTAT:
                lastHiPriMsg = System.currentTimeMillis();
                setAndShowRegister(R.drawable.thermometer, (String) value);
                break;

            case TIME_OF_DAY:
                // do nothing -- currently mapped to DISP button
                break;

            case VENTS:
                if (lastHiPriMsg + 1000 > System.currentTimeMillis()) return;
                lastHiPriMsg = System.currentTimeMillis();
                final Enums.Vents vents = (Enums.Vents) value;

                switch (vents) {
                    case DEFROST:
                        drawable = R.drawable.frontdefrost;
                        break;
                    case DEFROST_FLOOR:
                        drawable = R.drawable.defrostbottom;
                        break;
                    case FLOOR:
                        drawable = R.drawable.bottomvent;
                        break;
                    case FRONT:
                        drawable = R.drawable.topvent;
                        break;
                    case FRONT_FLOOR:
                        drawable = R.drawable.mixedvents;
                        break;
                    default:
                        break;
                }

                if (drawable != 0) setAndShowRegister(drawable, "");
                break;

            case VOLUME:
                setAndShowRegister(R.drawable.volume, (String) value);
                break;

            case RADIO_STATION:
                if (canBusTripleService.getBusData().getAudioSource() == Enums.AudioSource.RADIO) {
                    station = String.format(Locale.US, band.equals("AM") ? "%.0f" : "%.1f", (Float) value);
                    setAndShowRegister(R.drawable.radio, band + " " + station);
                }
                break;
			
			case SOUND_BALANCE:
                setSeekBarProgress(balanceSeek, (Integer) value);
				return;
				
			case SOUND_BASS:
                setSeekBarProgress(bassSeek, (Integer) value);
				return;

			case SOUND_FADER:
                setSeekBarProgress(faderSeek, (Integer) value);
				return;

			case SOUND_MIDRANGE:
                setSeekBarProgress(midRangeSeek, (Integer) value);
				return;

			case SOUND_TREBLE:
                setSeekBarProgress(trebleSeek, (Integer) value);
				return;

            case RADIO_PRESET:
                final String preset = (String) value;
                station = String.format(Locale.US, band.equals("AM") ? "%.0f" : "%.1f", canBusTripleService.getBusData().getRadioStation());
                setAndShowRegister(R.drawable.radio, band + " " + station + " " + preset);
                break;

            case FM_STEREO:
                if (canBusTripleService.getBusData().getAudioSource() == Enums.AudioSource.RADIO) {
                    station = String.format(Locale.US, band.equals("AM") ? "%.0f" : "%.1f", canBusTripleService.getBusData().getRadioStation());
                    setAndShowRegister(R.drawable.fmstereo, band + " " + station);
                }
                break;

            case XM_BAND:
                setAndShowRegister(R.drawable.xm, (String) value);
                break;

			default:
				break;
		}
		
	}

    private void setSeekBarProgress(final SeekBar seek, final int value) {
        seek.setProgress(value);

        // ensure the audio controls dialog stays visible
        if (audioControlsLayout.getVisibility() == View.VISIBLE) {
            audioControlsFadeHandler.removeCallbacks(audioControlsFadeRunnable);
            audioControlsFadeHandler.postDelayed(audioControlsFadeRunnable, 30000);
        }
    }

	private void setAndShowRegister(final int imageResource, final String text) {
		
		// bail if audio controls layout is visible
		if (audioControlsLayout.getVisibility() == View.VISIBLE) return;
		
		registerImage.setImageResource(imageResource);
		
		registerText.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
		registerText.setText(text);
		
		if (paused) {
		        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		        activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
		}
		
		if (audioControlsLayout.getVisibility() == View.VISIBLE) {
			audioControlsFadeHandler.removeCallbacks(audioControlsFadeRunnable);
			audioControlsFadeHandler.post(audioControlsFadeRunnable);
			return;
        }

        if (registerLayout.getVisibility() != View.VISIBLE) {
			registerLayout.setAlpha(0f);
			registerLayout.setVisibility(View.VISIBLE);
			registerLayout.animate()
			              .alpha(1f)
			              .setDuration(500)
			              .setListener(null);
            setWakeLock();
        }
		
		registerFadeHandler.removeCallbacks(RegisterFadeRunnable);
		registerFadeHandler.postDelayed(RegisterFadeRunnable, 3000);	
	}
	
	
	private final Runnable RegisterFadeRunnable = new Runnable() {
		@Override
		public void run() {
			
		    registerLayout.animate()
            .alpha(0f)
            .setDuration(500)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
        			registerLayout.setVisibility(View.GONE);
        			moveTaskToBack(true);
                    clearWakeLock();
                }
            });
		} 
	};
	
	private void showAudioControls() {
		
		if (paused) {
		        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		        activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
		}
		
		if (registerLayout.getVisibility() == View.VISIBLE) {
			registerFadeHandler.removeCallbacks(RegisterFadeRunnable);
			registerFadeHandler.post(RegisterFadeRunnable);
			return;
		}

		bassSeek.setProgress(canBusTripleService.getBusData().getBass());
		midRangeSeek.setProgress(canBusTripleService.getBusData().getMidRange());
		trebleSeek.setProgress(canBusTripleService.getBusData().getTreble());
		faderSeek.setProgress(canBusTripleService.getBusData().getFader());
		balanceSeek.setProgress(canBusTripleService.getBusData().getBalance());
		
		if (audioControlsLayout.getVisibility() != View.VISIBLE) {
			audioControlsLayout.setAlpha(0f);
			audioControlsLayout.setVisibility(View.VISIBLE);
			audioControlsLayout.animate()
			                   .alpha(1f)
                    .setDuration(500)
                    .setListener(null);
            setWakeLock();
		}
		
		audioControlsFadeHandler.removeCallbacks(audioControlsFadeRunnable);
		audioControlsFadeHandler.postDelayed(audioControlsFadeRunnable, 30000);	
	}
	
	private final Runnable audioControlsFadeRunnable = new Runnable() {
		@Override
		public void run() {
			
		    audioControlsLayout.animate()
            .alpha(0f)
            .setDuration(500)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
        			audioControlsLayout.setVisibility(View.GONE);
        			moveTaskToBack(true);
                    clearWakeLock();
                }
            });
		} 
	};

    public static void setWakeLock() {
//        getWindow().addFlags(
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
				                  PowerManager.ACQUIRE_CAUSES_WAKEUP |
				                  PowerManager.ON_AFTER_RELEASE, "WakeLock");
		wakeLock.acquire();

        Log.d(APP_NAME, "setWakeLock");
    }

    public static void clearWakeLock() {
//        getWindow().clearFlags(
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}

        Log.d(APP_NAME, "clearWakeLock");
    }
}
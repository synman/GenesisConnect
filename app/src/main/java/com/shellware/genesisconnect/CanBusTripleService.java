/*
 *   Copyright 2015 Shell M. Shrader
 *   
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.shellware.genesisconnect;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

public class CanBusTripleService extends Service
								 implements BusDataListener {
    
	private static final String SERVICE_NAME = "GenesisService";
	private static final int LONG_PAUSE = 5000;
	private static final int SHORT_PAUSE = 200;

    // indicates the state our service:
    public enum State {
        CONNECTING,
        CONNECTED,
        DISCONNECTED
    }

    public final static String ACTION_UI_ACTIVE = "com.shellware.genesisconnect.action.UI_ACTIVE";
    public final static String ACTION_SETTINGS_CHANGED = "com.shellware.genesisconnect.action.SETTINGS_CHANGED";

	private final static UUID CLIENT_CHARACTERISTIC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private final static UUID BLE_SERVICE_UUID = UUID.fromString("7a1fb359-735c-4983-8082-bdd7674c74d2");
	private final static UUID RECEIVE_NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("B0D6C9FE-E38A-4D31-9272-B8B3E93D8658");
	
	private final static byte[] INIT_GENESIS_CONNECT_REQUEST = { 0x01, 0x20 };
	private final static byte[] ENABLE_FILTER_REQUEST = { 0x03, 0x01, 0x01 };
	private final static byte[] DISABLE_FILTER_REQUEST = { 0x03, 0x01, 0x00 };
	
	private final static String MSG_SEPARATOR = "7C 7C";
	private final static String SOUND_AND_DISPLAY_MSG = "28";
	private final static String STEREO_POWER_MSG = "10";
	private final static String VOLUME_AND_SOURCE_MSG = "11";
	private final static String MUTE_MSG = "12";
	private final static String THERMOSTAT_MSG = "13";
	private final static String TIME_OF_DAY_MSG = "14";
	private final static String OUTSIDE_TEMPERATURE_MSG = "15";
	private final static String BLUETOOTH_CONNECTED_MSG = "16";
	private final static String VENTS_MSG = "17";
	private final static String RADIO_PRESET_AND_FM_STEREO_MSG = "18";
    private final static String XM_BAND_MSG = "19";

	private final static int CONNECT_TIMEOUT = 15000;
	private final static int READ_TIMEOUT = 15000;
	private final static int RECEIVE_TIMEOUTS_THRESHOLD = 3;
	
	private static String CBT_NAME = "CANBus Triple";
    private static boolean onlyConnectWhenCharging = false;
    private static boolean connectOnBtConnect = false;
	private static String btDevices = "";
    private static int devicesPresent = 0;

	private Context context;
	private SharedPreferences prefs;
		
	private BluetoothManager bluetoothManager;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothDevice bluetoothDevice;
	private BluetoothGatt bluetoothGatt;
	private BluetoothGattCharacteristic notifyCharacteristic;

    private PowerManager.WakeLock wakeLock;

	private final Handler dataHandler = new Handler();
	private final StringBuilder data = new StringBuilder(512);
	private State state = State.DISCONNECTED;
	
	private BusData busData;
	private long lastUpdated = getSystemMillis();
	private int receiveTimeouts = 0;
	
	private static boolean isCharging;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return new ServiceBinder();
	}
	
    public class ServiceBinder extends Binder {
        public CanBusTripleService getService() {
            return CanBusTripleService.this;
        }
    }

	@Override
	public void onCreate() {
		super.onCreate();

		context = this;
    	prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        busData = new BusData(context);
    	
    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    	Intent batteryStatus = context.registerReceiver(null, ifilter);
    	
    	// Are we charging / charged?
    	int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    	isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
    	             status == BatteryManager.BATTERY_STATUS_FULL;
    	
    	// register our power status receivers
        IntentFilter powerConnectedFilter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        registerReceiver(PowerStatusReceiver, powerConnectedFilter);

        IntentFilter powerDisconnectedFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(PowerStatusReceiver, powerDisconnectedFilter);

        // register our bt connection receivers
        IntentFilter btConnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(BtConnectionReceiver, btConnectedFilter);

        IntentFilter btDisconnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(BtConnectionReceiver, btDisconnectedFilter);

		Log.d(SERVICE_NAME, "onCreate charging: " + isCharging);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();

        dataHandler.removeCallbacks(DataRunnable);
        busData.removeBusDataListener((CanBusTripleService) context);

        unregisterReceiver(PowerStatusReceiver);
        unregisterReceiver(BtConnectionReceiver);

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            Log.i(SERVICE_NAME, "Stopping LE Scan");
            bluetoothAdapter.stopLeScan(leScanCallback);

            if (bluetoothGatt != null) {
                if (notifyCharacteristic != null) {
                    Log.d(SERVICE_NAME, "disabling filter");
                    notifyCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    notifyCharacteristic.setValue(DISABLE_FILTER_REQUEST);
                    bluetoothGatt.writeCharacteristic(notifyCharacteristic);

                    sleep(1000);
                }

                Log.d(SERVICE_NAME,"deallocating gatt");
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }

        stopForeground(true);
        Log.d(SERVICE_NAME, "CanBusTripleService destroy");
    }

    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		String text = getResources().getString(R.string.notification_content_text);
		String subtext = "";

		boolean showProgress = false;

		final String action = intent.getAction();
		
		// bail if no intent or already connecting / connected 
		if (action == null || state != State.DISCONNECTED) return START_NOT_STICKY;
		Log.d(SERVICE_NAME, "CanBusTriple start intent: " + action);

        // lets reload our settings
        CBT_NAME = prefs.getString("cbt_name", getResources().getString(R.string.cbt_name));
        onlyConnectWhenCharging = prefs.getBoolean("connect_on_charge", false);
        connectOnBtConnect = prefs.getBoolean("connect_on_bt_connect", false);
        btDevices = prefs.getString("bt_devices", "");
        devicesPresent = 0;

        if (action.equals(ACTION_SETTINGS_CHANGED)) {
            Log.d(SERVICE_NAME, "settings updated");
            return START_NOT_STICKY;
        }
        
		if (state == State.CONNECTED) {
			Log.d(SERVICE_NAME, "already connected");
			return START_NOT_STICKY;
		}
		
		lastUpdated = getSystemMillis() + CONNECT_TIMEOUT;
        dataHandler.postDelayed(DataRunnable, SHORT_PAUSE);

		if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
 			subtext = getResources().getString(R.string.bluetooth_disabled);
            state = State.DISCONNECTED;
		} else {
			if (bluetoothAdapter == null) {
	 			subtext = getResources().getString(R.string.no_bt_adapter);
                state = State.DISCONNECTED;
			} else {
				final String mac = prefs.getString("default_mac", "");
				
				showProgress = true;
				
				if (mac.length() > 0) {
		 			text = String.format(getResources().getString(R.string.connecting_to), CBT_NAME);
		            bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
					bluetoothGatt = bluetoothDevice.connectGatt(context, false, btleGattCallback);
				} else {
		 			text = String.format(getResources().getString(R.string.searching_for), CBT_NAME);
					bluetoothAdapter.startLeScan(leScanCallback);				
				}
				
				state = State.CONNECTING;
			}
		}

        updateNotification(text, subtext, showProgress);
        
        return START_NOT_STICKY;
	}

	private final Runnable DataRunnable = new Runnable() {

		public void run() {
		
			String[] messages;

            if (state == State.DISCONNECTED) {
                if (onlyConnectWhenCharging && !isCharging) {
                    dataHandler.postDelayed(DataRunnable, LONG_PAUSE);
                    Log.d(SERVICE_NAME, "long delay - NO CHARGE");
                    return;
                }
                if (connectOnBtConnect && devicesPresent < 1) {
                    dataHandler.postDelayed(DataRunnable, LONG_PAUSE);
                    Log.d(SERVICE_NAME, "long delay - NO DEVICE");
                    return;
                }
            }

			// we haven't received data for some time
			if (lastUpdated + READ_TIMEOUT < getSystemMillis()) {
				
	            String text = getResources().getString(R.string.notification_content_text);
				String subtext = "";
				boolean showProgress = false;

				receiveTimeouts++;
				
				if (receiveTimeouts >= RECEIVE_TIMEOUTS_THRESHOLD) {
		            updateNotification(getResources().getString(R.string.reinitializing_bluetooth), "", true);
	
					// bluetooth stack and/or CBT is hosed
					if (state == State.CONNECTED) {
						bluetoothGatt.disconnect();
						sleep(2000);
					}
					if (bluetoothAdapter != null) {
						bluetoothAdapter.disable();
						sleep(10000);
						bluetoothAdapter.enable();
						sleep(10000);
					}
					
					receiveTimeouts = 0;
				}
				
				if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
		 			subtext = getResources().getString(R.string.bluetooth_disabled);
		            state = State.DISCONNECTED;
				} else {
					if (bluetoothAdapter == null) {
			 			subtext = getResources().getString(R.string.no_bt_adapter);
		                state = State.DISCONNECTED;
					} else {
						final String mac = prefs.getString("default_mac", "");
						
						showProgress = true;
						
						if (mac.length() > 0) {
				 			text = String.format(getResources().getString(R.string.connecting_to), CBT_NAME);
				            bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
							bluetoothGatt = bluetoothDevice.connectGatt(context, false, btleGattCallback);
						} else {
				 			text = String.format(getResources().getString(R.string.searching_for), CBT_NAME);
							bluetoothAdapter.startLeScan(leScanCallback);				
						}
						
						state = State.CONNECTING;
					}
				}
				
				lastUpdated = getSystemMillis() + CONNECT_TIMEOUT;
		        updateNotification(text, subtext, showProgress);
			}
			
			synchronized (data) {
				messages = data.toString().trim().split(MSG_SEPARATOR);
				data.setLength(0);
			}

			for (String msg : messages) {
				final String message = msg.trim();
				if (message.length() > 0) processMessage(message);
			}
			
			dataHandler.postDelayed(DataRunnable, SHORT_PAUSE);
		}	
		
		private void processMessage(final String message) {
			
			// big try/catch because of data quality issues
			try {									
				// sound settings (bass/mid/treble/fader/balance) or DISP button
				if (message.startsWith(SOUND_AND_DISPLAY_MSG)) {
					final int balance = Integer.parseInt(message.substring(3, 5), 16);
					final int fader = Integer.parseInt(message.substring(6, 8), 16);
					final int bass  = Integer.parseInt(message.substring(9, 11), 16);
					final int midrange  = Integer.parseInt(message.substring(12, 14), 16);
					final int treble  = Integer.parseInt(message.substring(15, 17), 16);

					if (balance <= 20) busData.setBalance(balance);
					if (fader <= 20) busData.setFader(fader);
					if (bass <= 20) busData.setBass(bass);
					if (midrange <= 20) busData.setMidRange(midrange);
					if (treble <= 20) busData.setTreble(treble);

					if (busData.getAudioSource() != Enums.AudioSource.AUDIO_SETUP) {
						busData.setDisplayButtonPressed(true);
	       				Log.i(SERVICE_NAME, "DISP button pressed");
					} else {
						Log.i(SERVICE_NAME, String.format("Bass: %d Mid: %d Treble: %d Fader: %d Balance: %d", bass, midrange, treble, fader, balance));
					}
				}
				
				// radio power 
				if (message.startsWith(STEREO_POWER_MSG)) {
					busData.setRadioPoweredOn( message.endsWith("01") );
					
	       			Log.i(SERVICE_NAME, "Stereo Powered: " + busData.isRadioPoweredOn());
					return;
				}
				
				// volume, audio source, radio station, radio band
				if (message.startsWith(VOLUME_AND_SOURCE_MSG)) {
					final int volume = Integer.parseInt(message.substring(3, 5), 16);
					final int source = Integer.parseInt(message.substring(6, 8), 16);
					final int station = Integer.parseInt(message.substring(9, 11), 16);
					final int band  = Integer.parseInt(message.substring(12, 14), 16);

					if (volume <= 0x8C) busData.setVolume(volume);
					if (band == 1 || band == 129 || band == 2 || band == 3) busData.setRadioBand(band);
					busData.setAudioSource(source);
					busData.setRadioStation(station);
					
	       			Log.i(SERVICE_NAME, "Volume: " + busData.getVolume() +
							            " Source: " + busData.getAudioSource().toString() +
	       					            " Station: " + busData.getRadioStation() +
							            " Band: " + busData.getRadioBand());
					return;
				}
	
				// volume muted
				if (message.startsWith(MUTE_MSG)) {	
					busData.setMuted(message.endsWith("01"));
					
	       			Log.i(SERVICE_NAME, "Mute: " + busData.isMuted());
					return;
				}
				
				// thermostat
				if (message.startsWith(THERMOSTAT_MSG)) {
					final int thermostat = Integer.parseInt(message.substring(3, 5), 16);
					
					if (thermostat <= 30) busData.setThermostat(thermostat);
					
	       			Log.i(SERVICE_NAME, "Thermostat: " + busData.getThermostat());
					return;
				}
	
				// time of day
				if (message.startsWith(TIME_OF_DAY_MSG)) {
					final int hour = Integer.parseInt(message.substring(3, 5), 10);
					final int minute = Integer.parseInt(message.substring(6, 8), 10);
					
					busData.setTimeOfDay(String.format(Locale.US, "%02d:%02d", hour, minute));
					lastUpdated = getSystemMillis();
					receiveTimeouts = 0;
					
	       			Log.i(SERVICE_NAME, "Time of day: " + busData.getTimeOfDay());
					return;
				}
				
				// outside temperature
				if (message.startsWith(OUTSIDE_TEMPERATURE_MSG)) {
//					final int outsideTemperature = Integer.parseInt(message.substring(message.indexOf("15") + 3, message.indexOf("15") + 5), 10);
					final String outsideTemperature = message.substring(3, 5);
									
					busData.setOutsideTemperature(outsideTemperature);
					lastUpdated = getSystemMillis();
					receiveTimeouts = 0;

	       			Log.i(SERVICE_NAME, "Outside temperature: " + busData.getOutsideTemperature());
					return;
				}
	
				// bluetooth status
				if (message.startsWith(BLUETOOTH_CONNECTED_MSG)) {
					final int bluetoothConnected = Integer.parseInt(message.substring(3, 5), 16);
					
					busData.setBluetoothConnected(bluetoothConnected == 0x1 || bluetoothConnected == 0x11);
					
	       			Log.i(SERVICE_NAME, "Bluetooth connected: " + busData.isBluetoothConnected());
					return;
				}

                // ac vents, auto/fresh/recirc, ac compressor
                if (message.startsWith(VENTS_MSG)) {
                    final int vents = Integer.parseInt(message.substring(3, 5), 16);
                    final int airflow = Integer.parseInt(message.substring(6, 8), 16);
                    final int compressor = Integer.parseInt(message.substring(9, 11), 16);

                    busData.setVents(vents);
                    busData.setAirflow(airflow);
                    busData.setCompressorOn(compressor == 0x0D);

                    Log.i(SERVICE_NAME, "Vents: " + busData.getVents().toString() + " Airflow: " + busData.getAirflow().toString() + " Compressor: " + busData.isCompressorOn());
                    return;
                }

                // radio presets and fm stereo
                if (message.startsWith(RADIO_PRESET_AND_FM_STEREO_MSG)) {
//                    final int fmStereo = Integer.parseInt(message.substring(3, 5), 16);
//                    final int fmStereo = Integer.parseInt(message.substring(6, 8), 16);

//                    busData.setRadioPreset(preset);
                    busData.setFmStereo(true);

                    Log.i(SERVICE_NAME, "Radio Preset: " + busData.getRadioPreset() + " FM Stereo: " + busData.isFmStereo());
                    return;
                }

                // XM Band
//                if (message.startsWith(XM_BAND_MSG)) {
//                    final int band = Integer.parseInt(message.substring(3, 5), 16);
//
//                    busData.setXmBand(band);
//
//                    Log.i(SERVICE_NAME, "XM Band: " + busData.getXmBand());
//                    return;
//                }


            } catch (Exception ex) {
				Log.w(SERVICE_NAME, "Message rejected: [" + message + "] " + Log.getStackTraceString(ex));
			}
		}
	};

	private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			
			if (device.getName().contains(CBT_NAME)) {
	   			final String msg = String.format(Locale.US, "Found %s - %s",device.getName(), device.getAddress());
	   			Log.i(SERVICE_NAME, msg);
								
				bluetoothDevice = device;
				bluetoothAdapter.stopLeScan(leScanCallback);
				
				lastUpdated = getSystemMillis() + CONNECT_TIMEOUT;
	            updateNotification(String.format(getResources().getString(R.string.connecting_to), device.getName()), "", true);

				bluetoothGatt = bluetoothDevice.connectGatt(context, false, btleGattCallback);
			}
		}
	};
	
	private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

	    @Override
	    public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
	    	super.onCharacteristicChanged(gatt, characteristic);

	    	final byte[] response = characteristic.getValue();

	    	synchronized (data) {
		    	for (byte byteChar : response) {
		    		data.append(String.format(Locale.US, "%02X ", byteChar));
		    	}

//		    	Log.i(SERVICE_NAME, "[" + new String(response) + "]");
		    	Log.d(SERVICE_NAME, "[" + data.toString() + "]");
	    	}
	    }

	    @Override
	    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
	    	super.onConnectionStateChange(gatt, status, newState);

	        if (newState == BluetoothProfile.STATE_CONNECTED) {
				final Editor edit = prefs.edit();
				edit.putString("default_mac", bluetoothDevice.getAddress());
				edit.apply();

	        	bluetoothGatt.discoverServices();
	        } else {
	        	state = State.DISCONNECTED;
	            updateNotification(getResources().getString(R.string.notification_content_text), getResources().getString(R.string.disconnected), false);
	        }

        	Log.d(SERVICE_NAME, "onConnectionStateChange newState=" + newState);
	    }

	    @Override
	    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
	    	super.onServicesDiscovered(gatt, status);

    		BluetoothGattService service = bluetoothGatt.getService(BLE_SERVICE_UUID);
    		if (service == null) return;

			notifyCharacteristic = service.getCharacteristic(RECEIVE_NOTIFY_CHARACTERISTIC_UUID);
			BluetoothGattDescriptor notifyDescriptor = notifyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_UUID);

			bluetoothGatt.setCharacteristicNotification(notifyCharacteristic, true);
			notifyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		    bluetoothGatt.writeDescriptor(notifyDescriptor);

   			Log.d(SERVICE_NAME, "onServicesDiscovered");
	    }

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);

			// reinitialize GenesisConnect registers
			descriptor.getCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
			descriptor.getCharacteristic().setValue(INIT_GENESIS_CONNECT_REQUEST);
			bluetoothGatt.writeCharacteristic(descriptor.getCharacteristic());

            Log.d(SERVICE_NAME, "onDescriptorWrite");
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);

			if (state == State.CONNECTING) {
		        state = State.CONNECTED;

		        // unleash the kraken
		        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
				characteristic.setValue(ENABLE_FILTER_REQUEST);
				bluetoothGatt.writeCharacteristic(characteristic);

				lastUpdated = getSystemMillis();
		        updateNotification(getResources().getString(R.string.notification_content_text), "Connected to " + CBT_NAME, false);

		        busData.removeBusDataListener((CanBusTripleService) context);
		        busData.addBusDataListener((CanBusTripleService) context);
			}

			Log.d(SERVICE_NAME, "onCharacteristicWrite");
		}
	};

	private void updateNotification(final String text, final String subtext, final boolean showProgress) {
		
		stopForeground(true);

        Intent iClose = new Intent(context, MainActivity.class);
        iClose.setAction("shutdown");
        iClose.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent iSettings = new Intent(context, MainActivity.class);
        iSettings.setAction("settings");
        iSettings.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent piClose = PendingIntent.getActivity(getApplicationContext(), 0, iClose, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piSettings = PendingIntent.getActivity(getApplicationContext(), 0, iSettings, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notify);

    	Notification.Builder notifier = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notify)
                .setLargeIcon(Bitmap.createScaledBitmap(b, 96, 96, false))
                .setContentTitle(getResources().getString(R.string.service_name))
					.setAutoCancel(false)
					.setOngoing(true)
					.setPriority(Notification.PRIORITY_MAX)
					.setShowWhen(false)
					.addAction(android.R.drawable.ic_menu_close_clear_cancel, getResources().getString(R.string.close), piClose)
					.addAction(android.R.drawable.ic_menu_preferences, getResources().getString(R.string.settings), piSettings);

        if (text.length() > 0) notifier.setContentText(text);
        if (subtext.length() > 0 && !showProgress) notifier.setSubText(subtext);
        if (showProgress) notifier.setProgress(0, 0, true);

        startForeground(1, notifier.build());
	}

	public BusData getBusData() {
		return busData;
	}
	
	public State getState() {
		return state;
	}

	@Override
	public void onBusDataChanged(Enums.BusDataFields field, Object value) {
		lastUpdated = getSystemMillis();
		receiveTimeouts = 0;
	}
	
	private void sleep(final int duration) {
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			// do nothing
		}
	}
	
	private static long getSystemMillis() {
		return System.nanoTime() / 1000000;
	}
	
	private final BroadcastReceiver PowerStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        	Intent batteryStatus = context.registerReceiver(null, ifilter);
        	
        	// Are we charging / charged?
        	int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        	isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        	             status == BatteryManager.BATTERY_STATUS_FULL;

	        Log.d(SERVICE_NAME, "PowerStatusReceiver charging: " + isCharging);
        }
    };

    private final BroadcastReceiver BtConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                if (btDevices.contains(device.getName())) {
                    devicesPresent++;

                    //TODO: Clean this up at some point
                    MainActivity.setWakeLock();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.clearWakeLock();
                        }
                    }, 5000);
                }
                Log.d(SERVICE_NAME, "BtConnectedReceiver device connected: " + device.getName());
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                if (btDevices.contains(device.getName()) && devicesPresent > 0) devicesPresent--;
                Log.d(SERVICE_NAME, "BtConnectedReceiver device disconnected: " + device.getName());
            }
        }
    };

//    private String getBTMajorDeviceClass(int major){
//        switch(major) {
//            case BluetoothClass.Device.Major.AUDIO_VIDEO:
//                return "AUDIO_VIDEO";
//            case BluetoothClass.Device.Major.COMPUTER:
//                return "COMPUTER";
//            case BluetoothClass.Device.Major.HEALTH:
//                return "HEALTH";
//            case BluetoothClass.Device.Major.IMAGING:
//                return "IMAGING";
//            case BluetoothClass.Device.Major.MISC:
//                return "MISC";
//            case BluetoothClass.Device.Major.NETWORKING:
//                return "NETWORKING";
//            case BluetoothClass.Device.Major.PERIPHERAL:
//                return "PERIPHERAL";
//            case BluetoothClass.Device.Major.PHONE:
//                return "PHONE";
//            case BluetoothClass.Device.Major.TOY:
//                return "TOY";
//            case BluetoothClass.Device.Major.UNCATEGORIZED:
//                return "UNCATEGORIZED";
//            case BluetoothClass.Device.Major.WEARABLE:
//                return "AUDIO_VIDEO";
//            default: return "unknown!";
//        }
//    }


//    private void acquireWakeLock() {
//        if (wakeLock != null) wakeLock.release();
//
//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//
//        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
//                PowerManager.ACQUIRE_CAUSES_WAKEUP |
//                PowerManager.ON_AFTER_RELEASE, "WakeLock");
//        wakeLock.acquire();
//    }
}

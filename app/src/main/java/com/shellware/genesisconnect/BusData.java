package com.shellware.genesisconnect;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class BusData {

	private final static String CLASS_NAME = "GenesisData";

	public enum Vents {
	    FRONT(0x11), FRONT_FLOOR(0x12), FLOOR(0x13), DEFROST_FLOOR(0x14), DEFROST(0x15);
	    
	    private static class Cache {
	        private static final SparseArray<Vents> cache = new SparseArray<Vents>();

	        static {
	            for (final Vents d : Vents.values()) {
	                cache.put(d.fId, d);
	            }
	        }
	    }

	    public static Vents getVents(int i) {
	        return Cache.cache.get(i);
	    }

	    public final int fId;

        Vents(int id) {
	        this.fId = id;
	    }

		@Override
		public String toString() {
			switch (fId) {
				case 0x11:
					return "Front";
				case 0x12:
					return "Front/Floor";
				case 0x13:
					return "Floor";
				case 0x14:
					return "Defrost/Floor";
				case 0x15:
					return "Defrost";
				default:
					return "UNKNOWN - " + fId;
			}
		}
	}	
	public enum Airflow {
	    FRESH_AIR(0x00), AUTO_FRESH_AIR(0x01), RECIRCULATE(0x04), AUTO_RECIRCULATE(0x05);
	 
	    private static class Cache {
	        private static final SparseArray<Airflow> cache = new SparseArray<Airflow>();

	        static {
	            for (final Airflow d : Airflow.values()) {
	                cache.put(d.fId, d);
	            }
	        }
	    }

	    public static Airflow getAirflow(int i) {
	        return Cache.cache.get(i);
	    }

	    public final int fId;

        Airflow(int id) {
	        this.fId = id;
	    }

		@Override
		public String toString() {
			switch (fId) {
				case 0x00:
					return "Fresh Air";
				case 0x01:
					return "Auto Fresh Air";
				case 0x04:
					return "Recirculate";
				case 0x05:
					return "Auto Recirculate";
				default:
					return "UNKNOWN " + fId;
			}
		}
	}
	public enum AudioSource {
	    RADIO(0x02), XM(0x10), USB(0xA0), BLUETOOTH(0x51), CLOCK_SETUP(0x40), AUDIO_SETUP(0x41), BT_PAIRING(0x52);
	 
	    private static class Cache {
	        private static final SparseArray<AudioSource> cache = new SparseArray<AudioSource>();

	        static {
	            for (final AudioSource d : AudioSource.values()) {
	                cache.put(d.fId, d);
	            }
	        }
	    }

	    public static AudioSource getAudioSource(int i) {
	        return Cache.cache.get(i);
	    }

	    public final int fId;

        AudioSource(int id) {
	        this.fId = id;
	    }

		@Override
		public String toString() {
			switch (fId) {
				case 0x02:
					return "Radio";
				case 0x10:
					return "XM";
				case 0xA0:
					return "USB";
				case 0x51:
					return "Bluetooth";
				case 0x40:
					return "Clock Setup";
				case 0x41:
					return "Audio Setup";
				case 0x52:
					return "Bluetooth Pairing";
				default:
					return "UNKNOWN " + fId;
			}
		}
	}

	public enum BusDataFields {
		DISPLAY_BUTTON_PRESSED,
		RADIO_POWERED_ON,
		MUTED,
		VOLUME,
		THERMOSTAT,
		OUTSIDE_TEMPERATURE,
		TIME_OF_DAY,
		BLUETOOTH_CONNECTED,
		VENTS,
		AIRFLOW,
		COMPRESSOR_ON,
		AUDIO_SOURCE,
		RADIO_STATION,
		RADIO_BAND,
		SOUND_BASS,
		SOUND_MIDRANGE,
		SOUND_TREBLE,
		SOUND_BALANCE,
		SOUND_FADER
	}
	
	private boolean displayButtonPressed;
	private boolean radioPoweredOn;
	private boolean muted;
	private int volume;
	private int thermostat;
	private String outsideTemperature;
	private String timeOfDay;
	private boolean bluetoothConnected;
	private Vents vents;
	private Airflow airflow;
	private boolean compressorOn;
	private AudioSource audioSource;
	private int radioStation;
	private int radioBand;
	
	private int bass;
	private int midrange;
	private int treble;
	private int balance;
	private int fader;
	
	private final Context context;
	private final Collection<BusDataListener> busDataListeners = new ArrayList<BusDataListener>();
	
	public BusData(final Context context) {
		super();
		this.context = context;
	}

	public synchronized AudioSource getAudioSource() {
		return audioSource;
	}

	public synchronized void setAudioSource(int source) {
		if (this.audioSource != AudioSource.getAudioSource(source)) {
			this.audioSource = AudioSource.getAudioSource(source);

			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.AUDIO_SOURCE, getAudioSource());
			}
		}
	}

	public synchronized Vents getVents() {
		return vents;
	}

	public synchronized void setVents(int vents) {
		if (this.vents != Vents.getVents(vents)) {
			this.vents = Vents.getVents(vents);
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.VENTS, getVents());
			}
		}
	}

	public synchronized Airflow getAirflow() {
		return airflow;
	}

	public synchronized void setAirflow(final int airflow) {
		if (this.airflow != Airflow.getAirflow(airflow)) {
			this.airflow = Airflow.getAirflow(airflow);
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.AIRFLOW, getAirflow());
			}
		}
	}

	public synchronized boolean isCompressorOn() {
		return compressorOn;
	}

	public synchronized void setCompressorOn(boolean compressorOn) {
		if (this.compressorOn != compressorOn) {
			this.compressorOn = compressorOn;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.COMPRESSOR_ON, isCompressorOn());
			}
		}
	}

	public synchronized boolean isBluetoothConnected() {
		return bluetoothConnected;
	}

	public synchronized void setBluetoothConnected(boolean bluetoothConnected) {
		if (this.bluetoothConnected != bluetoothConnected) {
			this.bluetoothConnected = bluetoothConnected;

			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.BLUETOOTH_CONNECTED, isBluetoothConnected());
			}
		}
	}

	public synchronized String getTimeOfDay() {
		return timeOfDay;
	}

	public synchronized void setTimeOfDay(String timeOfDay) {
		if (!timeOfDay.equals(this.timeOfDay)) {
			this.timeOfDay = timeOfDay;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.TIME_OF_DAY, getTimeOfDay());
			}
		}
	}

	public synchronized boolean isMuted() {
		return muted;
	}

	public synchronized void setMuted(boolean muted) {
		if (this.muted != muted) {
			this.muted = muted;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.MUTED, isMuted());
			}
		}
	}

	public synchronized String getVolume() {
		return String.format(Locale.US, "%d%%", Math.round(volume / 1.4));
	}

	public synchronized void setVolume(int volume) {
		if (this.volume != volume) {
			this.volume = volume;

			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.VOLUME, getVolume());
			}
		}
	}

	public synchronized String getThermostat() {
		String therm;
		
		switch (thermostat) {
			case 0:
				therm = context.getResources().getString(R.string.off);
				break;
			case 2:
				therm = context.getResources().getString(R.string.lo);
				break;
			case 30:
				therm = context.getResources().getString(R.string.hi);
				break;
			default:
				therm = String.format(Locale.US, "%d\u00B0", 60 + thermostat);
				break;
		}
		return therm;
	}

	public synchronized void setThermostat(int thermostat) {
		if (this.thermostat != thermostat) {
			this.thermostat = thermostat;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.THERMOSTAT, getThermostat());
			}
		}
	}

	public synchronized String getOutsideTemperature() {
		final int int1 = Integer.parseInt(outsideTemperature.substring(0,1), 16);
		final int int2 = Integer.parseInt(outsideTemperature.substring(1,2), 16);
		final int int3 = int1 * 10 + int2;

		return String.format (Locale.US, "%d", int3);			
	}

	public synchronized void setOutsideTemperature(String outsideTemperature) {
		if (!outsideTemperature.equals(this.outsideTemperature)) {
			this.outsideTemperature = outsideTemperature;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.OUTSIDE_TEMPERATURE, getOutsideTemperature());
			}
		}
	}

	public synchronized boolean isRadioPoweredOn() {
		return radioPoweredOn;
	}

	public synchronized void setRadioPoweredOn(boolean radioPoweredOn) {
		if (this.radioPoweredOn != radioPoweredOn) {
			this.radioPoweredOn = radioPoweredOn;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.RADIO_POWERED_ON, isRadioPoweredOn());
			}
		}
	}

	public synchronized boolean isDisplayButtonPressed() {
		
		final boolean pressed = displayButtonPressed;
		displayButtonPressed = false;

		return pressed;
	}

	public synchronized void setDisplayButtonPressed(boolean displayButtonPressed) {
		if (this.displayButtonPressed != displayButtonPressed) {
			this.displayButtonPressed = displayButtonPressed;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.DISPLAY_BUTTON_PRESSED, isDisplayButtonPressed());
			}
		}
	}
	

	public synchronized float getRadioStation() {
		switch (radioBand) {
			case 2: // fm1
				// CH87.5=0+2/87.7
				return (float) (radioStation * .1f + 87.5);
			case 3: // fm2
				// CH87.5=0+2/87.7
				return (float) (radioStation * .1f + 87.5);
			default: // am
				// CH530=0+1/540
				return (float) radioStation * 20 + (radioBand == 129 ? 530 : 520);
		}
	}

	public synchronized void setRadioStation(int radioStation) {
		if (this.radioStation != radioStation) {
			this.radioStation = radioStation;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.RADIO_STATION, getRadioStation());
			}
		}
	}

	public synchronized String getRadioBand() {
		switch (radioBand) {
			case 1:
			case 129:
				return context.getResources().getString(R.string.am);
			case 2:
				return context.getResources().getString(R.string.fm1);
			case 3:
				return context.getResources().getString(R.string.fm2);
			default:
				return "UNKNOWN - " + radioBand;
		}
	}

	public synchronized void setRadioBand(int radioBand) {
		if (this.radioBand != radioBand) {
			this.radioBand = radioBand;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.RADIO_BAND, getRadioBand());
			}
		}
	}

	public synchronized int getBass() {
		return bass;
	}

	public synchronized void setBass(int bass) {
		if (this.bass != bass) {
			this.bass = bass;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.SOUND_BASS, getBass());
			}
		}
	}

	public synchronized int getMidRange() {
		return midrange;
	}

	public synchronized void setMidRange(int midrange) {
		if (this.midrange != midrange) {
			this.midrange = midrange;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.SOUND_MIDRANGE, getMidRange());
			}
		}
	}
	
	public synchronized int getTreble() {
		return treble;
	}

	public synchronized void setTreble(int treble) {
		if (this.treble != treble) {
			this.treble = treble;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.SOUND_TREBLE, getTreble());
			}
		}
	}

	public synchronized int getBalance() {
		return balance;
	}

	public synchronized void setBalance(int balance) {
		if (this.balance != balance) {
			this.balance = balance;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.SOUND_BALANCE , getBalance());
			}
		}
	}

	public synchronized int getFader() {
		return fader;
	}

	public synchronized void setFader(int fader) {
		if (this.fader != fader) {
			this.fader = fader;
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(BusDataFields.SOUND_FADER, getFader());
			}
		}
	}

	public boolean addBusDataListener(BusDataListener busDataListener) {
		boolean status = !busDataListeners.contains(busDataListener) && busDataListeners.add(busDataListener);
		Log.d(CLASS_NAME, "addBusDataListener " + status);
		return status;
	}
	
	public boolean removeBusDataListener(BusDataListener busDataListener) {
		boolean status = busDataListeners.remove(busDataListener);
		Log.d(CLASS_NAME, "removeBusDataListener " + status);
		return status;
	}
}
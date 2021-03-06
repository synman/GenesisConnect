package com.shellware.genesisconnect;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class BusData {

	private final static String CLASS_NAME = "GenesisData";

	private boolean displayButtonPressed;
	private boolean radioPoweredOn;
	private boolean muted;
	private int volume;
	private int thermostat;
	private String outsideTemperature;
	private String timeOfDay;
	private boolean bluetoothConnected;
	private  Enums.Vents vents;
	private  Enums.Airflow airflow;
	private boolean compressorOn;
	private  Enums.AudioSource audioSource;
	private int radioStation;
	private int radioBand;
	
	private int bass;
	private int midrange;
	private int treble;
	private int balance;
	private int fader;

    private int radioPreset;
    private boolean fmStereo;
    private int xmBand;
	
	private final Context context;
	private final Collection<BusDataListener> busDataListeners = new ArrayList<BusDataListener>();
	
	public BusData(final Context context) {
		super();
		this.context = context;
	}

	public synchronized  Enums.AudioSource getAudioSource() {
		return audioSource;
	}

	public synchronized void setAudioSource(int source) {
		if (this.audioSource !=  Enums.AudioSource.getAudioSource(source)) {
			this.audioSource =  Enums.AudioSource.getAudioSource(source);

			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(Enums.BusDataFields.AUDIO_SOURCE, getAudioSource());
			}
		}
	}

	public synchronized  Enums.Vents getVents() {
		return vents;
	}

	public synchronized void setVents(int vents) {
		if (this.vents !=  Enums.Vents.getVents(vents)) {
			this.vents =  Enums.Vents.getVents(vents);
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(Enums.BusDataFields.VENTS, getVents());
			}
		}
	}

	public synchronized  Enums.Airflow getAirflow() {
		return airflow;
	}

	public synchronized void setAirflow(final int airflow) {
		if (this.airflow !=  Enums.Airflow.getAirflow(airflow)) {
			this.airflow =  Enums.Airflow.getAirflow(airflow);
			
			for (BusDataListener busDataListener : busDataListeners) {
				busDataListener.onBusDataChanged(Enums.BusDataFields.AIRFLOW, getAirflow());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.COMPRESSOR_ON, isCompressorOn());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.BLUETOOTH_CONNECTED, isBluetoothConnected());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.TIME_OF_DAY, getTimeOfDay());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.MUTED, isMuted());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.VOLUME, getVolume());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.THERMOSTAT, getThermostat());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.OUTSIDE_TEMPERATURE, getOutsideTemperature());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.RADIO_POWERED_ON, isRadioPoweredOn());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.DISPLAY_BUTTON_PRESSED, isDisplayButtonPressed());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.RADIO_STATION, getRadioStation());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.RADIO_BAND, getRadioBand());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.SOUND_BASS, getBass());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.SOUND_MIDRANGE, getMidRange());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.SOUND_TREBLE, getTreble());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.SOUND_BALANCE , getBalance());
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
				busDataListener.onBusDataChanged(Enums.BusDataFields.SOUND_FADER, getFader());
			}
		}
	}

    public synchronized String getRadioPreset() {
        switch (radioPreset) {
            case 10:
                return "Preset #1";
            case 20:
                return "Preset #2";
            case 30:
                return "Preset #3";
            case 40:
                return "Preset #4";
            case 50:
                return "Preset #5";
            case 60:
                return "Preset #6";
            default:
                return "Unknown Preset";
        }
    }

    public synchronized void setRadioPreset(int preset) {
        if (this.radioPreset != preset) {
            this.radioPreset = preset;

            for (BusDataListener busDataListener : busDataListeners) {
                busDataListener.onBusDataChanged(Enums.BusDataFields.RADIO_PRESET, getRadioPreset());
            }
        }
    }

    public synchronized boolean isFmStereo() { return fmStereo; }

    public synchronized void setFmStereo(boolean fmStereo) {
//        if (this.fmStereo != fmStereo) {
            this.fmStereo = fmStereo;

            for (BusDataListener busDataListener : busDataListeners) {
                busDataListener.onBusDataChanged(Enums.BusDataFields.FM_STEREO, isFmStereo());
            }
//        }
    }

    public synchronized String getXmBand() {
        switch (xmBand) {
            case 1:
                return "XM1";
            case 2:
                return "XM2";
            case 3:
                return "XM3";
            default:
                return "Unknown XM Band";
        }
    }

    public synchronized void setXmBand(int xmBand) {
        if (this.xmBand != xmBand) {
            this.xmBand = xmBand;

            for (BusDataListener busDataListener : busDataListeners) {
                busDataListener.onBusDataChanged(Enums.BusDataFields.XM_BAND, getXmBand());
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
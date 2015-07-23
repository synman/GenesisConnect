package com.shellware.genesisconnect;

import android.util.SparseArray;

/**
 * Created by shell on 7/13/15.
 */
public class Enums {

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
        SOUND_FADER,
        RADIO_PRESET,
        FM_STEREO,
        XM_BAND
    }
}

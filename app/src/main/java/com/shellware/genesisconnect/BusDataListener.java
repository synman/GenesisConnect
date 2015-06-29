package com.shellware.genesisconnect;

import com.shellware.genesisconnect.BusData.BusDataFields;

interface BusDataListener {
	
	void onBusDataChanged(final BusDataFields field, final Object value);
}

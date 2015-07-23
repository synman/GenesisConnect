package com.shellware.genesisconnect;


interface BusDataListener {
	
	void onBusDataChanged(final Enums.BusDataFields field, final Object value);
}

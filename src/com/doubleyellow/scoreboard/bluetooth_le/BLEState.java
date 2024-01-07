package com.doubleyellow.scoreboard.bluetooth_le;

public enum BLEState {
    CONNECTING,
    CONNECTED_DiscoveringServices,
    CONNECTED_TO_1_of_2,
    CONNECTED_ALL,
    DISCONNECTED_Gatt,
    DISCONNECTED,
}

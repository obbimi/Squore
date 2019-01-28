package com.doubleyellow.scoreboard.bluetooth;

public enum BTState {
    // Constants that indicate the current connection state
    NONE       , // we're doing nothing
    LISTEN     , // now listening for incoming connections
    CONNECTING , // now initiating an outgoing connection
    CONNECTED  , // now connected to a remote device
}

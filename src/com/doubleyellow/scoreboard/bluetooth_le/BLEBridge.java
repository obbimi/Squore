/*
 * Copyright (C) 2025  Iddo Hoeve
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.doubleyellow.scoreboard.bluetooth_le;

import android.content.Intent;

public interface BLEBridge
{
    public boolean init();
    public void stop();


    public boolean clearBLEConfirmationStatus();
    public boolean clearBLEWaitForConfirmation();
    public void updateBLEConnectionStatus(int visibility, int nrOfDevicesConnected, String sMsg, int iDurationSecs);


    public void selectBleDevices();
    public void selectBleDevices_handleResult(boolean bResultOk, Intent data);


    public void undoLastBLE(String[] saMethodNArgs);
    public void changeScoreBLE(String[] saMethodNArgs);
    public void changeScoreBLEConfirm(String[] saMethodNArgs);
    public void undoScoreForInitiatorBLE(String[] saMethodNArgs);


    public void promoteAppToUseBLE();
}

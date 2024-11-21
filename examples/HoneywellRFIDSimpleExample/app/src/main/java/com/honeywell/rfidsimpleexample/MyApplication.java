package com.honeywell.rfidsimpleexample;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.honeywell.rfidservice.ConnectionState;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;

public class MyApplication extends Application {
    private static MyApplication mInstance;
    public RfidManager rfidMgr;
    public RfidReader mRfidReader;
    public String macAddress;
    public String mEPCSelected = "";

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static MyApplication getInstance() {
        return mInstance;
    }

    public void setRfidMgr(RfidManager rfidMgr) {
        this.rfidMgr = rfidMgr;
    }

    public boolean checkIsRFIDReady() {
        return checkIsRFIDReady(true);
    }

    public boolean checkIsRFIDReady(boolean showToast) {
        if (rfidMgr.getConnectionState() != ConnectionState.STATE_CONNECTED) {
            if (showToast) {
                Toast.makeText(this, "Device is not Connected!", Toast.LENGTH_SHORT)
                        .show();
            }
            return false;
        }
        if (!rfidMgr.readerAvailable()) {
            if (showToast) {
                Toast.makeText(this, "Reader is null!", Toast.LENGTH_SHORT)
                        .show();
            }
            return false;
        }
        if (rfidMgr.getTriggerMode() != TriggerMode.RFID) {
            if (showToast) {
                Toast.makeText(this, "Current mode is not RFID mode!",
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
}

package com.honeywell.rfidsimpleexample;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidsimpleexample.common.SpKeys;
import com.honeywell.rfidsimpleexample.rfid.WorkMode;
import com.honeywell.rfidsimpleexample.utils.SpUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private final String[] mPermissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private final List<String> mRequestPermissions = new ArrayList<>();
    private final boolean[] mPermissionGranted = new boolean[] {
            false, false
    };

    private final String TAG = "RfidSampleCode";
    private MyApplication mMyApplication;
    private RfidManager mRfidMgr;
    private BluetoothAdapter mBluetoothAdapter;

    private final Handler mHandler = new Handler();
    private ProgressDialog mWaitDialog;
    private TextView mTvInfo;
    private Button mBtnConnect;
    private Button mBtnCreateReader;
    private Button mBtnRead;
    private Button mBtnSettings;
    private Button mBtnEdit;
    private ListView mLv;
    private MyAdapter<BtDeviceInfo> mAdapter;
    private final List<BtDeviceInfo> mDevices = new ArrayList<>();
    private int mSelectedIdx = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMyApplication = MyApplication.getInstance();
        RfidManager.create(this, new RfidManager.CreatedCallback() {
            @Override
            public void onCreated(RfidManager rfidManager) {
                mRfidMgr=rfidManager;
                mMyApplication.setRfidMgr(mRfidMgr);
                mRfidMgr.addEventListener(mEventListener);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showBtn();
                    }
                });

            }
        });
        setContentView(R.layout.activity_main);
        mTvInfo = findViewById(R.id.tv_info);
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnCreateReader = findViewById(R.id.btn_create_reader);
        mBtnRead = findViewById(R.id.btn_read);
        mBtnSettings = findViewById(R.id.btn_settings);
        mBtnEdit = findViewById(R.id.btn_edit);
        showBtn();
        mLv = findViewById(R.id.lv);
        mAdapter = new MyAdapter<>(this, mDevices);
        mLv.setAdapter(mAdapter);
        mLv.setOnItemClickListener(mItemClickListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mRfidMgr!=null){
            mRfidMgr.addEventListener(mEventListener);
        }
        showBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        mHandler.removeCallbacksAndMessages(null);
        if(mRfidMgr!=null){
            mRfidMgr.removeEventListener(mEventListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bt, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.scan == id) {
            scan();
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestPermissions() {
        for (String mPermission : mPermissions) {
            if (ContextCompat.checkSelfPermission(this, mPermission)
                    != PERMISSION_GRANTED) {
                mRequestPermissions.add(mPermission);
            }
        }

        if (mRequestPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    mPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PERMISSION_REQUEST_CODE == requestCode) {
            for (int i = 0; i < permissions.length; ++i) {
                for (int j = 0; j < mPermissions.length; ++j) {
                    if (permissions[i].equals(mPermissions[j])
                            && PERMISSION_GRANTED == grantResults[i]) {
                        mPermissionGranted[j] = true;
                        break;
                    }
                }
            }
        }
    }

    private boolean isBluetoothPermissionGranted() {
        for (int i = 0; i < mPermissions.length; ++i) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(mPermissions[i])) {
                return mPermissionGranted[i];
            }
        }
        return false;
    }

    private void showBtn() {
        mTvInfo.setTextColor(Color.rgb(128, 128, 128));
        Log.i(TAG,"showBtn isConnected()="+isConnected());
        if (isConnected()) {
            if (mRfidMgr.isSerialDevice()) {
                mTvInfo.setText("Connected");
            } else {
                mTvInfo.setText(mMyApplication.macAddress + " connected.");
            }
            mTvInfo.setTextColor(Color.rgb(0, 128, 0));
            mBtnConnect.setText("Disconnect");
            mBtnCreateReader.setEnabled(true);
            mBtnRead.setEnabled(isAvailable());
            mBtnSettings.setEnabled(isAvailable());
            mBtnEdit.setEnabled(isAvailable());
        } else {
            mTvInfo.setText("Device not connected.");
            //mBtnConnect.setEnabled(mSelectedIdx != -1);
            mBtnConnect.setText("Connect");
            mBtnCreateReader.setEnabled(false);
            mBtnRead.setEnabled(false);
            mBtnSettings.setEnabled(false);
            mBtnEdit.setEnabled(false);
        }
    }

    private boolean isConnected() {
        return mRfidMgr != null && mRfidMgr.isConnected();
    }

    private boolean isAvailable() {
        return null != MyApplication.getInstance().mRfidReader
                && MyApplication.getInstance().mRfidReader.available();
    }

    public void clickBtnConn(View v) {
        if (isConnected()) {
            disconnect();
        } else {
            connect();
//            connectSerial();
        }
    }

    public void clickBtnInventory(View v) {
        Intent intent = new Intent(MainActivity.this, ReadActivity.class);
        startActivity(intent);
    }

    public void clickBtnCreateReader(View view) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRfidMgr.createReader();
            }
        }, 1000);

        mWaitDialog = ProgressDialog.show(this, null, "Creating reader...");
    }

    public void clickBtnSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void clickBtnEdit(View view) {
        Intent intent = new Intent(this, EditActivity.class);
        startActivity(intent);
    }

    private void scan() {
        if (!isBluetoothPermissionGranted()) {
            return;
        }

        mDevices.clear();
        mSelectedIdx = -1;
        mAdapter.notifyDataSetChanged();
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        mWaitDialog = ProgressDialog.show(this, null,
                "Scanning Bluetooth devices...");
        mWaitDialog.setCancelable(false);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, 5 * 1000);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        closeWaitDialog();
    }

    private void connect() {
        if (mRfidMgr == null) return;
        mWaitDialog = ProgressDialog.show(this, null, "Connecting...");
        if (mRfidMgr.isSerialDevice() || mSelectedIdx == -1 || mSelectedIdx >= mDevices.size()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mRfidMgr.connect(null)) {
                        mWaitDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Connect failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (mRfidMgr.isUsbDevice()) {
            if (!mRfidMgr.connect(null)) {
                mWaitDialog.dismiss();
                Toast.makeText(this, "Connect failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (!mRfidMgr.connect(mDevices.get(mSelectedIdx).dev.getAddress())) {
                mWaitDialog.dismiss();
                Toast.makeText(this, "Connect failed", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void disconnect() {
        if(mRfidMgr!=null){
            mRfidMgr.disconnect();
        }
    }

    private void closeWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    private final EventListener mEventListener = new EventListener() {
        @Override
        public void onDeviceConnected(Object o) {
            mMyApplication.macAddress = (String) o;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG,"onDeviceConnected");
                    showBtn();
                    closeWaitDialog();
                }
            });
        }

        @Override
        public void onDeviceDisconnected(Object o) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG,"onDeviceDisconnected");
                    MyApplication.getInstance().mRfidReader = null;
                    showBtn();
                    closeWaitDialog();
                }
            });
        }

        @Override
        public void onUsbDeviceAttached(Object o) {

        }

        @Override
        public void onUsbDeviceDetached(Object o) {

        }

        @Override
        public void onReaderCreated(boolean b, final RfidReader rfidReader) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MyApplication.getInstance().mRfidReader = rfidReader;
                    WorkMode.getInstance()
                            .enableMode(Integer.parseInt(SpUtils.getString(
                                    getResources().getString(R.string.settings_key_scenario),
                                    "" + WorkMode.MODE_ANTI_INTERENCE)),
                                    rfidReader);
                    mBtnRead.setEnabled(true);
                    mBtnSettings.setEnabled(true);
                    mBtnEdit.setEnabled(true);
                    mWaitDialog.dismiss();
                }
            });
        }

        @Override
        public void onRfidTriggered(boolean b) {
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode) {
        }

        @Override
        public void onReceivedFindingTag(int i) {

        }
    };

    private long mPrevListUpdateTime;
    private final BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() != null && !device.getName().isEmpty()) {
                synchronized (mDevices) {
                    boolean newDevice = true;

                    for (BtDeviceInfo info : mDevices) {
                        if (device.getAddress().equals(info.dev.getAddress())) {
                            newDevice = false;
                            info.rssi = rssi;
                        }
                    }

                    if (newDevice) {
                        mDevices.add(new BtDeviceInfo(device, rssi));
                    }

                    long cur = System.currentTimeMillis();

                    if (newDevice || cur - mPrevListUpdateTime > 500) {
                        mPrevListUpdateTime = cur;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        }
    };

    private static class BtDeviceInfo {
        BluetoothDevice dev;
        int rssi;

        private BtDeviceInfo(BluetoothDevice dev, int rssi) {
            this.dev = dev;
            this.rssi = rssi;
        }
    }

    private class MyAdapter<T> extends ArrayAdapter<T> {
        private final Context ctx;

        public MyAdapter(Context context, List<T> ls) {
            super(context, 0, ls);
            ctx = context;
        }

        public View getView(int position, @Nullable View v, @NonNull ViewGroup parent) {
            ViewHolder vh;

            if (v == null) {
                LayoutInflater inflater = LayoutInflater.from(ctx);
                v = inflater.inflate(R.layout.list_item_bt_device, null);
                vh = new ViewHolder();
                vh.tvName = v.findViewById(R.id.tvName);
                vh.tvAddr = v.findViewById(R.id.tvAddr);
                vh.tvRssi = v.findViewById(R.id.tvRssi);
                v.setTag(vh);
            } else {
                vh = (ViewHolder) v.getTag();
            }

            BtDeviceInfo item = mDevices.get(position);
            vh.tvName.setText(item.dev.getName());
            vh.tvAddr.setText(item.dev.getAddress());
            vh.tvRssi.setText(String.valueOf(item.rssi));

            if (position == mSelectedIdx) {
                v.setBackgroundColor(Color.rgb(220, 220, 220));
            } else {
                v.setBackgroundColor(Color.argb(0, 0, 0, 0));
            }

            return v;
        }

        class ViewHolder {
            TextView tvName;
            TextView tvAddr;
            TextView tvRssi;
        }
    }

    private final AdapterView.OnItemClickListener mItemClickListener =
            new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            mSelectedIdx = i;
            mAdapter.notifyDataSetChanged();
            showBtn();
        }
    };
}

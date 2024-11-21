package com.honeywell.rfidsimpleexample;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.BankInfo;
import com.honeywell.rfidservice.rfid.OnTagReadListener;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.RfidReaderException;
import com.honeywell.rfidservice.rfid.TagAdditionData;
import com.honeywell.rfidservice.rfid.TagReadData;
import com.honeywell.rfidservice.rfid.TagReadOption;
import com.honeywell.rfidservice.utils.ByteUtils;
import com.honeywell.rfidsimpleexample.common.SpKeys;
import com.honeywell.rfidsimpleexample.rfid.WorkMode;
import com.honeywell.rfidsimpleexample.utils.SpUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ReadActivity extends AppCompatActivity {
    private final static String TAG = "ReadActivity";

    private RfidManager mRfidMgr;
    private RfidReader mReader;
    private final MyApplication mApp = MyApplication.getInstance();
    private final List<TagInfo> mTagDataList = new ArrayList<>();

    private boolean mIsReadBtnClicked;
    private Button mBtnRead;
    private TagListAdapter mAdapter;

    private final String NORMAL_READ = "Normal Read";
    private final String FAST_READ = "Fast Read";

    private volatile boolean mNormalReadThreadRun = false;
    private Thread mNormalReadThread = null;

    private boolean mShowAddition = false;
    private int mTargetSwitchInterval = 3000;
    private int mCurrentTarget = 0;  // 0 A, 1 B

    static class TagInfo {
        TagReadData tagReadData;
        int count = 0;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRfidMgr = mApp.rfidMgr;
        mReader = mApp.mRfidReader;

        setContentView(R.layout.activity_read);
        mBtnRead = findViewById(R.id.btn_read);

        mAdapter = new TagListAdapter(this, mTagDataList);
        mAdapter.setOnItemClickListener(new MyTagItemClickListener());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView tags = findViewById(R.id.rv_tags);
        tags.setAdapter(mAdapter);
        tags.setLayoutManager(layoutManager);
        tags.setHasFixedSize(false);

        mShowAddition = !TagAdditionData.NONE.getName().equals(
                SpUtils.getString(getString(R.string.settings_key_addition_data),
                        TagAdditionData.NONE.getName()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRfidMgr.addEventListener(mEventListener);
        showBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRfidMgr.removeEventListener(mEventListener);
        mIsReadBtnClicked = false;
        stopRead();
    }

    private void showBtn() {
        if (mIsReadBtnClicked) {
            mBtnRead.setText("Stop");
            mBtnRead.setTextColor(Color.rgb(255, 128, 0));
        } else {
            // see "settings_scan_mode_entries" and "settings_scan_mode_entryValues" in string.xml
            if ("0".equals(SpUtils.getString(
                    getString(R.string.settings_key_scan_mode), "0"))) {
                mBtnRead.setText(NORMAL_READ);
            } else {
                mBtnRead.setText(FAST_READ);
            }
            mBtnRead.setTextColor(Color.rgb(0, 0, 0));
        }
    }

    public void clickBtnRead(View view) {
        if (mIsReadBtnClicked) {
            mIsReadBtnClicked = false;
            stopRead();
        } else {
            mIsReadBtnClicked = true;
            read();
        }

        showBtn();
    }

    private final EventListener mEventListener = new EventListener() {
        @Override
        public void onDeviceConnected(Object o) {
        }

        @Override
        public void onDeviceDisconnected(Object o) {
        }

        @Override
        public void onUsbDeviceAttached(Object o) {

        }

        @Override
        public void onUsbDeviceDetached(Object o) {

        }

        @Override
        public void onReaderCreated(boolean b, RfidReader rfidReader) {
        }

        @Override
        public void onRfidTriggered(boolean trigger) {
            if (mIsReadBtnClicked || !trigger) {
                mIsReadBtnClicked = false;
                stopRead();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showBtn();
                    }
                });
            } else {
                read();
            }
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode) {
        }

        @Override
        public void onReceivedFindingTag(int i) {

        }
    };

    private boolean isReaderAvailable() {
        return mReader != null && mReader.available();
    }

    private String getStringFromEditText(AppCompatEditText et) {
        Editable text = et.getText();
        if (null != text) {
            return text.toString();
        }
        return null;
    }

    private void read() {
        if (!isReaderAvailable()) {
            return;
        }
        synchronized (mTagDataList) {
            mTagDataList.clear();
            mAdapter.update(mTagDataList);
        }
        mTargetSwitchInterval = Integer.parseInt(SpUtils.getString(
                SpKeys.KEY_RFID_TARGET_INTERVAL, "3000"));
        final int mode = WorkMode.getInstance().getCurrentScenario();
        if ("0".equals(SpUtils.getString(
                getString(R.string.settings_key_scan_mode), "0"))) {
            // Normal mode
            mNormalReadThreadRun = true;
            mNormalReadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String additionDataType = SpUtils.getString(
                            getString(R.string.settings_key_addition_data), "None");
                    TagAdditionData tagAddition = TagAdditionData.get(additionDataType);
                    long lastReadTime = System.currentTimeMillis();
                    while (mNormalReadThreadRun) {
                        TagReadData[] tags = mReader.syncRead(tagAddition, 200);
                        if (null != tags) {
                            dataListener.onTagRead(tags);

                            long currentTime = System.currentTimeMillis();
                            if (mode == WorkMode.MODE_MASS_TAG_MULTI_INVENTORY) {
                                if (0 == tags.length) {
                                    if (currentTime - lastReadTime > mTargetSwitchInterval) {
                                        switchTarget();
                                        lastReadTime = currentTime;
                                    }
                                } else {
                                    lastReadTime = currentTime;
                                }
                            } else {
                                lastReadTime = currentTime;
                            }
                        }
                    }
                }
            });
            mNormalReadThread.start();
        } else {
            // Fast mode
            mReader.setOnTagReadListener(dataListener);
            mReader.read(TagAdditionData.get(SpUtils.getString(getString(R.string.settings_key_addition_data),
                    "None")), createTagReadOption());
        }
    }

    private void switchTarget() {
        Log.i(TAG, "switchTarget mCurrentTarget=" + mCurrentTarget);
        if (null == mApp.mRfidReader) {
            return;
        }
        if (0 == mCurrentTarget) {
            mApp.mRfidReader.setTarget(1);
            mCurrentTarget = 1;
        } else {
            mApp.mRfidReader.setTarget(0);
            mCurrentTarget = 0;
        }
    }

    private TagReadOption createTagReadOption() {
        TagReadOption readOption = new TagReadOption();
        readOption.setReadCount(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_count), false));
        readOption.setRssi(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_rssi), false));
        readOption.setAntennaId(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_ant), false));
        readOption.setFrequency(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_freq), false));
        readOption.setTimestamp(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_time), false));
        readOption.setProtocol(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_pro), false));
        readOption.setData(
                SpUtils.getBoolean(getString(R.string.fast_mode_key_data), false));
        readOption.setStopPercent(Integer.parseInt(
                SpUtils.getString(
                        getString(R.string.settings_key_pause_percentage), "0")));
        return readOption;
    }

    private void stopRead() {
        if (!isReaderAvailable()) {
            return;
        }
        if ("0".equals(SpUtils.getString(
                getString(R.string.settings_key_scan_mode), "0"))) {
            // Normal mode
            mNormalReadThreadRun = false;
            if (null != mNormalReadThread) {
                mNormalReadThread.interrupt();
                mNormalReadThread = null;
            }
        } else {
            // Fast mode
            mReader.stopRead();
            mReader.removeOnTagReadListener(dataListener);
        }
    }

    private final OnTagReadListener dataListener = new OnTagReadListener() {
        @Override
        public void onTagRead(final TagReadData[] t) {
            synchronized (mTagDataList) {
                for (TagReadData trd : t) {
                    String epc = trd.getEpcHexStr();
                    if (mShowAddition) {
                        epc += ByteUtils.bytes2HexStr(trd.getAdditionData());
                    }
                    boolean doUpdate = true;
                    for (TagInfo tagInfo : mTagDataList) {
                        if (mShowAddition) {
                            String key = tagInfo.tagReadData.getEpcHexStr()
                                    + ByteUtils.bytes2HexStr(tagInfo.tagReadData.getAdditionData());
                            if (key.equals(epc)) {
                                ++tagInfo.count;
                                doUpdate = false;
                                break;
                            }
                        } else {
                            if (tagInfo.tagReadData.getEpcHexStr().equals(trd.getEpcHexStr())) {
                                ++tagInfo.count;
                                doUpdate = false;
                                break;
                            }
                        }
                    }
                    if (doUpdate) {
                        TagInfo tagInfo = new TagInfo();
                        tagInfo.tagReadData = trd;
                        tagInfo.count = 1;
                        mTagDataList.add(tagInfo);
                        doUpdate = true;
                    }
                }

                mAdapter.update(mTagDataList);

                mHandler.sendEmptyMessage(0);
            }
        }
    };

    private final Handler mHandler = new MyHandle(this);

    static class MyHandle extends Handler {
        private final WeakReference<ReadActivity> mRef;

        MyHandle(ReadActivity activity) {
            mRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            ReadActivity activity = mRef.get();
            if (null != activity) {
                if (msg.what == 0) {
                    activity.mAdapter.notifyDataSetChanged();
                }
            }
            super.handleMessage(msg);
        }
    }

    static class TagListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<TagInfo> mList;
        private final Handler mHandler;
        private int mSelectedPos = -1;

        private final int TYPE_HEADER = 0;
        private final int TYPE_DATA = 1;

        private OnItemClickListener mOnItemClickListener = null;

        private final int[] itemColors;

        TagListAdapter(Context context, List<TagInfo> list) {
            mList = list;
            mHandler = new Handler();
            itemColors = new int[] {
                    context.getResources().getColor(R.color.colorTagInvItem1, null),
                    context.getResources().getColor(R.color.colorTagInvItem2, null)
            };
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(
                    parent.getContext()).inflate(
                    R.layout.list_item_tag_list, parent, false);
            if (TYPE_HEADER == viewType) {
                return new HeaderViewHolder(view);
            } else {
                return new DataViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DataViewHolder) {
                final int position_ = position - 1;
                DataViewHolder dvh = (DataViewHolder) holder;
                dvh.mEPC.setText(mList.get(position_).tagReadData.getEpcHexStr());
                dvh.mCount.setText(String.valueOf(mList.get(position_).count));
                dvh.mAddition.setText(ByteUtils.bytes2HexStr(mList.get(position_)
                        .tagReadData.getAdditionData()));

                if (position_ == mSelectedPos) {
                    dvh.mView.setBackgroundColor(Color.rgb(220, 220, 220));
                } else {
                    dvh.mView.setBackgroundColor(itemColors[(position_) % 2]);
                }
                dvh.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != mOnItemClickListener) {
                            mOnItemClickListener.onItemClick(v, position_);
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mList.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (0 == position) {
                return TYPE_HEADER;
            }
            return TYPE_DATA;
        }

        public void update(final List<TagInfo> list) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mList = list;
                    notifyDataSetChanged();
                }
            });
        }

        public void setSelected(int position) {
            mSelectedPos = position;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        interface OnItemClickListener {
            void onItemClick(View view, int position);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        static class DataViewHolder extends RecyclerView.ViewHolder {
            TextView mEPC;
            TextView mCount;
            TextView mAddition;
            View mView;

            public DataViewHolder(@NonNull View itemView) {
                super(itemView);
                mEPC = itemView.findViewById(R.id.epc);
                mCount = itemView.findViewById(R.id.count);
                mAddition = itemView.findViewById(R.id.addition);
                mView = itemView;
            }
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            public HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                TextView epc = itemView.findViewById(R.id.epc);
                TextView count = itemView.findViewById(R.id.count);
                TextView addition = itemView.findViewById(R.id.addition);

                epc.setText("EPC ID");
                count.setText("Count");
                addition.setText("Addition Data");

                epc.setTextColor(itemView.getResources().getColor(
                        R.color.colorWhite, null));
                count.setTextColor(itemView.getResources().getColor(
                        R.color.colorWhite, null));
                addition.setTextColor(itemView.getResources().getColor(
                        R.color.colorWhite, null));
                itemView.setBackgroundColor(Color.rgb(0xcc, 0xcc, 0xff));

                epc.setGravity(Gravity.CENTER);
//                count.setGravity(Gravity.CENTER);
                addition.setGravity(Gravity.CENTER);
            }
        }
    }

    class MyTagItemClickListener implements TagListAdapter.OnItemClickListener {
        @Override
        public void onItemClick(View view, int position) {
            mAdapter.setSelected(position);
            if (position >= 0 && position < mTagDataList.size()) {
                mApp.mEPCSelected = mTagDataList.get(position).tagReadData.getEpcHexStr();
            }
        }
    }
}

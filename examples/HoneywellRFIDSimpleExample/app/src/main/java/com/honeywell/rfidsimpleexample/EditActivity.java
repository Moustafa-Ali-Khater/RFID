package com.honeywell.rfidsimpleexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import com.honeywell.rfidservice.rfid.BankInfo;
import com.honeywell.rfidservice.rfid.Gen2;
import com.honeywell.rfidservice.rfid.RfidReaderException;

public class EditActivity extends AppCompatActivity {

    private final MyApplication mApp = MyApplication.getInstance();

    // Common
    private AppCompatSpinner mFilterBank;
    private AppCompatEditText mFilterData;
    private AppCompatEditText mFilterStartAddress;
    private AppCompatEditText mPassword;
    private SwitchCompat mUsePassword;

    // Read/Write
    private AppCompatSpinner mBank;
    private AppCompatEditText mStartAddress;
    private AppCompatEditText mBlocks;
    private AppCompatEditText mData;

    // Lock
    private AppCompatSpinner mLockArea;
    private AppCompatSpinner mLockType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        initViews();
        mFilterData.setText(mApp.mEPCSelected);
    }

    private void initViews() {
        mFilterBank = findViewById(R.id.sp_filter_bank);
        mFilterData = findViewById(R.id.et_epc);
        mFilterStartAddress = findViewById(R.id.et_filter_startAddr);
        mPassword = findViewById(R.id.et_password);
        mUsePassword = findViewById(R.id.sc_usePassword);
        mBank = findViewById(R.id.sp_bank);
        mStartAddress = findViewById(R.id.et_startAddr);
        mBlocks = findViewById(R.id.et_blocks);
        mData = findViewById(R.id.et_data);
        AppCompatButton mRead = findViewById(R.id.btn_read);
        mRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
//                    doReadOld();
                    doRead();
                } catch (RfidReaderException e) {
                    e.printStackTrace();
                    Toast.makeText(EditActivity.this, "Read failure",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        AppCompatButton mWrite = findViewById(R.id.btn_write);
        mWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    doWrite();
                } catch (RfidReaderException e) {
                    e.printStackTrace();
                    Toast.makeText(EditActivity.this, "Write failure",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        mLockArea = findViewById(R.id.sp_lockArea);
        mLockType = findViewById(R.id.sp_lockType);
        AppCompatButton mLock = findViewById(R.id.btn_lock);
        mLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    doLock();
                } catch (RfidReaderException e) {
                    e.printStackTrace();
                    Toast.makeText(EditActivity.this, "Lock failure",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    String getEditTextText(AppCompatEditText et) {
        Editable editable = et.getText();
        if (null != editable) {
            return editable.toString();
        }
        return null;
    }

    private int getEditTextIntValue(AppCompatEditText et) {
        String text = getEditTextText(et);
        if (null == text || 0 == text.length()) {
            return 0;
        }
        return Integer.parseInt(text);
    }

    private BankInfo getFilterInfo() {
        int filterBank = mFilterBank.getSelectedItemPosition() + 1;  // Cannot filter reserved bank
        int filterStartAddr = getEditTextIntValue(mFilterStartAddress);
        String filterData = getEditTextText(mFilterData);
        BankInfo filterInfo = new BankInfo(filterBank);
        filterInfo.setStartAddr(filterStartAddr);
        filterInfo.setBankValue(filterData);
        return filterInfo;
    }

    /**
     * Do the read tag operation
     *
     * This method can only filter out the tag according to the EPC bank
     *
     * <p>
     * note:
     * You have to take notice the start address and the block number(the block count).
     * The failure would happened while the read range is out of the bound of the tag.
     * For example, if the address range of a TID bank is from 0 to 12, you can only
     * pass the start address and block number inside [0, 12]. Once the range is out
     * of the bound, the failure is returned.
     * For example, start address with value 2 and block number with value 11,
     * which means the range [2, 13], would cause a failure call.
     * </p>
     *
     * @throws RfidReaderException The {@link RfidReaderException} may thrown
     */
    private void doReadOld() throws RfidReaderException {
        int bank = mBank.getSelectedItemPosition();
        int startAddr = getEditTextIntValue(mStartAddress);
        int blockCnt = getEditTextIntValue(mBlocks);
        String data = mApp.mRfidReader.readTagData(
                getEditTextText(mFilterData), bank, startAddr, blockCnt,
                mUsePassword.isChecked() ? getEditTextText(mPassword) : null);
        if (null != data) {
            mData.setText(data);
        } else {
            mData.setText("");
        }
    }

    /**
     * Do the read tag operation
     *
     * <p>
     * note:
     * You have to take notice the start address and the block number(the block count).
     * The failure would happened while the read range is out of the bound of the tag.
     * For example, if the address range of a TID bank is from 0 to 12, you can only
     * pass the start address and block number inside [0, 12]. Once the range is out
     * of the bound, the failure is returned.
     * For example, start address with value 2 and block number with value 11,
     * which means the range [2, 13], would cause a failure call.
     * </p>
     *
     * @throws RfidReaderException The {@link RfidReaderException} may thrown
     */
    private void doRead() throws RfidReaderException {
        int bank = mBank.getSelectedItemPosition();
        int startAddr = getEditTextIntValue(mStartAddress);
        int blockCnt = getEditTextIntValue(mBlocks);
        String filterData = getEditTextText(mFilterData);
        String password = mUsePassword.isChecked() ? getEditTextText(mPassword) : null;

        String data = mApp.mRfidReader.readTagData(
                filterData, bank, startAddr, blockCnt, password);
        if (null != data) {
            mData.setText(data);
        } else {
            mData.setText("");
        }
    }

    private void doWrite() throws RfidReaderException {
        int bank = mBank.getSelectedItemPosition();
        String data = getEditTextText(mData);
        int startAddress = getEditTextIntValue(mStartAddress);
        String password = mUsePassword.isChecked() ? getEditTextText(mPassword) : null;
        int blockCnt = getEditTextIntValue(mBlocks);
        String filterData = getEditTextText(mFilterData);
        // When the number of blocks is larger than 18, we split the data into two parts to write
        if (blockCnt > 18) {
            mApp.mRfidReader.writeTagData(filterData, bank, startAddress, password, data);
            startAddress += 18;
            data = data.substring(18 << 2);
        }
        mApp.mRfidReader.writeTagData(filterData, bank, startAddress, password, data);
        Toast.makeText(this, "Write successfully", Toast.LENGTH_SHORT).show();
    }

    private void doLock() throws RfidReaderException {
        String[] arrBank = getResources().getStringArray(R.array.edit_lock_area);
        String[] arrType = getResources().getStringArray(R.array.edit_lock_type);
        Gen2.LockBank lockBank = Gen2.LockBank.get(arrBank[mLockArea.getSelectedItemPosition()]);
        Gen2.LockType lockType = Gen2.LockType.get(arrType[mLockType.getSelectedItemPosition()]);
        String password = mUsePassword.isChecked() ? getEditTextText(mPassword) : null;
        String filterData = getEditTextText(mFilterData);
        mApp.mRfidReader.lockTag(filterData, lockBank, lockType, password);
        Toast.makeText(this, "Lock successfully", Toast.LENGTH_SHORT).show();
    }

    private String getText(AppCompatEditText et) {
        Editable editable = et.getText();
        if (null == editable) {
            return "";
        }
        return editable.toString();
    }
}
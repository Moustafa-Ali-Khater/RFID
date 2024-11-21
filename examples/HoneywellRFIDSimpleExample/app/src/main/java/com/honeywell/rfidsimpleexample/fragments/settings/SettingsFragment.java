package com.honeywell.rfidsimpleexample.fragments.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.chip.Chip;
import com.honeywell.rfidservice.ReaderVolume;
import com.honeywell.rfidservice.rfid.AntennaPower;
import com.honeywell.rfidservice.rfid.RfidReaderException;
import com.honeywell.rfidservice.rfid.TagAdditionData;
import com.honeywell.rfidsimpleexample.MyApplication;
import com.honeywell.rfidsimpleexample.R;
import com.honeywell.rfidsimpleexample.utils.SpUtils;
import com.honeywell.rfidsimpleexample.views.MyPreference;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {

    MyApplication mApp = MyApplication.getInstance();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);
        initCommonPreferences();
        initAntennaPowerPreferences();
        initAdditionData();
        initFastMode();
        initGeneral();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initCommonPreferences() {
        findPreference(getString(R.string.settings_key_scan_mode))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        preference.setSummary(
                                getResources().getStringArray(R.array.settings_scan_mode_entries)
                                        [Integer.parseInt((String) newValue)]);
                        return true;
                    }
                });
        findPreference(getString(R.string.settings_key_scan_mode)).setSummary(
                getResources().getStringArray(R.array.settings_scan_mode_entries)
                        [Integer.parseInt(SpUtils.getString(
                                getResources().getString(
                                        R.string.settings_key_scan_mode), "0"))]);
        findPreference(getString(R.string.settings_key_rfid_sound))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if ((boolean) newValue) {
                            mApp.rfidMgr.setReaderVolume(ReaderVolume.MEDIUM);
                        } else {
                            mApp.rfidMgr.setReaderVolume(ReaderVolume.MUTE);
                        }
                        return true;
                    }
                });
    }

    private ListPreference mAntennaReadPower = null;
    private ListPreference mAntennaWritePower = null;
    private void initAntennaPowerPreferences() {
        mAntennaReadPower = (ListPreference) findPreference(getString(R.string.settings_key_read_power));
        mAntennaWritePower = (ListPreference) findPreference(getString(R.string.settings_key_write_power));
        AntennaPower[] ap = new AntennaPower[0];
        try {
            ap = mApp.mRfidReader.getAntennaPower();
        } catch (RfidReaderException e) {
            e.printStackTrace();
            return;
        }
        if (null == ap || 0 >= ap.length) {
            return;
        }
        mAntennaReadPower.setValue(String.valueOf(ap[0].getReadPower()));
        mAntennaReadPower.setSummary(String.valueOf(ap[0].getReadPower()));
        mAntennaWritePower.setValue(String.valueOf(ap[0].getWritePower()));
        mAntennaWritePower.setSummary(String.valueOf(ap[0].getWritePower()));
        mAntennaReadPower.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String val = (String) newValue;
                        try {
                            AntennaPower[] ap = new AntennaPower[1];
                            for (int i = 0; i < 1; i++) {
                                ap[i] = new AntennaPower(i + 1, Integer.parseInt(val),
                                        Integer.parseInt(mAntennaWritePower.getValue()));
                            }
                            mApp.mRfidReader.setAntennaPower(ap);
                            preference.setSummary(val);
                        } catch (RfidReaderException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
        mAntennaWritePower.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String val = (String) newValue;
                        AntennaPower[] ap = new AntennaPower[1];
                        try {
                            for (int i = 0; i < 1; i++) {
                                ap[i] = new AntennaPower(i + 1,
                                        Integer.parseInt(mAntennaReadPower.getValue()),
                                        Integer.parseInt(val));
                            }
                            mApp.mRfidReader.setAntennaPower(ap);
                            preference.setSummary(val);
                        } catch (RfidReaderException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
    }

    private void initAdditionData() {
        Preference preference = findPreference(getString(R.string.settings_key_addition_data));
        preference.setSummary(SpUtils.getString(getString(R.string.settings_key_addition_data),
                TagAdditionData.NONE.getName()));
        preference.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                return true;
            }
        });
    }

    private final List<Chip> mChips = new ArrayList<>();
    private void initFastMode() {
        findPreference(getString(R.string.settings_key_pause_percentage))
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getResources().getStringArray(
                        R.array.settings_pause_percentage_entries)
                        [Integer.parseInt((String) newValue)] + "%");
                return true;
            }
        });
        MyPreference fastMode = (MyPreference) findPreference(getString(R.string.settings_key_fast_mode_params));
        fastMode.setOnBindViewListener(new MyPreference.OnBindViewListener() {
            @Override
            public void onBindViewCalled(PreferenceViewHolder view) {
                initView(view);
                loadSettings();
            }
        });

        String[] array = getResources().getStringArray(R.array.settings_pause_percentage_entryValues);
        String scenario = SpUtils.getString(
                getResources().getString(R.string.settings_key_pause_percentage), "0");
        for (int i = 0; i < array.length; ++i) {
            if (scenario.equals(array[i])) {
                String[] scenarios = getResources().getStringArray(
                        R.array.settings_pause_percentage_entries);
                findPreference(getString(R.string.settings_key_pause_percentage))
                        .setSummary(scenarios[i] + "%");
                break;
            }
        }
    }
    private void loadSettings() {
        for (int i = 0; i < mChips.size(); i++) {
            mChips.get(i).setChecked(
                    SpUtils.getBoolean((String) (mChips.get(i).getTag()), false));
        }
    }
    private void initView(PreferenceViewHolder view) {
        CompoundButton.OnCheckedChangeListener listener =
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String key = (String) buttonView.getTag();
                SpUtils.putBoolean(key, isChecked);
            }
        };
        Chip mCountChip = (Chip) view.findViewById(R.id.cp_count);
        mCountChip.setOnCheckedChangeListener(listener);
        Chip mRssiChip = (Chip) view.findViewById(R.id.cp_rssi);
        mRssiChip.setOnCheckedChangeListener(listener);
        Chip mAntChip = (Chip) view.findViewById(R.id.cp_ant);
        mAntChip.setOnCheckedChangeListener(listener);
        Chip mFreqChip = (Chip) view.findViewById(R.id.cp_frequency);
        mFreqChip.setOnCheckedChangeListener(listener);
        Chip mTimeChip = (Chip) view.findViewById(R.id.cp_time);
        mTimeChip.setOnCheckedChangeListener(listener);
        Chip mRfuChip = (Chip) view.findViewById(R.id.cp_rfu);
        mRfuChip.setOnCheckedChangeListener(listener);
        mRfuChip.setVisibility(View.GONE);
        Chip mProChip = (Chip) view.findViewById(R.id.cp_pro);
        mProChip.setOnCheckedChangeListener(listener);
        Chip mDataChip = (Chip) view.findViewById(R.id.cp_data);
        mDataChip.setOnCheckedChangeListener(listener);

        mCountChip.setTag(getString(R.string.fast_mode_key_count));
        mRssiChip.setTag(getString(R.string.fast_mode_key_rssi));
        mAntChip.setTag(getString(R.string.fast_mode_key_ant));
        mFreqChip.setTag(getString(R.string.fast_mode_key_freq));
        mTimeChip.setTag(getString(R.string.fast_mode_key_time));
        mRfuChip.setTag(getString(R.string.fast_mode_key_rfu));
        mProChip.setTag(getString(R.string.fast_mode_key_pro));
        mDataChip.setTag(getString(R.string.fast_mode_key_data));

        mChips.clear();
        mChips.add(mCountChip);
        mChips.add(mRssiChip);
        mChips.add(mAntChip);
        mChips.add(mFreqChip);
        mChips.add(mTimeChip);
        mChips.add(mRfuChip);
        mChips.add(mProChip);
        mChips.add(mDataChip);
    }

    private void initGeneral() {
        findPreference(getString(R.string.settings_key_temperature))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    preference.setSummary(mApp.mRfidReader.getTemperature() + " ℃");
                } catch (RfidReaderException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        findPreference(getString(R.string.settings_key_module_version))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    String hardwareVersion = mApp.mRfidReader.getHardwareVersion();
                    String softwareVersion = mApp.mRfidReader.getSoftwareVersion();
                    preference.setSummary(
                            String.format(getString(R.string.settings_version_result),
                                    hardwareVersion, softwareVersion));
                } catch (RfidReaderException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        findPreference(getString(R.string.settings_key_battery_level))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                preference.setSummary(mApp.rfidMgr.getBatteryLevel() + "%");
                return true;
            }
        });
        findPreference(getString(R.string.settings_key_battery_temperature))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                preference.setSummary(mApp.rfidMgr.getBatteryTemperature() + " ℃");
                return true;
            }
        });
        findPreference(getString(R.string.settings_key_ble_module_version))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String softwareVersion = mApp.rfidMgr.getBluetoothModuleSwVersion();
                String hardwareVersion = mApp.rfidMgr.getBluetoothModuleHwVersion();
                preference.setSummary(
                        String.format(getString(R.string.settings_version_result),
                                hardwareVersion, softwareVersion));
                return true;
            }
        });
    }
}

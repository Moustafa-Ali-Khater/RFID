package com.honeywell.rfidsimpleexample.fragments.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.honeywell.rfidservice.rfid.Region;
import com.honeywell.rfidservice.rfid.RfidReaderException;
import com.honeywell.rfidsimpleexample.MyApplication;
import com.honeywell.rfidsimpleexample.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsRegionFreqFragment extends Fragment {

    MyApplication mApp = MyApplication.getInstance();

    private boolean mEnableChangeFreq = false;
    private Region mRegion = Region.Unknown;

    private boolean mDoSaveFrequencies = false;

    private final FrequenciesAdapter mAdapter = new FrequenciesAdapter(this);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_region_frequency,
                container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        AppCompatSpinner spRegion = view.findViewById(R.id.sp_region);
        SwitchCompat allowChangeFreq = view.findViewById(R.id.sc_allow_change_frequency);

        String[] regions;
        if (mApp.mRfidReader.isHonReader()) {
            regions = getResources().getStringArray(R.array.settings_regions_hon);
        } else {
            regions = getResources().getStringArray(R.array.settings_regions);
        }
        ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_list_item_1, regions);
        spRegion.setAdapter(regionAdapter);

        try {
            Region region = mApp.mRfidReader.getRegion();
            mRegion = region;
            for (int i = 0; i < regions.length; ++i) {
                if (region.getName().equals(regions[i])) {
                    spRegion.setSelection(i);

                    updateFrequencies();
                    allowChangeFreq.setChecked(false);
                }
            }
        } catch (RfidReaderException e) {
            e.printStackTrace();
        }

        spRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean mFirstEnter = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mFirstEnter) {
                    mFirstEnter = false;
                    return;
                }
                String selected = parent.getSelectedItem().toString();
                Region region = Region.get(selected);
                if (Region.Unknown == region) {
                    Toast.makeText(getContext(), "Unknown region", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    mApp.mRfidReader.setRegion(region);
                    mRegion = region;
                    updateFrequencies();
                } catch (RfidReaderException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        allowChangeFreq.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEnableChangeFreq = isChecked;
            }
        });

        RecyclerView frequencies = view.findViewById(R.id.rv_frequencies);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        frequencies.setLayoutManager(layoutManager);
        frequencies.setAdapter(mAdapter);
    }

    private void updateFrequencies() {
        try {
            int[] freqHopTable = mApp.mRfidReader.getFreqHopTable();
            int[] entireFreqTable = mApp.mRfidReader.getEntireFreqHopTable(mRegion);
            if (null != entireFreqTable) {
                Arrays.sort(entireFreqTable);
            }
            List<FrequencyInfo> frequencyInfos = new ArrayList<>();
            for (int value : entireFreqTable) {
                FrequencyInfo info = new FrequencyInfo();
                info.check = false;
                for (int v : freqHopTable) {
                    if (v == value) {
                        info.check = true;
                        break;
                    }
                }
                info.frequency = value;
                frequencyInfos.add(info);
            }
            mAdapter.setFrequencies(frequencyInfos);
        } catch (RfidReaderException e) {
            e.printStackTrace();
        }
    }

    static class FrequencyInfo {
        int frequency;
        boolean check;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEnableChangeFreq &&
                mDoSaveFrequencies &&
                mApp.checkIsRFIDReady()) {
            mDoSaveFrequencies = false;
            List<FrequencyInfo> frequencyInfoList = mAdapter.getFrequencies();
            List<Integer> frequencies = new ArrayList<>();
            for (FrequencyInfo frequencyInfo : frequencyInfoList) {
                if (frequencyInfo.check) {
                    frequencies.add(frequencyInfo.frequency);
                }
            }
            try {
                mApp.mRfidReader.setFreqHopTable(frequencies);
            } catch (RfidReaderException e) {
                e.printStackTrace();
            }
        }
    }

    static class FrequenciesAdapter extends RecyclerView.Adapter<FrequenciesAdapter.ViewHolder> {

        private final WeakReference<SettingsRegionFreqFragment> mRef;
        private final List<FrequencyInfo> mFrequencies = new ArrayList<>();

        public FrequenciesAdapter(SettingsRegionFreqFragment fragment) {
            mRef = new WeakReference<>(fragment);
        }

        public void setFrequencies(List<FrequencyInfo> frequencies) {
            mFrequencies.clear();
            mFrequencies.addAll(frequencies);
            notifyDataSetChanged();
        }

        public List<FrequencyInfo> getFrequencies() {
            return mFrequencies;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(
                    parent.getContext()).inflate(
                            R.layout.recycler_region_frequency, parent, false);
            return new ViewHolder(view, mRef.get());
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position >= mFrequencies.size()) {
                holder.getCheck().setChecked(false);
                holder.getFrequency().setText("Unknown");
                return;
            }
            holder.setFrequencyInfo(mFrequencies.get(position));
            holder.getCheck().setChecked(mFrequencies.get(position).check);
            holder.getFrequency().setText(String.valueOf(mFrequencies.get(position).frequency));
        }

        @Override
        public int getItemCount() {
            return mFrequencies.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final WeakReference<SettingsRegionFreqFragment> mRef;
            private FrequencyInfo mFrequencyInfo;
            private final AppCompatCheckBox mCheck;
            private final AppCompatTextView mFrequency;
            public ViewHolder(@NonNull View itemView, SettingsRegionFreqFragment fragment) {
                super(itemView);
                mRef = new WeakReference<>(fragment);
                mCheck = itemView.findViewById(R.id.cb_check);
                mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mFrequencyInfo.check = isChecked;
                        SettingsRegionFreqFragment freqFragment = mRef.get();
                        if (null != freqFragment) {
                            freqFragment.mDoSaveFrequencies = true;
                        }
                    }
                });
                mFrequency = itemView.findViewById(R.id.tv_frequency);
            }
            public AppCompatCheckBox getCheck() {
                return mCheck;
            }
            public AppCompatTextView getFrequency() {
                return mFrequency;
            }
            public void setFrequencyInfo(FrequencyInfo info) {
                mFrequencyInfo = info;
            }
        }
    }
}

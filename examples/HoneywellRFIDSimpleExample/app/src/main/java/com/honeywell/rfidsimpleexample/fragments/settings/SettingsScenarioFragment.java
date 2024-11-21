package com.honeywell.rfidsimpleexample.fragments.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.honeywell.rfidsimpleexample.MyApplication;
import com.honeywell.rfidsimpleexample.R;
import com.honeywell.rfidsimpleexample.common.SpKeys;
import com.honeywell.rfidsimpleexample.rfid.WorkMode;
import com.honeywell.rfidsimpleexample.utils.SpUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsScenarioFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    MyApplication mApp = MyApplication.getInstance();
    private ListPreference mScenarioListPre;
    private EditTextPreference mTargetInterval;
    private Preference mEditPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_settings_scenario, rootKey);
        initScenario();
        updatePreference();
    }

    private void initScenario() {
        mEditPreference = findPreference(getString(R.string.settings_key_scenario_custom));
        mEditPreference.setVisible(
                WorkMode.getInstance().getCurrentScenario() == WorkMode.MODE_CUSTOM);
        mEditPreference.setOnPreferenceClickListener(preference -> {
            showScenarioEditDialog();
            return true;
        });
        mScenarioListPre = findPreference(SpKeys.KEY_SCENARIO);
        if (null == mScenarioListPre.getEntry()) {
            mScenarioListPre.setValue(String.valueOf(WorkMode.getInstance().getCurrentScenario()));
        }
        mScenarioListPre.setSummary(buildSummary());
        mScenarioListPre.setOnPreferenceChangeListener(this);

        mTargetInterval = findPreference(getString(R.string.settings_key_scenario_target_interval));
        mTargetInterval.setDialogLayoutResource(R.layout.dialog_edit);
        mTargetInterval.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTargetInterval) {
            String value = (String) newValue;
            try {
                if (value.length() > 6 || Integer.parseInt((String) newValue) <= 0) {
                    Toast.makeText(getContext(),
                            getString(R.string.settings_scenario_invalid_value),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                Toast.makeText(getContext(),
                        getString(R.string.settings_scenario_invalid_value),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (preference == mScenarioListPre) {
            String val = (String) newValue;
            WorkMode.getInstance().enableMode(Integer.parseInt(val), mApp.mRfidReader);
            ((ListPreference) preference).setValue(val);
            updatePreference();
            return true;
        }
        return true;
    }

    private String buildSummary() {
        return mScenarioListPre.getEntry() +
                "(" +
                WorkMode.getInstance().getModeDescription(
                        WorkMode.getInstance().getCurrentScenario(),
                        mApp.mRfidReader)
                + ")";
    }

    private void updatePreference() {
        mEditPreference.setVisible(WorkMode.getInstance()
                .getCurrentScenario() == WorkMode.MODE_CUSTOM);
        mTargetInterval.setVisible(WorkMode.getInstance()
                .getCurrentScenario() == WorkMode.MODE_MASS_TAG_MULTI_INVENTORY);
        mScenarioListPre.setSummary(buildSummary());
    }

    private void showScenarioEditDialog() {
        new ScenarioEditDialog().show();
    }

    private class ScenarioEditDialog {
        Spinner mSPSession;
        Spinner mSPProfile;
        Spinner mSPTarget;
        Spinner mSPQ;

        AlertDialog mDialog;

        protected ScenarioEditDialog() {
            init();
        }

        private <T> ArrayAdapter<T> createAdapter(T[] values) {
            ArrayAdapter<T> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, values);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            return adapter;
        }

        private void init() {
            View editView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_scenario_custom_edit, null);
            mSPSession = editView.findViewById(R.id.sp_session);
            mSPProfile = editView.findViewById(R.id.sp_profile);
            mSPTarget = editView.findViewById(R.id.sp_target);
            mSPQ = editView.findViewById(R.id.sp_qvalue);

            final JSONObject customObject = WorkMode.getInstance().getMode(
                    WorkMode.MODE_CUSTOM, mApp.mRfidReader);

            ArrayAdapter<String> session_adapter = createAdapter(
                    WorkMode.getInstance().getSessions(mApp.mRfidReader));
            mSPSession.setAdapter(session_adapter);
            ArrayAdapter<String> target_adapter = createAdapter(
                    WorkMode.getInstance().getTargets(mApp.mRfidReader));
            mSPTarget.setAdapter(target_adapter);
            ArrayAdapter<String> profile_adapter = createAdapter(
                    WorkMode.getInstance().getProfiles(mApp.mRfidReader));
            mSPProfile.setAdapter(profile_adapter);
            ArrayAdapter<String> qval_adapter = createAdapter(
                    WorkMode.getInstance().getQValues(mApp.mRfidReader));
            mSPQ.setAdapter(qval_adapter);

            try {
                int sessionId = customObject.getInt(WorkMode.KEY_SESSION);
                mSPSession.setSelection(sessionId);
                int profileId = customObject.getInt(WorkMode.KEY_PROFILEID);
                if (!mApp.checkIsRFIDReady() &&
                        profileId > WorkMode.getInstance().getProfiles(mApp.mRfidReader).length) {
                    profileId = WorkMode.getInstance().getProfiles(mApp.mRfidReader).length;
                }
                mSPProfile.setSelection(profileId - 1);
                int targetId = customObject.getInt(WorkMode.KEY_TARGET);
                mSPTarget.setSelection(targetId);
                int qValueId = customObject.getInt(WorkMode.KEY_QVALUE);
                mSPQ.setSelection(qValueId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            mDialog = builder.setView(editView).setPositiveButton(R.string.ok, (dialog, which) -> {
                try {
                    customObject.put(WorkMode.KEY_SESSION, mSPSession.getSelectedItemPosition());
                    customObject.put(WorkMode.KEY_TARGET, mSPTarget.getSelectedItemPosition());
                    customObject.put(WorkMode.KEY_QVALUE, mSPQ.getSelectedItemPosition());
                    // profile value is 1, 2, 3, 4，position index is 0, 1, 2, 3, so profile = position + 1
                    customObject.put(WorkMode.KEY_PROFILEID, mSPProfile.getSelectedItemPosition() + 1);
                    WorkMode.getInstance().saveCustomMode(mApp.mRfidReader);
                    updatePreference();
                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }).setTitle(R.string.settings_scenario_custom_edit).create();
        }

        void show() {
            mDialog.show();
        }
    }
}

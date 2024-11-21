package com.honeywell.rfidsimpleexample.rfid;

import android.util.Log;

import androidx.annotation.NonNull;

import com.honeywell.rfidsimpleexample.common.SpKeys;
import com.honeywell.rfidsimpleexample.utils.SpUtils;
import com.honeywell.rfidservice.rfid.Gen2;
import com.honeywell.rfidservice.rfid.Region;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.RfidReaderException;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class WorkMode {
    private static final String PRE_CUSTOM_SCENARIO = "custome_scenario";
    private static final String TAG = "WorkMode";

    public static final int MODE_ANTI_INTERENCE = 1001;
    public static final int MODE_MULTI_TAG_INVENTORY = 1002;
    public static final int MODE_MASS_TAG_SINGLE_INVENTORY = 1003;
    public static final int MODE_MASS_TAG_MULTI_INVENTORY = 1004;
    public static final int MODE_SINGLE_FAST = 1005;
    public static final int MODE_MULTI_FAST = 1006;
    public static final int MODE_CUSTOM = 1007;

    public static final String KEY_PROFILEID = "key_profileid";
    public static final String KEY_SESSION = "key_session";
    public static final String KEY_QVALUE = "key_qvalue";
    public static final String KEY_TARGET = "key_target";
    public static final String KEY_IS_HON_READER = "key_is_hon_reader";

    public static final int Q_AUTO = 0;

    private static WorkMode sInstance;
    private int mCurrentScenario = MODE_ANTI_INTERENCE;

    private final ScenarioCommon mScenarioSilion = new ScenarioSilion(this);
    private final ScenarioCommon mScenarioHon = new ScenarioHon(this);

    private Region mRegion = null;

    public static synchronized WorkMode getInstance() {
        if (null == sInstance) {
            sInstance = new WorkMode();
        }
        return sInstance;
    }

    private WorkMode() {
        init();
    }

    private void init() {
        mCurrentScenario = Integer.parseInt(SpUtils.getString(SpKeys.KEY_SCENARIO,
                "" + MODE_ANTI_INTERENCE));
    }

    public boolean enableMode(int mode, RfidReader rfidReader) {
        if (null == mRegion && isHonReader(rfidReader)) {
            return updateRegion(mode, rfidReader);
        }
        JSONObject modeObj = getModeInternal(mode, rfidReader);
        Log.i(TAG, "enableMode Mode =" + modeObj.toString() + " mode=" + mode);
        mCurrentScenario = mode;
        try {
            if (null != rfidReader) {
                Gen2.Session s = Gen2.Session.Session0;
                int sid = modeObj.getInt(KEY_SESSION);
                switch (sid) {
                    case 0:
                        s = Gen2.Session.Session0;
                        break;
                    case 1:
                        s = Gen2.Session.Session1;
                        break;
                    case 2:
                        s = Gen2.Session.Session2;
                        break;
                    case 3:
                        s = Gen2.Session.Session3;
                        break;
                }
                return rfidReader.setWorkMode(s, modeObj.getInt(KEY_PROFILEID),
                        modeObj.getInt(KEY_TARGET),
                        modeObj.getInt(KEY_QVALUE) - 1);  // Auto value is -1, minus 1
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private ScenarioCommon getScenarioImpl(RfidReader rfidReader) {
        return isHonReader(rfidReader) ? mScenarioHon : mScenarioSilion;
    }

    private ScenarioCommon getScenarioImpl(boolean isHonReader) {
        return isHonReader ? mScenarioHon : mScenarioSilion;
    }

    private JSONObject getModeInternal(int id, RfidReader rfidReader) {
        return getScenarioImpl(rfidReader).getScenarioParams(id).getJsonInternal();
    }

    public JSONObject getMode(int id, RfidReader rfidReader) {
        return getScenarioImpl(rfidReader).getScenarioParams(id).getJson();
    }

    public String getModeDescription(int id, RfidReader rfidReader) {
        return getScenarioImpl(rfidReader).getScenarioParams(id).toString();
    }

    public int getCurrentScenario() {
        return mCurrentScenario;
    }

    public boolean saveCustomMode(RfidReader reader) {
        JSONObject jsonObject = getModeInternal(MODE_CUSTOM, reader);
        if (null != jsonObject) {
            try {
                jsonObject.put(KEY_IS_HON_READER, isHonReader(reader));
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            boolean doSave = true;
            if (mCurrentScenario == MODE_CUSTOM && null != reader) {
                if (!enableMode(mCurrentScenario, reader)) {
                    doSave = false;
                }
            }
            if (doSave) {
                SpUtils.putString(PRE_CUSTOM_SCENARIO, jsonObject.toString());
                getScenarioImpl(reader).updateCustomMode();
            }
            return doSave;
        }
        return false;
    }

    private JSONObject readCustomMode(boolean isHonReader) {
        String json = SpUtils.getString(PRE_CUSTOM_SCENARIO, null);
        if (null != json && json.length() > 0) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
            try {
                boolean isHonReaderSaved = jsonObject.getBoolean(KEY_IS_HON_READER);
                if (isHonReader == isHonReaderSaved) {
                    return jsonObject;
                }
            } catch (JSONException e) {
                // Does not contains the key, maybe upgrade the app from
                // the old version which is not support HonReader.
                // It is normal, so not print the error message.
            }
        }
        // Not set before, or connect a different module, regarded as anti-interference.
        // Get json for user to prevent the json object is modified in some case.
        return getScenarioImpl(isHonReader).getScenarioParamsAntiInterference().getJson();
    }

    private boolean isHonReader(RfidReader reader) {
        return null != reader && reader.available() && reader.isHonReader();
    }

    public String[] getSessions(@NonNull RfidReader reader) {
        return getScenarioImpl(reader).getSessions();
    }

    public String[] getProfiles(@NonNull RfidReader reader) {
        return getScenarioImpl(reader).getProfiles();
    }

    public String[] getTargets(@NonNull RfidReader reader) {
        return getScenarioImpl(reader).getTargets();
    }

    public String[] getQValues(@NonNull RfidReader reader) {
        return getScenarioImpl(reader).getQValues();
    }

    private boolean updateRegion(int mode, RfidReader reader) {
        if (!isHonReader(reader)) {  // Silion module doesn't care about the region
            return true;
        }
        try {
            return updateRegion(mode, reader.getRegion(), reader);
        } catch (RfidReaderException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateRegion(int mode, Region region, RfidReader reader) {
        if (!isHonReader(reader)) {  // Silion module doesn't care about the region
            return true;
        }
        if (region != mRegion) {
            mRegion = region;
            return enableMode(mode, reader);
        }
        return true;
    }

    public boolean updateRegion(Region region, RfidReader reader) {
        return updateRegion(mCurrentScenario, region, reader);
    }

    private interface IToStrings {
        String session2String(int session);
        String profile2String(int profile);
        String target2String(int target);
        String qValue2String(int qValue);
    }

    public static abstract class ScenarioParams {

        private final IToStrings mToStrings;

        private ScenarioParams(IToStrings toStrings) {
            mToStrings = toStrings;
        }

        public abstract int getSession();
        public abstract int getProfile();
        public abstract int getTarget();
        public abstract int getQValue();

        public abstract boolean setSession(int session);
        public abstract boolean setProfile(int profile);
        public abstract boolean setTarget(int target);
        public abstract boolean setQValue(int qValue);

        public abstract JSONObject getJson();
        protected abstract JSONObject getJsonInternal();

        @Override
        public String toString() {
            if (null != mToStrings) {
                return "Session: " + mToStrings.session2String(getSession()) +
                        " Profile: " + mToStrings.profile2String(getProfile()) +
                        " Target: " + mToStrings.target2String(getTarget()) +
                        " Q: " + mToStrings.qValue2String(getQValue());
            } else {
                return "Session: " + getSession() +
                        " Profile: " + getProfile() +
                        " Target: " + getTarget() +
                        " Q: " + getQValue();
            }
        }
    }

    private static class ScenarioParamsMutable extends ScenarioParams {
        int session;
        int profile;
        int target;
        int qValue;

        private final JSONObject mJsonObj;

        ScenarioParamsMutable(int session, int profile, int target, int qValue,
                              IToStrings toStrings) {
            super(toStrings);
            this.session = session;
            this.profile = profile;
            this.target = target;
            this.qValue = qValue;
            mJsonObj = new JSONObject();
            try {
                mJsonObj.put(KEY_SESSION, session);
                mJsonObj.put(KEY_PROFILEID, profile);
                mJsonObj.put(KEY_TARGET, target);
                mJsonObj.put(KEY_QVALUE, qValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getSession() {
            return session;
        }
        @Override
        public int getProfile() {
            return profile;
        }
        @Override
        public int getTarget() {
            return target;
        }
        @Override
        public int getQValue() {
            return qValue;
        }

        @Override
        public boolean setSession(int session) {
            try {
                mJsonObj.put(KEY_SESSION, session);
                this.session = session;
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        public boolean setProfile(int profile) {
            try {
                mJsonObj.put(KEY_PROFILEID, profile);
                this.profile = profile;
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        public boolean setTarget(int target) {
            try {
                mJsonObj.put(KEY_TARGET, target);
                this.target = target;
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
        @Override
        public boolean setQValue(int qValue) {
            try {
                mJsonObj.put(KEY_QVALUE, qValue);
                this.qValue = qValue;
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public JSONObject getJson() {
            return mJsonObj;
        }

        @Override
        public JSONObject getJsonInternal() {
            return mJsonObj;
        }
    }

    private static class ScenarioParamsImmutable extends ScenarioParams {
        final int session;
        final int profile;
        final int target;
        final int qValue;

        private final JSONObject mJsonObj;
        private final JSONObject mJsonObjForUser;

        ScenarioParamsImmutable(int session, int profile, int target, int qValue,
                                IToStrings toStrings) {
            super(toStrings);
            this.session = session;
            this.profile = profile;
            this.target = target;
            this.qValue = qValue;
            mJsonObj = new JSONObject();
            mJsonObjForUser = new JSONObject();
            try {
                mJsonObj.put(KEY_SESSION, session);
                mJsonObj.put(KEY_PROFILEID, profile);
                mJsonObj.put(KEY_TARGET, target);
                mJsonObj.put(KEY_QVALUE, qValue);

                mJsonObjForUser.put(KEY_SESSION, session);
                mJsonObjForUser.put(KEY_PROFILEID, profile);
                mJsonObjForUser.put(KEY_TARGET, target);
                mJsonObjForUser.put(KEY_QVALUE, qValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getSession() {
            return session;
        }
        @Override
        public int getProfile() {
            return profile;
        }
        @Override
        public int getTarget() {
            return target;
        }
        @Override
        public int getQValue() {
            return qValue;
        }

        @Override
        public boolean setSession(int session) {
            return false;
        }
        @Override
        public boolean setProfile(int profile) {
            return false;
        }
        @Override
        public boolean setTarget(int target) {
            return false;
        }
        @Override
        public boolean setQValue(int qValue) {
            return false;
        }

        @Override
        public JSONObject getJson() {
            return mJsonObjForUser;
        }

        @Override
        public JSONObject getJsonInternal() {
            return mJsonObj;
        }
    }

    private static abstract class ScenarioCommon implements IToStrings {

        private final WeakReference<WorkMode> mRef;
        private ScenarioParams CUSTOM = null;
        private final boolean mIsHonReader;

        ScenarioCommon(WorkMode workMode, boolean isHonReader) {
            mRef = new WeakReference<>(workMode);
            mIsHonReader = isHonReader;
        }

        protected WorkMode getWorkMode() {
            return mRef.get();
        }

        public abstract String[] getSessions();
        public abstract String[] getProfiles();
        public abstract String[] getTargets();
        public abstract String[] getQValues();

        public abstract ScenarioParams getScenarioParamsAntiInterference();
        public abstract ScenarioParams getScenarioParamsMultiTagsInventory();
        public abstract ScenarioParams getScenarioParamsMassTagsSingleInventory();
        public abstract ScenarioParams getScenarioParamsMassTagsMultiInventory();
        public abstract ScenarioParams getScenarioParamsSingleTagFast();
        public abstract ScenarioParams getScenarioParamsMultiTagsFast();

        public ScenarioParams getScenarioParamsCustom() {
            if (null == CUSTOM) {
                if (!updateCustomMode()) {
                    return null;
                }
            }
            return CUSTOM;
        }

        public boolean updateCustomMode() {
            WorkMode workMode = getWorkMode();
            if (null == workMode) {
                return false;
            }
            final JSONObject customObject = workMode.readCustomMode(mIsHonReader);
            if (null != customObject) {
                try {
                    int sessionId = customObject.getInt(WorkMode.KEY_SESSION);
                    int profileId = customObject.getInt(WorkMode.KEY_PROFILEID);
                    int targetId = customObject.getInt(WorkMode.KEY_TARGET);
                    int qValueId = customObject.getInt(WorkMode.KEY_QVALUE);
                    CUSTOM = new ScenarioParamsMutable(
                            sessionId, profileId, targetId, qValueId, this);
                    return true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        public ScenarioParams getScenarioParams(int mode) {
            ScenarioParams scenarioParams;
            switch (mode) {
                case MODE_ANTI_INTERENCE:
                    scenarioParams = getScenarioParamsAntiInterference();
                    break;
                case MODE_MULTI_TAG_INVENTORY:
                    scenarioParams = getScenarioParamsMultiTagsInventory();
                    break;
                case MODE_MASS_TAG_SINGLE_INVENTORY:
                    scenarioParams = getScenarioParamsMassTagsSingleInventory();
                    break;
                case MODE_MASS_TAG_MULTI_INVENTORY:
                    scenarioParams = getScenarioParamsMassTagsMultiInventory();
                    break;
                case MODE_SINGLE_FAST:
                    scenarioParams = getScenarioParamsSingleTagFast();
                    break;
                case MODE_MULTI_FAST:
                    scenarioParams = getScenarioParamsMultiTagsFast();
                    break;
                case MODE_CUSTOM:
                default:
                    scenarioParams = getScenarioParamsCustom();
                    break;
            }
            return scenarioParams;
        }
    }

    private static class ScenarioSilion extends ScenarioCommon {

        private final String[] SESSIONS = {"S0", "S1", "S2", "S3"};
        private final String[] PROFILES = {"Prof1", "Prof2", "Prof3", "Prof4"};
        private final String[] TARGETS = {"A", "B", "A->B", "B->A"};
        private final String[] QS = {
                "Auto", "0", "1", "2", "3", "4", "5", "6", "7",
                "8", "9", "10", "11", "12", "13", "14", "15"
        };

        private final ScenarioParams ANTI_INTERFERENCE = new ScenarioParamsImmutable(0, 1, 3, Q_AUTO, this);
        private final ScenarioParams MULTI_TAGS_INVENTORY = new ScenarioParamsImmutable(1, 1, 0, Q_AUTO, this);
        private final ScenarioParams MASS_TAGS_SINGLE_INVENTORY = new ScenarioParamsImmutable(2, 1, 0, Q_AUTO, this);
        private final ScenarioParams MASS_TAGS_MULTI_INVENTORY = new ScenarioParamsImmutable(2, 1, 0, Q_AUTO, this);
        private final ScenarioParams SINGLE_TAG_FAST = new ScenarioParamsImmutable(0, 3, 2, 1, this);
        private final ScenarioParams MULTI_TAGS_FAST = new ScenarioParamsImmutable(0, 3, 2, Q_AUTO, this);

        ScenarioSilion(WorkMode workMode) {
            super(workMode, false);
        }

        @Override
        public String[] getSessions() {
            return SESSIONS;
        }

        @Override
        public String[] getProfiles() {
            return PROFILES;
        }

        @Override
        public String[] getTargets() {
            return TARGETS;
        }

        @Override
        public String[] getQValues() {
            return QS;
        }

        @Override
        public ScenarioParams getScenarioParamsAntiInterference() {
            return ANTI_INTERFERENCE;
        }

        @Override
        public ScenarioParams getScenarioParamsMultiTagsInventory() {
            return MULTI_TAGS_INVENTORY;
        }

        @Override
        public ScenarioParams getScenarioParamsMassTagsSingleInventory() {
            return MASS_TAGS_SINGLE_INVENTORY;
        }

        @Override
        public ScenarioParams getScenarioParamsMassTagsMultiInventory() {
            return MASS_TAGS_MULTI_INVENTORY;
        }

        @Override
        public ScenarioParams getScenarioParamsSingleTagFast() {
            return SINGLE_TAG_FAST;
        }

        @Override
        public ScenarioParams getScenarioParamsMultiTagsFast() {
            return MULTI_TAGS_FAST;
        }

        @Override
        public String session2String(int session) {
            if (session >= 0 && session < SESSIONS.length) {
                return SESSIONS[session];
            }
            return "Invalid<" + session + ">";
        }

        @Override
        public String profile2String(int profile) {
            if (profile > 0 && profile <= PROFILES.length) {
                return PROFILES[profile - 1];
            }
            return "Invalid<" + profile + ">";
        }

        @Override
        public String target2String(int target) {
            if (target >= 0 && target < TARGETS.length) {
                return TARGETS[target];
            }
            return "Invalid<" + target + ">";
        }

        @Override
        public String qValue2String(int qValue) {
            if (qValue >= 0 && qValue < QS.length) {
                return QS[qValue];
            }
            return "Invalid<" + qValue + ">";
        }
    }

    private static class ScenarioHon extends ScenarioCommon {

        private final String[] SESSIONS = {"S0", "S1", "S2", "S3"};
        private final String[] PROFILES = {
                "Prof1", "Prof3", "Prof5", "Prof7",
                "Prof11", "Prof12", "Prof13", "Prof15"
        };
        private final String[] TARGETS = {"A", "B", "A<->B"};
        private final String[] QS = {
                "Auto", "0", "1", "2", "3", "4", "5", "6", "7",
                "8", "9", "10", "11", "12", "13", "14", "15"
        };

        private final ScenarioParams ANTI_INTERFERENCE_FOR_NA = new ScenarioParamsImmutable(0, 4, 2, Q_AUTO, this);
        private final ScenarioParams MULTI_TAGS_INVENTORY_FOR_NA = new ScenarioParamsImmutable(1, 4, 0, Q_AUTO, this);
        private final ScenarioParams MASS_TAGS_SINGLE_INVENTORY_FOR_NA = new ScenarioParamsImmutable(2, 4, 0, Q_AUTO, this);
        private final ScenarioParams MASS_TAGS_MULTI_INVENTORY_FOR_NA = new ScenarioParamsImmutable(2, 4, 0, Q_AUTO, this);
        private final ScenarioParams SINGLE_TAG_FAST = new ScenarioParamsImmutable(0, 5, 2, 1, this);
        private final ScenarioParams MULTI_TAGS_FAST = new ScenarioParamsImmutable(0, 5, 2, Q_AUTO, this);

        private final ScenarioParams ANTI_INTERFERENCE = new ScenarioParamsImmutable(0, 3, 2, Q_AUTO, this);
        private final ScenarioParams MULTI_TAGS_INVENTORY = new ScenarioParamsImmutable(1, 3, 0, Q_AUTO, this);
        private final ScenarioParams MASS_TAGS_SINGLE_INVENTORY = new ScenarioParamsImmutable(2, 3, 0, Q_AUTO, this);
        private final ScenarioParams MASS_TAGS_MULTI_INVENTORY = new ScenarioParamsImmutable(2, 3, 0, Q_AUTO, this);

        ScenarioHon(WorkMode workMode) {
            super(workMode, true);
        }

        @Override
        public String[] getSessions() {
            return SESSIONS;
        }

        @Override
        public String[] getProfiles() {
            return PROFILES;
        }

        @Override
        public String[] getTargets() {
            return TARGETS;
        }

        @Override
        public String[] getQValues() {
            return QS;
        }

        private boolean isNARegion() {
            WorkMode workMode = getWorkMode();
            return null != workMode && Region.NA == workMode.mRegion;
        }

        @Override
        public ScenarioParams getScenarioParamsAntiInterference() {
            return isNARegion() ? ANTI_INTERFERENCE_FOR_NA : ANTI_INTERFERENCE;
        }

        @Override
        public ScenarioParams getScenarioParamsMultiTagsInventory() {
            return isNARegion() ? MULTI_TAGS_INVENTORY_FOR_NA : MULTI_TAGS_INVENTORY;
        }

        @Override
        public ScenarioParams getScenarioParamsMassTagsSingleInventory() {
            return isNARegion() ? MASS_TAGS_SINGLE_INVENTORY_FOR_NA : MASS_TAGS_SINGLE_INVENTORY;
        }

        @Override
        public ScenarioParams getScenarioParamsMassTagsMultiInventory() {
            return isNARegion() ? MASS_TAGS_MULTI_INVENTORY_FOR_NA : MASS_TAGS_MULTI_INVENTORY;
        }

        @Override
        public ScenarioParams getScenarioParamsSingleTagFast() {
            return SINGLE_TAG_FAST;
        }

        @Override
        public ScenarioParams getScenarioParamsMultiTagsFast() {
            return MULTI_TAGS_FAST;
        }

        @Override
        public String session2String(int session) {
            if (session >= 0 && session < SESSIONS.length) {
                return SESSIONS[session];
            }
            return "Invalid<" + session + ">";
        }

        @Override
        public String profile2String(int profile) {
            if (profile > 0 && profile <= PROFILES.length) {
                return PROFILES[profile - 1];
            }
            return "Invalid<" + profile + ">";
        }

        @Override
        public String target2String(int target) {
            if (target >= 0 && target < TARGETS.length) {
                return TARGETS[target];
            }
            return "Invalid<" + target + ">";
        }

        @Override
        public String qValue2String(int qValue) {
            if (qValue >= 0 && qValue < QS.length) {
                return QS[qValue];
            }
            return "Invalid<" + qValue + ">";
        }
    }
}

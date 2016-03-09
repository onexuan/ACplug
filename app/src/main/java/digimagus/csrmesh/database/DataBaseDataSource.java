package digimagus.csrmesh.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digimagus.csrmesh.entities.DeviceInfo;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.HideDeviceInfo;
import digimagus.csrmesh.entities.PhoneInfo;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.entities.Setting;

/**
 * 数据库管理类
 */
public class DataBaseDataSource {
    // Log Tag
    private String TAG = "DataBaseDataSource";

    // Database fields
    private SQLiteDatabase db;
    private ACSQLHelper dbHelper;

    public DataBaseDataSource(Context context) {
        dbHelper = new ACSQLHelper(context);
    }

    /**
     * Create a setting entry or update if it already exists.
     *
     * @param setting
     * @return
     */
    public Setting createSetting(Setting setting) {
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.SETTINGS_COLUMN_ID, setting.getId());
        values.put(ACSQLHelper.SETTINGS_COLUMN_LOCATION, setting.getLocate());
        values.put(ACSQLHelper.SETTINGS_COLUMN_COUNTRYNO, setting.getCountryNO());
        values.put(ACSQLHelper.SETTINGS_COLUMN_PRICE, setting.getPrice());
        values.put(ACSQLHelper.SETTINGS_COLUMN_UNIT, setting.getUnit());
        values.put(ACSQLHelper.SETTINGS_COLUMN_PRIVACY, setting.isPrivacy());
        values.put(ACSQLHelper.SETTINGS_COLUMN_CITY,setting.getCity());
        long id = db.replace(ACSQLHelper.TABLE_SETTINGS, null, values);
        close();
        if (id == -1) {
            return null;
        } else {
            return setting;
        }
    }

    public ScheduleInfo createGroupSchedule(ScheduleInfo schedule, int groupId) {
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.SCHEDULE_COLUMN_GROUPID, groupId);
        values.put(ACSQLHelper.SCHEDULE_COLUMN_START_TIME, String.valueOf(schedule.start_h + ":" + schedule.start_m));
        values.put(ACSQLHelper.SCHEDULE_COLUMN_START_WEEK, schedule.start_w);
        values.put(ACSQLHelper.SCHEDULE_COLUMN_END_TIME, String.valueOf(schedule.end_h + ":" + schedule.end_m));
        values.put(ACSQLHelper.SCHEDULE_COLUMN_REPEAT, schedule.repeat);
        values.put(ACSQLHelper.SCHEDULE_COLUMN_START_SENABLE, schedule.start_s);
        values.put(ACSQLHelper.SCHEDULE_COLUMN_START_EENABLE, schedule.end_s);
        long id = db.insert(ACSQLHelper.TABLE_SCHEDULE, null, values);
        Log.e(TAG, "createGroupSchedule  --->   " + id + "    groupId:" + groupId);
        close();
        if (id == -1) {
            return null;
        } else {
            schedule.id= (int) id;
            return schedule;
        }
    }

    public List<ScheduleInfo> getGroupSchedule(Integer groupId) {
        open();
        List<ScheduleInfo> scheduleInfos = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + ACSQLHelper.TABLE_SCHEDULE + " WHERE " + ACSQLHelper.SCHEDULE_COLUMN_GROUPID + " = " + groupId;
        Cursor c = db.rawQuery(selectQuery, null);
        while (c.moveToNext()) {
            ScheduleInfo schedule = new ScheduleInfo();
            schedule.id = c.getInt(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_ID));
            schedule.start_w = c.getInt(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_START_WEEK));
            String[] start_t = c.getString(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_START_TIME)).split(":");
            schedule.start_h = Integer.parseInt(start_t[0]);
            schedule.start_m = Integer.parseInt(start_t[1]);
            String[] end_t = c.getString(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_END_TIME)).split(":");
            schedule.end_h = Integer.parseInt(end_t[0]);
            schedule.end_m = Integer.parseInt(end_t[1]);
            schedule.repeat = c.getInt(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_REPEAT));
            schedule.start_s = c.getInt(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_START_SENABLE));
            schedule.end_s = c.getInt(c.getColumnIndex(ACSQLHelper.SCHEDULE_COLUMN_START_EENABLE));
            scheduleInfos.add(schedule);
        }
        close();
        return scheduleInfos;
    }

    /**
     * Get single setting entry by id.
     *
     * @return
     */
    public Setting getSetting() {
        String selectQuery = "SELECT  * FROM " + ACSQLHelper.TABLE_SETTINGS + " WHERE " + ACSQLHelper.SETTINGS_COLUMN_ID + " = ?";
        open();
        Cursor c = db.rawQuery(selectQuery, new String[]{1 + ""});
        if (c != null && c.moveToNext()) {
            Setting setting = new Setting();
            setting.setId(c.getInt(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_ID)));
            setting.setLocate(c.getString(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_LOCATION)));
            setting.setCountryNO(c.getString(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_COUNTRYNO)));
            setting.setPrice(c.getDouble(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_PRICE)));
            setting.setUnit(c.getString(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_UNIT)));
            setting.setPrivacy(c.getInt(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_PRIVACY)) == 1 ? true : false);
            setting.setCity(c.getString(c.getColumnIndex(ACSQLHelper.SETTINGS_COLUMN_CITY)));
            c.close();
            close();
            Log.e(TAG, "QUERY ： 查询 SETTING 表信息" + setting.getId());
            return setting;
        } else {
            Log.e(TAG, "QUERY ： 没有查询到SETTING表信息");
            close();
            return null;
        }
    }

    /**
     * Create a Device entry or update if it already exists.
     *
     * @param device
     * @return
     */
    public DeviceInfo createDevice(String type,DeviceInfo device) {
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.DEVICE_COLUMN_LOCATION, device.getLocation());
        values.put(ACSQLHelper.DEVICE_COLUMN_COUNTRYNO, device.getCountryNO());
        values.put(ACSQLHelper.DEVICE_COLUMN_NAME, device.getName());
        values.put(ACSQLHelper.DEVICE_COLUMN_SERIAL, device.getSerial());
        values.put(ACSQLHelper.DEVICE_COLUMN_DEVTYPE, device.getDevtype());
        values.put(ACSQLHelper.DEVICE_COLUMN_DEVSN, device.getDevsn());
        values.put(ACSQLHelper.DEVICE_COLUMN_UUID, device.getUuid());
        values.put(ACSQLHelper.DEVICE_COLUMN_TOKEN, device.getToken());
        values.put(ACSQLHelper.DEVICE_COLUMN_MAC, device.getMac());
        values.put(ACSQLHelper.DEVICE_COLUMN_PRICE, device.getPrice());
        values.put(ACSQLHelper.DEVICE_COLUMN_CITY, device.getCity());
        long id;
        if (device.getId() == 0||"add".equals(type)) {
            Log.e(TAG, "NAME : " + device.getName() + "                       -----ADD-----");
            id = db.insert(ACSQLHelper.TABLE_DEVICES, null, values);
        } else {
            Log.e(TAG, "NAME : " + device.getName() + "                       -----UPDATE-----");
            values.put(ACSQLHelper.DEVICE_COLUMN_ID, device.getId());
            id = db.replace(ACSQLHelper.TABLE_DEVICES, null, values);
        }
        close();
        if (id == -1) {
            Log.e(TAG, "NAME : " + device.getName() + "                       -----FAILED-----");
            // error, return null;
            return null;
        } else {
            device.setId((int) id);
            return device;
        }
    }

    /**
     * 移除Group
     *
     * @param groupId
     */
    public void removeGroupById(int groupId) {
        open();
        db.delete(ACSQLHelper.TABLE_GROUPS, ACSQLHelper.GROUP_COLUMN_ID + "=" + groupId, null);
        close();
    }

    /**
     * 移除设备
     *
     * @param deviceId
     */
    public void removeDeviceById(int deviceId) {
        open();
        db.delete(ACSQLHelper.TABLE_DEVICES, ACSQLHelper.DEVICE_COLUMN_ID + "=" + deviceId, null);
        close();
    }

    /**
     * 移除隐藏的设备
     *
     * @param serial
     */
    public void removeHideDevice(String serial) {
        open();
        db.delete(ACSQLHelper.TABLE_HIDE, ACSQLHelper.HIDE_COLUMN_SERIAL + "=?", new String[]{serial});
        close();
    }

    /**
     * 新增隐藏的设备
     *
     * @param hideInfo
     */
    public HideDeviceInfo addHideDevice(HideDeviceInfo hideInfo) {
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.HIDE_COLUMN_NAME, hideInfo.getName());
        values.put(ACSQLHelper.HIDE_COLUMN_SERIAL, hideInfo.getSerial());
        values.put(ACSQLHelper.HIDE_COLUMN_UUID, hideInfo.getUuid());
        values.put(ACSQLHelper.HIDE_COLUMN_MAC, hideInfo.getMac());
        values.put(ACSQLHelper.HIDE_COLUMN_PRODUCT, hideInfo.getProductid());
        values.put(ACSQLHelper.HIDE_COLUMN_FACTORY, hideInfo.getFactoryid());
        values.put(ACSQLHelper.HIDE_COLUMN_JOIN, hideInfo.getTime());
        long id;
        if (hideInfo.getId() == 0) {
            Log.e(TAG, "NAME : " + hideInfo.getName() + "                       -----ADD-----");
            id = db.insert(ACSQLHelper.TABLE_HIDE, null, values);
        } else {
            Log.e(TAG, "NAME : " + hideInfo.getName() + "                       -----UPDATE-----");
            values.put(ACSQLHelper.HIDE_COLUMN_ID, hideInfo.getId());
            id = db.replace(ACSQLHelper.TABLE_HIDE, null, values);
        }
        close();
        if (id == -1) {
            // error, return null;
            return null;
        } else {
            hideInfo.setId((int) id);
            return hideInfo;
        }
    }

    /**
     * 查询数据库中隐藏的设备
     *
     * @return
     */
    public List<HideDeviceInfo> getHideDevices() {
        open();
        ArrayList<HideDeviceInfo> hideInfos = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + ACSQLHelper.TABLE_HIDE;
        Cursor devicesCursor = db.rawQuery(selectQuery, null);
        while (devicesCursor.moveToNext()) {
            HideDeviceInfo info = new HideDeviceInfo();
            info.setId(devicesCursor.getInt(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_ID)));
            info.setName(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_NAME)));
            info.setSerial(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_SERIAL)));
            info.setUuid(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_UUID)));
            info.setMac(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_MAC)));
            info.setProductid(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_PRODUCT)));
            info.setFactoryid(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_FACTORY)));
            info.setTime(devicesCursor.getLong(devicesCursor.getColumnIndex(ACSQLHelper.HIDE_COLUMN_JOIN)));

            hideInfos.add(info);
        }
        close();
        return hideInfos;
    }


    /**
     * Get the list of Devices stored in the database.
     * *id,devsn,devtype,uuid,token,name,mac
     *
     * @return
     */
    public ArrayList<DeviceInfo> getAllDevices() {
        open();
        ArrayList<DeviceInfo> devices = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + ACSQLHelper.TABLE_DEVICES;
        Cursor devicesCursor = db.rawQuery(selectQuery, null);
        while (devicesCursor.moveToNext()) {
            DeviceInfo info = new DeviceInfo();
            info.setId(devicesCursor.getInt(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_ID)));
            info.setName(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_NAME)));
            info.setSerial(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_SERIAL)));
            info.setDevsn(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_DEVSN)));
            info.setDevtype(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_DEVTYPE)));
            info.setUuid(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_UUID)));
            info.setToken(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_TOKEN)));
            info.setMac(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_MAC)));
            info.setCountryNO(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_COUNTRYNO)));
            info.setLocation(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_LOCATION)));
            info.setPrice(devicesCursor.getDouble(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_PRICE)));
            info.setCity(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_CITY)));
            devices.add(info);
        }
        close();
        return devices;
    }

    public ArrayList<GroupDevice> getAllGroups() {
        open();
        ArrayList<GroupDevice> groups = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + ACSQLHelper.TABLE_GROUPS;
        Cursor devicesCursor = db.rawQuery(selectQuery, null);
        while (devicesCursor.moveToNext()) {
            GroupDevice info = new GroupDevice();
            info.setId(devicesCursor.getInt(devicesCursor.getColumnIndex(ACSQLHelper.GROUP_COLUMN_ID)));
            info.setName(devicesCursor.getString(devicesCursor.getColumnIndex(ACSQLHelper.GROUP_COLUMN_NAME)));
            String selectGroups = "SELECT  * FROM " + ACSQLHelper.TABLE_GROUP_DEVICE + " where " + ACSQLHelper.GROUP_COLUMN_ID + " = ?";
            Cursor deviceGroups = db.rawQuery(selectGroups, new String[]{info.getId() + ""});
            Map<String, DeviceInfo> mDevices = new HashMap<>();
            while (deviceGroups.moveToNext()) {
                DeviceInfo device = new DeviceInfo();
                device.setSerial(deviceGroups.getString(deviceGroups.getColumnIndex(ACSQLHelper.DEVICE_COLUMN_SERIAL)));
                mDevices.put(device.getSerial(), device);
            }
            info.setDevices(mDevices);
            groups.add(info);
        }
        close();
        return groups;
    }

    public GroupDevice createGroup(GroupDevice groupInfo) {
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.GROUP_COLUMN_NAME, groupInfo.getName());
        long id;
        if (groupInfo.getId() == 0) {
            id = db.insert(ACSQLHelper.TABLE_GROUPS, null, values);
        } else {
            values.put(ACSQLHelper.GROUP_COLUMN_ID, groupInfo.getId());
            id = db.replace(ACSQLHelper.TABLE_GROUPS, null, values);
        }
        close();
        if (id == -1) {
            // error, return null;
            return null;
        } else {
            groupInfo.setId((int) id);
            return groupInfo;
        }
    }

    /**
     * 查询手机注册信息
     */
    public PhoneInfo queryPhoneInfo() {
        PhoneInfo phoneInfo = null;
        open();
        String sql = "SELECT  * FROM " + ACSQLHelper.TABLE_PHONE + " where " + ACSQLHelper.PHONE_COLUMN_ID + "=?";
        Cursor cursor = db.rawQuery(sql, new String[]{1 + ""});
        while (cursor.moveToNext()) {
            phoneInfo = new PhoneInfo();
            phoneInfo.setId(cursor.getInt(cursor.getColumnIndex(ACSQLHelper.PHONE_COLUMN_ID)));
            phoneInfo.setMac(cursor.getString(cursor.getColumnIndex(ACSQLHelper.PHONE_COLUMN_MAC)));
            phoneInfo.setToken(cursor.getString(cursor.getColumnIndex(ACSQLHelper.PHONE_COLUMN_TOKEN)));
            phoneInfo.setType(cursor.getString(cursor.getColumnIndex(ACSQLHelper.PHONE_COLUMN_TYPE)));
            phoneInfo.setUuid(cursor.getString(cursor.getColumnIndex(ACSQLHelper.PHONE_COLUMN_UUID)));
        }
        close();
        return phoneInfo;
    }

    /**
     * 更新手机注册信息
     */
    public PhoneInfo updatePhoneInfo(PhoneInfo info) {
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.PHONE_COLUMN_ID, 1);
        values.put(ACSQLHelper.PHONE_COLUMN_MAC, info.getMac());
        values.put(ACSQLHelper.PHONE_COLUMN_TOKEN, info.getToken());
        values.put(ACSQLHelper.PHONE_COLUMN_TYPE, info.getType());
        values.put(ACSQLHelper.PHONE_COLUMN_UUID, info.getUuid());
        long id = db.update(ACSQLHelper.TABLE_PHONE, values,null,null);
        info.setId((int) id);
        close();
        return info;
    }

    public void updateGroupDevice(int groupId, String devsn) {
        Log.e(TAG, "GROUP_ID " + groupId + "  DEVS  : " + devsn);
        open();
        ContentValues values = new ContentValues();
        values.put(ACSQLHelper.GROUP_COLUMN_ID, groupId);
        values.put(ACSQLHelper.DEVICE_COLUMN_SERIAL, devsn);
        db.insert(ACSQLHelper.TABLE_GROUP_DEVICE, null, values);
        close();
    }

    public void deleteGroupDevice(String serial) {
        open();
        db.delete(ACSQLHelper.TABLE_GROUP_DEVICE, ACSQLHelper.DEVICE_COLUMN_SERIAL + "=?", new String[]{serial});
        close();
    }

    public void deleteGroupDevice(int groupId, String serial) {
        open();
        db.delete(ACSQLHelper.TABLE_GROUP_DEVICE, ACSQLHelper.DEVICE_COLUMN_SERIAL + "=? and " + ACSQLHelper.GROUP_COLUMN_ID + "=?", new String[]{serial, groupId + ""});
        close();
    }

    public void removeGroupScheduleById(int id) {
        open();
        db.delete(ACSQLHelper.TABLE_SCHEDULE, ACSQLHelper.SCHEDULE_COLUMN_ID + "=" + id, null);
        close();
    }

    /**
     * Open the database
     *
     * @throws SQLException
     */
    public void open() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    /**
     * Close the database
     */
    public void close() {
        dbHelper.close();
    }


}

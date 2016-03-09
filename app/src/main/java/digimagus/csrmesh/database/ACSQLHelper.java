package digimagus.csrmesh.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 数据库文件
 */
public class ACSQLHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String TAG = "ACSQLHelper";

    // Database version and name
    private static final int DATABASE_VERSION =3;
    private static final String DATABASE_NAME = "ac.db";

    // Database table names
    static final String TABLE_SETTINGS = "settings";
    static final String TABLE_DEVICES = "devices";
    static final String TABLE_GROUPS = "groups";
    static final String TABLE_PHONE = "phone";
    static final String TABLE_HIDE="hidedevice";
    static final String TABLE_GROUP_DEVICE = "group_device";

    static final String TABLE_SCHEDULE="schedule";

    //Group Schedule

    public static final String SCHEDULE_COLUMN_ID="id";
    public static final String SCHEDULE_COLUMN_GROUPID="groupId";//组的Id
    public static final String SCHEDULE_COLUMN_START_SENABLE="senable";//开始时间是否起效
    public static final String SCHEDULE_COLUMN_START_TIME="airtime";//开始时间
    public static final String SCHEDULE_COLUMN_START_EENABLE="eenable";//结束时间是否起效
    public static final String SCHEDULE_COLUMN_END_TIME="endtime";//结束时间
    public static final String SCHEDULE_COLUMN_START_WEEK="airweek";//开始周
    public static final String SCHEDULE_COLUMN_REPEAT="repeat";//重复

    // DEVICE table create statement
    private static final String CREATE_TABLE_SCHEDULE = "CREATE TABLE " + TABLE_SCHEDULE + "(" +
            SCHEDULE_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            SCHEDULE_COLUMN_GROUPID + "," + SCHEDULE_COLUMN_START_TIME + "," +SCHEDULE_COLUMN_START_SENABLE+","+
            SCHEDULE_COLUMN_END_TIME +","+SCHEDULE_COLUMN_START_EENABLE+"," + SCHEDULE_COLUMN_START_WEEK + "," +
            SCHEDULE_COLUMN_REPEAT+
            ")";



    //HIDE Device - Columns names
    public static final String HIDE_COLUMN_ID = "id";//ID
    public static final String HIDE_COLUMN_NAME = "name";//设备名称
    public static final String HIDE_COLUMN_SERIAL = "serial";//设备序列号
    public static final String HIDE_COLUMN_UUID = "uuid";//设备UUID
    public static final String HIDE_COLUMN_MAC = "mac";//设备MAC地址
    public static final String HIDE_COLUMN_PRODUCT = "product";//产品编号
    public static final String HIDE_COLUMN_FACTORY = "factory";//厂家编号
    public static final String HIDE_COLUMN_JOIN = "jtime";//添加时间


    // PHONE Table - Columns names
    public static final String PHONE_COLUMN_ID = "id";//ID
    public static final String PHONE_COLUMN_MAC = "mac"; //手机网卡的MAC地址
    public static final String PHONE_COLUMN_TYPE = "type"; //手机注册的类型
    public static final String PHONE_COLUMN_UUID = "uuid"; //手机注册成功之后的UUID
    public static final String PHONE_COLUMN_TOKEN = "token";//手机注册返回的TOKEN

    //id,location，price，unit，privacy
    // Settings Table - Columns names
    public static final String SETTINGS_COLUMN_ID = "id"; //ID
    public static final String SETTINGS_COLUMN_LOCATION = "location"; //地址
    public static final String SETTINGS_COLUMN_COUNTRYNO = "countryNO"; //地址
    public static final String SETTINGS_COLUMN_PRICE = "price"; //价格
    public static final String SETTINGS_COLUMN_UNIT = "unit"; //单位
    public static final String SETTINGS_COLUMN_PRIVACY = "privacy";//隐私
    public static final String SETTINGS_COLUMN_CITY = "city"; //城市

    //id,ip,port,devsn,devtype,uuid,token,name,mac,status
    // Device Table - Columns names
    public static final String DEVICE_COLUMN_ID = "id"; //ID
    public static final String DEVICE_COLUMN_NAME = "name"; //设备Name 名称
    public static final String DEVICE_COLUMN_SERIAL = "serial"; //设备序列号
    public static final String DEVICE_COLUMN_DEVSN = "devsn";//厂商
    public static final String DEVICE_COLUMN_DEVTYPE = "devtype"; //设备类型
    public static final String DEVICE_COLUMN_UUID = "uuid"; //设备UUID
    public static final String DEVICE_COLUMN_TOKEN = "token"; //设备TOKEN
    public static final String DEVICE_COLUMN_MAC = "mac"; //设备的MAC
    public static final String DEVICE_COLUMN_LOCATION = "location"; //地理位置
    public static final String DEVICE_COLUMN_COUNTRYNO = "countryNO"; //国家编号
    public static final String DEVICE_COLUMN_PRICE = "price"; //价格
    public static final String DEVICE_COLUMN_CITY = "city"; //城市

    // GROUP Table - Columns names
    public static final String GROUP_COLUMN_ID = "id"; //ID
    public static final String GROUP_COLUMN_NAME = "name"; //组名称

    // Group table create statement
    private static final String CREATE_TABLE_GROUPS = "CREATE TABLE " + TABLE_GROUPS + "(" +
            GROUP_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + GROUP_COLUMN_NAME + " TEXT" +
            ")";

    // GROUP Table - Columns names
    private static final String CREATE_TABLE_GROUP_DEVICE = "CREATE TABLE " + TABLE_GROUP_DEVICE + "(" +
            GROUP_COLUMN_ID + " INTEGER," + DEVICE_COLUMN_SERIAL + " TEXT" +
            ")";

    /**
     * autoincrement
     * create table a(d_id integer,g_id integer,primary key());
     * @param context
     */
    public ACSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     *
     */
    private static final String CREATE_TABLE_PHONE =
            "CREATE TABLE " + TABLE_PHONE + "(" +
                    PHONE_COLUMN_ID + " INTEGER PRIMARY KEY," + PHONE_COLUMN_MAC + "," + PHONE_COLUMN_TYPE + "," + PHONE_COLUMN_UUID + "," + PHONE_COLUMN_TOKEN
                    + ")";


    // DEVICE table create statement
    private static final String CREATE_TABLE_DEVICES = "CREATE TABLE " + TABLE_DEVICES + "(" +
            DEVICE_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            DEVICE_COLUMN_NAME + "," + DEVICE_COLUMN_SERIAL + " VARCHAR UNIQUE," +
            DEVICE_COLUMN_DEVTYPE + "," + DEVICE_COLUMN_UUID + "," +
            DEVICE_COLUMN_TOKEN + "," + DEVICE_COLUMN_MAC +"," +
            DEVICE_COLUMN_LOCATION + "," + DEVICE_COLUMN_COUNTRYNO +"," +
            DEVICE_COLUMN_PRICE +","+DEVICE_COLUMN_CITY+"," + DEVICE_COLUMN_DEVSN+
            ")";

    private static final String CREATE_TABLE_HIDE =
            "CREATE TABLE " + TABLE_HIDE + "(" +
                    HIDE_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    HIDE_COLUMN_NAME + "," + HIDE_COLUMN_SERIAL + "," +HIDE_COLUMN_UUID +","+
                    HIDE_COLUMN_MAC+","+HIDE_COLUMN_PRODUCT + "," + HIDE_COLUMN_FACTORY+","+HIDE_COLUMN_JOIN+
                    ")";

    // Setting table create statement
    private static final String CREATE_TABLE_SETTINGS = "CREATE TABLE " + TABLE_SETTINGS + "(" +
            SETTINGS_COLUMN_ID + " INTEGER PRIMARY KEY," + SETTINGS_COLUMN_LOCATION + "," +SETTINGS_COLUMN_COUNTRYNO+","+
            SETTINGS_COLUMN_PRICE + "," + SETTINGS_COLUMN_UNIT + "," + SETTINGS_COLUMN_PRIVACY + "," +
            SETTINGS_COLUMN_CITY+
    ")";



    @Override
    public void onCreate(SQLiteDatabase database) {
        // creating required tables
        database.execSQL(CREATE_TABLE_HIDE);
        Log.e(TAG, "HIDE:  " + CREATE_TABLE_HIDE);
        database.execSQL(CREATE_TABLE_SCHEDULE);
        Log.e(TAG, "SCHEDULE:  " + CREATE_TABLE_SCHEDULE);
        database.execSQL(CREATE_TABLE_SETTINGS);
        database.execSQL(CREATE_TABLE_DEVICES);
        database.execSQL(CREATE_TABLE_GROUPS);
        database.execSQL(CREATE_TABLE_PHONE);
        database.execSQL(CREATE_TABLE_GROUP_DEVICE);
        //初始化手机注册信息
        String phone = "INSERT INTO " +
                TABLE_PHONE + "(" +
                PHONE_COLUMN_ID + "," +
                PHONE_COLUMN_MAC + "," +
                PHONE_COLUMN_TYPE + "," +
                PHONE_COLUMN_UUID + "," +
                PHONE_COLUMN_TOKEN + ") VALUES(1,'','android','','')";

        //初始化Settings表数据
        database.execSQL("INSERT INTO " + TABLE_SETTINGS + "(" + SETTINGS_COLUMN_ID + "," + SETTINGS_COLUMN_LOCATION + "," + SETTINGS_COLUMN_COUNTRYNO + "," + SETTINGS_COLUMN_PRICE + "," + SETTINGS_COLUMN_UNIT + "," + SETTINGS_COLUMN_PRIVACY + "," + SETTINGS_COLUMN_CITY + ") values(1,'','',1.8,'KWh',1,'')");
        database.execSQL(phone);
    }

    /**
     * insert into table() values();
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HIDE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEVICES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHONE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP_DEVICE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULE);
        // create new tables
        onCreate(db);
    }
}

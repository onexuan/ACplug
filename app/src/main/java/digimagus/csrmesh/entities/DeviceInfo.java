package digimagus.csrmesh.entities;

/**
 * 设备信息
 */
public class DeviceInfo extends Information {
    private String mac;
    private String IP;
    private int PORT;
    public double power;
    private String devsn;
    private String devtype;
    private String serial;
    private String uuid;
    private String token;
    public int state;
    private boolean choose;
    public boolean online;
    public double IRMS;
    public double URMS;
    private String location;
    private String countryNO;
    private double price;
    private String city;//城市
    public String swversion;
    public boolean reset;
    public boolean activated;

    public int remaining;//倒计时剩余时间

    public String msg;//发送消息
    public byte msgtype;
    public long sendtime;


    public long sendmsgtime;

    public DeviceInfo(int id) {
        super(id);
    }
    public DeviceInfo() {
    }
    public String getMac() {
        return mac;
    }
    public void setMac(String mac) {
        this.mac = mac;
    }
    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getPORT() {
        return PORT;
    }

    public void setPORT(int PORT) {
        this.PORT = PORT;
    }

   public String getDevsn() {
        return devsn;
    }

    public void setDevsn(String devsn) {
        this.devsn = devsn;
    }

    public String getDevtype() {
        return devtype;
    }

    public void setDevtype(String devtype) {
        this.devtype = devtype;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }


    public boolean isChoose() {
        return choose;
    }

    public void setChoose(boolean choose) {
        this.choose = choose;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountryNO() {
        return countryNO;
    }

    public void setCountryNO(String countryNO) {
        this.countryNO = countryNO;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}

package digimagus.csrmesh.entities;

/**
 * 设备的在线信息状态
 */
public class OnlineInformation {

    private String ip;//本地模式下设备的IP
    private int port;//设备的端口号
    private String serial;//设备的序列号
    private boolean online;//在线状态
    private long uptime;//更新时间
    private boolean isupdate;




    public void setIsupdate(boolean isupdate){
        this.isupdate = isupdate;
    }
    public boolean getIsupdate(){
        return isupdate;
    }


    public void setOnline(boolean online){
        this.online = online;
    }
    public boolean getOnline(){
        return online;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }



    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

}
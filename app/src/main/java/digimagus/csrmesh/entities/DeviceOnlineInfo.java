package digimagus.csrmesh.entities;

/**
 * 设备在线下线信息
 */
public class DeviceOnlineInfo {

    private String IP;
    private int PROT;
    private String SERIAL;

    public DeviceOnlineInfo(String IP, int PROT, String SERIAL) {
        this.IP = IP;
        this.PROT = PROT;
        this.SERIAL = SERIAL;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getPROT() {
        return PROT;
    }

    public void setPROT(int PROT) {
        this.PROT = PROT;
    }

    public String getSERIAL() {
        return SERIAL;
    }

    public void setSERIAL(String SERIAL) {
        this.SERIAL = SERIAL;
    }


}

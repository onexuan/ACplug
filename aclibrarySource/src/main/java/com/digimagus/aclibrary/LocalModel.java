package com.digimagus.aclibrary;


public class LocalModel {

    public LocalModel() {
    }

    public LocalModel(String IP, int PORT, String MAC, String SERIAL, String firm, String type, String retention) {
        this.IP = IP;
        this.PORT = PORT;
        this.MAC = MAC;
        this.SERIAL = SERIAL;
        this.firm = firm;
        this.type = type;
        this.retention = retention;
    }

    public String IP;
    public int PORT;
    public String MAC;
    public String SERIAL;
    public String firm;
    public String type;
    public String retention;
    public long time;
}

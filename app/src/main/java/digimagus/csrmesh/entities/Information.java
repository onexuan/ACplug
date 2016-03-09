package digimagus.csrmesh.entities;

import java.io.Serializable;

/**
 * 信息类
 */
public class Information implements Serializable {
    private int id;
    private String name;
    public Information(){}

    public Information(int UKNOWN_ID){
        this.id=UKNOWN_ID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package digimagus.csrmesh.entities;

/**
 * 保存电量电费对象
 */
public class SummaryEntities {

    private String energyP;
    private double cost;
    private int day;
    private int switc;
    private String timestamp;
    private String IRMS;
    private String PWOER;
    private int hour;


    public double getCost() {
        return cost;
    }

    public void setCost(double price) {
        this.cost = price;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getSwitc() {
        return switc;
    }

    public void setSwitc(int switc) {
        this.switc = switc;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getIRMS() {
        return IRMS;
    }

    public void setIRMS(String IRMS) {
        this.IRMS = IRMS;
    }

    public String getEnergyP() {
        return energyP;
    }

    public void setEnergyP(String energyP) {
        this.energyP = energyP;
    }

    public String getPWOER() {
        return PWOER;
    }

    public void setPWOER(String PWOER) {
        this.PWOER = PWOER;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }
}

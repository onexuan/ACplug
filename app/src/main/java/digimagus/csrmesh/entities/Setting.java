package digimagus.csrmesh.entities;

/**
 * 手机的设置信息
 *
 */
public class Setting {
    private int id;
    private String locate;
    private String countryNO;
    private double price;
    private String unit;
    private boolean privacy;//0 false 1 true
    private String city;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocate() {
        return locate;
    }

    public void setLocate(String locate) {
        this.locate = locate;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isPrivacy() {
        return privacy;
    }

    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }

    public String getCountryNO() {
        return countryNO;
    }

    public void setCountryNO(String countryNO) {
        this.countryNO = countryNO;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}

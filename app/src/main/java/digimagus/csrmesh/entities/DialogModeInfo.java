package digimagus.csrmesh.entities;

/**
 * Created by Administrator on 2015/10/13.
 */
public class DialogModeInfo {
    private int index;
    private String item_title;
    private boolean state;
    public DialogModeInfo(){}
    public DialogModeInfo(int index,String item_title,boolean state){
        this.index=index;
        this.item_title=item_title;
        this.state=state;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }





    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public String getItem_title() {
        return item_title;
    }

    public void setItem_title(String item_title) {
        this.item_title = item_title;
    }
}

package digimagus.csrmesh.entities;

import java.io.Serializable;

/**
 * 手机设置定时
 */
public class ScheduleInfo implements Comparable<ScheduleInfo>, Serializable {

    final static String TAG = "ScheduleInfo";

    public int id;
    public int index;
    public int start_h;
    public int start_m;
    public int start_w;
    public int start_s;
    public int end_h;
    public int end_m;
    public int end_w;
    public int end_s;
    public int repeat;
    public int enable;
    public int running;
    public String json;
    public int duration;
    public long settime;

    @Override
    public int compareTo(ScheduleInfo info) {
        double c1 = start_h + start_m / 60.0;
        double c2 = end_h + end_m / 60.0;
        double b1 = info.start_h + info.start_m / 60.0;
        double b2 = info.end_h + info.end_m / 60.0;
        if (
                (start_s == 1 && info.start_s == 1 && c1 > b1) ||
                (start_s == 1 && info.start_s != 1 && c1 > b2) ||
                (start_s != 1 && info.start_s == 1 && c2 > b1) ||
                (start_s != 1 && info.start_s != 1 && c2 > b2))
        {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "ScheduleInfo{" +
                "id=" + id +
                ", index=" + index +
                ", start_h=" + start_h +
                ", start_m=" + start_m +
                ", start_w=" + start_w +
                ", start_s=" + start_s +
                ", end_h=" + end_h +
                ", end_m=" + end_m +
                ", end_w=" + end_w +
                ", end_s=" + end_s +
                ", repeat=" + repeat +
                ", enable=" + enable +
                ", running=" + running +
                ", duration=" + duration +
                ", settime=" + settime +
                '}';
    }
}

package digimagus.csrmesh.acplug.util;

import android.content.Context;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.ScheduleInfo;

/**
 * 验证 Schedule 是否合法
 * 我把  我设置的时间 一个个拿出来和另一个设备进行比较
 * 设置的时间 简化到每天
 */
public class VerificationSchedule {
    private final static String TAG = "VerificationSchedule";
    //保存我设置的 计划的周数
    Map<Integer, CompareTime> compare = null;
    //保存以及存在的 计划索引 定时计划的周数
    Map<Integer, Map<Integer, CompareTime>> compares = null;
    //记录索引
    Map<Integer, Integer> map = null;

    private static class LazyHolder {
        private static final VerificationSchedule INSTANCE = new VerificationSchedule();
    }

    DecimalFormat df = new DecimalFormat("00");

    public static final VerificationSchedule getInstance() {
        return LazyHolder.INSTANCE;
    }

    String getWeek(String week) {
        int strLen = week.length();
        while (strLen < 7) {
            week = (new StringBuffer().append("0").append(week)).toString();// 左(前)补0
            strLen = week.length();
        }
        return week;
    }

    /**
     * 验证保存的定时计划有没有交集
     * 单个设备Schedule放进来
     * 组设备的Schedule将每个设备的定时计划发送过来
     *
     * @param info
     * @param schedules
     * @return
     */
    public List<String> verification(Context context, String operationType, ScheduleInfo info, Map<Integer, ScheduleInfo> schedules) {
        List<String> repeatweek = new ArrayList<>();
        compare = new HashMap<>();
        compares = new HashMap<>();
        map = new HashMap<>();
        if ("update".equals(operationType)) {//如果是更新先移除  schedule的记录
            schedules.remove(info.index);
        }
        if (!schedules.isEmpty()) {
            //1、拆分 保存我设置的定时计划
            compare = setSchedule(info, compare);
            //2、拆分 保存设备已经存在的定时计划
            compares = existSchedule(compares, schedules);
            //3、 对比 我需要设置的定时计划 和 设备已经存在的定时计划
            map = getRepeatTimes(map);
            for (Integer i : map.values()) {
                ScheduleInfo schedule = schedules.get(i);
                String weeks = getWeek(Integer.toBinaryString(schedule.start_w));
                String week = "";
                if (schedule.start_w == 127) {
                    week = context.getString(R.string.weekdays);
                } else {
                    for (int j = 0; j < 7; j++) {
                        if (weeks.charAt(j) == '1') {
                            week = weeks_letter[j] + " " + week;
                        }
                    }
                }
                String start_time = schedule.start_s == 1 ? (df.format(schedule.start_h) + ":" + df.format(schedule.start_m)) : "";
                String end_time = schedule.end_s == 1 ? (df.format(schedule.end_h) + ":" + df.format(schedule.end_m)) : "";

                repeatweek.add("[" + start_time + " - " + end_time + week + "]");
            }
        }
        return repeatweek;
    }

    private Map<Integer, CompareTime> setSchedule(ScheduleInfo info, Map<Integer, CompareTime> compare) {
        String week = getWeek(Integer.toBinaryString(info.start_w));
        for (int i = 0; i < 7; i++) {
            char c = week.charAt(i);
            if (c == '1') {
                compare.put(weeks_digit[i], new CompareTime(info.index, weeks_digit[i], info.start_h, info.start_m, info.end_h, info.end_m, info.repeat, info.start_s, info.end_s));
            }
        }
        return compare;
    }

    private Map<Integer, Map<Integer, CompareTime>> existSchedule(Map<Integer, Map<Integer, CompareTime>> compares, Map<Integer, ScheduleInfo> schedules) {
        for (ScheduleInfo schedule : schedules.values()) {
            String week1 = getWeek(Integer.toBinaryString(schedule.start_w));
            Map<Integer, CompareTime> compareTimeMap = new HashMap<>();
            for (int i = 0; i < 7; i++) {//保存这周那几天设置的有定时计划
                char c = week1.charAt(i);
                if (c == '1') {
                    compareTimeMap.put(weeks_digit[i], new CompareTime(schedule.index, weeks_digit[i], schedule.start_h, schedule.start_m, schedule.end_h, schedule.end_m, schedule.repeat, schedule.start_s, schedule.end_s));
                }
            }
            compares.put(schedule.index, compareTimeMap);
        }
        return compares;
    }

    //3、 对比 我需要设置的定时计划 和 设备已经存在的定时计划
    //    还需要考虑 我修改定时计划的情况 (此时把他从 2 中移除 在和 2 的集合进行对比)
    // 暂时不考虑更新
    private Map<Integer, Integer> getRepeatTimes(Map<Integer, Integer> map) {
        for (CompareTime c : compare.values()) {//我输入的定时计划
            //Log.e(TAG, "SET_1 " + c.hour_s + ":" + c.minute_s + " - " + c.hour_e + ":" + c.minute_e + "  w:" + c.week);
            double c1 = c.hour_s + c.minute_s / 60.0;
            double c2 = c.hour_e + c.minute_e / 60.0;
            if (c2 < c1&&c.start_e==1) {
                c2 = c2 + 24;
            }
            //Log.e(TAG,"C: "+ c1 + "   " + c2 + "  " + c.start_e + "  " + c.end_e);
            //循环输输出，已经存在的定时计划 按照一周每天
            for (Map<Integer, CompareTime> d : compares.values()) {
                CompareTime m = d.get(c.week);
                CompareTime l_m = d.get(c.week == 0 ? 6 : c.week - 1);//上一天
                CompareTime n_m = d.get(c.week == 6 ? 0 : c.week + 1);//下一天
                /**
                 * 上部分
                 * ON OFF 都打开
                 * 上一天
                 */
                if (l_m != null) {
                    double l1 = l_m.hour_s + l_m.minute_s / 60.0;
                    double l2 = l_m.hour_e + l_m.minute_e / 60.0;
                    //Log.e(TAG,l1+"   "+l2+"  "+l_m.start_e+"  "+l_m.end_e);
                    if (l_m.end_e == 1 && l2 < l1) {
                        if (l_m.start_e == 1) {
                            if (c.start_e == 1 && c.end_e == 1 && c1 <= l2) {
                                Log.e(TAG, "1-1、设备中的定时计划与设置的定时计划存在交集 " + (l_m == null ? n_m.index : l_m.index));
                                map.put(l_m.index, l_m.index);
                            } else if ((c.start_e == 1 && c1 == l2) || (c.end_e == 1 && c2 == l2)) {
                                Log.e(TAG, "1-2、设备中的定时计划与设置的定时计划存在交集 " + (l_m == null ? n_m.index : l_m.index));
                                map.put(l_m.index, l_m.index);
                            }
                        } else if (l_m.start_e == 0 && ((c.start_e == 1 && c1 == l2) || (c.end_e == 1 && c2 == l2))) {
                            Log.e(TAG, "1-3、设备中的定时计划与设置的定时计划存在交集 " + (l_m == null ? n_m.index : l_m.index));
                            map.put(l_m.index, l_m.index);
                        }
                    }
                }
                /**
                 * 下一天
                 */
                if (n_m != null && c2 > 24 && c.end_e == 1) {
                    c2 = c2 - 24;
                    double n1 = n_m.hour_s + n_m.minute_s / 60.0;
                    double n2 = n_m.hour_e + n_m.minute_e / 60.0;
                    //Log.e(TAG,n1+"   "+n2+"  "+n_m.start_e+"  "+n_m.end_e);
                    if (c.start_e == 1) {
                        if (n_m.start_e == 1 && n_m.end_e == 1 && c2 >= n1) {
                            Log.e(TAG, "2-1、设备中的定时计划与设置的定时计划存在交集 " + (n_m == null ? n_m.index : n_m.index));
                            map.put(n_m.index, n_m.index);
                        } else if ((n_m.start_e == 1 && c2 == n1) || (n_m.end_e == 1 && c2 == n2)) {
                            Log.e(TAG, "2-2、设备中的定时计划与设置的定时计划存在交集 " + (n_m == null ? n_m.index : n_m.index));
                            map.put(n_m.index, n_m.index);
                        }
                    } else if (c.start_e == 0 && ((n_m.start_e == 1 && c2 == n1) || (n_m.end_e == 1 && c2 == n2))) {
                        Log.e(TAG, "2-3、设备中的定时计划与设置的定时计划存在交集 " + (n_m == null ? n_m.index : n_m.index));
                        map.put(n_m.index, n_m.index);
                    }
                }

                /**
                 * 当天
                 */
                if (m != null) {
                    double m1 = m.hour_s + m.minute_s / 60.0;
                    double m2 = m.hour_e + m.minute_e / 60.0;
                    if (m2 < m1) {
                        m2 = m2 + 24;
                    }
                    //Log.e(TAG,"M: "+ m1 + "   " + m2 + "  " + m.start_e + "  " + m.end_e);
                    if (m.start_e == 1 && m.end_e == 1) {
                        if (c.start_e == 1 && c.end_e == 1) {
                            if ((c1 >= m1 && c1 <= m2) || (c2 >= m1 && c2 <= m2)) {
                                Log.e(TAG, "3-1、设备中的定时计划与设置的定时计划存在交集 \n" + m1 + " - " + m2 + "  w:" + m.week + "\n" + c1 + " - " + c2 + "  w:" + c.week);
                                map.put(m.index, m.index);
                            } else if ((m1 >= c1 && m1 <= c2) || (m2 >= c1 && m2 <= c2)) {
                                map.put(m.index, m.index);
                            }
                        }else if(c.start_e == 1 || c.end_e == 1){
                            if ((c.start_e == 1 && (c1 == m1 || c1 == m2)) || (c.end_e == 1 && (c2 == m1 || c2 == m2))) {
                                Log.e(TAG, "3-2、设备中的定时计划与设置的定时计划存在交集 \n" + m1 + " - " + m2 + "  w:" + m.week + "\n" + c1 + " - " + c2 + "  w:" + c.week);
                                map.put(m.index, m.index);
                            }
                        }
                    } else if (m.start_e == 1 || m.end_e == 1) {
                        if ((m.start_e == 1 && c.start_e == 1 && (m1 == c1)) || (m.start_e == 1 && c.end_e == 1 && (m1 == c2)) || (m.end_e == 1 && c.end_e == 1 && (m2 == c2)) || (m.end_e == 1 && c.start_e == 1 && (m2 == c1))) {
                            Log.e(TAG, "3-3、设备中的定时计划与设置的定时计划存在交集 \n" + m1 + " - " + m2 + "  w:" + m.week + "\n" + c1 + " - " + c2 + "  w:" + c.week);
                            map.put(m.index, m.index);
                        }
                    }
                }
            }
        }
        return map;
    }

    int weeks_digit[] = new int[]{6, 5, 4, 3, 2, 1, 0};
    String weeks_letter[] = new String[]{"Sat", "Fri", "Thu", "Wed", "Tue", "Mon", "Sun"};

    class CompareTime {
        public CompareTime(int index, int week, int hour_s, int minute_s, int hour_e, int minute_e, int repeat, int start_e, int end_e) {
            this.index = index;
            this.week = week;
            this.hour_s = hour_s;
            this.minute_s = minute_s;
            this.hour_e = hour_e;
            this.minute_e = minute_e;
            this.repeat = repeat;
            this.start_e = start_e;
            this.end_e = end_e;
        }

        public int index;
        public int week;
        public int hour_s;
        public int minute_s;
        public int hour_e;
        public int minute_e;
        public int repeat;
        public int start_e;
        public int end_e;
    }

}
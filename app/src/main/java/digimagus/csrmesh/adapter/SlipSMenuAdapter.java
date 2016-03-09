package digimagus.csrmesh.adapter;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.GroupDevice;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.view.SwitchView;

/**
 * Group ListView item 侧滑菜单
 */
public class SlipSMenuAdapter extends BaseAdapter implements View.OnClickListener {
    private final static String TAG = "SlipSMenuAdapter";
    private List<ScheduleInfo> infoList;
    private Context context;
    private int screenWidth;
    ViewHolder holder;
    private Calendar timeCalendar;

    public SlipSMenuAdapter(Context context, List<ScheduleInfo> infoList, int screenWidth) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.infoList = infoList;
        Collections.sort(this.infoList);
    }

    @Override
    public void notifyDataSetChanged() {
        Collections.sort(infoList);
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return infoList.size();
    }

    @Override
    public Object getItem(int position) {
        return infoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ScheduleInfo info = infoList.get(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_schedule_menu, null);
            holder.scrollView = (HorizontalScrollView) convertView.findViewById(R.id.scrollView);
            holder.menu = (LinearLayout) convertView.findViewById(R.id.menu);
            holder.content = (LinearLayout) convertView.findViewById(R.id.content);
            holder.schedule_time = (TextView) convertView.findViewById(R.id.schedule_time);
            holder.delete = (ImageView) convertView.findViewById(R.id.delete);
            holder.weeks = (TextView) convertView.findViewById(R.id.weeks);
            holder.run = (SwitchView) convertView.findViewById(R.id.run);
            holder.runing = (ImageView) convertView.findViewById(R.id.runing);
            ViewGroup.LayoutParams lp = holder.content.getLayoutParams();
            lp.width = screenWidth;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        convertView.setOnTouchListener(onTouchListener);
        holder.delete.setTag(position);
        holder.content.setTag(position);
        holder.delete.setOnClickListener(this);
        holder.content.setOnClickListener(this);

        String a = "", b = "";
        if (info.start_h < 12) {
            a = "am";
        } else {
            a = "pm";
        }
        if (info.end_h < 12) {
            b = "am";
        } else {
            b = "pm";
        }
        holder.run.setOnStateChangedListener(new SwitchView.OnStateChangedListener() {
            @Override
            public void toggleToOn(SwitchView view) {
                controlListener.setScheduleEnable(info.json.replace("\"enable\":2", "\"enable\":1"));
                view.setState(true);
            }

            @Override
            public void toggleToOff(SwitchView view) {
                controlListener.setScheduleEnable(info.json.replace("\"enable\":1", "\"enable\":2"));
                view.setState(false);
            }
        });

        holder.schedule_time.setText(Html.fromHtml(context.getString(R.string.schedule_time, (info.start_s == 1 ? (df1.format(info.start_h) + ":" + df1.format(info.start_m)) : ""), info.start_s == 1 ? a : "", info.end_s == 1 ? (df1.format(info.end_h) + ":" + df1.format(info.end_m)) : "", info.end_s == 1 ? b : "", (info.end_s == 1 && info.start_s == 1) ? "-" : "").trim()) + " " + ((info.end_s == 1 && info.start_s == 0) ? "OFF" : "") + ((info.end_s == 0 && info.start_s == 1) ? "ON" : ""));
        holder.weeks.setText(getWeek(info.start_w).trim() + (info.repeat == 1 ? " " + context.getString(R.string.repeat) : ""));

        //enable   schedule 是否执行
        //running  schedule 是否正在执行
        //repeat   schedule 是否重复执行
        /**
         * 1、重复执行的话  两种状态 (不需要操作)
         * 2、running=1   正在执行  (不需要操作)
         * 3、running=0   未执行
         *
         */
        /**
         * 验证时间、如果时间打
         *
         * 在改变开关之前设置开关的类型
         */
        if (info.repeat == 0 && verificationTime(info)&&((info.start_s==1&&info.end_s==1&& info.running == 0)||((info.start_s==1&&info.end_s!=1)||(info.start_s!=1&&info.end_s==1)))) {
            holder.runing.setImageResource(R.drawable.uncurrent);
            holder.run.setSwitchViewType(SwitchView.SwitchViewType.SwitchViewTypeGray);
        } else {
            holder.runing.setImageResource((info.start_s == 1 && info.end_s == 1 && info.running == 1) ? R.drawable.current : R.drawable.uncurrent);
            holder.run.setSwitchViewType(SwitchView.SwitchViewType.SwitchViewTypeGreen);
        }
        if (info.enable == 1 && holder.run.getState() == 1) {
            holder.run.setState(true);
        } else if (info.enable == 2 && (holder.run.getState() == 2 || holder.run.getState() == 4)) {
            holder.run.setState(false);
        }
        if (info.enable == 1) {
            holder.content.setBackgroundResource(R.drawable.light_item_bg);
        } else if (info.enable == 2) {
            holder.content.setBackgroundResource(R.drawable.light_item_grybg);
        }
        return convertView;
    }

    private Calendar calendar = Calendar.getInstance();

    private boolean verificationTime(ScheduleInfo info) {
        calendar.setTimeInMillis(info.settime * 1000);
        double current = timeCalendar.get(Calendar.HOUR_OF_DAY) + timeCalendar.get(Calendar.MINUTE) / 60.0;
        double settime = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0;

        double devicetime = 0;
        if (info.end_s == 1) {
            devicetime = info.end_h + info.end_m / 60.0;
        } else {
            devicetime = info.start_h + info.start_m / 60.0;
        }
        /*
        Log.e(TAG, info.start_h + ":" + info.start_m + " - " + info.end_h + ":" + info.end_m + " -->   " + info.start_w + "   " + info.end_w);
        Log.e(TAG, "current:" + current);
        Log.e(TAG, "settime:" + settime);
        Log.e(TAG, "devicetime:" + devicetime);*/

        double w = Math.pow(2, timeCalendar.get(Calendar.DAY_OF_WEEK) - 1);
        if (info.start_w == w && settime < devicetime && current >= devicetime) {
            return true;
        }
        return false;
    }

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (oldHolder != null) {
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder = null;
                    } else if (view != null) {
                        ViewHolder holder = (ViewHolder) view.getTag();
                        scrollX = holder.scrollView.getScrollX();// 获得HorizontalScrollView滑动的水平方向值.
                        holder.scrollView.smoothScrollTo(0, 0);
                    }
                }
                case MotionEvent.ACTION_UP: {
                    if (oldHolder != null) {
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder = null;
                    } else {
                        ViewHolder holder = (ViewHolder) v.getTag();
                        view = v;
                        int actionW = holder.menu.getWidth();//获得操作区域的长度
                        if (scrollX < actionW / 2) {
                            holder.scrollView.smoothScrollTo(0, 0);
                        } else {
                            holder.scrollView.smoothScrollTo(actionW, 0);
                            oldHolder = holder;
                        }
                    }

                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    if (oldHolder != null) {
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder = null;
                    } else {
                        ViewHolder holder = (ViewHolder) v.getTag();
                        view = v;
                        int actionW = holder.menu.getWidth();//获得操作区域的长度
                        if (scrollX < actionW / 2) {
                            holder.scrollView.smoothScrollTo(0, 0);
                        } else {
                            holder.scrollView.smoothScrollTo(actionW, 0);
                            oldHolder = holder;
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (oldHolder != null) {
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder = null;
                    } else {
                        ViewHolder holder = (ViewHolder) v.getTag();
                        scrollX = holder.scrollView.getScrollX();// 获得HorizontalScrollView滑动的水平方向值.
                    }
                }
            }
            return false;
        }
    };

    private int scrollX = 0;
    private ViewHolder oldHolder;


    public void setControlScheduleListener(ControlScheduleListener listener) {
        this.controlListener = listener;
    }

    public ControlScheduleListener controlListener;

    public void setTimeCalendar(Calendar timeCalendar) {
        this.timeCalendar = timeCalendar;
    }

    public interface ControlScheduleListener {
        void controlSchedule(int position, View v);

        void setScheduleEnable(String schedule);
    }

    @Override
    public void onClick(View v) {
        if (view != null) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.scrollView.smoothScrollTo(0, 0);
        }
        controlListener.controlSchedule((Integer) v.getTag(), v);
    }

    class ViewHolder {
        HorizontalScrollView scrollView;
        LinearLayout content, menu;
        TextView schedule_time, weeks;
        ImageView delete, runing;
        SwitchView run;
    }

    private View view;


    private String getWeek(int week) {
        StringBuffer sb = new StringBuffer();
        String data = Integer.toBinaryString(week);
        int len = data.length();
        String add = "";
        for (int i = 0; i < 7 - len; i++) {
            add = add + "0";
        }
        data = add + data;
        String[] weeks = data.trim().split("");
        boolean brek = true;
        for (int i = weeks.length - 1; i > 0; i--) {
            if (!brek || !weeks[i].equals("1")) {
                brek = false;
            }
            sb.append(weeks[i].equals("1") ? Weeks[i - 1] : "");
            sb.append(" ");
        }
        if (brek) {
            return context.getString(R.string.weekdays);
        } else {
            return sb.toString();
        }
    }

    public String[] Weeks = new String[]{"Sat", "Fri", "Thu", "Wed", "Tue", "Mon", "Sun"};
    DecimalFormat df1 = new DecimalFormat("00");
}

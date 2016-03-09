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
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.ScheduleInfo;
import digimagus.csrmesh.view.SwitchView;

/**
 *
 *
 */
public class GroupScheduleAdapter extends BaseAdapter implements View.OnClickListener{

    private List<ScheduleInfo> scheduleInfos;
    private Context context;
    private int screenWidth;

    public GroupScheduleAdapter(Context context, List<ScheduleInfo> scheduleInfos, int screenWidth) {
        this.context = context;
        this.scheduleInfos = scheduleInfos;
        this.screenWidth = screenWidth;
        Collections.sort(this.scheduleInfos);
    }

    public void notifyDataSetChanged(List<ScheduleInfo> scheduleInfos) {
        this.scheduleInfos = scheduleInfos;
        Collections.sort(scheduleInfos);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return scheduleInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return scheduleInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScheduleInfo info = scheduleInfos.get(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_group_schedule, null);
            holder.scrollView = (HorizontalScrollView) convertView.findViewById(R.id.scrollView);
            holder.menu= (LinearLayout) convertView.findViewById(R.id.menu);
            holder.content = (LinearLayout) convertView.findViewById(R.id.content);
            holder.schedule_time= (TextView) convertView.findViewById(R.id.schedule_time);
            holder.delete = (ImageView) convertView.findViewById(R.id.delete);
            holder.weeks= (TextView) convertView.findViewById(R.id.weeks);
            holder.run= (SwitchView) convertView.findViewById(R.id.run);
            holder.runing= (ImageView) convertView.findViewById(R.id.runing);
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
        int h1,h2;
        h1=info.start_h;
        h2=info.end_h;
        if (h1< 12 || h1 == 0) {
            a = "am";
        } else {
            a = "pm";
            if (h1 > 12) {
                h1 -= 12;
            }
        }
        if (h2 < 12 || h2 == 0) {
            b = "am";
        } else {
            b = "pm";
            if (h2 > 12) {
                h2 -= 12;
            }
        }
        holder.schedule_time.setText(Html.fromHtml(context.getString(R.string.schedule_time, (info.start_s==1?(df1.format(info.start_h) + ":" + df1.format(info.start_m)):""),info.start_s==1?a:"", info.end_s==1?(df1.format(info.end_h) + ":" + df1.format(info.end_m)):"", info.end_s==1?b:"",(info.end_s==1&&info.start_s==1)?"-":"").trim())+" "+((info.end_s==1&&info.start_s==0)?"OFF":"")+((info.end_s==0&&info.start_s==1)?"ON":""));
        holder.weeks.setText(getWeek(info.start_w) + (info.repeat == 1 ? " " + context.getString(R.string.repeat) : ""));
        return convertView;
    }


    private int scrollX=0;
    private ViewHolder oldHolder;
    private View view;

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if(oldHolder!=null){
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder=null;
                    }else if (view != null) {
                        ViewHolder holder = (ViewHolder) view.getTag();
                        scrollX= holder.scrollView.getScrollX();// 获得HorizontalScrollView滑动的水平方向值.
                        holder.scrollView.smoothScrollTo(0, 0);
                    }
                }
                case MotionEvent.ACTION_UP: {
                    if(oldHolder!=null){
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder=null;
                    }else{
                        ViewHolder holder = (ViewHolder) v.getTag();
                        view = v;
                        int actionW = holder.menu.getWidth();//获得操作区域的长度
                        if (scrollX < actionW / 2) {
                            holder.scrollView.smoothScrollTo(0, 0);
                        } else {
                            holder.scrollView.smoothScrollTo(actionW, 0);
                            oldHolder=holder;
                        }
                    }

                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    if(oldHolder!=null){
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder=null;
                    }else{
                        ViewHolder holder = (ViewHolder) v.getTag();
                        view = v;
                        int actionW = holder.menu.getWidth();//获得操作区域的长度
                        if (scrollX < actionW / 2) {
                            holder.scrollView.smoothScrollTo(0, 0);
                        } else {
                            holder.scrollView.smoothScrollTo(actionW, 0);
                            oldHolder=holder;
                        }
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if(oldHolder!=null){
                        oldHolder.scrollView.smoothScrollTo(0, 0);
                        oldHolder=null;
                    }else {
                        ViewHolder holder = (ViewHolder) v.getTag();
                        scrollX = holder.scrollView.getScrollX();// 获得HorizontalScrollView滑动的水平方向值.
                    }
                }
            }
            return false;
        }
    };

    public interface ControlScheduleListener {
        void controlSchedule(int position, View v);
        void setScheduleEnable(String schedule);
    }

    public void setControlScheduleListener(ControlScheduleListener listener) {
        this.controlListener = listener;
    }

    public ControlScheduleListener controlListener;

    @Override
    public void onClick(View v) {
        if (view != null) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.scrollView.smoothScrollTo(0, 0);
        }
        controlListener.controlSchedule((Integer) v.getTag(), v);
    }

    private String getWeek(int week) {
        StringBuffer sb = new StringBuffer();
        if (week == 127) {
            sb.append(context.getString(R.string.weekdays));
        } else {
            String data = getWeek(Integer.toBinaryString(week));
            for (int i = 0; i < 7; i++) {
                if (data.charAt(i) == '1') {
                    sb.append(weeks[i]);
                    sb.append(" ");
                }
            }
        }
        return sb.toString().trim();
    }
    public String[] weeks = new String[]{"Sat", "Fri", "Thu", "Wed", "Tue", "Mon", "Sun"};
    DecimalFormat df1 = new DecimalFormat("00");

    String getWeek(String week) {
        int strLen = week.length();
        while (strLen < 7) {
            week = (new StringBuffer().append("0").append(week)).toString();// 左(前)补0
            strLen = week.length();
        }
        return week;
    }

    class ViewHolder {
        HorizontalScrollView scrollView;
        LinearLayout content,menu;
        TextView schedule_time,weeks;
        ImageView delete,runing;
        SwitchView run;
    }
}

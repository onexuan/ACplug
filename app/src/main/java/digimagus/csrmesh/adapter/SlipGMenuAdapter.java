package digimagus.csrmesh.adapter;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.GroupDevice;

/**
 * Group ListView item 侧滑菜单
 */
public class SlipGMenuAdapter extends BaseAdapter implements View.OnClickListener {
    private final static String TAG = "SlipMenuAdapter";
    private List<GroupDevice> groupInfos = new ArrayList<>();
    private Context context;
    private int screenWidth;
    ViewHolder holder;

    public SlipGMenuAdapter(Context context, List<GroupDevice> groupInfos, int screenWidth) {
        this.context = context;
        this.screenWidth = screenWidth;
        this.groupInfos = groupInfos;
    }

    public void notifyDataSetChanged(List<GroupDevice> groupInfos) {
        this.groupInfos = groupInfos;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return groupInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return groupInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final GroupDevice groupInfo = groupInfos.get(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_group_menu, null);

            holder.scrollView = (HorizontalScrollView) convertView.findViewById(R.id.scrollView);
            holder.content = (LinearLayout) convertView.findViewById(R.id.content);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.menu = (LinearLayout) convertView.findViewById(R.id.menu);
            holder.setting = (ImageView) convertView.findViewById(R.id.setting);
            holder.delete = (ImageView) convertView.findViewById(R.id.delete);
            holder.device_state = (ImageView) convertView.findViewById(R.id.device_state);
            holder.right = (RelativeLayout) convertView.findViewById(R.id.right);

            ViewGroup.LayoutParams lp = holder.content.getLayoutParams();
            lp.width = screenWidth;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setText(groupInfos.get(position).getName());

        holder.right.setEnabled(true);
        holder.setting.setTag(position);
        holder.delete.setTag(position);
        holder.right.setTag(position);

        convertView.setOnTouchListener(onTouchListener);
        holder.setting.setOnClickListener(this);
        holder.delete.setOnClickListener(this);
        holder.right.setOnClickListener(this);

        if(groupInfo.activated&&groupInfo.online){
            return convertView;
        }
        if (groupInfo.online) {
            holder.device_state.setImageResource(groupInfo.status?R.mipmap.icon_plugon: R.mipmap.icon_plugoff);
        } else {
            holder.device_state.setImageResource(R.mipmap.icon_plugoffline);
        }
        convertView.setBackgroundResource(groupInfo.online ? R.drawable.light_item_bg : R.drawable.light_item_grybg);
        holder.right.findViewById(R.id.send_msg).setVisibility(View.INVISIBLE);
        return convertView;
    }

    private int scrollX=0;
    private ViewHolder oldHolder;
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
                    ViewHolder holder = (ViewHolder) v.getTag();
                    scrollX = holder.scrollView.getScrollX();// 获得HorizontalScrollView滑动的水平方向值.
                }
            }
            return false;
        }
    };

    public void setControlGroupListener(ControlGroupListener listener) {
        this.controlListener = listener;
    }

    public ControlGroupListener controlListener;

    public interface ControlGroupListener {
        void controlGroup(int position, View v);
    }

    @Override
    public void onClick(View v) {
        if (view != null) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.scrollView.smoothScrollTo(0, 0);
        }
        controlListener.controlGroup((Integer) v.getTag(), v);
    }

    class ViewHolder {
        HorizontalScrollView scrollView;
        LinearLayout content, menu;
        TextView title;
        ImageView setting;
        ImageView delete;
        ImageView device_state;
        RelativeLayout right;
    }

    private View view;
}

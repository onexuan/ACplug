package digimagus.csrmesh.adapter;

import android.content.Context;
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

import java.util.List;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.DeviceInfo;

/**
 * Device ListView item 侧滑菜单
 */
public class SlipDMenuAdapter extends BaseAdapter implements View.OnClickListener {

    public final static String TAG="SlipDMenuAdapter";

    private Context mContext;
    private List<DeviceInfo> deviceInfos;
    private int screenWidth;

    public SlipDMenuAdapter(Context context, List<DeviceInfo> info, int screenWidth) {
        this.mContext = context;
        this.screenWidth = screenWidth;
        this.deviceInfos = info;
    }

    public void notifyDataSetChanged(List<DeviceInfo> info) {
        this.deviceInfos = info;
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return deviceInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeviceInfo info = deviceInfos.get(position);
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_device_menu, null);
            holder.scrollView = (HorizontalScrollView) convertView.findViewById(R.id.scrollView);
            holder.content = (LinearLayout) convertView.findViewById(R.id.content);
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.power = (TextView) convertView.findViewById(R.id.power);
            holder.menu = (LinearLayout) convertView.findViewById(R.id.menu);
            holder.setting = (ImageView) convertView.findViewById(R.id.setting);
            holder.delete = (ImageView) convertView.findViewById(R.id.delete);
            holder.device_state = (ImageView) convertView.findViewById(R.id.device_state);
            holder.right = (RelativeLayout) convertView.findViewById(R.id.right);
            holder.timeleft = (TextView) convertView.findViewById(R.id.timeleft);
            ViewGroup.LayoutParams lp = holder.content.getLayoutParams();
            lp.width = screenWidth;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.device_state.setVisibility(View.VISIBLE);

        holder.title.setText(info.getName());
        holder.power.setText(mContext.getString(R.string.power, info.power));

        holder.right.setEnabled(true);
        holder.setting.setTag(position);
        holder.delete.setTag(position);
        holder.right.setTag(position);
        holder.content.setTag(position);

        convertView.setOnTouchListener(onTouchListener);
        holder.setting.setOnClickListener(this);
        holder.delete.setOnClickListener(this);
        holder.right.setOnClickListener(this);
        holder.content.setOnClickListener(this);
        if (info.activated && info.online) {
            return convertView;
        }
        if (info.online) {
            holder.device_state.setImageResource(info.state == 1 ? (info.power > 0 ? R.mipmap.icon_operating : R.mipmap.icon_plugon) : R.mipmap.icon_plugoff);
        } else {
            holder.device_state.setImageResource(R.mipmap.icon_plugoffline);
        }
        convertView.setBackgroundResource(info.online ? R.drawable.light_item_bg : R.drawable.light_item_grybg);
        if (info.remaining > 0 && info.online) {
            holder.timeleft.setVisibility(View.VISIBLE);
            holder.timeleft.setText(mContext.getString(R.string.timeleft, info.remaining / 60, info.remaining % 60));
        } else {
            holder.timeleft.setVisibility(View.INVISIBLE);
        }
        holder.right.findViewById(R.id.send_msg).setVisibility(View.INVISIBLE);
        return convertView;
    }

    private int scrollX = 0;
    private ViewHolder oldHolder;
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

                        Log.e(TAG,"    "+actionW);


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


    public void setControlDeviceListener(ControlDeviceListener listener) {
        this.controlListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (view != null) {
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.scrollView.smoothScrollTo(0, 0);
        }
        controlListener.controlDevice(deviceInfos.get((Integer) v.getTag()), v);
    }


    public ControlDeviceListener controlListener;

    public interface ControlDeviceListener {
        void controlDevice(DeviceInfo device, View v);
    }

    class ViewHolder {
        HorizontalScrollView scrollView;
        LinearLayout content, menu;
        TextView title, power, timeleft;
        ImageView setting;
        ImageView delete;
        ImageView device_state;
        RelativeLayout right;
    }

    private View view;
}

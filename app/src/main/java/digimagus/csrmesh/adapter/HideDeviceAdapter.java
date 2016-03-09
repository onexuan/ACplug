package digimagus.csrmesh.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.HideDeviceInfo;

/**
 * 隐藏设备的 适配器
 */
public class HideDeviceAdapter extends BaseAdapter{

    private List<HideDeviceInfo> hideInfos;
    private Context context;

    public HideDeviceAdapter(Context context,List<HideDeviceInfo> hideInfos){
        this.context=context;
        this.hideInfos=hideInfos;
    }

    public void notifyDataSetChanged(List<HideDeviceInfo> hideInfos) {
        this.hideInfos=hideInfos;
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return hideInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return hideInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HideDeviceInfo info=hideInfos.get(position);
        ViewHolder holder;
        if(convertView==null){
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_hide_device, null);
            holder.name= (TextView) convertView.findViewById(R.id.name);
            holder.choose= (TextView) convertView.findViewById(R.id.choose);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.name.setText(info.getName());
        if(info.choose){
            holder.choose.setBackgroundResource(R.drawable.choose);
        }else{
            holder.choose.setBackgroundResource(R.drawable.unchoose);
        }
        return convertView;
    }


    class ViewHolder {
        TextView name,choose;
    }
}

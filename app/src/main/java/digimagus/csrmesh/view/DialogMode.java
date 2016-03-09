package digimagus.csrmesh.view;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import digimagus.csrmesh.acplug.R;
import digimagus.csrmesh.entities.DialogModeInfo;

public class DialogMode extends Dialog implements View.OnClickListener,AdapterView.OnItemClickListener {

    private DialogClickListener listener;
    private String title;

    private Context context;
    private List<DialogModeInfo> dialogInfos=new ArrayList<>();
    private DialogListAdapter listAdapter=new DialogListAdapter();

    public interface DialogClickListener{
        void onClick(DialogMode context,List<DialogModeInfo> dialogs);
    }

    public void setDialogModeListener(DialogClickListener listene,List<DialogModeInfo> infos){
        this.dialogInfos=infos;
        this.listener=listene;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(dialogInfos.get(position).isState()){
            dialogInfos.get(position).setState(false);
        }else{
            dialogInfos.get(position).setState(true);
        }
        listAdapter.notifyDataSetChanged();
    }

    private class DialogListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return dialogInfos.size();
        }
        @Override
        public Object getItem(int position) {
            return dialogInfos.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DialogModeInfo info=dialogInfos.get(position);
            ViewHolder holder;
            if(convertView==null){
                convertView= LayoutInflater.from(context).inflate(R.layout.item_dialogmode,null);
                holder=new ViewHolder();
                holder.dialog_title=(TextView)convertView.findViewById(R.id.dialog_title);
                holder.dialog_stat=(TextView)convertView.findViewById(R.id.dialog_stat);
                convertView.setTag(holder);
            }else{
                holder=(ViewHolder)convertView.getTag();
            }
            holder.dialog_title.setText(info.getItem_title());
            holder.dialog_stat.setBackgroundResource(info.isState()?R.drawable.dialog_selected:R.drawable.dialog_unselect);
            return convertView;
        }
    }

    class ViewHolder{
        public TextView dialog_title;
        public TextView dialog_stat;
    }

    public DialogMode(Context context, int theme,String title) {
        super(context, theme);
        this.title=title;
        this.context=context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_mode);
        initFindView();
    }

    private void initFindView() {
        dialog_title= (TextView) findViewById(R.id.dialog_title);
        dialog_list= (ListView) findViewById(R.id.dialog_list);
        cancel=(TextView) findViewById(R.id.cancel);
        confirm=(TextView) findViewById(R.id.confirm);
        cancel.setOnClickListener(this);
        confirm.setOnClickListener(this);


        dialog_title.setText(title);
        dialog_list.setAdapter(listAdapter);
        dialog_list.setOnItemClickListener(this);
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.cancel:
                dismiss();
                break;
            case R.id.confirm:
               /* String data="";
                String week2="";
                for (int i=0;i<dialogInfos.size();i++){
                    data=data+(dialogInfos.get(i).isState()?dialogInfos.get(i).getItem_title().toString().substring(0,3)+" ":"");
                    week2=week2+(dialogInfos.get(i).isState()?"1":"0");
                }
                for (DialogModeInfo mode:dialogInfos){
                    Log.e(TAG,"     "+mode.isState());
                }*/
                listener.onClick(this,dialogInfos);
                dismiss();
                break;
        }
    }

    private static final String TAG="DialogMode";
    private TextView dialog_title,cancel,confirm;
    private ListView dialog_list;
}

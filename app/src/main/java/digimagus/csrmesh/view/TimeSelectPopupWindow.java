package digimagus.csrmesh.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.lang.reflect.Field;

import digimagus.csrmesh.acplug.R;

/**
 * 日期选择
 */
public class TimeSelectPopupWindow extends PopupWindow {
    private View mMenuView;
    private TextView done;
    private NumberPicker hour, minute, am_pm, value1, value2;
    private ChooseTimeListener chooseTimeListener;
    String[] times = {"am", "pm"};
    String[] values = {" ", " "};
    String[] minutes = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
            "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59"};

    public interface ChooseTimeListener {
        void onChooseTime(int hour, int minute, int am, View clickView);
    }

    private void setNumberPickerDividerColor(Context context, NumberPicker numberPicker) {
        NumberPicker picker = numberPicker;
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                try {//设置分割线的颜色值
                    pf.setAccessible(true);
                    pf.set(picker, new ColorDrawable(context.getResources().getColor(R.color.transparent)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public TimeSelectPopupWindow(Context context) {
        super(context);
        chooseTimeListener = (ChooseTimeListener) context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMenuView = inflater.inflate(R.layout.dialog_select_time, null);
        done = (TextView) mMenuView.findViewById(R.id.done);

        hour = (NumberPicker) mMenuView.findViewById(R.id.hour);
        minute = (NumberPicker) mMenuView.findViewById(R.id.minute);
        am_pm = (NumberPicker) mMenuView.findViewById(R.id.am_pm);
        value1 = (NumberPicker) mMenuView.findViewById(R.id.value1);
        value2 = (NumberPicker) mMenuView.findViewById(R.id.value2);


        setNumberPickerDividerColor(context, hour);
        setNumberPickerDividerColor(context, minute);
        setNumberPickerDividerColor(context, am_pm);
        setNumberPickerDividerColor(context, value1);
        setNumberPickerDividerColor(context, value2);

        hour.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minute.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        am_pm.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        value1.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        value2.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        hour.setMaxValue(23);
        hour.setMinValue(0);
        minute.setMaxValue(59);
        minute.setMinValue(0);
        am_pm.setMinValue(0);
        am_pm.setMaxValue(1);
        hour.setDisplayedValues(minutes);
        minute.setDisplayedValues(minutes);
        value1.setDisplayedValues(values);
        value2.setDisplayedValues(values);
        am_pm.setDisplayedValues(times);

        //取消按钮
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chooseTimeListener.onChooseTime(hour.getValue(), minute.getValue(), am_pm.getValue(), clickView);
                dismiss();
            }
        });

        //设置SelectPicPopupWindow的View
        this.setContentView(mMenuView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.PopupAnimation);
        //实例化一个ColorDrawable颜色为透明
        ColorDrawable dw = new ColorDrawable(0x00000000);
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);
        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mMenuView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int height = mMenuView.findViewById(R.id.pop_layout).getTop();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    }
                }
                return true;
            }
        });
        hour.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (newVal >= 12) {
                    am_pm.setValue(1);
                } else {
                    am_pm.setValue(0);
                }
            }
        });
        am_pm.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (newVal == 0) {
                    if (hour.getValue() >= 12) {
                        hour.setValue(hour.getValue() - 12);
                    }
                } else {
                    if (hour.getValue() < 12) {
                        hour.setValue(hour.getValue() + 12);
                    }
                }
            }
        });
    }

    private View clickView;

    public void showAtLocation(View parent, int gravity, int hour, int minute, int am, View view) {
        clickView = view;
        this.hour.setValue(hour);
        this.minute.setValue(minute);
        this.am_pm.setValue(am);
        super.showAtLocation(parent, gravity, 0, 0);
    }
}

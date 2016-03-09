package digimagus.csrmesh.view;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;

/**
 * 下拉刷新
 */
public class SSwipeRefreshLayout extends SwipeRefreshLayout {
    private final static String TAG="SSwipeRefreshLayout";
    private float y1 = 0,y2 = 0;
    private View view;
    public SSwipeRefreshLayout(Context context) {
        super(context);
    }
    public SSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void setViewGroup(View view) {
        this.view = view;
    }
    @Override
    public boolean canChildScrollUp() {
        if (view != null && view instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) view;
            return absListView.getChildCount() > 0
                    && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                    .getTop() < absListView.getPaddingTop());
        }
        return super.canChildScrollUp();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y1 = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                y2 = ev.getY();
                if(y2 - y1 < 200){
                    return false;
                }
        }
        super.onTouchEvent(ev);
        return true;
    }
}

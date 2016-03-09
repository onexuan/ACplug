package digimagus.csrmesh.view;

import java.util.List;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public class MyPagerAdapter extends PagerAdapter {

	private List<View> mListView;

	public MyPagerAdapter(List<View> mListView) {
		super();
		this.mListView = mListView;
	}


	// 销毁position位置的界面
	public void destroyItem(View arg0, int arg1, Object arg2) {
		((ViewGroup) arg0).removeView(mListView.get(arg1));
	}

	// //获取当前窗体界面数
	public int getCount() {
		return mListView.size();
	}

	// 初始化position位置的界面
	@Override
	public Object instantiateItem(View arg0, int arg1) {
		((ViewGroup) arg0).addView(mListView.get(arg1), 0);
		return mListView.get(arg1);
	}
	// 判断是否由对象生成界面
	public boolean isViewFromObject(View arg0, Object arg1) {
		return arg0 == (arg1);
	}
	@Override
	public void restoreState(Parcelable arg0, ClassLoader arg1) {}
	@Override
	public Parcelable saveState() {
		return null;
	}
}

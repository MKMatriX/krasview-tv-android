package ru.krasview.kvlib.interfaces;

public interface ViewPropotionerInterface {
	public void setViewProposeListener(ViewProposeListener listener);
	public ViewProposeListener getViewProposeListener();
	public void init();
	public void refresh();
	public void enter();
	public void exit();
}

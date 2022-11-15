package ru.krasview.kvlib.interfaces;

import android.content.Context;
import android.view.View;

import java.util.Map;

public interface Factory {
	public View getView(Map<String, Object> map, Context context);
}

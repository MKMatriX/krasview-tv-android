package ru.krasview.tv;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.example.kvlib.R;

import ru.krasview.kvlib.indep.ListAccount;

public class PrMainActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("Krasview/corePr", "onCreate");

		if(ListAccount.fromLauncher) {
			getActionBar().setIcon(R.drawable.kv_logo);
			getActionBar().setLogo(R.drawable.kv_logo);
			getActionBar().setTitle("Настройки");
		}

		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, new PrFragment()).commit();
	}
}

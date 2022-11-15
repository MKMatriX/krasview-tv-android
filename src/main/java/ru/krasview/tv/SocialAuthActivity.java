package ru.krasview.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.regex.Pattern;

import ru.krasview.kvlib.indep.AuthAccount;
import ru.krasview.kvlib.indep.HTTPClient;
import ru.krasview.kvlib.indep.Parser;
import ru.krasview.kvlib.indep.consts.IntentConst;
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener;
import ru.krasview.kvsecret.secret.ApiConst;

public class SocialAuthActivity extends Activity {
	WebView wv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.social_auth_activity);

		wv = (WebView)findViewById(R.id.webView1);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36 Kadu");
		wv.setWebViewClient(new CustomWebViewClient());
		if(getIntent().hasExtra("address")) {
			wv.loadUrl(getIntent().getExtras().getString("address"));
		} else {
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		HTTPClient.setContext(this);
	}

	private class CustomWebViewClient extends WebViewClient {
		@SuppressWarnings("deprecation")
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			final Uri uri = Uri.parse(url);
			return handleUri(view, uri);
		}

		@RequiresApi(Build.VERSION_CODES.N)
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			final Uri uri = request.getUrl();
			return handleUri(view, uri);
		}

		private boolean handleUri(WebView view, final Uri uri) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				CookieSyncManager.createInstance(getApplication());
			}
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.setAcceptCookie(true);
			String cookie = cookieManager.getCookie("hlamer.ru");
			Log.i("Krasview/Oauth", "cookie " +  cookie + "; address: " + uri.getSchemeSpecificPart());
			if(cookie!=null) {
				String[] x = Pattern.compile(";").split(cookie);
				String hash = "";
				for (int i = 0; i < x.length; i++) {
					if (x[i].contains("user=")) hash = x[i].replaceFirst("user=", "").trim();
				}
				Log.i("Krasview/Oauth", "hash " +  hash);
				if (hash == "") SocialAuthActivity.this.finish();

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				prefs.edit().putString("pref_hash", hash).commit();

				final String get_user_info = ApiConst.GET_USER_INFO;
				HTTPClient.getXMLAsync(get_user_info, "hash="+hash ,new OnLoadCompleteListener() {
					@Override
					public void loadComplete(String result) {
					}

					@Override
					public void loadComplete(String address, String result) {
						if(address.equals(get_user_info)) {
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
							Document mDocument = Parser.XMLfromString(result);
							if (mDocument == null) { return; }
							mDocument.normalizeDocument();
							Node node = mDocument.getElementsByTagName("user").item(0);
							Element user = (Element)node;
							String name = user.getElementsByTagName("name").item(0).getTextContent();
							String tv_hash = user.getElementsByTagName("tv_hash").item(0).getTextContent();
							prefs.edit().putString("pref_login", name).putString("pref_hash_tv", tv_hash).commit();
							//Log.i("Debug", "получен тв хеш с красвью " + tv_hash);
						}
					}
				});

				prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				prefs.edit().putString("pref_login", "")
					.putString("pref_password", "")
					.putInt("pref_auth_type", AuthAccount.AUTH_TYPE_KRASVIEW_SOCIAL)
					.commit();
				Intent a = new Intent(IntentConst.ACTION_MAIN_ACTIVITY);
				startActivity(a);
				SocialAuthActivity.this.setResult(SocialAuthActivity.RESULT_OK);
				SocialAuthActivity.this.finish();
			} else if(uri.getQueryParameter("error") != null) {
				SocialAuthActivity.this.finish();
			} else {
				view.loadUrl(uri.getScheme() + ":" + uri.getSchemeSpecificPart());
			}

			return true;
		}
	};
}

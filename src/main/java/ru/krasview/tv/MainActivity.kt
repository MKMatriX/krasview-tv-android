package ru.krasview.tv

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.widget.FrameLayout
import com.example.kvlib.R
import ru.krasview.kvlib.animator.NewAnimator
import ru.krasview.kvlib.indep.*
import ru.krasview.kvlib.indep.KVHttpClient.Companion.getXMLAsync
import ru.krasview.kvlib.indep.consts.IntentConst
import ru.krasview.kvlib.indep.consts.RequestConst
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener
import ru.krasview.kvlib.interfaces.PropotionerView
import ru.krasview.kvlib.widget.List
import ru.krasview.kvlib.widget.NavigationViewFactory

class MainActivity : KVSearchAndMenuActivity() {
    var animator: NewAnimator? = null
    var start = TypeConsts.MAIN
    var layout: FrameLayout? = null

    //OnCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        val intent = intent
        ListAccount.fromLauncher =
            intent != null && intent.action != null && intent.action == IntentConst.ACTION_FROM_LAUNCHER
        super.onCreate(savedInstanceState)
        if (ListAccount.fromLauncher) {
            overridePendingTransition(
                ru.krasview.tv.R.anim.anim_enter_right,
                ru.krasview.tv.R.anim.anim_leave_left
            )
        }

        //Здесь обработка запуска из ланчера, поэтому здесь идет
        //сравнение со старыми адресами
        if (ListAccount.fromLauncher) {
            when (getIntent().extras!!.getString("address")) {
                ru.krasview.kvsecret.secret.ApiConst.TV -> {
                    start = TypeConsts.TV
                    supportActionBar!!.title = "Телевидение"
                }
                ru.krasview.kvsecret.secret.ApiConst.OLD_ALL_SHOW -> {
                    start = TypeConsts.ALL_SHOW
                    supportActionBar!!.title = "Сериалы"
                }
                ru.krasview.kvsecret.secret.ApiConst.OLD_ALL_ANIME -> {
                    start = TypeConsts.ALL_ANIME
                    supportActionBar!!.title = "Аниме"
                }
                ru.krasview.kvsecret.secret.ApiConst.OLD_ALL_MOVIE -> {
                    start = TypeConsts.ALL_MOVIE
                    supportActionBar!!.title = "Фильмы"
                }
            }
        }
        styleActionBar()
        setContentView(R.layout.activity_main_new)
        layout = findViewById<View>(R.id.root) as FrameLayout
        animator = NewAnimator(this, NavigationViewFactory())
        layout!!.addView(animator)
        setSearchWidget()
        styleBackground()
        packetAndStart
    }

    var pd: ProgressDialog? = null

    //получение данных о подключенном пакете
    private val packetAndStart: Unit
        get() {
            getPrefs()
            if (ListAccount.fromLauncher) {
                animator!!.init(start)
                return
            }
            //получение данных о подключенном пакете
            pd = ProgressDialog(this)
            pd!!.setTitle("Подождите")
            pd!!.setCancelable(false)
            if (!ListAccount.fromLauncher) {
                pd!!.show()
            }
            getXMLAsync(
                ru.krasview.kvsecret.secret.ApiConst.USER_PACKET, "hash=" + AuthAccount.getInstance().tvHash,
                object : OnLoadCompleteListener {
                    override fun loadComplete(result: String) {
                        if (!ListAccount.fromLauncher) {
                            pd!!.dismiss()
                        }
                        if (result != "Бесплатный") {
                            HeaderAccount.hh()
                        }
                        animator!!.init(start)
                    }

                    override fun loadComplete(address: String, result: String) {}
                })
        }

    //настройка actionbar-a
    private fun styleActionBar() {
        if (ListAccount.fromLauncher) {
            supportActionBar!!.setBackgroundDrawable(resources.getDrawable(android.R.drawable.dark_header))
            supportActionBar!!.setIcon(R.drawable.kv_logo)
            supportActionBar!!.setLogo(R.drawable.kv_logo)
        } else {
            supportActionBar!!.setBackgroundDrawable(resources.getDrawable(R.drawable.action_bar_background))
        }
    }

    //настройка фона
    private fun styleBackground() {
        if (ListAccount.fromLauncher) {
            val grad = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(
                    resources.getColor(R.color.black_2),  //светлый в центре
                    resources.getColor(R.color.black_1)
                )
            ) //темный с краю
            grad.gradientType = GradientDrawable.RADIAL_GRADIENT
            grad.gradientRadius = 100f
            grad.setGradientCenter(0.5f, 0.5f)
            layout!!.setBackgroundDrawable(grad)
            val observer = layout!!.viewTreeObserver
            observer.addOnGlobalLayoutListener { grad.gradientRadius = layout!!.width.toFloat() }
        } else {
            layout!!.setBackgroundColor(Color.rgb(20, 20, 20))
        }
    }

    var prefs: SharedPreferences? = null
    private fun getPrefs() {
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        var tv_pl = prefs?.getString("video_player_tv", "std")
        if (tv_pl == "Стандартный плеер") {
            prefs?.edit()?.putString("video_player_tv", "std")?.commit()
        }
        tv_pl = prefs?.getString("video_player_serial", "std")
        if (tv_pl == "Стандартный плеер") {
            prefs?.edit()?.putString("video_player_serial", "std")?.commit()
        }
        AuthAccount.getInstance().setType(
            prefs?.getInt("pref_auth_type", AuthAccount.AUTH_TYPE_UNKNOWN)
                ?: AuthAccount.AUTH_TYPE_UNKNOWN
        )
        if (account.isUnknownAccount) {
            prefs?.edit()?.putBoolean("pref_now_logout", true)?.commit()
            val a = Intent(this, MainAuthActivity::class.java)
            startActivity(a)
            finish()
            return
        }
        prefs?.edit()?.putBoolean("pref_now_logout", false)
            ?.putInt("pref_last_interface", MainAuthActivity.INTERFACE_KRASVIEW)
            ?.commit()
        HTTPClient.setContext(this)
        HTTPClient.exitListener = null
        AuthAccount.getInstance().login = prefs?.getString("pref_login", "")
        AuthAccount.getInstance().password = prefs?.getString("pref_password", "")
        AuthAccount.getInstance().hash = prefs?.getString("pref_hash", "1")
        AuthAccount.getInstance().tvHash = prefs?.getString("pref_hash_tv", "1")
    }

    private var pref_orientation: String? = "default"
    public override fun onResume() {
        Log.d("Krasview/core", "onResume")
        super.onResume()
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        pref_orientation = prefs?.getString("orientation", "default")
        when (pref_orientation) {
            "default" -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            "album" -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            "book" -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    override fun onBackPressed() {
        Log.d("Krasview/core", "onBackPressed")
        super.onBackPressed()
        if (ListAccount.fromLauncher) {
            overridePendingTransition(
                ru.krasview.tv.R.anim.anim_enter_left,
                ru.krasview.tv.R.anim.anim_leave_right
            )
        }
    }

    public override fun onPause() {
        Log.d("Krasview/core", "onPause")
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        when (requestCode) {
            RequestConst.REQUEST_CODE_BILLING -> (animator!!.currentView as PropotionerView).enter()
            RequestConst.REQUEST_CODE_VIDEO -> {
                if (data == null) {
                    return
                }
                val index = data.getIntExtra("index", 0)
                ListAccount.currentList.setSelection(index + ((ListAccount.currentList as List).adapter?.constDataCount ?: 0))
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && ListAccount.fromLauncher && animator!!.hasFocus()) {
            return dispatchKeyEvent(KeyEvent(event.action, KeyEvent.KEYCODE_BACK))
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            val result: Boolean = if (animator!!.visibility == View.VISIBLE) {
                animator!!.dispatchKeyEvent(event)
            } else {
                searchFragment!!.dispatchKeyEvent(event)
            }
            if (result) {
                return result
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onError() {
        exit()
    }

    override fun requestFocus() {
        animator!!.requestFocus()
    }

    override fun exit() {
        HeaderAccount.shh()
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        prefs?.edit()?.putBoolean("pref_now_logout", true)?.putString("pref_hash", "")
            ?.putString("pref_hash_tv", "")?.commit()
        CookieSyncManager.createInstance(this.application)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookie()
        val a = Intent(this, MainAuthActivity::class.java)
        startActivity(a)
        finish()
    }

    override fun home() {
        if (ListAccount.fromLauncher) {
            finish()
            return
        }
        if (animator!!.visibility == View.VISIBLE) {
            animator!!.home()
        } else {
            editsearch!!.setText("")
            editsearch!!.clearFocus()
            SearchAccount.search_string = null
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editsearch!!.windowToken, 0)
            searchFragment!!.home()
        }
    }

    override fun refresh() {
        if (animator!!.visibility == View.VISIBLE) {
            animator!!.refresh()
        } else {
            searchFragment!!.refresh()
        }
    }

    override fun setSearch(a: Boolean) {
        if (a) {
            animator!!.visibility = View.GONE
            searchHost!!.visibility = View.VISIBLE
        } else {
            animator!!.visibility = View.VISIBLE
            searchHost!!.visibility = View.GONE
        }
    }
}
package ru.krasview.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import org.w3c.dom.Document
import ru.krasview.kvlib.indep.AuthAccount
import ru.krasview.kvlib.indep.HTTPClient
import ru.krasview.kvlib.indep.KVHttpClient.Companion.getImage
import ru.krasview.kvlib.indep.KVHttpClient.Companion.getXMLAsync
import ru.krasview.kvlib.indep.Parser
import ru.krasview.kvlib.indep.consts.IntentConst
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener
import java.net.URLEncoder

class MainAuthActivity : Activity() {
    //	public final static int AUTH_TYPE_UNKNOWN = AuthEnterConsts.AUTH_TYPE_UNKNOWN;//без логина, как гость
    //	public final static int AUTH_TYPE_TV = AuthEnterConsts.AUTH_TYPE_TV;//как абонент красноярской сети
    //	public final static int AUTH_TYPE_KRASVIEW = AuthEnterConsts.AUTH_TYPE_KRASVIEW;//как пользователь krasview
    //	public final static int AUTH_TYPE_GUEST = AuthEnterConsts.AUTH_TYPE_GUEST;//как неавторизованный пользователь
    //	public final static int AUTH_TYPE_KRASVIEW_SOCIAL = AuthEnterConsts.AUTH_TYPE_KRASVIEW_SOCIAL;//как пользователь krasview через социальную сеть */
    private val REQUEST_CODE_GUEST = 0
    private val REQUEST_CODE_SOCIAL = 1
    private var kraslan_login = "" //логин(номер счета) для красноярской сети
    private var login //("pref_login")//сохраненный логин
            : String? = null
    private var password //("pref_password")//сохраненный пароль
            : String? = null
    private var logout = false

    //элементы разметки
    var edit_login: EditText? = null
    var edit_password: EditText? = null
    var button_enter: Button? = null
    var button_kraslan: Button? = null
    var button_registration: Button? = null
    var button_help: ImageButton? = null
    var social_grid: GridView? = null

    //интенты для вызова активити
    var krasviewIntent: Intent? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HTTPClient.setContext(this)
        krasviewIntent = Intent(IntentConst.ACTION_MAIN_ACTIVITY)
        setContentView(R.layout.kv_activity_auth_small)
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        logout = prefs!!.getBoolean("pref_now_logout", true)
        fastAuth(!logout)
        initLayout()
    }

    override fun onResume() {
        super.onResume()
        HTTPClient.setContext(this)
    }

    private fun fastAuth(fast: Boolean) {
        if (!fast) {
            prefs!!.edit().putInt("pref_auth_type", AuthAccount.AUTH_TYPE_UNKNOWN).commit()
            return
        }
        var local: Intent? = null
        local = krasviewIntent
        startActivity(local)
        finish()
    }

    public override fun onStart() {
        super.onStart()
        authPrefs
    }

    private val authPrefs: Unit
        private get() {
            prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
            auth_type = prefs!!.getInt("pref_auth_type", AuthAccount.AUTH_TYPE_UNKNOWN)
            logout = prefs!!.getBoolean("pref_now_logout", true)
            login = prefs!!.getString("pref_login", "")
            edit_login!!.setText(login)
            password = prefs!!.getString("pref_password", "")
            val kraslan_addr = ru.krasview.kvsecret.secret.ApiConst.TV_AUTH
            val oauth_api_addr = ru.krasview.kvsecret.secret.ApiConst.KRASVIEW_OAUTH
            val listener: OnLoadCompleteListener = object : OnLoadCompleteListener {
                override fun loadComplete(result: String) {}
                override fun loadComplete(address: String, result: String) {
                    if (address == kraslan_addr) {
                        checkKraslanLogin(result)
                    } else if (address == oauth_api_addr) {
                        checkSocialButton(result)
                    }
                }
            }
            getXMLAsync(kraslan_addr, "", listener)
            getXMLAsync(oauth_api_addr, "", listener)
        }

    private fun checkKraslanLogin(str: String) {
        if (str == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
            return
        }
        kraslan_login = str
        if (kraslan_login == "") {
            return
        }
        button_kraslan!!.visibility = View.VISIBLE
        button_kraslan!!.text = "Абонент Красноярской сети"
    }

    private fun checkSocialButton(str: String) {
        if (str == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
            return
        }
        if (str == "") {
            return
        }
        social_grid!!.adapter = SocialButtonAdapter()
        adjustGridView()
        social_grid!!.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val m = parent.getItemAtPosition(position) as Map<String, Any?>
            val socialIntent = Intent(this@MainAuthActivity, SocialAuthActivity::class.java)
            socialIntent.putExtra("address", m["url"] as String?)
            this@MainAuthActivity.startActivityForResult(socialIntent, REQUEST_CODE_SOCIAL)
        }
        (social_grid!!.adapter as SocialButtonAdapter).addData(str)
    }

    private fun adjustGridView() {
        social_grid!!.numColumns = GridView.AUTO_FIT
        social_grid!!.columnWidth = 48
        social_grid!!.verticalSpacing = 3
        social_grid!!.horizontalSpacing = 15
    }

    private inner class SocialButtonAdapter : BaseAdapter() {
        var mData = ArrayList<Map<String, Any?>>()
        fun addData(xml: String?) {
            setDataTask().execute(xml)
        }

        @SuppressLint("StaticFieldLeak")
        private inner class setDataTask : AsyncTask<String, HashMap<String, Any?>, Void?>() {
            override fun onProgressUpdate(vararg progress: HashMap<String, Any?>?) {
                progress[0]?.let { mData.add(it) }
                notifyDataSetChanged()
            }

            override fun onPostExecute(result: Void?) {}
            protected override fun doInBackground(vararg params: String): Void? {
                var m: MutableMap<String, Any?>
                val mDocument: Document?
                mDocument = Parser.XMLfromString(params[0])
                if (mDocument == null) {
                    return null
                }
                mDocument.normalizeDocument()
                val nListChannel = mDocument.getElementsByTagName("unit")
                val numOfChannel = nListChannel.length
                for (nodeIndex in 0 until numOfChannel) {
                    val locNode = nListChannel.item(nodeIndex)
                    m = HashMap()
                    m["title"] = Html.fromHtml(Parser.getValue("title", locNode))
                    val image_uri = Parser.getValue("image", locNode)
                    m["image_uri"] = image_uri
                    m["image"] = getImage(image_uri)
                    m["url"] = Parser.getValue("url", locNode)
                    publishProgress(m)
                }
                return null
            }
        }

        override fun getCount(): Int {
            return mData.size
        }

        override fun getItem(position: Int): Map<String, Any?> {
            return mData[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = ImageView(this@MainAuthActivity)
            v.setImageBitmap(getItem(position)["image"] as Bitmap?)
            val p = AbsListView.LayoutParams(48, 48)
            v.layoutParams = p
            return v
        }
    }

    private fun initLayout() {
        edit_login = findViewById<View>(R.id.login) as EditText
        edit_password = findViewById<View>(R.id.password) as EditText
        button_kraslan = findViewById<View>(R.id.kv_auth_kraslan_button) as Button
        social_grid = findViewById<View>(R.id.social_grid) as GridView
    }

    var enterProgressDialog: ProgressDialog? = null
    private fun enter() {
        enterProgressDialog = ProgressDialog(this)
        login = edit_login!!.text.toString()
        password = edit_password!!.text.toString()
        prefs!!.edit().putString("pref_login", login)
            .putString("pref_password", password)
            .putInt("pref_auth_type", AuthAccount.AUTH_TYPE_UNKNOWN)
            .putString("pref_hash", "1").putString("pref_hash_tv", "1").commit()
        if (login == "" || password == "") {
            val toast = Toast.makeText(
                applicationContext,
                "Логин и пароль не должны быть пустыми. Если у вас нет учетной записи krasview, зарегистрируйтесь или войдите как гость",
                Toast.LENGTH_SHORT
            )
            toast.show()
            return
        }
        enterProgressDialog!!.show()
        val auth_address_tv = ru.krasview.kvsecret.secret.ApiConst.TV_AUTH
        val auth_address_krasview = ru.krasview.kvsecret.secret.ApiConst.KRASVIEW_AUTH
        val listener: OnLoadCompleteListener = object : OnLoadCompleteListener {
            var tv = false
            var krasview = false
            var check_tv = false
            var check_krasview = false
            override fun loadComplete(result: String) {}
            override fun loadComplete(get_address: String, result: String) {
                if (get_address == auth_address_tv) {
                    if (result == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Невозможно подключиться к серверу, проверьте подключение, попробуйте позже",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        return
                    }
                    check_tv = true
                    tv =
                        if (result == "fail" || result == "auth failed" || result == "too many attempts" || result == "error" || result == "") {
                            false
                        } else {
                            prefs!!.edit().putString("pref_hash_tv", result).commit()
                            true
                        }
                }
                if (get_address == auth_address_krasview) {
                    if (result == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Невозможно подключиться к серверу, проверьте подключение, попробуйте позже",
                            Toast.LENGTH_SHORT
                        )
                        toast.show()
                        return
                    }
                    check_krasview = true
                    krasview = if (result == "error" || result == "too many attempts") {
                        false
                    } else {
                        prefs!!.edit().putString("pref_hash", result).commit()
                        true
                    }
                }
                if (check_tv && check_krasview) {
                    enterProgressDialog!!.dismiss()
                } else {
                    return
                }
                if (krasview) {
                    startActivity(krasviewIntent)
                    finish()
                    prefs!!.edit().putInt("pref_auth_type", AuthAccount.AUTH_TYPE_KRASVIEW).commit()
                }
                if (tv && !krasview) {
                    startActivity(krasviewIntent)
                    finish()
                    val toast = Toast.makeText(
                        applicationContext,
                        "Будут недоступны функции красвью, требующие авторизации ",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    prefs!!.edit().putInt("pref_auth_type", AuthAccount.AUTH_TYPE_TV)
                        .commit()
                }
                if (!tv && !krasview) {
                    val toast = Toast.makeText(
                        applicationContext,
                        "Ошибка авторизации, возможно, вы неправильно ввели логин или пароль ",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    edit_password!!.setText("")
                    return
                }
            }
        }
        getXMLAsync(
            auth_address_tv,
            "login=" + URLEncoder.encode(login) + "&password=" + URLEncoder.encode(password),
            listener
        )
        getXMLAsync(
            auth_address_krasview,
            "login=" + URLEncoder.encode(login) + "&password=" + URLEncoder.encode(password),
            listener
        )
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.kv_auth_enter_button -> enter()
            R.id.kv_auth_registration_button -> {
                val b = Intent(Intent.ACTION_VIEW)
                b.data = Uri.parse(ru.krasview.kvsecret.secret.ApiConst.CREATE_ACCOUNT)
                startActivity(b)
            }
            R.id.kv_auth_guest_button -> guestAuth()
            R.id.kv_auth_kraslan_button -> {
                prefs!!.edit().putString("pref_login", "")
                    .putString("pref_password", "")
                    .putInt("pref_auth_type", AuthAccount.AUTH_TYPE_TV).commit()
                fastAuth(true)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            REQUEST_CODE_GUEST, REQUEST_CODE_SOCIAL -> finish()
        }
    }

    private fun guestAuth() {
        if (!prefs!!.getBoolean("pref_guest_check", false)) {
            val c = Intent(this, GuestAuthActivity::class.java)
            this.startActivityForResult(c, REQUEST_CODE_GUEST)
        } else {
            prefs!!.edit().putString("pref_login", "")
                .putString("pref_password", "")
                .putInt("pref_auth_type", AuthAccount.AUTH_TYPE_GUEST).commit()
            val a = Intent(IntentConst.ACTION_MAIN_ACTIVITY)
            startActivity(a)
            finish()
        }
    }

    companion object {
        var prefs: SharedPreferences? = null

        //какой интерфейс был включен в прошлый раз
        const val INTERFACE_TV = 0 //телевидение(старый)
        const val INTERFACE_KRASVIEW = 1 //красвью(новый)
        var auth_type //("pref_auth_type")Предыдущий заход был при помощи:
                = 0
    }
}
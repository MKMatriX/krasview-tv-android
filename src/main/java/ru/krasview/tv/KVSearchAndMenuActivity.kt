package ru.krasview.tv

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.preference.PreferenceManager
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.example.kvlib.R
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.indep.AuthAccount
import ru.krasview.kvlib.indep.HTTPClient.getXML
import ru.krasview.kvlib.indep.ListAccount
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvlib.indep.consts.TagConsts
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvlib.interfaces.FatalErrorExitListener
import ru.krasview.kvlib.widget.List

abstract class KVSearchAndMenuActivity : AppCompatActivity(), SearchView.OnQueryTextListener,
    FatalErrorExitListener {
    var account = AuthAccount.getInstance()
    var editsearch: EditText? = null
    var searchFragment: SearchFragment? = null
    var searchHost: View? = null
    private val mSearchEditFrame: View? = null
    protected abstract fun setSearch(a: Boolean)
    protected abstract fun exit()
    protected abstract fun home()
    protected abstract fun refresh()
    protected abstract fun requestFocus()
    override fun onError() {}
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Krasview/core", "onCreate")
        super.onCreate(savedInstanceState)
    }

    protected fun setSearchWidget() {
        //установка поисковых штук
        searchHost = findViewById(R.id.search)
        val fragmentManager = supportFragmentManager
        searchFragment = fragmentManager.findFragmentById(R.id.fragment1) as SearchFragment?
        setSearch(false)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuSearch = menu.findItem(R.id.kv_search_item)
        val searchView = MenuItemCompat.getActionView(menuSearch) as SearchView
        //SearchView searchView = (SearchView) menuSearch.getActionView();
        if (searchView != null) {
            searchView.isFocusable = true
            searchView.setOnQueryTextListener(this)
        }
        MenuItemCompat.setOnActionExpandListener(
            menuSearch,
            object : MenuItemCompat.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    setSearch(true)
                    //editsearch.requestFocus();
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    setSearch(false)
                    /*editsearch.setText("");
	            editsearch.clearFocus();
	            SearchAccount.search_string = null;
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editsearch.getWindowToken(), 0);*/return true
                }
            })
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.kv_activity_animator, menu)
        val loginItem = menu.findItem(R.id.kv_login_item)
        var str = ""
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val auth_type = prefs.getInt("pref_auth_type", AuthAccount.AUTH_TYPE_UNKNOWN)
        val login = prefs.getString("pref_login", "")
        val password = prefs.getString("pref_password", "")
        when (auth_type) {
            AuthAccount.AUTH_TYPE_GUEST -> str = "Гость"
            AuthAccount.AUTH_TYPE_TV -> str = "Абонент"
            AuthAccount.AUTH_TYPE_UNKNOWN -> str = "Неизвестно"
        }
        val locLog = if (login == "") str else login!!
        loginItem.title = locLog
        if (ListAccount.fromLauncher) {
            menu.findItem(R.id.kv_search_item).isVisible = false
        }

        //requestFocus();
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.new_game) {
            val currentapiVersion = Build.VERSION.SDK_INT
            val settingsActivity: Intent
            settingsActivity = if (currentapiVersion >= 11) {
                Intent(baseContext, PrMainActivity::class.java)
            } else {
                Intent(baseContext, OldPreferenceActivity::class.java)
            }
            startActivity(settingsActivity)
            return true
        } else if (id == R.id.kv_login_item) {
            return true
        } else if (id == R.id.exit) {
            // exit();
            return true
        } else if (id == R.id.exitlogin) {
            exit()
            return true
        } else if (id == R.id.kv_home_item) {
            home()
            return true
        } else if (id == R.id.kv_refresh_item) {
            refresh()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    var contextMenuMap: MutableMap<String, Any?>? = null
    var contextMenuAdapter: CombineSimpleAdapter? = null
    override fun onCreateContextMenu(
        menu: ContextMenu, v: View,
        menuInfo: ContextMenuInfo
    ) {
        val info = menuInfo as AdapterContextMenuInfo
        contextMenuAdapter = (v as List).adapter
        contextMenuMap = v.adapter?.getItem(info.position) as? MutableMap<String, Any?> ?: emptyMap<String, Any?>().toMutableMap()
        menu.setHeaderTitle(contextMenuMap!!["name"] as CharSequence?)
        if (contextMenuMap!![TagConsts.TYPE] != null
            && contextMenuMap!![TagConsts.TYPE] == TypeConsts.CHANNEL
        ) {
            if (!account.isTVAccount) {
                return
            }
            if (contextMenuMap!!["star"] == "0") {
                menu.add(Menu.NONE, 0, 0, "добавить в избранное")
            } else {
                menu.add(Menu.NONE, 1, 0, "удалить из избранного")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuItemIndex = item.itemId
        val menuItems = arrayOf("add", "remove")
        val menuItemName = menuItems[menuItemIndex]
        if (menuItemName == "add") {
            if (contextMenuMap != null) {
                @SuppressLint("StaticFieldLeak")
                val task: AsyncTask<String?, Void?, String> =
                    object : AsyncTask<String?, Void?, String>() {
                        override fun doInBackground(vararg arg0: String?): String {
                            val address = ru.krasview.kvsecret.secret.ApiConst.STAR
                            val params = "channel_id=" + arg0[0]
                            return getXML(address, params, AuthRequestConst.AUTH_TV) ?: ""
                        }

                        override fun onPostExecute(result: String) {
                            val str: String
                            if (result == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
                                str = "Невозможно подключиться к серверу"
                            } else {
                                contextMenuMap!!["star"] = "1"
                                contextMenuAdapter!!.notifyDataSetChanged()
                                str = "Канал добавлен в избранное: " + contextMenuMap!!["name"]
                            }
                            val toast = Toast.makeText(
                                applicationContext,
                                str, Toast.LENGTH_SHORT
                            )
                            toast.show()
                            return
                        }
                    }
                task.execute(contextMenuMap!!["id"] as String?)
            }
        } else if (menuItemName == "remove") {
            if (contextMenuMap != null) {
                val task: AsyncTask<String?, Void?, String> =
                    object : AsyncTask<String?, Void?, String>() {
                        override fun doInBackground(vararg arg0: String?): String {
                            val address = ru.krasview.kvsecret.secret.ApiConst.UNSTAR
                            val params = "channel_id=" + arg0[0]
                            return getXML(address, params, AuthRequestConst.AUTH_TV) ?: ""
                        }

                        override fun onPostExecute(result: String) {
                            val str: String = if (result == "<results status=\"error\"><msg>Can't connect to server</msg></results>") {
                                    "Невозможно подключиться к серверу"
                                } else {
                                    contextMenuAdapter!!.data.remove(contextMenuMap)
                                    contextMenuAdapter!!.notifyDataSetChanged()
                                    "Канал удален из избранного: " + contextMenuMap!!["name"]
                                }
                            val toast = Toast.makeText(
                                applicationContext,
                                str, Toast.LENGTH_SHORT
                            )
                            toast.show()
                            return
                        }
                    }
                task.execute(contextMenuMap!!["id"] as String?)
            }
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchFragment!!.goSearch(query)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        // todo searchFragment.goSearch(newText);
        return false
    }

    override fun onDestroy() {
        Log.d("Krasview/core", "onDestroy")
        super.onDestroy()
        val pid = Process.myPid()
        Process.killProcess(pid)
    }
}
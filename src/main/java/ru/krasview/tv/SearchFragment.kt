package ru.krasview.tv

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TabHost
import android.widget.TabHost.*
import androidx.fragment.app.Fragment
import com.example.kvlib.R
import ru.krasview.kvlib.animator.NewAnimator
import ru.krasview.kvlib.interfaces.PropotionerView
import ru.krasview.kvlib.widget.NavigationViewFactory

class SearchFragment : Fragment(), OnTabChangeListener {
    var tabHost: TabHost? = null
    var mContext: Context? = null
    private var search_str = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        tabHost = inflater.inflate(R.layout.my_tab_host, null) as? TabHost
        if (tabHost == null)
                return null
        tabHost?.setup()
        var tabSpec: TabSpec = tabHost!!.newTabSpec(TAB_1)
        tabSpec.setContent(TabFactory)
        tabSpec.setIndicator("Фильмы")
        tabHost?.addTab(tabSpec)
        tabSpec = tabHost!!.newTabSpec(TAB_2)
        tabSpec.setContent(TabFactory)
        tabSpec.setIndicator("Сериалы")
        tabHost?.addTab(tabSpec)
        tabSpec = tabHost!!.newTabSpec(TAB_3)
        tabSpec.setContent(TabFactory)
        tabSpec.setIndicator("Аниме")
        tabHost?.addTab(tabSpec)
        tabHost?.setOnTabChangedListener(this)
        return tabHost
    }

    private var TabFactory = TabContentFactory { tag ->
        if (mContext == null) {
            return@TabContentFactory null
        }
        when (tag) {
            TAB_1 -> {
                val result = NewAnimator(mContext, NavigationViewFactory())
                result.init("search_movie")
                return@TabContentFactory result
            }
            TAB_2 -> {
                val result = NewAnimator(mContext, NavigationViewFactory())
                result.init("search_show")
                return@TabContentFactory result
            }
            TAB_3 -> {
                val result = NewAnimator(mContext, NavigationViewFactory())
                result.init("search_anime")
                return@TabContentFactory result
            }
            else -> null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        mContext = activity
    }

    fun goSearch(str: String) {
        search_str = str
        ((tabHost!!.currentView as NewAnimator).currentView as PropotionerView).goSearch(str)
    }

    fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return tabHost!!.currentView.dispatchKeyEvent(event)
    }

    fun home() {
        search_str = ""
        (tabHost!!.currentView as NewAnimator).home()
    }

    fun refresh() {
        (tabHost!!.currentView as NewAnimator).refresh()
    }

    override fun onTabChanged(tabId: String) {
        //Log.i("Debug", "Открыта вкладка " + host.getCurrentTab());
        goSearch(search_str)
    }

    companion object {
        private const val TAB_1 = "tab1"
        private const val TAB_2 = "tab2"
        private const val TAB_3 = "tab3"
    }
}
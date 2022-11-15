package ru.krasview.kvlib.widget

import android.annotation.SuppressLint
import android.content.Context
import com.example.kvlib.R
import ru.krasview.kvlib.indep.ListAccount
import ru.krasview.kvlib.interfaces.ViewProposeListener
import ru.krasview.kvlib.interfaces.ViewPropotionerInterface

@SuppressLint("ViewConstructor")
class Serial(context: Context?, private val mMap: Map<String?, Any?>) : SerialDescription(context),
    ViewPropotionerInterface {
    override fun getViewProposeListener(): ViewProposeListener {
        return mViewProposeListener!!
    }

    override fun init() {
        super.setTag(mMap)
        button!!.visibility = GONE
        button!!.text = "Смотреть"
        if (ListAccount.fromLauncher) {
            button!!.setBackgroundResource(R.drawable.selector)
        }
    }

    override fun refresh() {}
    override fun enter() {}
    override fun exit() {}
}
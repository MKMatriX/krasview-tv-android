package ru.krasview.kvlib.widget

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.widget.ListView
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.adapter.LoadDataToGUITask
import ru.krasview.kvlib.indep.AuthAccount
import ru.krasview.kvlib.indep.ListAccount
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvlib.indep.consts.TagConsts
import ru.krasview.kvlib.indep.consts.TypeConsts
import ru.krasview.kvlib.interfaces.Factory
import ru.krasview.kvlib.interfaces.ViewProposeListener
import ru.krasview.kvlib.interfaces.ViewPropotionerInterface

abstract class List(context: Context?, map: Map<String?, Any?>) : ListView(context),
    ViewPropotionerInterface {
    var factory: Factory? = null
    private var mViewProposeListener: ViewProposeListener? = null
    protected val map: Map<String?, Any?>
    @JvmField
	protected var data: ArrayList<Map<String?, Any?>> = ArrayList()

    @JvmField
	protected var account: AuthAccount
    protected open fun setConstData() {}
    protected open val apiAddress: String?
        get() = null
    protected open val authRequest: Int
        get() = AuthRequestConst.AUTH_NONE

    open fun parseData(doc: String?, task: LoadDataToGUITask?) {}

    init {
        this.map = map
        if (ListAccount.fromLauncher) {
            this.divider = null
        }
        account = AuthAccount.getInstance()
        (getContext() as Activity).registerForContextMenu(this)
    }

    //--------------------------------------------------------------------
    // Работа с биллингом
    //--------------------------------------------------------------------
    protected open fun showBilling(): Boolean {
        /*if(account.isKrasviewAccount()) {
			if(HeaderAccount.hideHeader()) {
				return false;
			}
			return true;
		}*/
        return false
    }

    fun addBillingHeader() {
        if (showBilling()) {
            val m: MutableMap<String?, Any?>
            m = HashMap()
            m[TagConsts.TYPE] = TypeConsts.BILLING
            m[TagConsts.NAME] = ""
            data.add(0, m)
        }
    }

    //--------------------------------------------------------------------
    // Работа с адаптером
    //--------------------------------------------------------------------
    protected open fun createAdapter(): CombineSimpleAdapter? {
        return CombineSimpleAdapter(this, data, apiAddress, authRequest)
    }

    override fun getAdapter(): CombineSimpleAdapter? {
        return super.getAdapter() as? CombineSimpleAdapter
    }

    //--------------------------------------------------------------------
    // Implementation of View method
    //--------------------------------------------------------------------
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        var handled = super.dispatchKeyEvent(event)
        if (!handled) {
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                val e = KeyEvent(event.action, KeyEvent.KEYCODE_DPAD_CENTER)
                handled = super.dispatchKeyEvent(e)
            }
        }
        return handled
    }

    //--------------------------------------------------------------------
    // Implementation of ViewPropotionerInterface
    //--------------------------------------------------------------------
    override fun setViewProposeListener(listener: ViewProposeListener) {
        mViewProposeListener = listener
    }

    override fun getViewProposeListener(): ViewProposeListener {
        return mViewProposeListener!!
    }

    override fun init() {
        addBillingHeader()
        setConstData()
        adapter = createAdapter()
        // this.setOnItemClickListener(mOnItemClickListener);
    }

    override fun refresh() {
        editConstData()
        adapter?.refresh()
    }

    override fun enter() {
        editConstData()
    }

    private fun editConstData() {
        adapter?.editConstData()
        setConstData()
        addBillingHeader()
        adapter?.notifyDataSetChanged()
    }

    override fun exit() {}
}
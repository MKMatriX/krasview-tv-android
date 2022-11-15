package ru.krasview.kvlib.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.example.kvlib.R
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener
import ru.krasview.kvlib.indep.ListAccount
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import java.util.*

open class CombineSimpleAdapter(
    parent: ru.krasview.kvlib.widget.List,
    constData: MutableList<Map<String?, Any?>>?, address: String?, auth: Int
) : BaseAdapter() {
    private val options: DisplayImageOptions
    private val animateFirstListener: ImageLoadingListener = AnimateFirstDisplayListener()
    protected var mCount = 0
    protected var mConstData: MutableList<Map<String?, Any?>>
    @JvmField
	var mData: MutableList<MutableMap<String, Any?>> = ArrayList()
    @JvmField
	protected var mAddress: String?
    protected var mParent: ru.krasview.kvlib.widget.List
    var withAuth = AuthRequestConst.AUTH_NONE
    @JvmField
	protected var colors: ColorStateList? = null
    private var mRecFocus = true
    fun parseData(doc: String?, task: LoadDataToGUITask?) {
        mParent.parseData(doc, task)
    }

    open fun postExecute() {}

    constructor(
        parent: ru.krasview.kvlib.widget.List,
        constData: MutableList<Map<String?, Any?>>?, address: String?, auth: Int, focus: Boolean
    ) : this(parent, constData, address, auth) {
        mRecFocus = focus
    }

    init {
        var constData = constData
        options = DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.series)
            .showImageForEmptyUri(R.drawable.series)
            .showImageOnFail(null)
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .considerExifParams(true)
            .displayer(SimpleBitmapDisplayer())
            .build()
        if (constData == null) {
            constData = ArrayList()
        }
        mConstData = constData
        mAddress = address
        withAuth = auth
        if (withAuth > 2 || withAuth < 0) {
            withAuth = 0
        }
        mParent = parent
//        val parser = mParent.context.resources.getXml(R.color.text_selector)
//        try {
//            colors = ColorStateList.createFromXml(mParent.context.resources, parser)
//        } catch (e: XmlPullParserException) {
//            e.printStackTrace()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
    }

    val data: MutableList<MutableMap<String, Any?>>
        get() = mData

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val map = getItem(position) as MutableMap<String, Any?>
        val view: View
        val holder: ViewHolder
        if (convertView == null) {
            val inflater = parent.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.kv_multi_item, parent, false)
            holder = ViewHolder()
            holder.background = view.findViewById(R.id.top)
            holder.name = view.findViewById<View>(R.id.txt) as TextView
            holder.image = view.findViewById<View>(R.id.image) as ImageView
            holder.progress = view.findViewById<View>(R.id.progress) as ProgressBar
            holder.time = view.findViewById<View>(R.id.time) as TextView
            holder.current_program = view.findViewById<View>(R.id.current_program) as TextView
            holder.currentLayout = view.findViewById(R.id.current_layout)
            holder.new_series = view.findViewById<View>(R.id.new_series) as TextView
            if (ListAccount.fromLauncher) {
                view.setBackgroundResource(R.drawable.selector)
                holder.name!!.setTextColor(colors)
                holder.current_program!!.setTextColor(colors)
                val progress_tv = mParent.context.resources.getDrawable(R.drawable.progress_tv)
                holder.progress!!.progressDrawable = progress_tv
            }
            view.tag = holder
        } else {
            view = convertView
        }
        return BindView(view, map)
    }

    private fun BindView(view: View, map: MutableMap<String, Any?>): View {
        val holder = view.tag as ViewHolder

        //название
        if (map["name"] != null) {
            holder.name!!.visibility = View.VISIBLE
            holder.name!!.text = map["name"] as CharSequence?
        } else {
            holder.name!!.visibility = View.GONE
        }
        val type = map["type"] as String?
        if (type == null) {
            holder.background!!.visibility = View.GONE
            holder.image!!.visibility = View.GONE
            holder.currentLayout!!.visibility = View.GONE
            holder.new_series!!.visibility = View.GONE
            return view
        }
        if (type == "billing") {
            holder.background!!.visibility = View.VISIBLE
            holder.currentLayout!!.visibility = View.GONE
            holder.new_series!!.visibility = View.GONE
        } else {
            holder.background!!.visibility = View.GONE
        }
        //статус
        if (map["state"] != null) {
            if (map["state"] == "0") {
                holder.background!!.setBackgroundColor(Color.argb(100, 100, 100, 100))
            } else {
                holder.background!!.setBackgroundColor(Color.argb(0, 0, 0, 0))
            }
        }

        //картинка
        if (map["img_uri"] != null) {
            holder.image!!.visibility = View.VISIBLE
            //загрузить картинку
            ImageLoader.getInstance().displayImage(
                map["img_uri"] as String?,
                holder.image,
                options,
                animateFirstListener
            )
        } else {
            holder.image!!.visibility = View.INVISIBLE
        }

        //текущая программа
        if (type == "channel") {
            holder.currentLayout!!.visibility = View.VISIBLE
            if (map["current_program_name"] == null) {
                if (map["current_program_name_old"] != null) {
                    map["current_program_name"] = map["current_program_name_old"] ?: ""
                } else {
                    map["current_program_name"] = ""
                    map["current_program_time"] = ""
                    map["current_program_progress"] = 0
                }
                notifyDataSetChanged()
                LoadCurrentProgram(this, map).execute()
            } else if (map["current_program_name"] != null) {
                var pr = (map["current_program_name"] as CharSequence?)!!
                if (pr == "<пусто>") {
                    pr = ""
                }
                holder.current_program!!.text = pr
                holder.progress!!.progress = (map["current_program_progress"] as Int?)!!
                holder.time!!.text = map["current_program_time"] as CharSequence?
            }
        } else {
            holder.currentLayout!!.visibility = View.GONE
        }

        //число новых серий
        if (type == "series") {
            if (map["new_series"] == null) {
                holder.new_series!!.visibility = View.GONE
                LoadNewSeriesNumber(this, map).execute()
                map["new_series"] = 0
            } else if (map["new_series"] != null && map["new_series"] as Int? == 0) {
                holder.new_series!!.visibility = View.GONE
            } else {
                holder.new_series!!.text = "+" + map["new_series"]
                holder.new_series!!.visibility = View.VISIBLE
            }
        }
        if (type == "video") {
            holder.image!!.visibility = View.GONE
        }
        return view
    }

    internal class ViewHolder {
        var background: View? = null
        var name: TextView? = null
        var image: ImageView? = null
        var time: TextView? = null
        var current_program: TextView? = null
        var progress: ProgressBar? = null
        var currentLayout: View? = null
        var new_series: TextView? = null
    }

    override fun getCount(): Int {
        return mConstData.size + mData.size
    }

    val constDataCount: Int
        get() = mConstData.size

    override fun getItem(position: Int): Any? {
        return if (position >= 0 && position < mConstData.size) {
            mConstData[position]
        } else {
            val obj: Any
            obj = try {
                mData[position - mConstData.size]
            } catch (e: Exception) {
                Log.e(
                    "Debug",
                    "" + position + " " + (position - mConstData.size) + " " + e.toString()
                )
                return null
            }
            obj
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun refresh() {
        val params = ""
        if (mAddress == null) {
            return
        }
        mData.clear()
        loadDataFromAddress(mAddress, params)
    }

    protected fun loadDataFromAddress(uri: String?, params: String?) {
        val task = LoadDataFromAddressTask(this)
        task.execute(uri, params)
    }

    override fun notifyDataSetChanged() {
        val size = mData.size
        super.notifyDataSetChanged()
        if (size == 1 && mRecFocus) {
            mParent.requestFocus()
        }
    }

    fun setAddress(address: String?) {
        mAddress = address
    }

    open fun emptyList(task: LoadDataToGUITask?): Boolean {
        return false
    }

    fun editConstData() {
        if (mConstData.isEmpty()) {
            return
        }
        mConstData.clear()
    }

    private class AnimateFirstDisplayListener : SimpleImageLoadingListener() {
        override fun onLoadingComplete(imageUri: String, view: View, loadedImage: Bitmap) {
            if (loadedImage != null) {
                val imageView = view as ImageView
                val firstDisplay = !displayedImages.contains(imageUri)
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500)
                    if (displayedImages.size > 100) {
                        displayedImages.removeAt(0)
                    }
                    displayedImages.add(imageUri)
                    Log.i("Debug", "" + displayedImages.size)
                }
            }
        }

        companion object {
            val displayedImages = Collections.synchronizedList(LinkedList<String>())
        }
    }
}
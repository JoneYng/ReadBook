package com.zx.read.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.zx.read.App
import com.zx.read.ReadActivity
import com.zx.read.ReadBook
import com.zx.read.base.BaseDialogFragment
import com.zx.read.config.ReadBookConfig
import com.zx.read.extensions.getIndexById
import com.zx.read.extensions.viewBinding
import com.zx.read.ui.CircleImageView
import com.zx.readbook.R
import com.zx.readbook.databinding.DialogReadBookStyleBinding
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadStyleDialog(var upConfig: () -> Unit) : BaseDialogFragment() {
    private val binding by viewBinding(DialogReadBookStyleBinding::bind)
    val callBack get() = activity as? ReadActivity
    private lateinit var styleAdapter: StyleAdapter

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 0, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_book_style, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
        initViewEvent()
    }

    private fun initView() = with(binding) {
//        val bg = requireContext().bottomBackground
//        rootView.setBackgroundColor(bg)
        styleAdapter = StyleAdapter(ReadBookConfig.configList){
            position->
            changeBg(position)
        }
        rvStyle.adapter = styleAdapter

    }

    private fun initData() {
        upView()
//        styleAdapter.setList(ReadBookConfig.configList)
    }

    private fun initViewEvent() = with(binding) {
//        flTextBold.onChanged {
//            postEvent(EventBus.UP_CONFIG, true)
//        }
        rgPageAnim.onCheckedChange { _, checkedId ->
            ReadBookConfig.pageAnim = -1
            ReadBookConfig.pageAnim = binding.rgPageAnim.getIndexById(checkedId)
            callBack?.upPageAnim()
        }
        sliderTextSize.onValueChanged = {
            sliderTextSize.setThumbText(it.toInt().toString())
            ReadBookConfig.textSize = it.toInt()
            upConfig()
        }

        sliderLineSpacing.onValueChanged = {
            ReadBookConfig.lineSpacingExtra = it.toInt()
            upConfig()
        }

        sliderBookPadding.onValueChanged = {
            ReadBookConfig.paddingTop = it.toInt()
            ReadBookConfig.paddingLeft = it.toInt()
            ReadBookConfig.paddingRight = it.toInt()
            ReadBookConfig.paddingBottom = it.toInt()
            upConfig()
        }


    }

    class StyleAdapter(val itemList: List<ReadBookConfig.Config>, val changeBg: (Int) -> Unit) :
        RecyclerView.Adapter<StyleAdapter.ItemViewHolder>() {

        // ViewHolder 实现
        class ItemViewHolder(itemView: View,val changeBg: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val ivStyle: CircleImageView = itemView.findViewById(R.id.iv_style)

            fun bind(item: ReadBookConfig.Config) {
                ivStyle.setText(item.name.ifBlank { "文字" })
                ivStyle.setTextColor(item.curTextColor())
                ivStyle.setImageDrawable(item.curBgDrawable(100, 150))
                if (ReadBookConfig.styleSelect == layoutPosition) {
                    ivStyle.borderColor = App.INSTANCE.getColor(R.color.purple_200)
                    ivStyle.setTextBold(true)
                } else {
                    ivStyle.borderColor = item.curTextColor()
                    ivStyle.setTextBold(false)
                }
                ivStyle.onClick {
                    if (ivStyle.isInView) {
                        changeBg(layoutPosition)
                    }
                }
            }
        }

        // 创建 ViewHolder
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_read_style, parent, false)
            return ItemViewHolder(view,changeBg)
        }

        // 绑定数据
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = itemList[position]
            holder.bind(item)
        }

        // 返回数据项总数
        override fun getItemCount(): Int = itemList.size
    }

    private fun changeBg(index: Int) {
        val oldIndex = ReadBookConfig.styleSelect
        if (index != oldIndex) {
            ReadBookConfig.styleSelect = index
            ReadBookConfig.upBg()
            upView()
            styleAdapter.notifyItemChanged(oldIndex)
            styleAdapter.notifyItemChanged(index)
            upConfig()
        }
    }


    private fun upView() = with(binding) {
        ReadBook.pageAnim().let {
            if (it >= 0 && it < rgPageAnim.childCount) {
                rgPageAnim.check(rgPageAnim[it].id)
            }
        }
        ReadBookConfig.let {

        }
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }
}
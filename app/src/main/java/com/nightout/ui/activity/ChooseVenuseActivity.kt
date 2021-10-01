package com.nightout.ui.activity

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.nightout.R
import com.nightout.adapter.*
import com.nightout.base.BaseActivity
import com.nightout.databinding.ChossesvenuesActvityBinding
import com.nightout.model.DashboardModel
import com.nightout.utils.*
import com.nightout.vendor.services.Status
import com.nightout.viewmodel.CommonViewModel
import com.nightout.viewmodel.HomeViewModel
import com.nightout.viewmodel.LostItemSubmitViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ChooseVenuseActivity : BaseActivity() {
    lateinit var binding: ChossesvenuesActvityBinding
    private var customProgressDialog = CustomProgressDialog()
    lateinit var homeViewModel: CommonViewModel
    lateinit var dashList: DashboardModel.Data
    var strID: StringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this@ChooseVenuseActivity,
            R.layout.chossesvenues_actvity
        )
        initView()
        dashboardAPICALL()
        setToolBar()
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        if (v == binding.chooseVenuesSend) {
            if (isValidateData())
                collectData()
        }
    }

    private fun isValidateData(): Boolean {
        var flag: Boolean = false
        strID = StringBuilder()
        for (i in 0 until dashList.all_records.size) {
            var subList = dashList.all_records[i]
            for (j in 0 until subList.sub_records.size) {
                if (subList.sub_records[j].isChked) {
                    flag = true
                    strID.append(subList.sub_records[j].id + ",")
                }
            }
        }
        if (!flag) {
            MyApp.popErrorMsg("", "Please Select Venues !!", THIS!!)
            return false
        }

        return true
    }

    private fun collectData() {
        var mdl = DataManager.instance.lostItemDetailCstmModel
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        if(!mdl.itemID.equals("0")) {
            //send ID durring EditItem
            builder.addFormDataPart("id", mdl.itemID)
        }
        builder.addFormDataPart("venue_id", strID.substring(0, strID.length - 1))
        builder.addFormDataPart("product_name", mdl.itemName)
        builder.addFormDataPart("customer_name", mdl.fullName)
        builder.addFormDataPart("email", mdl.emailID)
        var mobNo = Utills.getMobNoSimpleFormat(mdl.phno)
        builder.addFormDataPart("phonenumber", mobNo)
        builder.addFormDataPart("product_detail", mdl.pDetail)
        if(mdl.imgPathUri.equals("")){
            builder.addFormDataPart("image","")
        }else{
            var resultUri = Uri.parse(mdl.imgPathUri)
            val bitmap: Bitmap?
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, resultUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, resultUri)
                bitmap = ImageDecoder.decodeBitmap(source)
            }
            builder.addPart(setBody(bitmap!!, "image"))
        }

        addLostItemApiCall(builder.build())

    }

    lateinit var lostItemSubmitViewModel: LostItemSubmitViewModel
    private fun addLostItemApiCall(build: MultipartBody) {
        customProgressDialog.show(this@ChooseVenuseActivity)
        lostItemSubmitViewModel.submitLostItem(build).observe(this@ChooseVenuseActivity, {
            when (it.status) {
                Status.SUCCESS -> {
                    customProgressDialog.dialog.dismiss()
                    if (it.data?.status_code == 200) {
                      //  MyApp.ShowTost(THIS!!, "" + it.data?.status_code)
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                Status.LOADING -> {
                    Log.d("ok", "loginCall:LOADING ")
                }
                Status.ERROR -> {
                    customProgressDialog.dialog.dismiss()
                    try {
                        Utills.showSnackBarOnError(
                            binding.chooseVenuesToolbarConstrent,
                            it.message!!,
                            this@ChooseVenuseActivity
                        )
                    } catch (e: Exception) {
                    }
                }
            }
        })
    }

    private var filePath: File? = null
    private lateinit var reqFile: RequestBody
    var body: MultipartBody.Part? = null
    private fun setBody(bitmap: Bitmap, flag: String): MultipartBody.Part {
        val filePath = Utills.saveImage(this@ChooseVenuseActivity, bitmap)
        this.filePath = File(filePath)
        reqFile = RequestBody.create(
            "multipart/form-data".toMediaTypeOrNull(),
            this.filePath!!
        )


        body = MultipartBody.Part.createFormData(
            flag,
            this.filePath!!.name,
            reqFile
        )

        return body!!
    }

    private fun initView() {
        lostItemSubmitViewModel = LostItemSubmitViewModel(this@ChooseVenuseActivity)
        homeViewModel = CommonViewModel(this@ChooseVenuseActivity)
        binding.chooseVenuesSend.setOnClickListener(this)

    }


    private fun setToolBar() {
        setTouchNClick(binding.chooseVenuesToolbar.toolbarBack)
        binding.chooseVenuesToolbar.toolbarBack.setOnClickListener { finish() }
        binding.chooseVenuesToolbar.toolbarTitle.setText("Choose Venues")
        binding.chooseVenuesToolbar.toolbar3dot.visibility = View.GONE
        binding.chooseVenuesToolbar.toolbarBell.visibility = View.GONE
    }

    private var responseItemList: MutableList<String>? = ArrayList()
    private fun dashboardAPICALL() {
        customProgressDialog.show(this@ChooseVenuseActivity, "")
        homeViewModel.dashBoard().observe(this@ChooseVenuseActivity, {
            when (it.status) {
                Status.SUCCESS -> {
                    customProgressDialog.dialog.dismiss()
                    it.data?.let { users ->
                        try {
                            dashList = users.data
                            if (!(dashList.all_records == null || dashList.all_records.size <= 0)) {
                                //  setListAllRecord(dashList.all_records)
                                //  for (i in 0 until dashList.all_records.size) {
                                for (i in 0 until 3) {
                                    responseItemList?.add(dashList.all_records[i].title!!)
                                }
                                for (aq in responseItemList!!.indices) {
                                    binding.tabs!!.addTab(
                                        binding.tabs!!.newTab().setText(responseItemList!![aq])
                                    )
                                    binding.tabs!!.tabGravity = TabLayout.GRAVITY_FILL

                                }
                                setProductListWidSection()
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                Status.LOADING -> {
                    Log.d("ok", "loginCall:LOADING ")
                }
                Status.ERROR -> {
                    customProgressDialog.dialog.dismiss()
                    try {
                        Utills.showSnackBarOnError(
                            binding.chooseVenuesToolbarConstrent,
                            it.message!!,
                            this@ChooseVenuseActivity
                        )
                    } catch (e: Exception) {
                    }
                }
            }
        })
    }

    lateinit var allRecordAdapter: AllRecordVenuseAdapter
    private fun setProductListWidSection() {
        allRecordAdapter =
            AllRecordVenuseAdapter(this@ChooseVenuseActivity, dashList.all_records, object :
                AllRecordVenuseAdapter.ClickListener {
                override fun onClickNext(pos: Int) {
//                startActivity(
//                    Intent(this@ChooseVenuseActivity, VenuListActvity::class.java)
//                    .putExtra(AppConstant.INTENT_EXTRAS.StoreType,allRecordsList[pos].type ))
                }

                override fun onClickSub(subpos: Int, pos: Int) {
                    if (dashList.all_records[pos].sub_records[subpos].isChked)
                        dashList.all_records[pos].sub_records[subpos].isChked = false
                    else
                        dashList.all_records[pos].sub_records[subpos].isChked = true
                    allRecordAdapter.notifyItemChanged(pos)
                }
            })

        linearLayoutManager =
            LinearLayoutManager(this@ChooseVenuseActivity, LinearLayoutManager.VERTICAL, false)
        binding.chooseVenuesRecyle.layoutManager = linearLayoutManager
        binding.chooseVenuesRecyle.adapter = allRecordAdapter

        setScrollListener()
        bindWidgetsWithAnEvent()

    }

    private var linearLayoutManager: LinearLayoutManager? = null
    private var checkCallBack = false // not using now
    private var checkScroll = true
    private var posSelected = 0
    private var totalCount = 0// not using now
    private var chkTabClicked = false
    private fun setScrollListener() {
        checkScroll = true
        binding?.chooseVenuesRecyle!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (chkTabClicked) {
                    selectionBackground(posSelected, "onScrollStateChanged>>if")
                    binding.tabs!!.setScrollPosition(posSelected, 0f, true)
                }
                if (newState == 0) {
                    chkTabClicked = false
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!chkTabClicked) {
                    val visiblePosition = linearLayoutManager!!.findFirstVisibleItemPosition()
                    val firstCompletelyVisiblePosition =
                        linearLayoutManager!!.findFirstCompletelyVisibleItemPosition()
                    totalCount = linearLayoutManager!!.itemCount
                    checkScroll = false
                    if (visiblePosition > -1) {
                        // if (!checkCallBack) {
                        if (firstCompletelyVisiblePosition == -1) {
                            selectionBackground(
                                visiblePosition,
                                "onScrolled>>if visiblePosition>>" + visiblePosition + " firstCompletelyVisiblePosition>>" + firstCompletelyVisiblePosition
                            )
                            binding.tabs!!.setScrollPosition(visiblePosition, 0f, true)
                        } else {
                            selectionBackground(
                                firstCompletelyVisiblePosition,
                                "onScrolled>>else visiblePosition>>" + visiblePosition + " firstCompletelyVisiblePosition>>" + firstCompletelyVisiblePosition
                            )
                            binding.tabs!!.setScrollPosition(
                                firstCompletelyVisiblePosition,
                                0f,
                                true
                            )
                        }
//                        } else {
//                            selectionBackground(visiblePosition, "onScrolled>>else2 visiblePosition>>"+visiblePosition+" firstCompletelyVisiblePosition>>"+firstCompletelyVisiblePosition)
//                            binding.tabs!!.setScrollPosition(visiblePosition, 0f, true)
//                            if (firstCompletelyVisiblePosition == visiblePosition) {
//                                checkCallBack = false
//                            }
//                        }
                    }
                }
            }
        })
    }

    fun bindWidgetsWithAnEvent() {

        val smoothScroller: RecyclerView.SmoothScroller =
            object : LinearSmoothScroller(this@ChooseVenuseActivity) {
                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }

        binding.tabs!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                checkCallBack = true
                chkTabClicked = true
                posSelected = tab.position
                smoothScroller.setTargetPosition(tab.position);
                linearLayoutManager!!.startSmoothScroll(smoothScroller);
                binding.tabs!!.setSelectedTabIndicatorHeight((2 * resources.displayMetrics.density).toInt())
                selectionBackground(posSelected, "addOnTabSelectedListener")
                if (tab.position == totalCount - 1) {
                    checkCallBack = false
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                checkCallBack = false
                //  chkTabClicked=true
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                checkCallBack = true
                //  chkTabClicked=true
                posSelected = tab.position
                //  selectionBackground(posSelected)
                smoothScroller.setTargetPosition(tab.position);
                linearLayoutManager!!.startSmoothScroll(smoothScroller);
                binding.tabs!!.setSelectedTabIndicatorHeight((2 * resources.displayMetrics.density).toInt())
                if (tab.position == totalCount - 1) {
                    checkCallBack = false
                }
            }
        })
    }

    private fun selectionBackground(position: Int, strMsg: String) {
        for (i in responseItemList!!.indices) {
            if (i == position) {
                val tabLayout =
                    (binding.tabs!!.getChildAt(0) as ViewGroup).getChildAt(position) as LinearLayout
                val tabTextView = tabLayout.getChildAt(1) as TextView
                if (tabTextView != null) {
                    tabTextView.setTextColor(resources.getColor(R.color.text_yello))
                    tabTextView.setPadding(32, 14, 32, 14)
                    tabTextView.setBackgroundResource(R.drawable.border_yello)
                }
                val tab = (binding.tabs!!.getChildAt(0) as ViewGroup).getChildAt(position)
                val p = tab.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(0, 0, 0, 0)
                tab.requestLayout()
                binding.tabs!!.isSmoothScrollingEnabled = true
                binding.tabs!!.setScrollPosition(position, 0f, true)
            } else {
                val tabLayout =
                    (binding.tabs!!.getChildAt(0) as ViewGroup).getChildAt(i) as LinearLayout
                val tabTextView = tabLayout.getChildAt(1) as TextView
                if (tabTextView != null) {
                    tabTextView.setPadding(32, 14, 32, 14)
                    tabTextView.setTextColor(resources.getColor(R.color.white))
                    tabTextView.setBackgroundResource(R.drawable.border_primaryclr)
                }
                val tab = (binding.tabs!!.getChildAt(0) as ViewGroup).getChildAt(i)
                val p = tab.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(0, 0, 0, 0)
                tab.requestLayout()
            }
        }
    }
}
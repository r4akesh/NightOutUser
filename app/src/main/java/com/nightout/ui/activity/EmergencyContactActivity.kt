package com.nightout.ui.activity

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.nightout.R
import com.nightout.adapter.EmergencyContactAdapter
import com.nightout.adapter.PreviousEmergencyInfoAdatper
import com.nightout.base.BaseActivity
import com.nightout.databinding.ActivityEmergencyContactBinding


class EmergencyContactActivity : BaseActivity() {
    lateinit var binding: ActivityEmergencyContactBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_emergency_contact)
        setToolBar()
    }

    private fun setToolBar() {
        binding.termCondToolBar.toolbarTitle.text = resources.getString(R.string.Emergency_Contact)
        binding.termCondToolBar.toolbarBack.setOnClickListener {
            finish()
        }
        binding.termCondToolBar.toolbar3dot.visibility = View.GONE
        binding.termCondToolBar.toolbarBell.visibility = View.GONE
        setUpView()
    }

    private fun setUpView(){
        binding.emergencyList.layoutManager = LinearLayoutManager(this)
        val emergencyContactAdapter = EmergencyContactAdapter(this)
        binding.emergencyList.adapter = emergencyContactAdapter
    }
}
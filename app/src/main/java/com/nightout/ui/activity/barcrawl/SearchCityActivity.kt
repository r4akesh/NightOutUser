package com.nightout.ui.activity.barcrawl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.nightout.R
import com.nightout.base.BaseActivity
import com.nightout.databinding.SrchCityBinding
import java.util.*

class SearchCityActivity : BaseActivity(), OnMapReadyCallback {
    lateinit var binding: SrchCityBinding
    lateinit var gMap: GoogleMap
    var LAUNCH_GOOGLE_ADDRESS = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this@SearchCityActivity, R.layout.srch_city)
        setToolBar()
        initView()
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        if (v == binding.barcralCity) {
            Places.initialize(
                this@SearchCityActivity,
                resources.getString(R.string.google_place_picker_key)
            )
            val fieldList =
                Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fieldList)
                .build(this@SearchCityActivity)
            startActivityForResult(intent, LAUNCH_GOOGLE_ADDRESS)
        }
        if (v == binding.barcrawlNextBtn) {
            startActivity(Intent(this@SearchCityActivity, BarcrawlListActivity::class.java))
        }
    }

    private fun initView() {
        val supportMapFragment =
            (supportFragmentManager.findFragmentById(R.id.barcrawleSerchMap) as SupportMapFragment?)!!
        supportMapFragment.getMapAsync(this@SearchCityActivity)
        binding.barcralCity.setOnClickListener(this@SearchCityActivity)
        binding.barcrawlNextBtn.setOnClickListener(this@SearchCityActivity)
    }

    private fun setToolBar() {
        binding.srchBarCrawlToolBar.toolbarTitle.setText(resources.getString(R.string.SearchCity))
        setTouchNClick(binding.srchBarCrawlToolBar.toolbarBack)
        binding.srchBarCrawlToolBar.toolbarBack.setOnClickListener { finish() }
        binding.srchBarCrawlToolBar.toolbar3dot.visibility = View.GONE
        binding.srchBarCrawlToolBar.toolbarBell.visibility = View.GONE
        binding.srchBarCrawlToolBar.toolbarCreateGrop.visibility = View.GONE
    }

    override fun onMapReady(p0: GoogleMap?) {
        gMap = p0!!
        p0!!.setMapStyle(MapStyleOptions(resources.getString(R.string.style_json)))//set night mode
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentt: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentt)
        try {
            val place = Autocomplete.getPlaceFromIntent(intentt!!)
            Log.d("location", "location: " + place.address)
            binding.barcralCity.text = place.address
            gMap!!.clear()
            val latLng = LatLng(place.latLng!!.latitude, place.latLng!!.longitude)
            val yourBitmap = getDrawable(R.drawable.ic_crnt_loc)!!.toBitmap(50, 55)//svg img
            gMap!!.addMarker(
                MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(yourBitmap))
            )
            val cameraPosition = CameraPosition.Builder().target(latLng).zoom(17f).build()
            gMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } catch (e: Exception) {
        }
    }
}
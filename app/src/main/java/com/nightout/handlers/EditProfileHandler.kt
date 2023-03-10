package com.nightout.handlers

//import com.github.drjacky.imagepicker.ImagePicker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.lassi.common.utils.KeyUtils
import com.lassi.data.media.MiMedia
import com.lassi.domain.media.LassiOption
import com.lassi.domain.media.MediaType
import com.lassi.presentation.builder.Lassi
import com.nightout.R
import com.nightout.callbacks.OnSelectOptionListener
import com.nightout.chat.activity.CreateGroupActvity
import com.nightout.model.LoginModel
import com.nightout.ui.activity.EditProfileActivity
import com.nightout.ui.fragment.SelectSourceBottomSheetFragment
import com.nightout.utils.CustomProgressDialog
import com.nightout.utils.MyApp
import com.nightout.utils.PreferenceKeeper
import com.nightout.utils.Utills
import com.nightout.vendor.services.Status
import com.nightout.vendor.viewmodel.EditProfileViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.IOException
import java.util.*


open class EditProfileHandler(val activity: EditProfileActivity) : OnSelectOptionListener {
    private lateinit var editProfileViewModel: EditProfileViewModel
  //  private lateinit var selectSourceBottomSheetFragment: SelectSourceBottomSheetFragment
    private var imageUrl: Uri? = null
    private var filePath: File? = null
    private lateinit var reqFile: RequestBody
    var body: MultipartBody.Part? = null
    private val progressDialog = CustomProgressDialog()
    var LAUNCH_GOOGLE_ADDRESS = 1005
    var LAUNCH_GOOGLE_ADDRESS2 = 1006
    var RequestCodeCamera = 100
    var RequestCodeGallery = 200
    private lateinit var selectSourceBottomSheetFragment: SelectSourceBottomSheetFragment

//    init {
//        editProfileViewModel = EditProfileViewModel(activity)
//    }

    fun onFinishScreen() {
        activity.finish()
    }




    fun onSelectImage2(){
        if (!Utills.checkingPermissionIsEnabledOrNot(activity)) {
            Utills.requestMultiplePermission(activity, CreateGroupActvity.requestPermissionCode)
        }else{
            selectSourceBottomSheetFragment = SelectSourceBottomSheetFragment(this, "")
            selectSourceBottomSheetFragment.show(
               activity.supportFragmentManager,
                "selectSourceBottomSheetFragment"
            )
        }
    }

    private fun selectCameraImage() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(takePictureIntent, RequestCodeGallery)
        }

    }

    fun saveProfile(editProfileViewModel: EditProfileViewModel) {
        this.editProfileViewModel = editProfileViewModel
        MyApp.hideSoftKeyboard(activity)
        if (editProfileViewModel.isValidate(activity)) {
            val builder = MultipartBody.Builder()
            builder.setType(MultipartBody.FORM)
            builder.addFormDataPart("first_name", editProfileViewModel.fName)
            builder.addFormDataPart("last_name", editProfileViewModel.lName)
            builder.addFormDataPart("address1", editProfileViewModel.addrs1)
            builder.addFormDataPart("address2", editProfileViewModel.addrs2)
            builder.addFormDataPart("about_me", editProfileViewModel.aboutMe)
            builder.addFormDataPart("city", city)
            builder.addFormDataPart("latitude", latitudeGlobal)
            builder.addFormDataPart("longitude", longitudeGlobal)
            // if (editProfileViewModel.profilePic != null) {
            if (body != null) {
                //  builder.addPart(editProfileViewModel.profilePic!!)
                builder.addPart(body!!)
            } else {
                builder.addFormDataPart("profile", "")
            }

            saveProfileAPICall(builder.build())
        }
    }

    fun openLoactionActvity(editProfileViewModel: EditProfileViewModel) {
        Places.initialize(activity, activity.resources.getString(R.string.google_place_picker_key))
        val fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fieldList)
            .build(activity)
        activity.startActivityForResult(intent, LAUNCH_GOOGLE_ADDRESS)

    }

    fun openLoactionActvity2(editProfileViewModel: EditProfileViewModel) {
        Places.initialize(activity, activity.resources.getString(R.string.google_place_picker_key))
        val fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fieldList)
            .build(activity)
        activity.startActivityForResult(intent, LAUNCH_GOOGLE_ADDRESS2)

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionSelect(option: String) {
        if (option == "camera") {
            selectSourceBottomSheetFragment.dismiss()
            //  ImagePicker.onCaptureImage(this)
            val intent = Lassi(activity)
                .with(LassiOption.CAMERA)
                .setMaxCount(1)
                .setGridSize(3)
                .setMediaType(MediaType.IMAGE)
                .setCompressionRation(10)
                .build()
            receiveData.launch(intent)

        } else {
            selectSourceBottomSheetFragment.dismiss()
            val intent = Lassi(activity)
                .with(LassiOption.GALLERY)
                .setMaxCount(1)
                .setGridSize(3)

                .setMediaType(MediaType.IMAGE)
                .setCompressionRation(10)
                .build()
            receiveData.launch(intent)


        }
    }

    private val cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data!!
                //startCropActivity(uri)
            } else parseError(it)
        }

    private val galleryLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data!!
             //   startCropActivity(uri)
            } else parseError(it)
        }

    private fun parseError(activityResult: ActivityResult) {
       /* if (activityResult.resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(activity, ImagePicker.getError(activityResult.data), Toast.LENGTH_SHORT)
                .show()
        } else {
            Utills.showSnackBarFromTop(
                activity.binding.etFName,
                "You have cancelled the image upload process.",
                activity
            )
        }*/
    }

  /*  private fun startCropActivity(imageUri: Uri) {
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON)
            .setMultiTouchEnabled(true)
            .setOutputCompressQuality(100)
            .setAspectRatio(1, 1)
            .start(activity)
    }*/

    @RequiresApi(Build.VERSION_CODES.Q)
    private val receiveData = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
           // activity.binding.userProfile.setImageResource(R.drawable.app_icon)
           val selectedMedia = it.data?.getSerializableExtra(KeyUtils.SELECTED_MEDIA) as ArrayList<MiMedia>
            if (!selectedMedia.isNullOrEmpty()) {
                val bitmap: Bitmap?

//                try {
//                    activity.binding.userProfile.setImageBitmap(null)
//                } catch (e: Exception) {
//                    Log.d("ok", ""+e)
//                }


                imageUrl = Uri.parse(selectedMedia[0].path)

                println("results>>>>>>>" + Uri.parse(selectedMedia[0].path))
                val resultUri = Uri.parse(selectedMedia[0].path)
                try {
                    bitmap = BitmapFactory.decodeFile(selectedMedia[0].path)
                    if(bitmap!=null)
                    activity.binding.userProfile.setImageBitmap(bitmap)
                   // activity.binding.userProfile.setImageURI(resultUri)
                   // activity.binding.userProfile.setImageResource(R.drawable.app_icon)

                    setBody(bitmap!!, "profile")


                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* if (requestCode == RequestCodeCamera && resultCode == AppCompatActivity.RESULT_OK && data != null) {//Gallery
             imageUri = data.data
             startCropActivity(imageUri!!)

            /* Glide.with(this)
                 .asBitmap()
                 .load(imageUri)
                 .centerCrop()
                 .into(binding.profilePic)
             addProfile()*/
         }else if (requestCode == RequestCodeGallery && resultCode == AppCompatActivity.RESULT_OK && data != null) { //camera
             val extras: Bundle = data.extras!!
             val imageBitmap = extras["data"] as Bitmap?
             imageUri = getImageUri(activity,imageBitmap!!)
             Log.d("TAG", "iamgedsfas:: $imageUri")
             startCropActivity(imageUri!!)
            /* val image = imageUri
             Glide.with(this)
                 .asBitmap()
                 .load(image)
                 .centerCrop()
                 .into(binding.profilePic)
             addProfile()*/
         }


       else   if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
             val imageUri = CropImage.getPickImageResultUri(activity, data)
             if (CropImage.isReadExternalStoragePermissionsRequired(activity, imageUri)) {
                 // mCropImageUri = imageUri
                 activity.requestPermissions(
                     listOf(Manifest.permission.READ_EXTERNAL_STORAGE).toTypedArray(),
                     0
                 )
             } else {
                 startCropActivity(imageUri)
             }

         }

       else  if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
             val result = CropImage.getActivityResult(data)
             if (resultCode == Activity.RESULT_OK) {
                 val bitmap: Bitmap?
                 activity.binding.userProfile.setImageBitmap(null)
                 imageUrl = result.originalUri
                 val resultUri = result.uri
                 try {
                     if (Build.VERSION.SDK_INT < 28) {
                         bitmap =
                             MediaStore.Images.Media.getBitmap(activity.contentResolver, resultUri)
                     } else {
                         val source = ImageDecoder.createSource(activity.contentResolver, resultUri)
                         bitmap = ImageDecoder.decodeBitmap(source)
                     }
                     if(bitmap!=null){
                     activity.binding.userProfile.setImageBitmap(bitmap)
                     setBody(bitmap!!, "profile")}
                     else{
                         MyApp.popErrorMsg("","bitmap is null",activity)
                     }
                     //  editProfileViewModel.profilePic = setBody(bitmap!!, "profile")
                     //  Log.d("TAG", "onActivityResult: "+editProfileViewModel.profilePic)
                 } catch (e: Exception) {
                     e.printStackTrace()
                     Utills.showSnackBarFromTop(activity.binding.etFName, "catch-> $e", activity)
                 }
             }
         }

        else if (requestCode == LAUNCH_GOOGLE_ADDRESS && resultCode == Activity.RESULT_OK) {
             val place = Autocomplete.getPlaceFromIntent(data!!)
             activity.binding.editProfileLocation.text = place.address
          //   activity.binding.editProfileLocation2.setText(place.address)
             getAddrsFrmLatlang(place.latLng!!.latitude,place.latLng!!.longitude)
         }*/
         if (requestCode == LAUNCH_GOOGLE_ADDRESS2 && resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(data!!)
            activity.binding.editProfileLocation2.text = place.address
           // getAddrsFrmLatlang(place.latLng!!.latitude,place.latLng!!.longitude)
        }
    }

    var geocoder: Geocoder? = null
    var city = ""
    var addresses: List<Address>? = null
    var latitudeGlobal =""
    var longitudeGlobal =""
    private fun getAddrsFrmLatlang(latitude: Double, longitude: Double) {
        geocoder = Geocoder(activity, Locale.getDefault())
        try {
           latitudeGlobal = "" + latitude
            longitudeGlobal = "" + longitude
            addresses = geocoder!!.getFromLocation(latitude, longitude, 1) // Here 1 represent max location result to returned, by documents it recommended 1 to 5
          //  val addrs = addresses?.get(0)?.getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
              city = addresses?.get(0)?.getLocality()!!

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setBody(bitmap: Bitmap, flag: String): MultipartBody.Part {
        val filePath = Utills.saveImage(activity, bitmap)
        this.filePath = File(filePath)
        reqFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), this.filePath!!)

//        if (flag == "store_logo") {
//            activity.binding.iconName.text = this.filePath!!.name
//        }

        body = MultipartBody.Part.createFormData(flag, this.filePath!!.name, reqFile)
        return body!!
    }

    private fun saveProfileAPICall(requestBody: MultipartBody) {
        progressDialog.show(activity)
        editProfileViewModel.updateProfile(requestBody).observe(activity) {
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.dialog.dismiss()
                    it.data?.let {
                        var logModel: LoginModel.Data = it.data
                        PreferenceKeeper.instance.loginResponse = logModel
                        Log.d("ok", "SUCCESS: ")

                        activity.finish()
                    }

                }
                Status.LOADING -> {
                    Log.d("ok", "LOADING: ")
                }
                Status.ERROR -> {
                    progressDialog.dialog.dismiss()
                    Log.d("ok", "ERROR: ")
                    Utills.showErrorToast(activity, it.message!!)
                }
            }
        }
    }


}
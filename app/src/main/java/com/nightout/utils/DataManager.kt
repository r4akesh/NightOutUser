package com.nightout.utils

import com.nightout.model.LostItemDetailCstmModel


class DataManager {
    var dManager: DataManager? = null

    //   public boolean  isUsrSetLoctionOnMap;
    companion object {
        var dManager: DataManager? = null

        val instance: DataManager
            get() {

                if (dManager == null) {
                    dManager = DataManager()
                    return dManager!!
                } else {
                    return dManager as DataManager
                }

            }
    }
    var lostItemDetailCstmModel = LostItemDetailCstmModel("","","","","","","")
    var isFirstShowPopupReview=false

}
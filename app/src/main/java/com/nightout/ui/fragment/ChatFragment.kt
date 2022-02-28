package com.nightout.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nightout.R
import com.nightout.adapter.ChatAdapter
import com.nightout.chat.chatinterface.ResponseType
import com.nightout.chat.chatinterface.WebSocketObserver
import com.nightout.chat.chatinterface.WebSocketSingleton
import com.nightout.chat.model.FSRoomModel
import com.nightout.chat.model.ResponseModel
import com.nightout.chat.model.RoomNewResponseModel
import com.nightout.chat.model.RoomResponseModel
import com.nightout.chat.utility.UserDetails
import com.nightout.databinding.FragmentChatBinding
import com.nightout.interfaces.OnMenuOpenListener
import com.nightout.model.ChatModel
import com.nightout.model.FSUsersModel
import com.nightout.ui.activity.ChatPersonalActvity
import com.nightout.ui.activity.CreateGroupActvity
import com.nightout.utils.PreferenceKeeper
import com.nightout.vendor.services.APIClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap

class ChatFragment() : Fragment() , View.OnClickListener , WebSocketObserver {

    lateinit var binding : FragmentChatBinding
    private var onMenuOpenListener: OnMenuOpenListener? = null

    constructor(onMenuOpenListener: OnMenuOpenListener) : this() {
        this.onMenuOpenListener = onMenuOpenListener
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        WebSocketSingleton.getInstant()!!.register(this)
        setRoomList()
        initView()
        joinCommand()
        return binding.root
    }

    private fun joinCommand() {
        val jsonObject = JSONObject()
        try {
            val userList = JSONArray()
          //  userList.put(UserDetails.myDetail.id)
         //   userList.put( PreferenceKeeper.instance.loginUser?.id)
            userList.put( 1)
            jsonObject.put("type", "allRooms")
            jsonObject.put("userList", userList)
            jsonObject.put(APIClient.KeyConstant.REQUEST_TYPE_KEY, APIClient.KeyConstant.REQUEST_TYPE_ROOM)
            //			jsonObject.put("room", roomId);
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        WebSocketSingleton.getInstant()!!.sendMessage(jsonObject)
    }

    override fun onClick(v: View?) {
        if(v==binding.headerChat.headerSideMenu){
            onMenuOpenListener?.onOpenMenu()
        }

        else   if(v==binding.headerChat.headerCreateGroup){
            startActivity(Intent(requireContext(), CreateGroupActvity::class.java))

        }
    }
    private fun initView() {
        binding.headerChat.headerSideMenu.setOnClickListener(this)
        binding.headerChat.headerCreateGroup.setOnClickListener(this)
    }

    lateinit var chatAdapter: ChatAdapter
    private fun setRoomList() {
        chatAdapter = ChatAdapter(requireContext(),object:ChatAdapter.ClickListener{
            override fun onClick(item: FSRoomModel) {
                val intent = Intent(requireActivity(), ChatPersonalActvity::class.java)
                intent.putExtra(ChatPersonalActvity.INTENT_EXTRAS_KEY_IS_GROUP, item.isGroup)
                intent.putExtra(ChatPersonalActvity.INTENT_EXTRAS_KEY_GROUP_DETAILS, item.groupDetails)
                intent.putExtra(ChatPersonalActvity.INTENT_EXTRAS_KEY_ROOM_ID, item.roomId)
                intent.putExtra(ChatPersonalActvity.INTENT_EXTRAS_KEY_SENDER_DETAILS, item.senderUserDetail)
                startActivity(intent)
            }

        })
        binding.fragmentChatRecycler.also {
            it.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
            it.adapter = chatAdapter
        }
    }

    override fun onWebSocketResponse(response: String, type: String, statusCode: Int, message: String?) {
        try {
            requireActivity().runOnUiThread {
                println("received message: $response")
                val gson = Gson()
                if (ResponseType.RESPONSE_TYPE_ROOM.equalsTo(type)) {
                    if (statusCode == 200) {
                        val type1 = object : TypeToken<ResponseModel<RoomResponseModel?>?>() {}.type
                        val roomResponseModelResponseModel: ResponseModel<RoomResponseModel> =
                            gson.fromJson(response, type1)
                        UserDetails.chatUsers = roomResponseModelResponseModel.getData().userListMap
                        for (element in roomResponseModelResponseModel.getData().roomList) {
                            for (userId in element.userList) {
                                if (userId != UserDetails.myDetail.id) {
                                    element.senderUserDetail = UserDetails.chatUsers[userId]
                                    break
                                }
                            }
                        }
                        // data filter by  group
                        var roomListGroup = ArrayList<FSRoomModel>()
                        for (i in 0 until roomResponseModelResponseModel.getData().roomList.size){
                            if(roomResponseModelResponseModel.getData().roomList[i].isGroup){
                                roomListGroup.add(roomResponseModelResponseModel.getData().roomList[i])
                            }
                        }
                        if(roomListGroup.size>0)
                        chatAdapter.addAll(roomListGroup)
                    } else {
                        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
                    }
                } else if (ResponseType.RESPONSE_TYPE_ROOM_MODIFIED.equalsTo(type)) {
                    if (statusCode == 200) {
                        val type1 = object : TypeToken<ResponseModel<FSRoomModel?>?>() {}.type
                        val roomResponseModelResponseModel: ResponseModel<FSRoomModel> =
                            gson.fromJson<ResponseModel<FSRoomModel>>(response, type1)
                        for (userId in roomResponseModelResponseModel.getData().userList) {
                            if (userId != UserDetails.myDetail.id) {
                                roomResponseModelResponseModel.getData().senderUserDetail =
                                    UserDetails.chatUsers[userId]
                                break
                            }
                        }
                        chatAdapter.updateElement(roomResponseModelResponseModel.getData())
                    } else {
                        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
                    }
                } else if (ResponseType.RESPONSE_TYPE_CREATE_ROOM.equalsTo(type)) {
                    if (statusCode == 200) {
                        val type1 =
                            object : TypeToken<ResponseModel<RoomNewResponseModel?>?>() {}.type
                        val roomResponseModelResponseModel: ResponseModel<RoomNewResponseModel> =
                            gson.fromJson(response, type1)
                        val tmpUserList: HashMap<String, FSUsersModel> =
                            roomResponseModelResponseModel.getData().userListMap
                        for (key in tmpUserList.keys) {
                            UserDetails.chatUsers[key] = tmpUserList[key]!!
                        }
                        val element: FSRoomModel =
                            roomResponseModelResponseModel.getData().newRoom!!
                        for (userId in element.userList) {
                            if (userId != UserDetails.myDetail.id) {
                                element.senderUserDetail = UserDetails.chatUsers[userId]
                                break
                            }
                        }
                        chatAdapter.addOrUpdate(element)
                    } else if (ResponseType.RESPONSE_TYPE_USER_MODIFIED.equalsTo(type)) {
                        Log.d("ok", "received message: $response")
                        val type1 = object : TypeToken<ResponseModel<FSUsersModel?>?>() {}.type
                        val fsUsersModelResponseModel: ResponseModel<FSUsersModel> =
                            Gson().fromJson<ResponseModel<FSUsersModel>>(response, type1)
                        chatAdapter.updateUserElement(fsUsersModelResponseModel.getData())
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("ok", "onWebSocketResponse: $type")
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override val activityName: String = ChatFragment::class.java.name

    override fun registerFor(): Array<ResponseType> {
        return arrayOf(
            ResponseType.RESPONSE_TYPE_MESSAGES,
            ResponseType.RESPONSE_TYPE_USER_MODIFIED,
            ResponseType.RESPONSE_TYPE_USER_BLOCK_MODIFIED,
            ResponseType.RESPONSE_TYPE_USER_ALL_BLOCK,
            ResponseType.RESPONSE_TYPE_ROOM_DETAILS
        )
    }

}
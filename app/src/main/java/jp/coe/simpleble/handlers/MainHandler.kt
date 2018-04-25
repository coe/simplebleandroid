package jp.coe.simpleble.handlers

import android.net.Uri

interface MainHandler {
    fun onClickCentral()
    fun onClickPeripheral()
    fun onClickImage()
    fun onClickSend(imageUrl:Uri?)
}
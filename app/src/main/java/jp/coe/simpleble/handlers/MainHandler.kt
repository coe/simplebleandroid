package jp.coe.simpleble.handlers

import android.graphics.Bitmap

interface MainHandler {
    fun onClickCentral()
    fun onClickPeripheral()
    fun onClickImage()
    fun onClickSend(imageBitmap: Bitmap?)
}
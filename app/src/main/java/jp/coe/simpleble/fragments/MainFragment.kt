package jp.coe.simpleble.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.databinding.BindingAdapter
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import jp.coe.simpleble.R
import jp.coe.simpleble.databinding.FragmentMainBinding
import jp.coe.simpleble.handlers.MainHandler
import jp.coe.simpleble.observable.MainObservable






class MainFragment : Fragment(),MainHandler {
    override fun onClickSend(imageBitmap:Bitmap?)
    {
        listener?.onClickSend(imageBitmap)
    }

    override fun onClickImage() {
        //画像Intent
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        intent.setType("image/jpeg")
//        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(context?.getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }


    }

    override fun onClickCentral() {
        listener?.onClickCentral()
    }

    override fun onClickPeripheral() {
        listener?.onClickPeripheral()
    }

    lateinit var binding:FragmentMainBinding
    var mainObservable:MainObservable = MainObservable()
    private var listener: MainHandler? = null
    private var mContext:Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_main, container, false)
        binding.handler = this
        binding.mainObservable = mainObservable
        return binding.root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is MainHandler) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement MainHandler")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG,"onActivityResult:"+requestCode+" resultCode:"+resultCode)
        when(resultCode) {
            RESULT_OK -> {
                when(requestCode) {
                    REQUEST_IMAGE_CAPTURE -> {
                        val extras = data?.getExtras()
                        val imageBitmap = extras?.get("data") as Bitmap
                        mainObservable.imageBitmap = imageBitmap

                    }
                }
            }
        }
//        when(requestCode) {
//            REQUEST_IMAGE_CAPTURE -> {
//                val resultUri: Uri? = if (data != null) data.data else null
//                mainObservable.imageUri = resultUri
//
//            }
//        }
    }

    companion object {
        private val TAG = "MainFragment"
        private val REQUEST_IMAGE_CAPTURE = 1

        @JvmStatic
        fun newInstance() =
                MainFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}

object ImageViewBindingAdapter {
    @BindingAdapter("bind:imageUri")
    @JvmStatic
    fun loadImage(view: ImageButton, uri: Uri?) {
        view.setImageURI(uri)
    }

    @BindingAdapter("bind:imageBitmap")
    @JvmStatic
    fun loadImageBitmap(view: ImageButton, bitmap: Bitmap?) {
        Log.d("ImageViewBindingAdapter","loadImageBitmap")
        view.setImageBitmap(bitmap)
    }
}
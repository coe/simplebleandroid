package jp.coe.simpleble.fragments

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.coe.simpleble.R
import jp.coe.simpleble.databinding.FragmentMainBinding
import jp.coe.simpleble.handlers.MainHandler



class MainFragment : Fragment(),MainHandler {
    override fun onClickSend() {
        listener?.onClickSend()
    }

    override fun onClickImage() {
        //画像Intent
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("image/jpeg")
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onClickCentral() {
        listener?.onClickCentral()
    }

    override fun onClickPeripheral() {
        listener?.onClickPeripheral()
    }

    lateinit var binding:FragmentMainBinding
    private var listener: MainHandler? = null

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
        return binding.root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
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
        when(requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                val resultUri = if (data != null) data.data else null
                binding.imageButton.setImageURI(resultUri)

            }
        }
    }

    companion object {
        private val REQUEST_IMAGE_CAPTURE = 1

        @JvmStatic
        fun newInstance() =
                MainFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}

package jp.coe.simpleble.fragments

import android.content.Context
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import jp.coe.simpleble.R
import jp.coe.simpleble.databinding.FragmentMainBinding
import jp.coe.simpleble.handlers.MainHandler

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MainFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MainFragment : Fragment(),MainHandler {
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
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
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


    companion object {

        @JvmStatic
        fun newInstance() =
                MainFragment().apply {
                    arguments = Bundle().apply {
//                        putString(ARG_PARAM1, param1)
//                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}

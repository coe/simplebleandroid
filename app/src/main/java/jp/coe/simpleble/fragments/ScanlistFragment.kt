package jp.coe.simpleble.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.coe.simpleble.R
import jp.coe.simpleble.handlers.ScanListHandler
import jp.coe.simpleble.viewmodels.ScanListViewModel

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ScanlistFragment.OnListFragmentInteractionListener] interface.
 */
class ScanlistFragment : Fragment() {

    // TODO: Customize parameters
    private var columnCount = 1

    private var listener: ScanListHandler? = null

//    private var dataList:ArrayList<Parcelable> = ArrayList()

    private lateinit var myItemRecyclerViewAdapter:MyItemRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate")

        super.onCreate(savedInstanceState)
        val model = ViewModelProviders.of(this).get(ScanListViewModel::class.java)
        model.getData().observe(this, Observer {
            it?.let {
                Log.d("hyuu","データ受信")
                val list:List<Parcelable> = it.map {
                    it.value
                }
                myItemRecyclerViewAdapter.updateList(list)
            }
        })

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d(TAG,"onCreateView")
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                myItemRecyclerViewAdapter = MyItemRecyclerViewAdapter(listener)
                adapter = myItemRecyclerViewAdapter
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ScanListHandler) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement ScanListHandler")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "ScanlistFragment"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance() =
                ScanlistFragment().apply {
                    arguments = Bundle().apply {
//                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }
}

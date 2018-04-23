package jp.coe.simpleble.fragments


import android.os.Parcelable
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import jp.coe.simpleble.R
import jp.coe.simpleble.fragments.ScanlistFragment.OnListFragmentInteractionListener
import jp.coe.simpleble.fragments.dummy.DummyContent.DummyItem
import kotlinx.android.synthetic.main.fragment_item.view.*



/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyItemRecyclerViewAdapter(
        var mValues: List<Parcelable>,
        private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as DummyItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
//        holder.mIdView.text = item.describeContents()
        holder.mContentView.text = item.toString()

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    fun updateList(newList: List<Parcelable>) {
        val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(this.mValues, newList))
        this.mValues = newList
        diffResult.dispatchUpdatesTo(this)
//                dataList.clear()
//                dataList.addAll(it)
//
//                myItemRecyclerViewAdapter.notifyDataSetChanged()

    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_number
        val mContentView: TextView = mView.content

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }


    internal class DiffUtilCallback(
            private val oldItems: List<Parcelable>,
            private val newItems: List<Parcelable>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                = oldItems[oldItemPosition].toString() == newItems[newItemPosition].toString()

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                = oldItems[oldItemPosition] == newItems[newItemPosition]
    }

}

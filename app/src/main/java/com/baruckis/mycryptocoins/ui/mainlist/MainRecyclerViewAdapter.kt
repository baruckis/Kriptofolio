/*
 * Copyright 2018-2019 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baruckis.mycryptocoins.ui.mainlist

import android.net.Uri
import android.os.Parcelable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.databinding.FragmentMainListItemBinding
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.dependencyinjection.GlideApp
import com.baruckis.mycryptocoins.utilities.*
import com.baruckis.mycryptocoins.utilities.glide.WhiteBackground
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.flipview_front_custom.view.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainRecyclerViewAdapter : RecyclerView.Adapter<MainRecyclerViewAdapter.BindingViewHolder>() {

    private var dataList: List<Cryptocurrency> = ArrayList<Cryptocurrency>()

    private lateinit var selectionTracker: SelectionTracker<String>
    private var selectedData: HashMap<Int, Cryptocurrency> = HashMap()


    private var selectionSequencesToDelete = ArrayList<HashMap<Int, Cryptocurrency>>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentMainListItemBinding.inflate(inflater, parent, false)

        // We should select an item when we make a click on image icon and flip it.
        binding.itemImageIcon.setOnClickListener() { _ ->
            binding.cryptocurrency?.let {
                selectionTracker.select(it.id.toString())
            }
        }

        return BindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        val cryptocurrency = dataList[position]
        var isSelected = false

        // Here we manage selection state.
        if (this::selectionTracker.isInitialized) {
            if (selectionTracker.isSelected(cryptocurrency.id.toString())) {
                selectedData[position] = cryptocurrency
                isSelected = true
            }
        }

        holder.bind(cryptocurrency, isSelected)
    }

    override fun getItemCount(): Int = dataList.size


    fun setData(newDataList: List<Cryptocurrency>) {
        dataList = newDataList
        notifyDataSetChanged()
    }

    fun getData(): List<Cryptocurrency> {
        return dataList
    }

    // We need somehow to save this custom data array list and the only way seems to make it
    // parcelable. Not sure if there is a better way, that we would not need to create this extra
    // helper class.
    @Parcelize
    class SelectionSequencesToDelete(val maplist: ArrayList<HashMap<Int, Cryptocurrency>>) : Parcelable

    fun setSelectionSequencesToDelete(newSelectionSequences: SelectionSequencesToDelete?) {
        selectionSequencesToDelete = newSelectionSequences?.maplist ?: ArrayList()
    }

    fun getSelectionSequencesToDelete(): SelectionSequencesToDelete {
        return SelectionSequencesToDelete(selectionSequencesToDelete)
    }


    fun setSelectionTracker(selectionTracker: SelectionTracker<String>) {
        this.selectionTracker = selectionTracker
    }


    // Delete functionality becomes rather complicated as we want to animate items that we remove.
    // To do that we need to provide one item position or multiple items range.
    fun deleteSelectedItems(): List<Cryptocurrency> {

        val iterator = selectedData.iterator()

        selectionSequencesToDelete = ArrayList<HashMap<Int, Cryptocurrency>>()
        var selectionSingleSequence: HashMap<Int, Cryptocurrency> = HashMap()

        var current: MutableMap.MutableEntry<Int, Cryptocurrency>? = null

        // To animate items that we need to delete, first we need to arrange them to the separate
        // sequences.
        while (iterator.hasNext()) {
            val next = iterator.next()

            // We will iterate through the selected data one by one. We check if current selected
            // item is in the common range (means arranged one after the other in same group) of
            // selected items or is it just a separate one.
            if (current != null && next.key != current.key + 1) {
                // Just when we find out that items position in recyclerview compared to previous
                // one is not in the same sequence visually, than we stop collecting current
                // sequence and start new one.
                selectionSequencesToDelete.add(selectionSingleSequence)
                selectionSingleSequence = HashMap()
            }

            selectionSingleSequence.put(next.key, next.value)
            current = next
        }

        selectionSequencesToDelete.add(selectionSingleSequence)


        // As we have selection sequences formed already, it is time to animate removal process.
        var alreadyRemoved = 0

        selectionSequencesToDelete.forEach { sequence ->
            if (sequence.size > 1) {
                // Here you see why we needed to have a range of selected items. We remove all the
                // group by a single function which will animate all them together.
                notifyItemRangeRemoved(sequence.keys.first() - alreadyRemoved, sequence.size)
                alreadyRemoved += sequence.size
            } else {
                // Here we remove one item with animation.
                notifyItemRemoved(sequence.keys.first() - alreadyRemoved)
                alreadyRemoved += 1
            }
        }

        // We remove all data values.
        (dataList as ArrayList).removeAll(selectedData.values)

        return selectedData.values.toMutableList()
    }


    fun restoreDeletedItems() {

        selectionSequencesToDelete.forEach { sequence ->
            if (sequence.size > 1) {
                notifyItemRangeInserted(sequence.keys.first() , sequence.size)
                (dataList as ArrayList).addAll(sequence.keys.first(), sequence.values)
            } else {
                notifyItemInserted(sequence.keys.first() )
                (dataList as ArrayList).add(sequence.keys.first(), sequence.values.first())
            }
        }

        selectionSequencesToDelete.clear()
    }


    fun clearSelected() {
        selectedData.clear()
    }


    inner class BindingViewHolder(var binding: FragmentMainListItemBinding) : RecyclerView.ViewHolder(binding.root), ViewHolderWithDetails {

        fun bind(cryptocurrency: Cryptocurrency, isSelected: Boolean) {
            binding.cryptocurrency = cryptocurrency

            // Will allow to indicate to the user that the item has been selected.
            binding.root.isSelected = isSelected

            binding.itemRanking.text = String.format("${cryptocurrency.rank}")

            binding.itemImageIcon.setFrontText(getTextFirstChars(cryptocurrency.symbol, FLIPVIEW_CHARACTER_LIMIT))

            // We make an Uri of image that we need to load. Every image unique name is its id.
            val imageUri = Uri.parse(CRYPTOCURRENCY_IMAGE_URL).buildUpon()
                    .appendPath(CRYPTOCURRENCY_IMAGE_SIZE_PX)
                    .appendPath(cryptocurrency.id.toString() + CRYPTOCURRENCY_IMAGE_FILE)
                    .build()

            // Glide generated API from AppGlideModule.
            GlideApp
                    // We need to provide context to make a call.
                    .with(binding.root)
                    // Here you specify which image should be loaded by providing Uri.
                    .load(imageUri)
                    // The way you combine and execute multiple transformations.
                    // WhiteBackground is our own implemented custom transformation.
                    // CircleCrop is default transformation that Glide ships with.
                    .transform(MultiTransformation(WhiteBackground(), CircleCrop()))
                    // The target ImageView your image is supposed to get displayed in.
                    .into(binding.itemImageIcon.imageview_front)

            binding.itemImageIcon.flip(isSelected)


            binding.itemAmountCode.text = String.format("${roundValue(cryptocurrency.amount, ValueType.Crypto)} ${cryptocurrency.symbol}")
            binding.itemPrice.text = String.format("${roundValue(cryptocurrency.priceFiat, ValueType.Fiat)} ${cryptocurrency.currencyFiat}")
            binding.itemAmountFiat.text = String.format("${roundValue(cryptocurrency.amountFiat, ValueType.Fiat)} ${cryptocurrency.currencyFiat}")
            binding.itemPricePercentChange1h7d.text = SpannableStringBuilder(getSpannableValueStyled(binding.root.context, cryptocurrency.pricePercentChange1h, SpannableValueColorStyle.Foreground, ValueType.Percent, "", "%"))
                    .append(binding.root.context.getString(R.string.string_column_coin_separator_change)).append(getSpannableValueStyled(binding.root.context, cryptocurrency.pricePercentChange7d, SpannableValueColorStyle.Foreground, ValueType.Fiat, "", "%"))
            binding.itemPricePercentChange24h.text = getSpannableValueStyled(binding.root.context, cryptocurrency.pricePercentChange24h, SpannableValueColorStyle.Foreground, ValueType.Percent, "", "%")
            binding.itemAmountFiatChange24h.text = getSpannableValueStyled(binding.root.context, cryptocurrency.amountFiatChange24h, SpannableValueColorStyle.Foreground, ValueType.Percent, "", " ${cryptocurrency.currencyFiat}")

            binding.executePendingBindings()
        }

        // Implementation for selection library to access information about specific area.
        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return MainListItemDetails(dataList[adapterPosition].id.toString(), adapterPosition)
        }

    }
}
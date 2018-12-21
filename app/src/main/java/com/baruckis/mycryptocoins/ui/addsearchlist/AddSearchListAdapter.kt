/*
 * Copyright 2018 Andrius Baruckis www.baruckis.com | mycryptocoins.baruckis.com
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

package com.baruckis.mycryptocoins.ui.addsearchlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.db.Cryptocurrency
import com.baruckis.mycryptocoins.databinding.ActivityAddSearchListItemBinding
import com.baruckis.mycryptocoins.utilities.FLIPVIEW_CHARACTER_LIMIT
import com.baruckis.mycryptocoins.utilities.getTextFirstChars

class AddSearchListAdapter(context: Context, private val cryptocurrencyClickCallback: ((Cryptocurrency) -> Unit)?) : BaseAdapter() {

    private var dataList: List<Cryptocurrency> = ArrayList<Cryptocurrency>()
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    
    fun setData(newDataList: List<Cryptocurrency>) {

        dataList = newDataList
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val itemBinding: ActivityAddSearchListItemBinding

        if (view == null) {

            view = inflater.inflate(R.layout.activity_add_search_list_item, parent, false)
            itemBinding = DataBindingUtil.bind<ActivityAddSearchListItemBinding>(view)!!

            itemBinding.root.setOnClickListener {
                itemBinding.cryptocurrency?.let {
                    cryptocurrencyClickCallback?.invoke(it)
                }
            }

            view.tag = itemBinding

        } else {

            itemBinding = view.tag as ActivityAddSearchListItemBinding
        }

        val cryptocurrency = getItem(position) as Cryptocurrency
        itemBinding.cryptocurrency = cryptocurrency
        itemBinding.itemRanking.text = String.format("${cryptocurrency.rank}")

        // Show only first 3 characters of symbol. If symbol has less than 3 characters than show less.
        itemBinding.itemImageIcon.setFrontText(getTextFirstChars(cryptocurrency.symbol, FLIPVIEW_CHARACTER_LIMIT))

        itemBinding.itemName.text = cryptocurrency.name
        itemBinding.itemSymbol.text = cryptocurrency.symbol

        return itemBinding.root
    }

    override fun getItem(position: Int): Any {
        return dataList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataList.size
    }
}
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
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.databinding.ActivityAddSearchListItemBinding

class AddSearchListAdapter(context: Context) : BaseAdapter() {

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
            view.tag = itemBinding

        } else {

            itemBinding = view.tag as ActivityAddSearchListItemBinding
        }

        val cryptocurrency = getItem(position) as Cryptocurrency
        itemBinding.cryptocurrency = cryptocurrency
        itemBinding.itemRanking.text = String.format("${cryptocurrency.rank}")
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
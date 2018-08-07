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

package com.baruckis.mycryptocoins.addsearchlist

import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency

class AddSearchListAdapter(context: Context) : BaseAdapter() {

    private var dataList: List<Cryptocurrency> = ArrayList<Cryptocurrency>()
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    fun setData(newDataList: List<Cryptocurrency>) {
        dataList = newDataList
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: CustomViewHolder

        if (convertView == null) {

            view = inflater.inflate(R.layout.activity_add_search_list_item, parent, false)

            holder = CustomViewHolder()
            holder.rankingTextView = view.findViewById(R.id.item_ranking)
            holder.nameTextView = view.findViewById(R.id.item_name)
            holder.symbolTextView = view.findViewById(R.id.item_symbol)

            view.tag = holder

        } else {

            view = convertView
            holder = convertView.tag as CustomViewHolder
        }

        val cryptocurrency = getItem(position) as Cryptocurrency
        holder.rankingTextView.text = cryptocurrency.rank.toString()
        holder.nameTextView.text = cryptocurrency.name
        holder.symbolTextView.text = cryptocurrency.symbol

        return view
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


    inner class CustomViewHolder {
        lateinit var rankingTextView: AppCompatTextView
        lateinit var nameTextView: AppCompatTextView
        lateinit var symbolTextView: AppCompatTextView
    }

}
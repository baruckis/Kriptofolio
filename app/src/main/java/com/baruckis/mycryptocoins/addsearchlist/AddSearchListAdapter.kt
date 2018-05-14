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

class AddSearchListAdapter(context: Context, private val dataSource: ArrayList<String>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: CustomViewHolder

        if (convertView == null) {

            view = inflater.inflate(R.layout.activity_add_search_list_item, parent, false)

            holder = CustomViewHolder()
            holder.nameTextView = view.findViewById(R.id.item_name)

            view.tag = holder

        } else {

            view = convertView
            holder = convertView.tag as CustomViewHolder
        }

        val nameTextView = holder.nameTextView

        nameTextView.text = getItem(position) as String

        return view
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataSource.size
    }


    inner class CustomViewHolder {
        lateinit var nameTextView: AppCompatTextView
    }

}
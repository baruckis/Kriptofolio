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

package com.baruckis.mycryptocoins.mainlist

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.plusAssign
import androidx.core.text.toSpannable
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency


class MainRecyclerViewAdapter() : RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder>() {

    private lateinit var dataList: ArrayList<Cryptocurrency>

    fun setData(newDataList: ArrayList<Cryptocurrency>) {
        dataList = newDataList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.fragment_main_list_item, parent, false)
        return CustomViewHolder(v)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.txtName?.text = dataList[position].name
        holder.txtRanking?.text = String.format("${dataList[position].rank}")
        holder.txtAmountAndSymbol?.text = String.format("${dataList[position].amount} ${dataList[position].symbol}")
        holder.txtPrice?.text = String.format("${dataList[position].price} ${holder.context.getString(R.string.pref_default_fiat_currency_value)}")
        holder.txtAmountFiat?.text = String.format("${dataList[position].amountFiat} ${holder.context.getString(R.string.pref_default_fiat_currency_value)}")
        holder.txtPricePercentChange1hAnd7d?.text =
                SpannableStringBuilder(getChangeValueStyled(dataList[position].pricePercentChange1h, holder.context))
                        .append(holder.context.getString(R.string.string_column_coin_separator_change))
                        .append(getChangeValueStyled(dataList[position].pricePercentChange7d, holder.context))
        holder.txtPricePercentChange24h?.text = getChangeValueStyled(dataList[position].pricePercentChange24h, holder.context)
        holder.txtAmountFiatChange24h?.text = getChangeValueStyled(dataList[position].amountFiatChange24h, holder.context, true)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    private fun getChangeValueStyled(value:Double, context:Context, isFiat:Boolean = false): Spannable {
        val valueString:String = String.format("$value").plus(if (isFiat) " ${context.getString(R.string.pref_default_fiat_currency_value)}" else "%")
        val valueSpannable:Spannable

        when {
            value > 0 -> {
                valueSpannable = "+$valueString".toSpannable()
                valueSpannable.plusAssign(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorForValueChangePositive)))
            }
            value < 0 -> {
                valueSpannable = valueString.toSpannable()
                valueSpannable.plusAssign(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorForValueChangeNegative)))
            }
            else -> {
                valueSpannable = valueString.toSpannable()
                valueSpannable.plusAssign(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorForMainListItemText)))
            }
        }
        return valueSpannable
    }


    inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val context:Context = itemView.context

        val txtName = itemView.findViewById<TextView>(R.id.item_name)
        val txtRanking = itemView.findViewById<TextView>(R.id.item_ranking)
        val txtAmountAndSymbol = itemView.findViewById<TextView>(R.id.item_amount_symbol)
        val txtPrice = itemView.findViewById<TextView>(R.id.item_price)
        val txtAmountFiat = itemView.findViewById<TextView>(R.id.item_amount_fiat)
        val txtPricePercentChange1hAnd7d = itemView.findViewById<TextView>(R.id.item_price_percent_change_1h_7d)
        val txtPricePercentChange24h = itemView.findViewById<TextView>(R.id.item_price_percent_change_24h)
        val txtAmountFiatChange24h = itemView.findViewById<TextView>(R.id.item_amount_fiat_change_24h)
    }
}
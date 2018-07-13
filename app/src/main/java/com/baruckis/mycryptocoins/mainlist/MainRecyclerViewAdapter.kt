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
import android.view.ViewGroup
import androidx.core.text.plusAssign
import androidx.core.text.toSpannable
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.databinding.FragmentMainListItemBinding


class MainRecyclerViewAdapter() : RecyclerView.Adapter<MainRecyclerViewAdapter.BindingViewHolder>() {

    private var dataList: List<Cryptocurrency> = ArrayList<Cryptocurrency>()

    fun setData(newDataList: List<Cryptocurrency>) {
        dataList = newDataList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = FragmentMainListItemBinding.inflate(inflater, parent, false)

        return BindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) = holder.bind(dataList[position])

    override fun getItemCount(): Int = dataList.size

    private fun getChangeValueStyled(value: Double, context: Context, isFiat: Boolean = false): Spannable {
        val valueString: String = String.format("$value").plus(if (isFiat) " ${context.getString(R.string.pref_default_fiat_currency_value)}" else "%")
        val valueSpannable: Spannable

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

    inner class BindingViewHolder(var binding: FragmentMainListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cryptocurrency: Cryptocurrency) {
            binding.cryptocurrency = cryptocurrency

            binding.itemRanking.text = String.format("${cryptocurrency.rank}")
            binding.itemAmountSymbol.text = String.format("${cryptocurrency.amount} ${cryptocurrency.symbol}")
            binding.itemPrice.text = String.format("${cryptocurrency.price} ${binding.root.context.getString(R.string.pref_default_fiat_currency_value)}")
            binding.itemAmountFiat.text = String.format("${cryptocurrency.amountFiat} ${binding.root.context.getString(R.string.pref_default_fiat_currency_value)}")
            binding.itemPricePercentChange1h7d.text = SpannableStringBuilder(getChangeValueStyled(cryptocurrency.pricePercentChange1h, binding.root.context))
                    .append(binding.root.context.getString(R.string.string_column_coin_separator_change))
                    .append(getChangeValueStyled(cryptocurrency.pricePercentChange7d, binding.root.context))
            binding.itemPricePercentChange24h.text = getChangeValueStyled(cryptocurrency.pricePercentChange24h, binding.root.context)
            binding.itemAmountFiatChange24h.text = getChangeValueStyled(cryptocurrency.amountFiatChange24h, binding.root.context, true)

            binding.executePendingBindings()
        }
    }
}
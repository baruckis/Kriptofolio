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

import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import com.baruckis.mycryptocoins.R
import com.baruckis.mycryptocoins.data.Cryptocurrency
import com.baruckis.mycryptocoins.databinding.FragmentMainListItemBinding
import com.baruckis.mycryptocoins.utilities.SpannableValueColorStyle
import com.baruckis.mycryptocoins.utilities.ValueType
import com.baruckis.mycryptocoins.utilities.getSpannableValueStyled
import com.baruckis.mycryptocoins.utilities.roundValue


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

    inner class BindingViewHolder(var binding: FragmentMainListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cryptocurrency: Cryptocurrency) {
            binding.cryptocurrency = cryptocurrency

            binding.itemRanking.text = String.format("${cryptocurrency.rank}")
            binding.itemAmountCode.text = String.format("${roundValue(cryptocurrency.amount, ValueType.Crypto)} ${cryptocurrency.symbol}")
            binding.itemPrice.text = String.format("${roundValue(cryptocurrency.priceFiat, ValueType.Fiat)} ${cryptocurrency.currencyFiat}")
            binding.itemAmountFiat.text = String.format("${roundValue(cryptocurrency.amountFiat, ValueType.Fiat)} ${cryptocurrency.currencyFiat}")
            binding.itemPricePercentChange1h7d.text = SpannableStringBuilder(getSpannableValueStyled(binding.root.context, cryptocurrency.pricePercentChange1h, SpannableValueColorStyle.Foreground, ValueType.Percent, "", "%"))
                    .append(binding.root.context.getString(R.string.string_column_coin_separator_change)).append(getSpannableValueStyled(binding.root.context, cryptocurrency.pricePercentChange7d, SpannableValueColorStyle.Foreground, ValueType.Fiat, "", "%" ))
            binding.itemPricePercentChange24h.text = getSpannableValueStyled(binding.root.context, cryptocurrency.pricePercentChange24h, SpannableValueColorStyle.Foreground, ValueType.Percent, "", "%")
            binding.itemAmountFiatChange24h.text = getSpannableValueStyled(binding.root.context, cryptocurrency.amountFiatChange24h, SpannableValueColorStyle.Foreground, ValueType.Percent, "", " ${cryptocurrency.currencyFiat}")

            binding.executePendingBindings()
        }
    }
}
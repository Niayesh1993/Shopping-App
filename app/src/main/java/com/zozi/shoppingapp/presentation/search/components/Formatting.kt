package com.zozi.shoppingapp.presentation.search.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val priceFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
private val ratingFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.US))

fun Double.formatAsPrice(): String = "€ ${priceFormat.format(this)}"

fun Double.formatAsRating(): String = ratingFormat.format(this)

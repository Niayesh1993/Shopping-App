package com.zozi.shoppingapp.presentation.search.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.zozi.shoppingapp.domain.model.Availability
import com.zozi.shoppingapp.domain.model.Price
import com.zozi.shoppingapp.domain.model.Product
import com.zozi.shoppingapp.domain.model.Promotion
import com.zozi.shoppingapp.domain.model.PromotionType
import com.zozi.shoppingapp.domain.model.Rating
import com.zozi.shoppingapp.ui.theme.ShoppingAppTheme

/**
 * Wrapper for every preview in this package.
 *
 * Dynamic colour is switched off on purpose: it is on by default in the real app, but it reads the
 * palette from the device wallpaper, so previews would otherwise render differently per machine and
 * Android version. The [Surface] stands in for the background the real screen gets from `Scaffold`.
 */
@Composable
internal fun PreviewContainer(content: @Composable () -> Unit) {
    ShoppingAppTheme(dynamicColor = false) {
        Surface {
            content()
        }
    }
}

/*
 * Sample products for the previews. `imageUrl` is null throughout: the preview renderer has no
 * network, so a real URL would only ever draw an empty box.
 */

/** Everything populated: rating, several USPs, next-day delivery, no discount. */
internal val previewProduct = Product(
    id = 793652L,
    name = "Apple iPhone X 256GB Zilver",
    imageUrl = null,
    usps = listOf(
        "256 GB opslagcapaciteit",
        "iOS 11",
        "5,8 inch Super Retina-display",
    ),
    price = Price(salesIncVat = 1279.0),
    rating = Rating(average = 9.2, count = 209),
    availability = Availability(2),
    hasNextDayDelivery = true,
    promotion = null,
)

/** Exercises the struck-through list price, the discount badge and the promotion label. */
internal val previewDiscountedProduct = Product(
    id = 733904L,
    name = "Apple MacBook Air (2017) MQD32N/A",
    imageUrl = null,
    usps = listOf("13,3 inch scherm", "128 GB SSD"),
    price = Price(salesIncVat = 579.0, listIncVat = 626.0),
    rating = Rating(average = 8.6, count = 42),
    availability = Availability(3),
    hasNextDayDelivery = true,
    promotion = Promotion(text = "Actieprijs", type = PromotionType.ACTION_PRICE),
)

/** The sparse end: no rating, no USPs, no promotion, no next-day delivery, and a name that wraps. */
internal val previewMinimalProduct = Product(
    id = 1L,
    name = "Apple Watch Series 3 42mm Zilver Aluminium met Wit Sportbandje",
    imageUrl = null,
    usps = emptyList(),
    price = Price(salesIncVat = 24.99),
    rating = null,
    availability = Availability.UNKNOWN,
    hasNextDayDelivery = false,
    promotion = null,
)

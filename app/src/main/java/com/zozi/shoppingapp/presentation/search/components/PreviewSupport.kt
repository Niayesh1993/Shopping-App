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

@Composable
internal fun PreviewContainer(content: @Composable () -> Unit) {
    ShoppingAppTheme(dynamicColor = false) {
        Surface {
            content()
        }
    }
}

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

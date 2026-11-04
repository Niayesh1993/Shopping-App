package com.zozi.shoppingapp.testing

import com.zozi.shoppingapp.domain.model.Availability
import com.zozi.shoppingapp.domain.model.Price
import com.zozi.shoppingapp.domain.model.Product
import com.zozi.shoppingapp.domain.model.ProductPage
import com.zozi.shoppingapp.domain.model.Promotion
import com.zozi.shoppingapp.domain.model.PromotionType
import com.zozi.shoppingapp.domain.model.Rating

fun product(
    id: Long = 1L,
    name: String = "Apple iPhone X 256GB Zilver",
    imageUrl: String? = null,
    usps: List<String> = listOf("256 GB opslagcapaciteit"),
    price: Price = Price(salesIncVat = 1279.0),
    rating: Rating? = Rating(average = 9.2, count = 209),
    availability: Availability = Availability(2),
    hasNextDayDelivery: Boolean = true,
    promotion: Promotion? = null,
): Product = Product(
    id = id,
    name = name,
    imageUrl = imageUrl,
    usps = usps,
    price = price,
    rating = rating,
    availability = availability,
    hasNextDayDelivery = hasNextDayDelivery,
    promotion = promotion,
)

fun productPage(
    products: List<Product> = listOf(product()),
    currentPage: Int = 1,
    pageCount: Int = 1,
    totalResults: Int = products.size,
): ProductPage = ProductPage(
    products = products,
    currentPage = currentPage,
    pageCount = pageCount,
    totalResults = totalResults,
)

fun promotion(
    text: String = "aanbieding",
    type: PromotionType = PromotionType.ACTION_PRICE,
): Promotion = Promotion(text = text, type = type)

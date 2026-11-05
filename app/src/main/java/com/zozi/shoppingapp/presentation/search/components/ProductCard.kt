package com.zozi.shoppingapp.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.model.Product

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = stringResource(
                    R.string.product_image_description,
                    product.name,
                ),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                product.promotion?.let { promotion ->
                    Text(
                        text = promotion.text,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                product.rating?.takeIf { it.hasReviews }?.let { rating ->
                    RatingRow(
                        average = rating.average,
                        count = rating.count,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (product.usps.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        product.usps.take(MAX_VISIBLE_USPS).forEach { usp ->
                            Text(
                                text = "• $usp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                PriceRow(product = product, modifier = Modifier.padding(top = 8.dp))

                DeliveryRow(
                    hasNextDayDelivery = product.hasNextDayDelivery,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RatingRow(
    average: Double,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val formattedAverage = average.formatAsRating()
    val description = pluralStringResource(R.plurals.product_rating, count, formattedAverage, count)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = formattedAverage,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 2.dp),
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun PriceRow(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = product.price.salesIncVat.formatAsPrice(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        val listPrice = product.price.listIncVat
        if (product.price.hasDiscount && listPrice != null) {
            Text(
                text = listPrice.formatAsPrice(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = TextDecoration.LineThrough,
            )
            product.price.discountPercentage?.let { percentage ->
                Text(
                    text = stringResource(R.string.product_discount, percentage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DeliveryRow(
    hasNextDayDelivery: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!hasNextDayDelivery) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.product_next_day_delivery),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private const val MAX_VISIBLE_USPS = 3

@PreviewLightDark
@Composable
private fun ProductCardPreview() {
    PreviewContainer {
        ProductCard(product = previewProduct, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "Discount and promotion")
@Composable
private fun ProductCardDiscountedPreview() {
    PreviewContainer {
        ProductCard(product = previewDiscountedProduct, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "No rating, USPs or delivery")
@Composable
private fun ProductCardMinimalPreview() {
    PreviewContainer {
        ProductCard(product = previewMinimalProduct, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "More USPs than fit", showBackground = true)
@Composable
private fun ProductCardTruncatedUspsPreview() {
    PreviewContainer {
        ProductCard(
            product = previewProduct.copy(
                usps = previewProduct.usps + listOf("Face ID", "Draadloos opladen"),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingRowPreview() {
    PreviewContainer {
        RatingRow(average = 9.2, count = 209, modifier = Modifier.padding(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceRowPreview() {
    PreviewContainer {
        PriceRow(product = previewDiscountedProduct, modifier = Modifier.padding(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun DeliveryRowPreview() {
    PreviewContainer {
        DeliveryRow(hasNextDayDelivery = true, modifier = Modifier.padding(8.dp))
    }
}

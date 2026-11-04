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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zozi.shoppingapp.R
import com.zozi.shoppingapp.domain.model.Availability
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

                AvailabilityRow(
                    availability = product.availability,
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
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$formattedAverage out of 10, based on $count reviews"
        },
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
        if (product.price.hasDiscount) {
            Text(
                text = product.price.listIncVat.orZero().formatAsPrice(),
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
private fun AvailabilityRow(
    availability: Availability,
    hasNextDayDelivery: Boolean,
    modifier: Modifier = Modifier,
) {
    val availabilityLabel = when (availability) {
        Availability.IN_STOCK -> stringResource(R.string.availability_in_stock)
        Availability.LIMITED_STOCK -> stringResource(R.string.availability_limited_stock)
        Availability.OUT_OF_STOCK -> stringResource(R.string.availability_out_of_stock)
        Availability.UNKNOWN -> null
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (availabilityLabel != null) {
            Text(
                text = availabilityLabel,
                style = MaterialTheme.typography.labelSmall,
                color = when (availability) {
                    Availability.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (hasNextDayDelivery) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.product_next_day_delivery),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp),
                )
            }
        }
    }
}

private const val MAX_VISIBLE_USPS = 3

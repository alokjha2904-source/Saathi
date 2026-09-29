package com.saathi.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.ui.theme.ErrorSOS
import com.saathi.app.ui.theme.PrimaryTeal
import com.saathi.app.ui.theme.SafeGreen
import com.saathi.app.ui.theme.SecondaryAmber
import com.saathi.app.util.LocationResult

@Composable
fun LiveLocationMap(
    currentLocation: LocationResult?,
    locationHistory: List<LocationResult>,
    isTracking: Boolean,
    isStale: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101416)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Tactical Canvas Map Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxDim = minOf(size.width, size.height)

                // Grid lines
                val gridSpacing = 40.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }

                // Range Rings (Concentric Circles)
                val ringRadii = listOf(maxDim * 0.18f, maxDim * 0.32f, maxDim * 0.44f)
                ringRadii.forEachIndexed { idx, radius ->
                    drawCircle(
                        color = PrimaryTeal.copy(alpha = 0.12f + (idx * 0.05f)),
                        radius = radius,
                        center = center,
                        style = Stroke(
                            width = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )
                    )
                }

                // Breadcrumb path connecting location history
                if (locationHistory.size > 1 && currentLocation != null) {
                    val path = Path()
                    val count = locationHistory.size
                    locationHistory.forEachIndexed { index, loc ->
                        // Relative delta from current location mapped to canvas space
                        val dLat = (loc.latitude - currentLocation.latitude) * 50000f
                        val dLng = (loc.longitude - currentLocation.longitude) * 50000f
                        val pt = Offset(
                            (center.x + dLng.toFloat()).coerceIn(20f, size.width - 20f),
                            (center.y - dLat.toFloat()).coerceIn(20f, size.height - 20f)
                        )
                        if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)

                        // Breadcrumb dot
                        drawCircle(
                            color = SecondaryAmber.copy(alpha = (index.toFloat() / count) * 0.8f),
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                    }
                    drawPath(
                        path = path,
                        color = SecondaryAmber.copy(alpha = 0.6f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Radar pulse circle
                if (isTracking && currentLocation != null) {
                    val accRadius = (maxDim * 0.22f) * pulseRadiusFactor
                    drawCircle(
                        color = PrimaryTeal.copy(alpha = pulseAlpha),
                        radius = accRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Current Location Accuracy Shell
                val accuracyColor = if (isStale) SecondaryAmber else SafeGreen
                drawCircle(
                    color = accuracyColor.copy(alpha = 0.15f),
                    radius = maxDim * 0.12f,
                    center = center
                )
                drawCircle(
                    color = accuracyColor.copy(alpha = 0.6f),
                    radius = maxDim * 0.12f,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Center Beacon Point
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = accuracyColor,
                    radius = 5.dp.toPx(),
                    center = center
                )
            }

            // Top Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isTracking) SafeGreen else SecondaryAmber,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStale) "LAST KNOWN LOCATION" else if (isTracking) "LIVE TRACKING ● ACTIVE" else "GPS PENDING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStale) SecondaryAmber else if (isTracking) SafeGreen else Color.White
                        )
                    }
                }

                if (currentLocation != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "±${currentLocation.accuracy.toInt()}m precision",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Bottom Navigation Card & Coordinates
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (currentLocation != null) {
                            Text(
                                text = "${"%.5f".format(currentLocation.latitude)}, ${"%.5f".format(currentLocation.longitude)}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Path Points: ${locationHistory.size} updates",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "Acquiring live GPS satellites...",
                                color = SecondaryAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (currentLocation != null) {
                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:${currentLocation.latitude},${currentLocation.longitude}?q=${currentLocation.latitude},${currentLocation.longitude}(Emergency+Location)")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

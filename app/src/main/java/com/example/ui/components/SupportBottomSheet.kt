package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val DEVELOPER_UPI_ID = "BHARATPE2Z0E0U7P7C67386@unitype"
private const val DEVELOPER_PAYEE_NAME = "Deep Singh"
private const val PAYMENT_NOTE_TAG = "SupportGToolX"

private data class SupportTier(
    val amount: Int,
    val title: String,
    val icon: ImageVector
)

/**
 * Builds a strict NPCI-compliant UPI URI with exact 2-decimal formatting and percent-encoding.
 */
private fun buildNpciUpiUri(amountValue: Double): Uri {
    val formattedAmount = String.format(Locale.US, "%.2f", amountValue)
    return Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", DEVELOPER_UPI_ID)
        .appendQueryParameter("pn", DEVELOPER_PAYEE_NAME)
        .appendQueryParameter("am", formattedAmount)
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", PAYMENT_NOTE_TAG)
        .build()
}

/**
 * High-performance, offline QR Code Generator using ZXing.
 */
private suspend fun generateQrImageBitmap(content: String, sizePx: Int = 450): ImageBitmap? {
    return withContext(Dispatchers.Default) {
        try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportBottomSheet(
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val tiers = remember {
        listOf(
            SupportTier(21, "Chai", Icons.Rounded.LocalCafe),
            SupportTier(51, "Coffee", Icons.Rounded.Coffee),
            SupportTier(101, "Booster", Icons.Rounded.Bolt),
            SupportTier(501, "Patron", Icons.Rounded.WorkspacePremium)
        )
    }

    var selectedAmount by remember { mutableStateOf<Int?>(51) }
    var customAmountText by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    val activeAmountDouble = remember(selectedAmount, customAmountText, isCustomSelected) {
        if (isCustomSelected && customAmountText.isNotBlank()) {
            customAmountText.trim().toDoubleOrNull() ?: 51.0
        } else {
            selectedAmount?.toDouble() ?: 51.0
        }
    }

    val displayAmountString = remember(activeAmountDouble) {
        if (activeAmountDouble % 1.0 == 0.0) {
            activeAmountDouble.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", activeAmountDouble)
        }
    }

    val npciUpiUri = remember(activeAmountDouble) {
        buildNpciUpiUri(activeAmountDouble)
    }

    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(npciUpiUri) {
        qrBitmap = generateQrImageBitmap(npciUpiUri.toString(), sizePx = 450)
    }

    fun copyUpiId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("GTOOL X UPI ID", DEVELOPER_UPI_ID)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "UPI ID copied to clipboard!", Toast.LENGTH_SHORT).show()
        onShowSnackbar("UPI ID ($DEVELOPER_UPI_ID) copied!")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E131F),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.22f))
            )
        },
        modifier = Modifier.testTag("support_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolunteerActivism,
                        contentDescription = "Support GTOOL X",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fuel GTOOL X",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Keep development independent & 100% ad-free",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. QR Section Card (Clean rounded surface with no bottom lines)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF131824).copy(alpha = 0.85f))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap!!,
                                contentDescription = "Dynamic UPI Payment QR Code",
                                modifier = Modifier.size(180.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = "Loading QR Code",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Scan with any UPI App • Dynamic ₹$displayAmountString",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Amount Tier Header Label
            Text(
                text = "Select Amount Tier",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Pure Tier Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiers.forEach { tier ->
                    val isSelected = !isCustomSelected && selectedAmount == tier.amount
                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.65f) else Color.Transparent,
                        animationSpec = tween(200),
                        label = "tier_border"
                    )
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.14f) else Color(0xFF131824).copy(alpha = 0.85f),
                        animationSpec = tween(200),
                        label = "tier_bg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(animatedBgColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, animatedBorderColor, RoundedCornerShape(16.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                isCustomSelected = false
                                selectedAmount = tier.amount
                                focusManager.clearFocus()
                            }
                            .padding(vertical = 11.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = tier.icon,
                                contentDescription = tier.title,
                                tint = if (isSelected) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "₹${tier.amount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color(0xFFF59E0B) else Color.White
                            )
                            Text(
                                text = tier.title,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Custom Amount Input Surface (Clean Borderless Capsule)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131B2E).copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        color = Color(0xFFF59E0B),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = customAmountText,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                customAmountText = input
                                if (input.isNotBlank()) {
                                    isCustomSelected = true
                                    selectedAmount = null
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_support_amount"),
                        decorationBox = { innerTextField ->
                            if (customAmountText.isEmpty()) {
                                Text(
                                    text = "Enter custom amount (e.g. 250)",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Official UPI ID Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131824).copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "OFFICIAL UPI ID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                letterSpacing = 0.5.sp
                            )
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = DEVELOPER_UPI_ID,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { copyUpiId() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy UPI",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

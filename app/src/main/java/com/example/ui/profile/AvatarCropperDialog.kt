package com.example.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.VibrantOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun AvatarCropperDialog(
    sourceUri: Uri,
    userId: String,
    onDismiss: () -> Unit,
    onCroppedSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decodedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            val bmp = decodeScaledBitmap(context, sourceUri, maxDim = 1024)
            if (bmp != null) {
                decodedBitmap = bmp
            } else {
                loadError = "Unable to decode selected image."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("avatar_cropper_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Crop Profile Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pinch, drag, or use the slider to frame your circular avatar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                val cropSizeDp = 240.dp
                val cropSizePx = with(LocalDensity.current) { cropSizeDp.toPx() }

                Box(
                    modifier = Modifier
                        .size(cropSizeDp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianBlack)
                        .pointerInput(decodedBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3.5f)
                                offsetX = (offsetX + pan.x).coerceIn(-cropSizePx * 0.75f, cropSizePx * 0.75f)
                                offsetY = (offsetY + pan.y).coerceIn(-cropSizePx * 0.75f, cropSizePx * 0.75f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = decodedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected avatar preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )
                    } else if (loadError != null) {
                        Text(loadError!!, color = MaterialTheme.colorScheme.error)
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val circlePath = Path().apply {
                            addOval(
                                androidx.compose.ui.geometry.Rect(
                                    offset = Offset.Zero,
                                    size = size
                                )
                            )
                        }
                        clipPath(circlePath, clipOp = ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.65f))
                        }
                        drawCircle(
                            color = VibrantOrange,
                            radius = size.minDimension / 2f - 2f,
                            style = Stroke(width = 4f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 1f..3.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = VibrantOrange,
                        activeTrackColor = VibrantOrange
                    ),
                    modifier = Modifier.testTag("crop_zoom_slider")
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val bmp = decodedBitmap ?: return@Button
                            isSaving = true
                            scope.launch {
                                val savedPath = withContext(Dispatchers.IO) {
                                    saveCroppedBitmap(
                                        context = context,
                                        source = bmp,
                                        userId = userId,
                                        viewportSizePx = cropSizePx,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY
                                    )
                                }
                                isSaving = false
                                if (savedPath != null) {
                                    onCroppedSaved(savedPath)
                                } else {
                                    loadError = "Failed to save cropped avatar."
                                }
                            }
                        },
                        enabled = decodedBitmap != null && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("save_cropped_avatar_button")
                    ) {
                        Text(if (isSaving) "Saving…" else "Save Avatar", color = ObsidianBlack)
                    }
                }
            }
        }
    }
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxSide = max(boundsOpts.outWidth, boundsOpts.outHeight)
        var sample = 1
        while (maxSide / sample > maxDim * 2) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveCroppedBitmap(
    context: Context,
    source: Bitmap,
    userId: String,
    viewportSizePx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): String? {
    return try {
        val outSize = 512
        val output = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(output)

        val baseScale = max(
            outSize.toFloat() / source.width.toFloat(),
            outSize.toFloat() / source.height.toFloat()
        )
        val totalScale = baseScale * scale
        val ratioToOut = outSize / max(1f, viewportSizePx)
        val drawnW = source.width * totalScale
        val drawnH = source.height * totalScale
        val left = (outSize - drawnW) / 2f + offsetX * ratioToOut
        val top = (outSize - drawnH) / 2f + offsetY * ratioToOut

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, RectF(left, top, left + drawnW, top + drawnH), paint)

        val safeUid = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(context.filesDir, "custom_avatar_${safeUid}_${System.currentTimeMillis()}.jpg")
        // Clean previous custom avatar files for this user
        context.filesDir.listFiles()?.filter {
            it.name.startsWith("custom_avatar_${safeUid}_") && it.absolutePath != file.absolutePath
        }?.forEach { it.delete() }

        FileOutputStream(file).use { out ->
            output.compress(Bitmap.CompressFormat.JPEG, min(90, 100), out)
        }
        file.absolutePath
    } catch (_: Exception) {
        null
    }
}

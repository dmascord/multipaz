package org.multipaz.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.bytestring.ByteString
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.multipaz.compose.camera.CameraFrame
import org.multipaz.compose.camera.CameraImage
import org.multipaz.util.toNSData
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGDataProviderCopyData
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageCreateCopyWithColorSpace
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.CoreGraphics.CGImageGetBytesPerRow
import platform.CoreGraphics.CGImageGetDataProvider
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import kotlin.coroutines.CoroutineContext
import kotlin.math.PI

actual fun getApplicationInfo(appId: String): ApplicationInfo {
    throw NotImplementedError("This information is not available not implemented on iOS")
}

@OptIn(ExperimentalForeignApi::class)
actual fun decodeImage(encodedData: ByteArray): ImageBitmap {
    val source = UIImage(data = encodedData.toNSData()) ?: return ImageBitmap(1, 1)
    val imageRef = CGImageCreateCopyWithColorSpace(source.CGImage, CGColorSpaceCreateDeviceRGB())
        ?: return ImageBitmap(1, 1)
    val width = CGImageGetWidth(imageRef).toInt()
    val height = CGImageGetHeight(imageRef).toInt()
    val bytesPerRow = CGImageGetBytesPerRow(imageRef).toInt()
    val data = CGDataProviderCopyData(CGImageGetDataProvider(imageRef))
    val bytePointer = data?.let { CFDataGetBytePtr(it) } ?: run {
        CFRelease(imageRef)
        return ImageBitmap(1, 1)
    }
    val length = CFDataGetLength(data).toInt()
    val alphaType = when (CGImageGetAlphaInfo(imageRef)) {
        CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast -> ColorAlphaType.PREMUL
        CGImageAlphaInfo.kCGImageAlphaFirst,
        CGImageAlphaInfo.kCGImageAlphaLast -> ColorAlphaType.UNPREMUL
        CGImageAlphaInfo.kCGImageAlphaNone,
        CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst,
        CGImageAlphaInfo.kCGImageAlphaNoneSkipLast -> ColorAlphaType.OPAQUE
        else -> ColorAlphaType.UNKNOWN
    }
    val bytes = bytePointer.readBytes(length)
    CFRelease(data)
    CFRelease(imageRef)
    return Image.makeRaster(
        imageInfo = ImageInfo(
            width = width,
            height = height,
            colorType = ColorType.RGBA_8888,
            alphaType = alphaType
        ),
        bytes = bytes,
        rowBytes = bytesPerRow
    ).toComposeImageBitmap()
}

actual fun encodeImageToPng(image: ImageBitmap): ByteString {
    val data = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG, 100)
        ?: throw IllegalStateException("Error encoding image to PNG")
    return ByteString(data.bytes)
}


actual fun cropRotateScaleImage(
    frameData: CameraFrame,
    cx: Double, // From top-left of image data, Y increases downwards.
    cy: Double, // From top-left of image data, Y increases downwards.
    angleDegrees: Double,
    outputWidthPx: Int,
    outputHeightPx: Int,
    targetWidthPx: Int
): ImageBitmap {
    return cropRotateScaleImage(
        originalUIImage = frameData.cameraImage.uiImage,
        isLandscape = frameData.isLandscape,
        cx = cx,
        cy = cy,
        angleDegrees = angleDegrees,
        outputWidthPx = outputWidthPx,
        outputHeightPx = outputHeightPx,
        targetWidthPx = targetWidthPx
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun cropRotateScaleImage(
    originalUIImage: UIImage,
    isLandscape: Boolean,
    cx: Double, // ASSUMED: from top-left of image data, Y increases downwards
    cy: Double, // ASSUMED: from top-left of image data, Y increases downwards
    angleDegrees: Double,
    outputWidthPx: Int,
    outputHeightPx: Int,
    targetWidthPx: Int
): ImageBitmap {
    val originalImageWidth = originalUIImage.size.useContents { width }
    val originalImageHeight = originalUIImage.size.useContents { height }

    // Ensure the output is always square based on targetWidthPx.
    val finalCanvasWidth = targetWidthPx.toDouble()
    val finalCanvasHeight = targetWidthPx.toDouble()
    val scaleFactor = targetWidthPx.toDouble() / outputWidthPx.toDouble()
    val rendererFormat = UIGraphicsImageRendererFormat.defaultFormat().apply {
        opaque = true
        scale = 1.0 // Force 1:1 pixel mapping.
        preferredRange = 2
    }
    val renderer = UIGraphicsImageRenderer(
        size = CGSizeMake(finalCanvasWidth, finalCanvasHeight),
        format = rendererFormat
    )

    val resultUIImage = renderer.imageWithActions { rendererContext ->
        val cgContext = rendererContext?.CGContext

        // Transformations to center (cx,cy) and crop/scale.
        CGContextTranslateCTM(cgContext, finalCanvasWidth / 2.0, finalCanvasHeight / 2.0)
        CGContextScaleCTM(cgContext, scaleFactor, scaleFactor)
        CGContextRotateCTM(cgContext, angleDegrees * PI / 180.0)
        CGContextTranslateCTM(cgContext, -cx, -cy)

        // Draw.
        originalUIImage.drawInRect(
            CGRectMake(0.0, 0.0, originalImageWidth, originalImageHeight)
        )
    }
    return CameraImage(resultUIImage).toImageBitmap()
}

/** Convert ImageBitmap to iOS UIImage. */
@OptIn(ExperimentalForeignApi::class)
fun ImageBitmap.toUIImage(): UIImage? {
    val width = this.width
    val height = this.height
    val buffer = IntArray(width * height)

    this.readPixels(buffer)

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        data = buffer.refTo(0),
        width = width.toULong(),
        height = height.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (4 * width).toULong(),
        space = colorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    )

    val cgImage = CGBitmapContextCreateImage(context)
    return cgImage?.let { UIImage.imageWithCGImage(it) }
}

actual fun ImageBitmap.cropRotateScaleImage(
    cx: Double,
    cy: Double,
    angleDegrees: Double,
    outputWidthPx: Int,
    outputHeightPx: Int,
    targetWidthPx: Int
): ImageBitmap {
    return cropRotateScaleImage(
        originalUIImage = toUIImage()!!,
        isLandscape = false,
        cx = cx,
        cy = cy,
        angleDegrees = angleDegrees,
        outputWidthPx = outputWidthPx,
        outputHeightPx = outputHeightPx,
        targetWidthPx = targetWidthPx
    )
}

@Composable
actual fun rememberUiBoundCoroutineScope(
    getContext: @DisallowComposableCalls () -> CoroutineContext
): CoroutineScope = rememberCoroutineScope(getContext)

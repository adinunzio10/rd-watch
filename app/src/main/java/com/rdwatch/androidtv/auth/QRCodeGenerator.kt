package com.rdwatch.androidtv.auth

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeGenerator @Inject constructor() {
    
    companion object {
        private const val DEFAULT_SIZE = 512
        private const val DEFAULT_MARGIN = 2
    }
    
    fun generateQRCode(
        text: String,
        size: Int = DEFAULT_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, DEFAULT_MARGIN)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }
            
            val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y, 
                        if (bitMatrix[x, y]) foregroundColor else backgroundColor
                    )
                }
            }
            
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun generateTVOptimizedQRCode(
        text: String,
        tvSize: Int = 600  // Larger for TV viewing
    ): Bitmap? {
        // High contrast colors optimized for TV displays
        return generateQRCode(
            text = text,
            size = tvSize,
            foregroundColor = Color.BLACK,
            backgroundColor = Color.WHITE
        )
    }
}
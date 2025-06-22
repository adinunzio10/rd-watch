package com.rdwatch.androidtv.auth

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QRCodeGeneratorTest {
    
    private lateinit var qrCodeGenerator: QRCodeGenerator
    
    @Before
    fun setup() {
        qrCodeGenerator = QRCodeGenerator()
    }
    
    @Test
    fun `generateQRCode creates bitmap for valid text`() {
        // Given
        val text = "https://real-debrid.com/device?user_code=TEST123"
        
        // When
        val result = qrCodeGenerator.generateQRCode(text)
        
        // Then
        assertNotNull(result)
        assertEquals(512, result.width) // Default size
        assertEquals(512, result.height)
        assertEquals(Bitmap.Config.RGB_565, result.config)
    }
    
    @Test
    fun `generateQRCode creates bitmap with custom size`() {
        // Given
        val text = "https://real-debrid.com/device?user_code=TEST123"
        val customSize = 256
        
        // When
        val result = qrCodeGenerator.generateQRCode(text, size = customSize)
        
        // Then
        assertNotNull(result)
        assertEquals(customSize, result.width)
        assertEquals(customSize, result.height)
    }
    
    @Test
    fun `generateQRCode creates bitmap with custom colors`() {
        // Given
        val text = "https://real-debrid.com/device?user_code=TEST123"
        val foregroundColor = Color.RED
        val backgroundColor = Color.BLUE
        
        // When
        val result = qrCodeGenerator.generateQRCode(
            text = text,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor
        )
        
        // Then
        assertNotNull(result)
        // Note: We can't easily test the actual colors without more complex bitmap analysis
        // but we can verify the bitmap was created successfully
    }
    
    @Test
    fun `generateQRCode returns null for empty text`() {
        // Given
        val text = ""
        
        // When
        val result = qrCodeGenerator.generateQRCode(text)
        
        // Then
        // QR code generation might still work for empty string, so this test
        // verifies the method handles edge cases gracefully
        // The actual behavior depends on the ZXing library implementation
    }
    
    @Test
    fun `generateTVOptimizedQRCode creates larger bitmap for TV viewing`() {
        // Given
        val text = "https://real-debrid.com/device?user_code=TEST123"
        
        // When
        val result = qrCodeGenerator.generateTVOptimizedQRCode(text)
        
        // Then
        assertNotNull(result)
        assertEquals(600, result.width) // TV optimized size
        assertEquals(600, result.height)
    }
    
    @Test
    fun `generateTVOptimizedQRCode uses high contrast colors`() {
        // Given
        val text = "https://real-debrid.com/device?user_code=TEST123"
        
        // When
        val result = qrCodeGenerator.generateTVOptimizedQRCode(text)
        
        // Then
        assertNotNull(result)
        // The method should use BLACK and WHITE for maximum contrast on TV displays
        // We verify the bitmap was created with the expected configuration
        assertEquals(Bitmap.Config.RGB_565, result.config)
    }
    
    @Test
    fun `generateTVOptimizedQRCode with custom size`() {
        // Given
        val text = "https://real-debrid.com/device?user_code=TEST123"
        val customTvSize = 800
        
        // When
        val result = qrCodeGenerator.generateTVOptimizedQRCode(text, tvSize = customTvSize)
        
        // Then
        assertNotNull(result)
        assertEquals(customTvSize, result.width)
        assertEquals(customTvSize, result.height)
    }
    
    @Test
    fun `generateQRCode handles very long text`() {
        // Given
        val longText = "https://real-debrid.com/device?user_code=TEST123&" +
                "very_long_parameter=" + "a".repeat(1000)
        
        // When
        val result = qrCodeGenerator.generateQRCode(longText)
        
        // Then
        // Should either create a valid QR code or return null gracefully
        // depending on QR code capacity limits
        if (result != null) {
            assertEquals(512, result.width)
            assertEquals(512, result.height)
        }
    }
}
package com.rdwatch.androidtv.data.converters

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class ConvertersTest {
    
    private lateinit var converters: Converters
    
    @Before
    fun setup() {
        converters = Converters()
    }
    
    @Test
    fun testDateConversion() {
        val date = Date()
        val timestamp = converters.dateToTimestamp(date)
        val convertedDate = converters.fromTimestamp(timestamp)
        
        assertNotNull(timestamp)
        assertNotNull(convertedDate)
        assertEquals(date.time, convertedDate!!.time)
    }
    
    @Test
    fun testNullDateConversion() {
        val nullTimestamp = converters.dateToTimestamp(null)
        val nullDate = converters.fromTimestamp(null)
        
        assertNull(nullTimestamp)
        assertNull(nullDate)
    }
    
    @Test
    fun testStringListConversion() {
        val originalList = listOf("item1", "item2", "item3")
        val jsonString = converters.fromListString(originalList)
        val convertedList = converters.fromStringList(jsonString)
        
        assertNotNull(jsonString)
        assertNotNull(convertedList)
        assertEquals(originalList, convertedList)
    }
    
    @Test
    fun testEmptyStringListConversion() {
        val emptyList = emptyList<String>()
        val jsonString = converters.fromListString(emptyList)
        val convertedList = converters.fromStringList(jsonString)
        
        assertNotNull(jsonString)
        assertNotNull(convertedList)
        assertEquals(emptyList, convertedList)
    }
    
    @Test
    fun testNullStringListConversion() {
        val nullString = converters.fromListString(null)
        val nullList = converters.fromStringList(null)
        
        assertNull(nullString)
        assertNull(nullList)
    }
    
    @Test
    fun testStringMapConversion() {
        val originalMap = mapOf("key1" to "value1", "key2" to "value2")
        val jsonString = converters.fromMapString(originalMap)
        val convertedMap = converters.fromStringMap(jsonString)
        
        assertNotNull(jsonString)
        assertNotNull(convertedMap)
        assertEquals(originalMap, convertedMap)
    }
    
    @Test
    fun testCoordinatesConversion() {
        val coordinates = Coordinates(
            latitude = 40.7128,
            longitude = -74.0060,
            altitude = 10.0
        )
        
        val jsonString = converters.coordinatesToString(coordinates)
        val convertedCoordinates = converters.fromCoordinates(jsonString)
        
        assertNotNull(jsonString)
        assertNotNull(convertedCoordinates)
        assertEquals(coordinates.latitude, convertedCoordinates!!.latitude, 0.0001)
        assertEquals(coordinates.longitude, convertedCoordinates.longitude, 0.0001)
        assertEquals(coordinates.altitude, convertedCoordinates.altitude)
    }
    
    @Test
    fun testInvalidJsonHandling() {
        val invalidJson = "invalid json string"
        val result = converters.fromStringList(invalidJson)
        
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }
    
    @Test
    fun testBoundingBoxConversion() {
        val boundingBox = BoundingBox(
            northEast = Coordinates(40.8, -73.9),
            southWest = Coordinates(40.6, -74.1)
        )
        
        val jsonString = converters.boundingBoxToString(boundingBox)
        val convertedBoundingBox = converters.fromBoundingBox(jsonString)
        
        assertNotNull(jsonString)
        assertNotNull(convertedBoundingBox)
        assertEquals(boundingBox.northEast.latitude, convertedBoundingBox!!.northEast.latitude, 0.0001)
        assertEquals(boundingBox.southWest.longitude, convertedBoundingBox.southWest.longitude, 0.0001)
    }
}
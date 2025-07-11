package com.rdwatch.androidtv.scraper.api

/**
 * Quick standalone test that can be run to verify scraper functionality
 */
fun main() {
    println("🧪 Quick Scraper Test")
    println("=" * 30)
    
    try {
        val results = ScraperTestRunner.runAllTests()
        println(results)
    } catch (e: Exception) {
        println("❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
    
    println("\n✅ Test completed!")
}

private operator fun String.times(n: Int): String = this.repeat(n)
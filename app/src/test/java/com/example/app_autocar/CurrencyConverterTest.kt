package com.example.app_autocar
import org.junit.Assert.assertEquals
import org.junit.Test
class CurrencyConverterTest {
    @Test fun convertsBothWaysWithDecimalRounding() {
        assertEquals("100.00",CurrencyConverter.convert("375","3.75",true).toPlainString())
        assertEquals("375.00",CurrencyConverter.convert("100","3.75",false).toPlainString())
        assertEquals("37.88",CurrencyConverter.convert("10,10","3.75",false).toPlainString())
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsZeroRate(){CurrencyConverter.convert("100","0",true)}
}

package com.sijan.kharcha.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class KharchaQuickBalanceWidgetTest {

    @Test
    fun formatWidgetBalance_formatsPaisaCorrectly() {
        assertEquals("Rs. 1,500.00", KharchaQuickBalanceWidget.formatPaisa(150000L))
        assertEquals("Rs. 0.00", KharchaQuickBalanceWidget.formatPaisa(0L))
        assertEquals("-Rs. 450.50", KharchaQuickBalanceWidget.formatPaisa(-45050L))
    }
}

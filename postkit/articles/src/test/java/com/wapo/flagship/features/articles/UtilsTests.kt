package com.wapo.flagship.features.articles

import com.wapo.Utils.floatPositionToIntValue
import com.wapo.view.FlowableLayout
import org.junit.Assert
import org.junit.Test

class UtilsTests {
    @Test
    fun floatPositionToIntValueTest() {
        Assert.assertEquals(floatPositionToIntValue("asd"), FlowableLayout.FLOAT_NONE)
        Assert.assertEquals(floatPositionToIntValue(""), FlowableLayout.FLOAT_NONE)
        Assert.assertEquals(floatPositionToIntValue(null), FlowableLayout.FLOAT_NONE)

        Assert.assertEquals(floatPositionToIntValue("CEnter"), FlowableLayout.FLOAT_NONE)
        Assert.assertEquals(floatPositionToIntValue("center"), FlowableLayout.FLOAT_NONE)
        Assert.assertEquals(floatPositionToIntValue("CENTER"), FlowableLayout.FLOAT_NONE)

        Assert.assertEquals(floatPositionToIntValue("left"), FlowableLayout.FLOAT_LEFT)
        Assert.assertEquals(floatPositionToIntValue("LEFT"), FlowableLayout.FLOAT_LEFT)
        Assert.assertEquals(floatPositionToIntValue("lEFt"), FlowableLayout.FLOAT_LEFT)

        Assert.assertEquals(floatPositionToIntValue("right"), FlowableLayout.FLOAT_RIGHT)
        Assert.assertEquals(floatPositionToIntValue("RIGHT"), FlowableLayout.FLOAT_RIGHT)
        Assert.assertEquals(floatPositionToIntValue("riGHT"), FlowableLayout.FLOAT_RIGHT)
    }
}
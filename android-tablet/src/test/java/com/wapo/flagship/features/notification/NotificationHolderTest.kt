package com.wapo.flagship.features.notification

import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elamgodilj on 5/14/17.
 */
class NotificationHolderTest {
    val formatter = SimpleDateFormat("h:mm a", Locale.US)

    @Test
    fun testDateToBeDisplayedForNotifcations() {
        // 14th May 05:45 am
        println(getTimeToBeDisplayed(1494755100000))

        // 13th May 11:50pm
        println(getTimeToBeDisplayed(1494733800000))

        // 12th May 9pm
        println(getTimeToBeDisplayed(1494637200000))

        // 11th May 9pm
        println(getTimeToBeDisplayed(1494550800000))

        // 15th May 12:18am
        println(getTimeToBeDisplayed(1494821888588))
    }

    /**
     * As for the timestamps attached to alerts, rules should be as follows:
     If an alert was sent more recently then midnight, it should read "Sent at [time]" (ie, "Sent at 5:45AM")
     If an alert was sent yesterday, it should read "Sent Yesterday at [time]" (ie, "Sent Yesterday at 11:50PM")
     If an alert was sent more recently than 48 hours, but from the day before yesterday, it should read "Sent [day of week] at [time]" (ie, "Sent Monday at 9:00PM")

     */
    fun getTimeToBeDisplayed(time: Long): String? {
        var timeInMillis = time
        var daysArray =
            arrayOf(
                "Sunday",
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
            )

        var calToday = Calendar.getInstance()
        var calNotificationDay = Calendar.getInstance()
        calNotificationDay.timeInMillis = timeInMillis

        var calTodayDayOfWeek = calToday.get(Calendar.DAY_OF_WEEK)
        var calNotificationDayOfWeek = calNotificationDay.get(Calendar.DAY_OF_WEEK)

        if (calTodayDayOfWeek == calNotificationDayOfWeek) {
            return "Sent at ${formatter.format(calNotificationDay.time)}"
        } else if (calTodayDayOfWeek - calNotificationDayOfWeek == 1 || (calTodayDayOfWeek - calNotificationDayOfWeek + 7) == 1) {
            return "Sent Yesterday at ${formatter.format(calNotificationDay.time)}"
        } else if (calTodayDayOfWeek - calNotificationDayOfWeek == 2 || (calTodayDayOfWeek - calNotificationDayOfWeek + 7) == 2) {
            return "Sent ${daysArray[calNotificationDayOfWeek - 1]} at ${formatter.format(
                calNotificationDay.time,
            )}"
        }
        return null
    }
}

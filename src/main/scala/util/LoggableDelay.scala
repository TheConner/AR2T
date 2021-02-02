package ca.advtech.ar2t
package util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

object LoggableDelay {
  private val tickDuration: Long = 1000

  private def tick(time: Long, message: String): Unit = {

    // Clear line
    print("\r")
    print(f"${message} ${new SimpleDateFormat("mm:ss").format(new Date(time))}")
  }

  def Delay(duration: Long, message: String = "Time remaining"): Unit = {

    var elapsedTime: Long = duration

    while (0 <= elapsedTime) {
      tick(elapsedTime, message)
      Thread.sleep(tickDuration)
      elapsedTime -= tickDuration
    }

    println("")
  }
}

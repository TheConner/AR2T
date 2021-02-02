package ca.advtech.ar2t

import util.LoggableDelay.Delay

object TestLoggableDelay {
  def main(args: Array[String]): Unit = {
    println("Loggable delay test, default")
    Delay(5000)

    println("With message")
    Delay(5000, "Message, blah")
  }
}

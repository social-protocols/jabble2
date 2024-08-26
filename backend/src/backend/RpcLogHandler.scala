package backend

import sloth.LogHandler
import cats.effect.IO

object RpcLogHandlerAnsi extends LogHandler[IO] {
  def logRequest[A, T](
    method: sloth.Method,
    argumentObject: A,
    result: IO[T],
  ): IO[T] = {
    val width = 250

    val coloredArgString = pprint.apply(argumentObject, width = width, showFieldNames = false).toString()

    // val apiName = s"${method.traitName}.${method.methodName}"
    val apiName = s"${method.methodName}"
    println(s"${fansi.Color.Cyan(s"-> ${apiName}")}($coloredArgString)")
    result.attempt.timed.map { (duration, result) =>
      val durationMs = duration.toMillis

      result match {
        case r @ Right(res) =>
          val durationString = f"${duration.toMicros / 1000.0}%.3fms"
          val durationColored = if (durationMs < 100) {
            durationString
          } else if (durationMs < 500) {
            fansi.Color.Yellow(fansi.Bold.On(durationString))
          } else {
            fansi.Color.Red(fansi.Bold.On(durationString))
          }
          val call = res
          println(s"${fansi.Color.Yellow("<-")} $call [${durationColored}]")

          r
        case r @ Left(error) =>
          println(fansi.Color.Red(s"${apiName} error: ${error.getMessage} [${durationMs}ms]"))

          r
      }
    }.rethrow
  }
}

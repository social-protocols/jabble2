package backend

import cats.effect.IO
import httpApi.*

object HttpApiImpl extends HttpApiService[IO] {
  def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure {
    town match {
      case None    => Greeting(s"Hello $name!")
      case Some(t) => Greeting(s"Hello $name from $t!")
    }
  }
}

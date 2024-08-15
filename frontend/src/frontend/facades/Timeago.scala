package frontend.facades

import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js

@JSImport("timeago.js", JSImport.Namespace)
@js.native
object Timeago extends js.Object {
  def format(date: js.Date): String = js.native
}

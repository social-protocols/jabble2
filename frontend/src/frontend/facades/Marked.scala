package frontend.facades

import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js

@js.native
@JSImport("marked", JSImport.Namespace)
object marked extends js.Object {
  def parse(markdownString: String): String = js.native
}

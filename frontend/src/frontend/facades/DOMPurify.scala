package frontend.facades

import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js

@js.native
@JSImport("dompurify", JSImport.Default)
object DOMPurify extends js.Object {
  def sanitize(dirty: String): String = js.native
}

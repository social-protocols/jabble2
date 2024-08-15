package frontend.components

import outwatch._
import outwatch.dsl._
import frontend.facades.marked
import frontend.facades.DOMPurify

object Markdown {

  def parseMarkdownToRawHTMLString(md: String): String = {
    // remove zero-width characters at the start of the input (e.g., BOMs).
    // see https://marked.js.org and https://github.com/markedjs/marked/issues/2139
    // val stripped: String = md.replaceAll("^[\u200B\u200C\u200D\u200E\u200F\uFEFF]", "")
    // render input to HTML using marked
    val renderedMd: String = marked.parse(md)
    // purify output using DOMPurify
    DOMPurify.sanitize(renderedMd)
  }

  def apply(md: String): VNode =
    div.thunk("markdown-rendering")(md)(
      VMod(
        innerHTML := UnsafeHTML(parseMarkdownToRawHTMLString(md))
      )
    )
}

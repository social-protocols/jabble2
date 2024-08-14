package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import frontend.RpcClient
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{value as _, *}
import webcodegen.shoelace.SlInput

def createPostForm(refreshTrigger: VarEvent[Unit]) = {
  val contentState = Var("")

  div(
    slInput(
      SlInput.placeholder := "What's on your mind?",
      value <-- contentState,
      onSlInput.map(_.target.value) --> contentState,
    ),
    slButton(
      "Post",
      onClick(contentState).foreachEffect { content =>
        lift {
          unlift(RpcClient.call.createPost(content, true))
          refreshTrigger.set(())
        }
      },
    ),
    display := "flex",
    width := "600px",
    border := "1px solid lightgrey",
    padding := "10px",
  )
}

package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import frontend.RpcClient
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{value as _, *}
import webcodegen.shoelace.SlInput

def frontPageFeed(refreshTrigger: VarEvent[Unit]) = {
  div(
    lift {
      div(
        unlift(RpcClient.call.getPosts()).map(post => frontPagePost(post.id, post.content, post.authorId, refreshTrigger)),
        display := "flex",
        flexDirection := "column",
      )
    }
  )
}

def frontPagePost(postId: Long, content: String, authorId: String, refreshTrigger: VarEvent[Unit]) = {
  val contentState = Var("")

  div(
    div("by: ", authorId, color := "grey"),
    content,
    div(
      slInput(
        SlInput.placeholder := "Reply",
        value <-- contentState,
        onSlInput.value --> contentState,
      ),
      slButton(
        "Reply",
        onClick(contentState).foreachEffect { content =>
          lift {
            unlift(RpcClient.call.createReply(parentId = postId, content = content))
            refreshTrigger.set(())
          }
        },
      ),
      display := "flex",
      width := "100%",
    ),
    width := "600px",
    border := "1px solid lightgrey",
    padding := "10px",
  )
}

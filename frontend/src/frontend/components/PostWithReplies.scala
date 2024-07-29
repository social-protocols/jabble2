package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import frontend.RpcClient
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{value as _, *}
import webcodegen.shoelace.SlInput

def postWithReplies(replyTree: rpc.ReplyTree, refreshTrigger: VarEvent[Unit]): VNode = {
  div(
    postDetails(replyTree.post, refreshTrigger),
    replyTree.replies.map { tree => postWithReplies(tree, refreshTrigger) },
    marginLeft := "50px",
  )
}

def postDetails(post: rpc.Post, refreshTrigger: VarEvent[Unit]): VNode = {
  div(
    postInfoBar(post),
    post.content,
    postActionBar(post, refreshTrigger),
  )
}

def postInfoBar(post: rpc.Post): VNode = {
  div("created at: ", post.createdAt)
}

def postActionBar(post: rpc.Post, refreshTrigger: VarEvent[Unit]): VNode = {
  val contentState = Var("")

  val showReplyForm = Var(false)

  div(
    div(
      button(
        "⇧",
        onClick.doEffect {
          RpcClient.call.vote(postId = post.id, parentId = post.parentId, direction = rpc.Direction.Up)
        },
      ),
      span("Vote"),
      button(
        "⇩",
        onClick.doEffect {
          RpcClient.call.vote(postId = post.id, parentId = post.parentId, direction = rpc.Direction.Down)
        },
      ),
      button(
        "🗨 Reply",
        onClick.doAction {
          showReplyForm.update(!_)
        },
      ),
      showReplyForm.map { show =>
        if (show) {
          button(
            "✕",
            cls := "ml-auto self-center pr-2",
            onClick.doAction {
              showReplyForm.set(false)
            },
          )
        } else {
          VMod.empty
        }
      },
      cls := "flex w-full flex-wrap items-start gap-3 text-xl opacity-50 sm:text-base",
    ),
    showReplyForm.map { show =>
      if (show) {
        div(
          slInput(
            SlInput.placeholder := "Enter your reply",
            value <-- contentState,
            onSlInput.value --> contentState,
          ),
          slButton(
            "Reply",
            onClick(contentState).foreachEffect { content =>
              lift {
                unlift(RpcClient.call.createReply(parentId = post.id, content = content))
                refreshTrigger.set(())
              }
            },
          ),
        )
      } else {
        VMod.empty
      }
    },
  )
}

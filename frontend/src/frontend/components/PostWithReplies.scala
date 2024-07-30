package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import frontend.RpcClient
import webcodegen.shoelace.SlButton.{title as _, value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{title as _, value as _, *}
import webcodegen.shoelace.SlInput

def postWithReplies(replyTree: rpc.ReplyTree, commentTreeState: rpc.CommentTreeState, refreshTrigger: VarEvent[Unit]): VNode = {
  val postState = commentTreeState.posts.get(replyTree.post.id)
  div(
    postDetails(replyTree.post, postState, refreshTrigger),
    replyTree.replies.map { tree => postWithReplies(tree, commentTreeState, refreshTrigger) },
    marginLeft := "50px",
  )
}

def postDetails(post: rpc.Post, postState: Option[rpc.PostState], refreshTrigger: VarEvent[Unit]): VNode = {
  div(
    postInfoBar(post, postState),
    post.content,
    postActionBar(post, refreshTrigger),
    cls := "mb-5",
  )
}

val effectSizeThresholds: Vector[Float] = Vector(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)

def convincingnessScale(effectSize: Float): String = {
  val numberOfFlames = effectSizeThresholds.filter(e => if (effectSize < e) true else false).length
  "🔥".repeat(numberOfFlames)
}

def postInfoBar(post: rpc.Post, postState: Option[rpc.PostState]): VNode = {
  val nVotes = postState.fold(0L)(_.voteCount)
  div(
    span(
      span(
        "convincing: ",
        cls := "opacity-50",
      ),
      convincingnessScale(0.4), // TODO: show the actual score
      title := "Convincingness Score. How much this post changed people's opinion on the target post.",
    ),
    div(nVotes, " votes"),
    div("created at: ", post.createdAt),
    cls := "mb-1 flex w-full items-center gap-2 text-xs sm:items-baseline",
  )
}

def postActionBar(post: rpc.Post, refreshTrigger: VarEvent[Unit]): VNode = {
  val contentState = Var("")

  val showReplyForm = Var(false)

  div(
    div(
      cls := "flex w-full flex-wrap items-start gap-3 text-xl opacity-50 sm:text-base",
      button(
        "⇧",
        onClick.doEffect {
          lift {
            unlift(RpcClient.call.vote(postId = post.id, parentId = post.parentId, direction = rpc.Direction.Up))
            refreshTrigger.set(())
          }
        },
      ),
      span("Vote"),
      button(
        "⇩",
        onClick.doEffect {
          lift {
            unlift(RpcClient.call.vote(postId = post.id, parentId = post.parentId, direction = rpc.Direction.Down))
            refreshTrigger.set(())
          }
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

package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import frontend.RpcClient
import webcodegen.shoelace.SlButton.{title as _, value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{title as _, value as _, *}
import webcodegen.shoelace.SlInput
import frontend.TreeContext
import cats.effect.IO

def postWithReplies(postTree: rpc.PostTree, treeContext: TreeContext, refreshTrigger: VarEvent[Unit]): VNode = {
  val postData = treeContext.postTreeData.posts(postTree.post.id)
  div(
    postDetails(postTree.post, postData, refreshTrigger),
    div(
      postTree.replies.map { tree => postWithReplies(tree, treeContext, refreshTrigger) },
      cls := "ml-2 pl-3",
    ),
  )
}

def postDetails(post: rpc.Post, postData: rpc.PostData, refreshTrigger: VarEvent[Unit]): VNode = {
  div(
    postInfoBar(post, postData),
    post.content,
    postActionBar(post, refreshTrigger),
    cls := "mb-4",
  )
}

val effectSizeThresholds: Vector[Float] = Vector(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)

def convincingnessScale(effectSize: Double): String = {
  val numberOfFlames = effectSizeThresholds.filter(e => if (effectSize >= e) true else false).length
  "🔥".repeat(numberOfFlames)
}

def postInfoBar(post: rpc.Post, postData: rpc.PostData): VNode = {
  val effectSize: Double = postData.effectOnTargetPost.map(_.effectSizeOnTarget) match {
    case Some(effectSize) => effectSize
    case None =>
      println(s"No effect found for post ${postData.postId} on post ${post.id}, defaulting to 0.0")
      0.0
  }

  // TODO: entire postInfoBar should be a link to stats page
  div(
    cls := "mb-1 flex w-full items-center gap-2 text-xs sm:items-baseline",
    if (effectSize > effectSizeThresholds(0)) {
      span(
        title := "Convincingness Score. How much this post changed people's opinion on the target post.",
        span(cls := "opacity-50", "convincing: "),
        convincingnessScale(effectSize),
      )
    } else {
      VMod.empty
    },
    span(
      cls := "opacity-50",
      postData.voteCount,
      if (postData.voteCount == 1) " vote" else " votes",
    ),
    span(
      cls := "opacity-50",
      "created at: ",
      post.createdAt,
    ), // TODO: display with moment.js (build facade)
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

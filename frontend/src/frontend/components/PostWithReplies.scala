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
import webcodegen.shoelace.SlIcon.*
import webcodegen.shoelace.SlIcon

def postWithReplies(postTree: rpc.PostTree, treeContext: TreeContext, refreshTrigger: VarEvent[Unit]): VMod = {
  val hidePost     = treeContext.collapsedState.hidePost.getOrElse(postTree.post.id, false)
  val hideChildren = treeContext.collapsedState.hideChildren.getOrElse(postTree.post.id, false)

  val postData = treeContext.postTreeDataState.posts(postTree.post.id)

  if (!hidePost) {
    div(
      postDetails(postTree.post, postData, postTree, treeContext, refreshTrigger),
      if (!hideChildren) {
        div(
          postTree.replies.map { tree => postWithReplies(tree, treeContext, refreshTrigger) },
          cls := "ml-2 pl-3",
        )
      } else {
        VMod.empty
      },
    )
  } else {
    VMod.empty
  }
}

def postDetails(
  post: rpc.Post,
  postData: rpc.PostData,
  postTree: rpc.PostTree,
  treeContext: TreeContext,
  refreshTrigger: VarEvent[Unit],
): VNode = {
  div(
    postInfoBar(post, postData),
    post.content,
    postActionBar(post, postTree, treeContext, refreshTrigger),
    cls := "mb-4",
  )
}

val effectSizeThresholds: Vector[Float] = Vector(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)

def convincingnessScale(effectSize: Double): String = {
  val numberOfFlames = effectSizeThresholds.count(effectSize >= _)
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

def postActionBar(post: rpc.Post, postTree: rpc.PostTree, treeContext: TreeContext, refreshTrigger: VarEvent[Unit]): VNode = {
  val contentState = Var("")

  val showReplyForm = Var(false)

  def submitVote(direction: rpc.Direction) = lift {
    val newPostTreeData = unlift(RpcClient.call.vote(post.id, treeContext.targetPostId, direction))
    treeContext.setPostTreeDataState(newPostTreeData)
  }

  val currentVote  = treeContext.postTreeDataState.posts(post.id).userVote
  val upvoteIcon   = if (currentVote == rpc.Direction.Up) "arrow-up-circle-fill" else "arrow-up-circle"
  val downvoteIcon = if (currentVote == rpc.Direction.Down) "arrow-down-circle-fill" else "arrow-down-circle"

  val hasChildren    = postTree.replies.nonEmpty
  val childrenHidden = treeContext.collapsedState.hideChildren.getOrElse(post.id, false)

  def toggleHideChildren() = {
    if (childrenHidden) {
      // expand
      var newHideChildrenState = treeContext.collapsedState.hideChildren.updated(post.id, false)
      // collapse direct children
      postTree.replies.foreach { reply =>
        {
          newHideChildrenState = newHideChildrenState.updated(reply.post.id, true)
        }
      }
      treeContext.setCollapsedState(treeContext.collapsedState.copy(hideChildren = newHideChildrenState))
    } else {
      // collapse
      treeContext.setCollapsedState(
        treeContext.collapsedState.copy(hideChildren = treeContext.collapsedState.hideChildren.updated(post.id, true))
      )
    }
  }

  div(
    div(
      cls := "flex w-full flex-wrap items-start gap-3 text-xl opacity-50 sm:text-base",
      if (hasChildren) {
        if (childrenHidden) {
          button(slIcon(SlIcon.name := "chevron-right"), title := "Expand this comment", onClick.doAction { toggleHideChildren() })
        } else {
          button(slIcon(SlIcon.name := "chevron-down"), title := "Collapse this comment", onClick.doAction { toggleHideChildren() })
        }
      } else {
        VMod.empty
      },
      button(
        slIcon(SlIcon.name := upvoteIcon),
        onClick.doEffect { submitVote(rpc.Direction.Up) },
      ),
      span("Vote"),
      button(
        slIcon(SlIcon.name := downvoteIcon),
        onClick.doEffect { submitVote(rpc.Direction.Down) },
      ),
      button(
        slIcon(SlIcon.name := "chat-right"),
        " Reply",
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
                unlift(RpcClient.call.createReply(post.id, treeContext.targetPostId, content))
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

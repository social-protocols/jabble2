package frontend

import cats.effect.IO
import cats.effect.IOApp
import outwatch.*
import outwatch.dsl.*
import colibri.*
import colibri.reactive.*
import authn.frontend.*
import scala.scalajs.js
import cats.effect.unsafe.implicits.global
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{value as _, *}
import webcodegen.shoelace.SlInput
import colibri.router.*
import webcodegen.shoelace.SlCard.*
import webcodegen.shoelace.SlCard

// Outwatch documentation: https://outwatch.github.io/docs/readme.html

enum Page {
  case Index
  case Login
  case Post(id: Int)
  case NotFound
}

def pageToPath(page: Page): Path = page match {
  case Page.Index    => Root
  case Page.Login    => Root / "login"
  case Page.Post(id) => Root / "post" / id.toString
  // TODO: case Page.NotFound =>
}

def pathToPage(path: Path): Page = path match {
  case Root               => Page.Index
  case Root / "login"     => Page.Login
  case Root / "post" / id => Page.Post(id.toInt)
  case _                  => Page.NotFound
}

object Main extends IOApp.Simple {
  def run = lift {
    // render the component into the <div id="app"></div> in index.html
    unlift(Outwatch.renderReplace[IO]("#app", app, RenderConfig.showError))
  }
}

val authnClient = AuthnClient[IO](
  AuthnClientConfig(
    hostUrl = "http://localhost:3000",
    sessionStorage = SessionStorage.LocalStorage("session"),
  )
)

def app: VNode = {
  val page: Var[Page] = {
    val pageSubject: Subject[Page] = Router.path
      .imapSubject[Page](pageToPath)(pathToPage)
    Var.createStateless[Page](RxWriter.observer(pageSubject), Rx.observableSync(pageSubject))
  }

  val refreshTrigger = VarEvent[Unit]()

  div(
    slButton("Jabble", onClick.as(Page.Index) --> page),
    RpcClient.call.getUsername(),
    slButton("Login", onClick.as(Page.Login) --> page),
    refreshTrigger.observable
      .prepend(())
      .map(_ =>
        page.map[VMod] {
          case Page.Index    => frontPage(refreshTrigger)
          case Page.Login    => loginPage
          case Page.Post(id) => postPage(id.toLong, refreshTrigger)
          case _             => div("page not found")
        },
      ),
  )
}

def frontPage(refreshTrigger: VarEvent[Unit]) = {
  div(
    slCard(
      createPostForm(refreshTrigger)
    ),
    postFeed(refreshTrigger),
    display := "flex",
    flexDirection := "column",
    width := "600px",
    margin := "0 auto",
  )
}

def loginPage = {
  authControl(width := "600px", margin := "0 auto")
}

def postPage(postId: Long, refreshTrigger: VarEvent[Unit]) = {
  val replyTree = RpcClient.call.getReplyTree(postId)
  div(
    replyTree.map(_.map { tree => postWithReplies(tree, refreshTrigger) }),
    maxWidth := "960px",
    margin := "0 auto",
  )
}

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
          unlift(RpcClient.call.createReply(parentId = post.id, content = content))
          refreshTrigger.set(())
        }
      },
    ),
    slButton(
      "Upvote",
      onClick.doEffect {
        RpcClient.call.vote(postId = post.id, parentId = post.parentId, direction = 1)
      },
    ),
    slButton(
      "Downvote",
      onClick.doEffect {
        RpcClient.call.vote(postId = post.id, parentId = post.parentId, direction = -1)
      },
    )
  )
}

def authControl = {

  val usernameState = Var("")
  val passwordState = Var("")

  div(
    slInput(
      SlInput.placeholder := "Username",
      value <-- usernameState,
      onSlInput.map(_.target.value) --> usernameState,
    ),
    slInput(
      SlInput.placeholder := "Password",
      SlInput.`type` := "password",
      value <-- passwordState,
      onSlInput.map(_.target.value) --> passwordState,
    ),
    slButton(
      "Register",
      onClick(usernameState).withLatest(passwordState).foreachEffect { case (username, password) =>
        RpcClient.call.register(username = username, password = password)
      },
    ),
    slButton(
      "Login",
      onClick(usernameState).withLatest(passwordState).foreachEffect { case (username, password) =>
        authnClient.login(Credentials(username = username, password = password))
      },
    ),
    // b(authn.session),
    slButton(
      "Logout",
      onClick.doEffect {
        authnClient.logout
      },
    ),
  )
}

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
          unlift(RpcClient.call.createPost(content = content))
          refreshTrigger.set(())
        }
      },
    ),
  )
}

def postFeed(refreshTrigger: VarEvent[Unit]) = {
  div(
    lift {
      div(
        unlift(RpcClient.call.getPosts()).map(post => postCard(post.id, post.content, post.authorId, refreshTrigger)),
        display := "flex",
        flexDirection := "column",
      )
    }
  )
}

def postCard(postId: Long, content: String, authorId: String, refreshTrigger: VarEvent[Unit]) = {
  val contentState = Var("")

  slCard(
    content,
    div("authorId: ", authorId, slot := "header", color := "grey"),
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
      slot := "footer",
    ),
    color := "grey",
    background := "black",
    width := "600px",
    SlCard.borderRadius := "0px",
  )
}

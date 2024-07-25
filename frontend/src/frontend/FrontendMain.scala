package frontend

import cats.effect.IO
import cats.effect.IOApp
import outwatch.*
import outwatch.dsl.*
import colibri.*
import colibri.reactive.*
import authn.frontend.*
import org.scalajs.dom
import scala.scalajs.js
import scala.annotation.nowarn
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

def app: VNode = {
  val page: Var[Page] = {
    val pageSubject: Subject[Page] = Router.path
      .imapSubject[Page](pageToPath)(pathToPage)
    Var.createStateless[Page](RxWriter.observer(pageSubject), Rx.observableSync(pageSubject))
  }

  val refreshTrigger = VarEvent[Unit]()

  div(
    slButton("Jabble", onClick.as(Page.Index) --> page),
    slButton("Login", onClick.as(Page.Login) --> page),
    refreshTrigger.observable
      .prepend(())
      .map(_ =>
        page.map[VMod] {
          case Page.Index    => frontPage(refreshTrigger)
          case Page.Login    => loginPage
          case Page.Post(id) => postPage(id.toLong)
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

def postPage(postId: Long) = {
  val replyTree = RpcClient.call.getReplyTree(postId)
  replyTree.map(_.map(postWithReplies))
}


def postWithReplies(replyTree: rpc.ReplyTree): VNode = {
  div(
    div(replyTree.post.content),
    replyTree.replies.map(postWithReplies)
  )
}

def authControl = {

  val authn = AuthnClient[IO](
    AuthnClientConfig(
      hostUrl = "http://localhost:3000",
      sessionStorage = SessionStorage.LocalStorage("session"),
    )
  )

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
    p(usernameState),
    slButton(
      "Register",
      onClick(usernameState).withLatest(passwordState).foreachEffect { case (username, password) =>
        RpcClient.call.register(username = username, password = password)
      },
    ),
    slButton(
      "Login",
      onClick(usernameState).withLatest(passwordState).foreachEffect { case (username, password) =>
        authn.login(Credentials(username = username, password = password))
      },
    ),
    // b(authn.session),
    slButton(
      "Logout",
      onClick.doEffect {
        authn.logout
      },
    ),
  )
}

def createPostForm(refreshTrigger: VarEvent[Unit]) = {

  val authn = AuthnClient[IO](
    AuthnClientConfig(
      hostUrl = "http://localhost:3000",
      sessionStorage = SessionStorage.LocalStorage("session"),
    )
  )

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
  val authn = AuthnClient[IO](
    AuthnClientConfig(
      hostUrl = "http://localhost:3000",
      sessionStorage = SessionStorage.LocalStorage("session"),
    )
  )

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
      contentState,
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

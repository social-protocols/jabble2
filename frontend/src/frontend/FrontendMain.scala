package frontend

import cats.effect.IO
import cats.effect.IOApp
import outwatch.*
import outwatch.dsl.*
import colibri.*
import colibri.reactive.*
import authn.frontend.*
import cats.effect.unsafe.implicits.global
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import colibri.router.*
import frontend.components.*

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
  case Page.NotFound => Root
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
    div(
      slButton("Jabble", onClick.as(Page.Index) --> page),
      div(
        div(RpcClient.call.getUsername(), marginRight := "10px"),
        slButton("Login", onClick.as(Page.Login) --> page),
        marginLeft := "auto",
        display := "flex",
        alignItems := "baseline",
      ),
      display := "flex",
    ),
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
    width := "1200px",
    margin := "0 auto",
  )
}

def frontPage(refreshTrigger: VarEvent[Unit]) = {
  div(
    div(
      createPostForm(refreshTrigger)
    ),
    frontPageFeed(refreshTrigger),
    display := "flex",
    flexDirection := "column",
    width := "600px",
    margin := "0 auto",
  )
}

def loginPage = {
  authControl(authnClient)
}

def postPage(postId: Long, refreshTrigger: VarEvent[Unit]) = {
  val replyTree = RpcClient.call.getReplyTree(postId)
  div(
    replyTree.map(_.map { tree => postWithReplies(tree, refreshTrigger) }),
    maxWidth := "960px",
    margin := "0 auto",
  )
}

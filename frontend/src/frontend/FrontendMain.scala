package frontend

import cats.effect.IO
import cats.effect.IOApp
import outwatch.*
import outwatch.dsl.*
import colibri.*
import colibri.reactive.*
import authn.frontend.*
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import colibri.router.*
import frontend.components.*
import webcodegen.shoelace.SlIcon.{name as _, *}
import webcodegen.shoelace.SlIcon
import rpc.UserProfile
import scala.scalajs.js

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
  def run: IO[Unit] = lift {
    // render the component into the <div id="app"></div> in index.html
    unlift(Outwatch.renderReplace[IO]("#app", app, RenderConfig.showError))
  }
}

val authnClient = AuthnClient[IO](
  AuthnClientConfig(
    hostUrl = js.Dynamic.global.AUTHN_URL.asInstanceOf[String],
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
    cls := "flex flex-col",
    refreshTrigger.observable
      .prepend(())
      .map(_ =>
        RpcClient.call
          .getUserProfile()
          .map(userProfile =>
            VMod(
              header(
                cls := "flex flex-col w-full px-2 py-2",
                div(
                  cls := "flex flex-wrap items-center justify-between gap-4 sm:flex-nowrap md:gap-8",
                  button(
                    "Jabble ",
                    span("alpha ", slIcon(SlIcon.name := "rocket"), cls := "opacity-50"),
                    onClick.as(Page.Index) --> page,
                    cls := "font-bold",
                  ),
                  div(
                    cls := "flex items-center gap-10 ml-auto",
                    userProfile match {
                      case Some(profile) =>
                        VMod(
                          div(profile.userName, marginRight := "10px"),
                          slButton(
                            "Logout",
                            onClick.doEffect {
                              lift {
                                val result = unlift(authnClient.logout.attempt)
                                result match {
                                  case Left(error) => println(error.getMessage)
                                  case Right(_) =>
                                    println("logged out")
                                    refreshTrigger.set(())
                                }
                              }
                            },
                          ),
                        )
                      case None =>
                        slButton("Login", onClick.as(Page.Login) --> page)
                    },
                  ),
                ),
              ),
              div(
                cls := "mx-auto w-full max-w-3xl px-2",
                page.map[VMod] {
                  case Page.Index    => frontPage(refreshTrigger)
                  case Page.Login    => loginPage(refreshTrigger)
                  case Page.Post(id) => postPage(id.toLong, refreshTrigger, userProfile)
                  case _             => div("page not found")
                },
              ),
            )
          )
      ),
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

def loginPage(refreshTrigger: VarEvent[Unit]) = {
  authControl(authnClient, refreshTrigger)
}

case class TreeContext(
  targetPostId: Long,
  postTree: rpc.PostTree,
  postTreeDataState: rpc.PostTreeData,
  setPostTreeDataState: (postTreeData: rpc.PostTreeData) => Unit,
  collapsedState: CollapsedStatus,
  setCollapsedState: (collapsedState: CollapsedStatus) => Unit,
)

case class CollapsedStatus(
  currentlyFocussedPostId: Long,
  hidePost: Map[Long, Boolean],
  hideChildren: Map[Long, Boolean],
)

def postPage(postId: Long, refreshTrigger: VarEvent[Unit], userProfile: Option[rpc.UserProfile]): VMod = lift {
  val initialPostTree: Option[rpc.PostTree] = unlift(RpcClient.call.getPostTree(postId))
  val initialPostTreeData                   = unlift(RpcClient.call.getPostTreeData(postId))
  val parents                               = unlift(RpcClient.call.getParentThread(postId))

  initialPostTree match {
    case Some(tree) => renderPostPage(tree, initialPostTreeData, parents, refreshTrigger, userProfile)
    case None       => div(s"Post with id ${postId} not found")
  }
}

def renderPostPage(
  initialPostTree: rpc.PostTree,
  initialPostTreeData: rpc.PostTreeData,
  parents: Vector[rpc.Post],
  refreshTrigger: VarEvent[Unit],
  userProfile: Option[rpc.UserProfile],
): VMod = {
  val postTreeState     = Var(initialPostTree)
  val postTreeDataState = Var(initialPostTreeData)
  val collapsedState    = Var(CollapsedStatus(initialPostTreeData.targetPostId, Map(), Map()))

  Rx {
    val treeContext: TreeContext = TreeContext(
      initialPostTreeData.targetPostId,
      postTreeState(),
      postTreeDataState(),
      postTreeDataState.set,
      collapsedState(),
      collapsedState.set,
    )

    div(
      parentThread(parents),
      postWithReplies(initialPostTree, treeContext, refreshTrigger, userProfile),
      maxWidth := "960px",
      margin := "0 auto",
    )
  }
}

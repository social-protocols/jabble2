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

// Outwatch documentation: https://outwatch.github.io/docs/readme.html

object Main extends IOApp.Simple {

  def run = lift {

    val myComponent = div(
      "Hello World",
      // RpcClient.call.increment(6),
      authControl,
      button("inc", onClick.doEffect(RpcClient.call.increment(7).void)),
      button("inc auth", onClick.doEffect(RpcClient.call.incrementAuthorized(7).void)),
      slButton("SLButton"),

      createPostForm,
      postFeed,
    )

    // render the component into the <div id="app"></div> in index.html
    unlift(Outwatch.renderReplace[IO]("#app", myComponent, RenderConfig.showError))
  }

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
      SlInput.placeholder:="Username",
      value <-- usernameState,
      onSlInput.map(_.target.value) --> usernameState,
    ),
    slInput(
      SlInput.placeholder:="Password", SlInput.`type`:="password",
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
    b(authn.session),
    slButton(
      "Logout",
      onClick.doEffect {
        authn.logout
      },
    ),
  )
}

def createPostForm = {

  val contentState = Var("")

  div(
    slInput(
      SlInput.placeholder:="What's on your mind?",
      value <-- contentState,
      onSlInput.map(_.target.value) --> contentState,
    ),
    slButton("Post", onClick(contentState).foreachEffect { content => 
      RpcClient.call.createPost(content = content)
    }),
  )
}


def postFeed = {

  div(
    lift {
      unlift(RpcClient.call.getPosts()).map(post => 
        div(post.content)
      )

    }
  )
}


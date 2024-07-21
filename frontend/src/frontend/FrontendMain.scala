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

// Outwatch documentation: https://outwatch.github.io/docs/readme.html

object Main extends IOApp.Simple {
  import webcodegen.shoelace.SlButton.{value as _, *}
  import webcodegen.shoelace.SlButton

  def run = lift {

    val myComponent = div(
      "Hello World",
      // RpcClient.call.increment(6),
      authControl,
      button("inc", onClick.doEffect(RpcClient.call.increment(7).void)),
      button("inc auth", onClick.doEffect(RpcClient.call.incrementAuthorized(7).void)),
      slButton("SLButton"),
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
  div(
    button(
      "Register",
      onClick.doEffect {
        RpcClient.call.register(username = "u2", password = "wolfgang254!!??")
      },
    ),
    button(
      "Login",
      onClick.doEffect {
        authn.login(Credentials(username = "u2", password = "wolfgang254!!??"))
      },
    ),
    b(authn.session),
    button(
      "Logout",
      onClick.doEffect {
        authn.logout
      },
    ),
  )
}

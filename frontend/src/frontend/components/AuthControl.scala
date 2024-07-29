package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import cats.effect.IO
import frontend.RpcClient
import authn.frontend.*
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{value as _, *}
import webcodegen.shoelace.SlInput

def authControl(authnClient: AuthnClient[IO]) = {
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
    width := "600px",
    margin := "0 auto",
  )
}

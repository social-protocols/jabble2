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
  val usernameForLoginState = Var("")
  val passwordForLoginState = Var("")

  val usernameForSignupState = Var("")
  val passwordForSignupState = Var("")

  div(
    cls := "flex flex-col gap-16",
    div(
      cls := "flex flex-col gap-2",
      h2("Login", cls := "text-xl font-bold text-center"),
      slInput(
        SlInput.placeholder := "Username",
        value <-- usernameForLoginState,
        onSlInput.map(_.target.value) --> usernameForLoginState,
      ),
      slInput(
        SlInput.placeholder := "Password",
        SlInput.`type` := "password",
        value <-- passwordForLoginState,
        onSlInput.map(_.target.value) --> passwordForLoginState,
      ),
      slButton(
        "Login",
        onClick(usernameForLoginState).withLatest(passwordForLoginState).foreachEffect { case (username, password) =>
          authnClient.login(Credentials(username = username, password = password))
        },
      ),
    ),
    div(
      cls := "flex flex-col gap-2",
      h2("Create account", cls := "text-xl font-bold text-center"),
      slInput(
        SlInput.placeholder := "Username",
        value <-- usernameForSignupState,
        onSlInput.map(_.target.value) --> usernameForSignupState,
      ),
      slInput(
        SlInput.placeholder := "Password",
        SlInput.`type` := "password",
        value <-- passwordForSignupState,
        onSlInput.map(_.target.value) --> passwordForSignupState,
      ),
      slButton(
        "Create account",
        onClick(usernameForSignupState).withLatest(passwordForSignupState).foreachEffect { case (username, password) =>
          RpcClient.call.register(username = username, password = password)
        },
      ),
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

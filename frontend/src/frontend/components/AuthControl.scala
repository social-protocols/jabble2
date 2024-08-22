package frontend.components

import colibri.reactive.*
import outwatch.*
import outwatch.dsl.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import frontend.RpcClient
import authn.frontend.*
import authn.frontend.authnJS.keratinAuthn.distTypesMod.Credentials
import webcodegen.shoelace.SlButton.{value as _, *}
import webcodegen.shoelace.SlButton
import webcodegen.shoelace.SlInput.{value as _, *}
import webcodegen.shoelace.SlInput

def authControl(authnClient: AuthnClient[IO], refreshTrigger: VarEvent[Unit]) = {
  val usernameForLoginState = Var("")
  val passwordForLoginState = Var("")

  val usernameForSignupState = Var("")
  val passwordForSignupState = Var("")

  val errorState = Var[Option[String]](None)

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
      errorState,
      slButton(
        "Login",
        Rx {
          onClick.doEffect {
            lift {
              errorState.set(None)
              val result =
                unlift(authnClient.login(Credentials(username = usernameForLoginState(), password = passwordForLoginState())).attempt)
              result match {
                case Left(error) =>
                  println(s"login error: ${error.getMessage()}")
                  errorState.set(Some(error.getMessage()))
                case Right(_) =>
                  println("login successful")
                  refreshTrigger.set(())
              }
            }
          }
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
        Rx {
          onClick.stopPropagation.doAction {
            lift {
              println("registering...")
              val result = unlift(RpcClient.call.register(username = usernameForSignupState(), password = passwordForSignupState()).attempt)
              result match {
                case Left(error) =>
                  println(s"registration error: ${error.getMessage()}")
                case Right(success) =>
                  if (success) {
                    println("logging in...")
                    unlift(authnClient.login(Credentials(username = usernameForSignupState(), password = passwordForSignupState())))
                    refreshTrigger.set(())
                  } else {
                    println("could not register")
                  }
              }
            }.unsafeRunAndForget()
          }
        },
      ),
    ),
    // b(authn.session),
  )
}

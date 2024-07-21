package frontend

import cats.effect.IO
import sloth.ext.jsdom.client.*

import authn.frontend.AuthnClient
import authn.frontend.AuthnClientConfig

object RpcClient {
  import chameleon.ext.upickle.given // TODO: Option as null

  private val headers: IO[Map[String, String]] = lift {
    val client = AuthnClient[IO](AuthnClientConfig("http://localhost:3000"))
    unlift(client.session).map(token => "Authorization" -> s"Bearer $token").toMap
  }

  private val httpConfig    = headers.map(headers => HttpRequestConfig(headers = headers))
  private val requestClient = sloth.Client[String, IO](HttpRpcTransport(httpConfig))

  val call: rpc.RpcApi = requestClient.wire[rpc.RpcApi]
}

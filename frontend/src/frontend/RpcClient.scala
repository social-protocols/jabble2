package frontend

import cats.effect.IO
import sloth.ext.jsdom.client.*

object RpcClient {
  import chameleon.ext.upickle.given

  private val headers: IO[Map[String, String]] = lift {
    unlift(authnClient.session).map(token => "Authorization" -> s"Bearer $token").toMap
  }

  private val httpConfig    = headers.map(headers => HttpRequestConfig(headers = headers))
  private val requestClient = sloth.Client[String, IO](HttpRpcTransport(httpConfig))

  val call: rpc.RpcApi = requestClient.wire[rpc.RpcApi]
}

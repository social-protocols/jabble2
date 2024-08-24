package frontend

import cats.effect.{Async, IO}
import cats.implicits.*
import org.scalajs.dom
import sloth.{Request, RequestTransport}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*


object RpcClient {
  import chameleon.ext.upickle.given

  private val headers: IO[Map[String, String]] = lift {
    unlift(authnClient.session).map(token => "Authorization" -> s"Bearer $token").toMap
  }

  private val httpConfig    = headers.map(headers => HttpRequestConfig(headers = headers))
  private val requestClient = sloth.Client[String, IO](HttpRpcTransport(httpConfig))

  val call: rpc.RpcApi = requestClient.wire[rpc.RpcApi]
}

case class HttpRequestConfig(
                              baseUri: String = "/",
                              headers: Map[String, String] = Map.empty,
                            )

object HttpRpcTransport {
  def apply[F[_]: Async]: RequestTransport[String, F] = apply(HttpRequestConfig().pure[F])

  def apply[F[_]: Async](config: F[HttpRequestConfig]): RequestTransport[String, F] = new RequestTransport[String, F] {
    override def apply(request: Request[String]): F[String] = for {
      config <- config
      url = s"${config.baseUri}${request.method.traitName}/${request.method.methodName}"
      requestArgs = new dom.RequestInit { headers = config.headers.toJSDictionary; method = dom.HttpMethod.POST; body = request.payload }
      result <- Async[F].fromThenable(Async[F].delay[js.Thenable[String]](dom.fetch(url, requestArgs).`then`[String](_.text())))
    } yield result
  }
}

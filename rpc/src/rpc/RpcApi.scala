package rpc

import cats.effect.IO
import upickle.default.ReadWriter

trait RpcApi {
  def register(username: String, password: String): IO[Unit]
  def increment(x: Int): IO[Int]
  def incrementAuthorized(x: Int): IO[Int]
}

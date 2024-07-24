package rpc

import cats.effect.IO
import upickle.default.ReadWriter

trait RpcApi {
  def register(username: String, password: String): IO[Unit]
  def increment(x: Int): IO[Int]
  def incrementAuthorized(x: Int): IO[Int]
  def createPost(content: String): IO[Unit]
  def getPosts(): IO[Vector[Post]]
}

case class Post(id: Long, parentId: Option[Long], authorId: String, content: String, createdAt: Long) derives ReadWriter

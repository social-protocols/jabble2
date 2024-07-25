package rpc

import cats.effect.IO
import upickle.default.ReadWriter

trait RpcApi {
  def register(username: String, password: String): IO[Unit]
  def getUsername(): IO[String]
  def increment(x: Int): IO[Int]
  def incrementAuthorized(x: Int): IO[Int]
  def createPost(content: String): IO[Unit]
  def createReply(parentId: Long, content: String): IO[Unit]
  def getPosts(): IO[Vector[Post]]
  def getReplyTree(rootPostId: Long): IO[Option[ReplyTree]]
  def vote(postId: Long, parentId: Option[Long], direction: Long): IO[Unit]
}

case class Post(id: Long, parentId: Option[Long], authorId: String, content: String, createdAt: Long) derives ReadWriter

case class ReplyTree(post: Post, replies: Vector[ReplyTree]) derives ReadWriter

case class VoteEvent(
  voteEventId: Long,
  userId: String,
  postId: Long,
  vote: Long,
  voteEventTime: Long,
  parentId: Option[Long],
)

case class Vote(
  userId: String,
  postId: Long,
  vote: Long,
  lastVoteEventId: Long,
  voteEventTime: Long,
)

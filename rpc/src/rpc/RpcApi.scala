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
  def vote(postId: Long, parentId: Option[Long], direction: Direction): IO[Unit]
  def getCommentTreeState(targetPostId: Long): IO[CommentTreeState]
}

case class Post(
  id: Long,
  parentId: Option[Long],
  authorId: String,
  content: String,
  createdAt: Long,
  deletedAt: Option[Long],
  isPrivate: Long, // TODO: convert to boolean when reading from database
) derives ReadWriter

case class ReplyTree(post: Post, replies: Vector[ReplyTree]) derives ReadWriter

case class Effect(
  postId: Long,
  commentId: Option[Long],
  p: Long,
  pCount: Long,
  pSize: Long,
  q: Long,
  qCount: Long,
  qSize: Long,
  r: Long,
  weight: Long,
) derives ReadWriter

case class PostState(
  voteState: VoteState,
  voteCount: Long,
  p: Option[Long],
  effectOnTargetPost: Option[Effect],
  isDeleted: Boolean,
) derives ReadWriter

case class CommentTreeState(
  targetPostId: Long,
  posts: Map[Long, PostState],
) derives ReadWriter

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
  latestVoteEventId: Long,
  voteEventTime: Long,
)

enum Direction(val value: Int) derives ReadWriter {
  case Up      extends Direction(1)
  case Neutral extends Direction(0)
  case Down    extends Direction(-1)
}

case class VoteState(
  postId: Long,
  vote: Direction,
) derives ReadWriter

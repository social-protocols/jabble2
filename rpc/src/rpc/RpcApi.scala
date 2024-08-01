package rpc

import cats.effect.IO
import upickle.default.ReadWriter
import scala.math.log

trait RpcApi {
  def register(username: String, password: String): IO[Unit]
  def getUsername(): IO[String]
  def increment(x: Int): IO[Int]
  def incrementAuthorized(x: Int): IO[Int]
  def createPost(content: String): IO[Unit]
  def createReply(parentId: Long, content: String): IO[Unit]
  def getPosts(): IO[Vector[Post]]
  def getPostTree(rootPostId: Long): IO[Option[PostTree]]
  def vote(postId: Long, parentId: Option[Long], direction: Direction): IO[Unit]
  def getPostTreeData(targetPostId: Long): IO[PostTreeData]
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

case class PostTree(post: Post, replies: Vector[PostTree]) derives ReadWriter

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
) derives ReadWriter:
  lazy val effectSizeOnTarget: Double = relativeEntropy(p, q) * pSize

def relativeEntropy(p: Double, q: Double): Double = {
  val logP    = if (p == 0.0) 0.0 else log(p)
  val logNotP = if (p == 1.0) 0.0 else log(1.0 - p)
  val logQ    = if (q == 0.0) 0 else log(q)
  val logNotQ = if (q == 1.0) 0.0 else log(1.0 - q)
  p * (logP - logQ) + (1.0 - p) * (logNotP - logNotQ)
}

case class PostData(
  postId: Long,
  userVote: Direction,
  voteCount: Long,
  p: Option[Long],
  effectOnTargetPost: Option[Effect],
  isDeleted: Boolean,
) derives ReadWriter

case class PostTreeData(
  targetPostId: Long,
  posts: Map[Long, PostData],
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

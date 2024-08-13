package backend.globalbrain

import org.http4s.syntax.all.uri
import upickle.default._
import upickle.implicits.key
import org.http4s.Method
import org.http4s.client.Client
import cats.effect.IO
import org.http4s.Request

case class VoteEvent(
  @key("user_id") userId: String,
  @key("parent_id") parentId: Option[Long],
  @key("post_id") postId: Long,
  @key("vote") vote: Int,
  @key("vote_event_time") voteEventTime: Long,
  @key("vote_event_id") voteEventId: Long,
) derives ReadWriter

case class ScoreEvent(
  @key("vote_event_id") voteEventId: Long,
  @key("vote_event_time") voteEventTime: Long,
  @key("score") score: Option[Score] = None,
  @key("effect") effect: Option[Effect] = None,
) derives ReadWriter

case class Score(
  @key("post_id") postId: Long,
  @key("o") o: Double,
  @key("o_count") oCount: Int,
  @key("o_size") oSize: Int,
  @key("p") p: Double,
  @key("score") score: Double,
) derives ReadWriter

case class Effect(
  @key("post_id") postId: Long,
  @key("comment_id") commentId: Long,
  @key("p") p: Double,
  @key("p_count") pCount: Int,
  @key("p_size") pSize: Int,
  @key("q") q: Double,
  @key("q_count") qCount: Int,
  @key("q_size") qSize: Int,
  @key("r") r: Double,
  @key("weight") weight: Double,
) derives ReadWriter

def sendVoteEvents(events: Vector[VoteEvent], httpClient: Client[IO]): IO[Vector[ScoreEvent]] = lift {
  val result = unlift(
    httpClient.expect[String](
      Request[IO](
        method = Method.POST,
        uri = uri"http://localhost:8000/score",
      ).withEntity(write(events))
    )
  )
  read[Vector[ScoreEvent]](result)
}

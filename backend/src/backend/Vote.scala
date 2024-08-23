package backend

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import org.http4s.client.Client
import cats.effect.IO
import cats.effect.unsafe.implicits.global // TODO

def submitVote(userId: String, postId: Long, direction: rpc.Direction, httpClient: Client[IO])(using con: DbCon): Unit = {
  val currentVote = getVote(userId, postId)
  val newState    = if (direction == currentVote) rpc.Direction.Neutral else direction
  val parentId    = db.PostRepo.findById(postId).flatMap(_.parentId)
  val dbVoteEvent = db.VoteEventRepo.insertReturning(
    db.VoteEvent.Creator(
      userId = userId,
      postId = postId,
      vote = newState.value,
      parentId = parentId,
    )
  )
  val updateScoreOrEffectEvents = globalbrain
    .sendVoteEvents(
      Vector(
        globalbrain.VoteEvent(
          userId = dbVoteEvent.userId,
          parentId = dbVoteEvent.parentId,
          postId = dbVoteEvent.postId,
          vote = newState.value,
          voteEventTime = dbVoteEvent.voteEventTime,
          voteEventId = dbVoteEvent.voteEventId,
        )
      ),
      httpClient,
    )
    .unsafeRunSync() // TODO: real async
  for (updateScoreOrEffectEvent <- updateScoreOrEffectEvents) {
    updateScoreOrEffectEvent.score.foreach { score =>
      insertScoreEvent(
        db.ScoreEvent.Creator(
          voteEventId = updateScoreOrEffectEvent.voteEventId,
          voteEventTime = updateScoreOrEffectEvent.voteEventTime,
          postId = score.postId,
          o = score.o,
          oCount = score.oCount,
          oSize = score.oSize,
          p = score.p,
          score = score.score,
        )
      )
    }
    updateScoreOrEffectEvent.effect.foreach { effect =>
      insertEffectEvent(
        db.EffectEvent.Creator(
          voteEventId = updateScoreOrEffectEvent.voteEventId,
          voteEventTime = updateScoreOrEffectEvent.voteEventTime,
          postId = effect.postId,
          commentId = effect.commentId,
          p = effect.p,
          pCount = effect.pCount,
          pSize = effect.pSize,
          q = effect.q,
          qCount = effect.qCount,
          qSize = effect.qSize,
          r = effect.r,
        )
      )
    }
  }
}

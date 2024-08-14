package backend

import com.augustnagro.magnum
import com.augustnagro.magnum.*

def submitVote(userId: String, postId: Long, direction: rpc.Direction)(using con: DbCon) = {
  val currentVote = getVote(userId, postId)
  val newState    = if (direction == currentVote) rpc.Direction.Neutral else direction
  val parentId    = db.PostRepo.findById(postId).flatMap(_.parentId)
  db.VoteEventRepo.insert(
    db.VoteEvent.Creator(
      userId = userId,
      postId = postId,
      vote = newState.value,
      parentId = parentId,
    )
  )
}

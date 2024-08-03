package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

type Row_getVote = Long

def getVote(
  user_id: String,
  post_id: Long,
)(using con: DbCon): Vector[Row_getVote] = {
  Frag(
    """
  select vote
from vote
where user_id = ?
and post_id = ?
  """,
    params = IArray(
      user_id,
      post_id,
    ),
  ).query[Row_getVote].run()
}

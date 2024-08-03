package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

type Row_getVoteCount = Long

def getVoteCount(
  post_id: Long
)(using con: DbCon): Row_getVoteCount = {
  Frag(
    """
  select count(*)
from vote
where post_id = ?
and vote != 0
  """,
    params = IArray(
      post_id
    ),
  ).query[Row_getVoteCount].run().head
}

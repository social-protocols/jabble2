package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

//  https://docs.sqlc.dev/en/stable/reference/query-annotations.html

type Row_getReplyIds = Long

def getReplyIds(
  parent_id: Long
)(using con: DbCon): Vector[Row_getReplyIds] = {
  Frag(
    """
  
select id
from post
where parent_id = ?
  """,
    params = IArray(
      parent_id
    ),
  ).query[Row_getReplyIds].run()
}

type Row_getDescendantIds = Long

def getDescendantIds(
  ancestor_id: Long
)(using con: DbCon): Vector[Row_getDescendantIds] = {
  Frag(
    """
  select descendant_id
from lineage
where ancestor_id = ?
  """,
    params = IArray(
      ancestor_id
    ),
  ).query[Row_getDescendantIds].run()
}

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

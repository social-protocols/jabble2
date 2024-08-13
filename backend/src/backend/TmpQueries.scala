package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

/*
  This is a temporary replacement for the generated query code in backend.queries.
  Once the sqlc code generation works, this file can be deleted.
 */

def getReplyIds(
  parentId: Long
)(using con: DbCon): Vector[Long] = {
  sql"""
    select id
    from post
    where parent_id = $parentId
  """
    .query[Long]
    .run()
}

def getDescendantIds(
  ancestorId: Long
)(using con: DbCon): Vector[Long] = {
  sql"""
    select descendant_id
    from lineage
    where ancestor_id = $ancestorId
  """
    .query[Long]
    .run()
}

def getVote(
  userId: String,
  postId: Long,
)(using con: DbCon): Vector[Long] = {
  sql"""
    select vote
    from vote
    where user_id = $userId
    and post_id = $postId
  """
    .query[Long]
    .run()
}

def getVoteCount(
  postId: Long
)(using con: DbCon): Long = {
  sql"""
    select count(*)
    from vote
    where post_id = $postId
    and vote != 0
  """
    .query[Long]
    .run()
    .head
}

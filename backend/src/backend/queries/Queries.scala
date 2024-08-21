// This file is generated from queries.sql using queries_template.go.tmpl
package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

def fragWriter(params: Seq[Any]): FragWriter = { (preparedStatement, startPos) =>
  var i = 0
  val n = params.size
  while (i < n) {
    val param = params(i)
    param match {
      case param: Int    => preparedStatement.setInt(startPos + i, param)
      case param: Long   => preparedStatement.setLong(startPos + i, param)
      case param: String => preparedStatement.setString(startPos + i, param)
      case param: Double => preparedStatement.setDouble(startPos + i, param)
    }
    i += 1
  }
  startPos + params.size
}

//  https://docs.sqlc.dev/en/stable/reference/query-annotations.html

type Row_getReplyIds = Long

def getReplyIds(
  parent_id: Long
)(using con: DbCon): Vector[Row_getReplyIds] = {
  val params = IArray(
    parent_id
  )
  val result = Frag(
    """
  
select id
from post
where parent_id = ?
  """,
    params,
    fragWriter(params),
  ).query[Row_getReplyIds].run()
  println(s"getReplyIds(parent_id=${parent_id}, ) => ${result}")
  result
}

type Row_getDescendantIds = Long

def getDescendantIds(
  ancestor_id: Long
)(using con: DbCon): Vector[Row_getDescendantIds] = {
  val params = IArray(
    ancestor_id
  )
  val result = Frag(
    """
  select descendant_id
from lineage
where ancestor_id = ?
  """,
    params,
    fragWriter(params),
  ).query[Row_getDescendantIds].run()
  println(s"getDescendantIds(ancestor_id=${ancestor_id}, ) => ${result}")
  result
}

type Row_getVote = Long

def getVote(
  user_id: String,
  post_id: Long,
)(using con: DbCon): Vector[Row_getVote] = {
  val params = IArray(
    user_id,
    post_id,
  )
  val result = Frag(
    """
  select vote
from vote
where user_id = ?
and post_id = ?
  """,
    params,
    fragWriter(params),
  ).query[Row_getVote].run()
  println(s"getVote(user_id=${user_id}, post_id=${post_id}, ) => ${result}")
  result
}

type Row_getVoteCount = Long

def getVoteCount(
  post_id: Long
)(using con: DbCon): Row_getVoteCount = {
  val params = IArray(
    post_id
  )
  val result = Frag(
    """
  select count(*)
from vote
where post_id = ?
and vote != 0
  """,
    params,
    fragWriter(params),
  ).query[Row_getVoteCount].run().head
  println(s"getVoteCount(post_id=${post_id}, ) => ${result}")
  result
}

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

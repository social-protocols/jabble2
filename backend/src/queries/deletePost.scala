package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

def deletePost(
  id: Long
)(using con: DbCon): Unit = {
  Frag(
    """
  delete from post where id = ?
  """,
    params = IArray(
      id
    ),
  ).update.run()
}

package backend.queries

import com.augustnagro.magnum
import com.augustnagro.magnum.*

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

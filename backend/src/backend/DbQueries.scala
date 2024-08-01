package backend

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import io.github.arainko.ducktape.*

given DbCodec[rpc.Direction] = DbCodec[Long].biMap(
  {
    case 1     => rpc.Direction.Up
    case 0     => rpc.Direction.Neutral
    case -1    => rpc.Direction.Down
    case other => throw Exception(s"Not a valid Direction encoding: $other")
  },
  {
    case rpc.Direction.Up      => 1
    case rpc.Direction.Neutral => 0
    case rpc.Direction.Down    => -1
  },
)

def getReplyIds(postId: Long)(using con: DbCon): Vector[Long] = {
  sql"""
    select id
    from Post
    where parent_id = ${postId}
  """.query[Long].run()
}

def getRecursiveComments(postId: Long)(using con: DbCon): Option[rpc.PostTree] = {
  val post = db.PostRepo.findById(postId)
  post.map { post =>
    val replyIds = getReplyIds(postId)
    val replies  = replyIds.flatMap(getRecursiveComments)
    rpc.PostTree(post.to[rpc.Post], replies)
  }
}

def getSubtreePostIds(subrootPostId: Long)(using con: DbCon): Vector[Long] = {
  val descendantIds = sql"""
    select descendant_id
    from lineage
    where ancestor_id = ${subrootPostId}
  """.query[Long].run()
  Vector(subrootPostId).concat(descendantIds)
}

def getAllSubtreePosts(subrootPostId: Long)(using con: DbCon): Vector[rpc.Post] = {
  val subtreePostIds: Vector[Long] = getSubtreePostIds(subrootPostId)
  subtreePostIds.flatMap { id =>
    db.PostRepo.findById(id).map { _.to[rpc.Post] }
  }
}

def getVote(userId: String, postId: Long)(using con: DbCon): rpc.Direction = {
  val direction = sql"""
    select vote
    from vote
    where user_id = ${userId}
    and post_id = ${postId}
  """.query[rpc.Direction].run().headOption

  direction.getOrElse(rpc.Direction.Neutral)
}

def getVoteCount(postId: Long)(using con: DbCon): Long = {
  sql"""
    select count(*)
    from vote
    where post_id = ${postId}
    and vote != 0
  """.query[Long].run().head
}

def getDbPostTreeData(targetPostId: Long, userId: String)(using con: DbCon): rpc.PostTreeData = {
  rpc.PostTreeData(
    targetPostId,
    getAllSubtreePosts(targetPostId).view.map { post =>
      post.id -> rpc.PostData(
        post.id,
        getVote(userId, post.id),
        getVoteCount(post.id),
        None, // TODO: actual informed probability
        None, // TODO: actual effectOnTargetPost
        post.deletedAt.isDefined,
      )
    }.toMap,
  )
}

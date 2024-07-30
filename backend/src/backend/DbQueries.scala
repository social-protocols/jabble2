package backend

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import io.github.arainko.ducktape.*

def getReplyIds(postId: Long)(using con: DbCon): Vector[Long] = {
  sql"""
    select id
    from Post
    where parent_id = ${postId}
  """.query[Long].run()
}

def getRecursiveReplies(postId: Long)(using con: DbCon): Option[rpc.ReplyTree] = {
  val post = db.PostRepo.findById(postId)
  post.map { post =>
    val replyIds = getReplyIds(postId)
    val replies  = replyIds.map(getRecursiveReplies).flatten
    rpc.ReplyTree(post.to[rpc.Post], replies)
  }
}

def getSubtreePostIds(subrootPostId: Long)(using con: DbCon): Vector[Long] = {
  val descendantIds = sql"""
    select descendant_id
    from lineage
    where ancestor_id = ${subrootPostId}
  """.query[Long].run()
  println(descendantIds)
  Vector(subrootPostId).concat(descendantIds)
}

def getAllSubtreePosts(subrootPostId: Long)(using con: DbCon): Vector[rpc.Post] = {
  val subtreePostIds = getSubtreePostIds(subrootPostId)
  subtreePostIds.map { id =>
    val post = db.PostRepo.findById(id).map { _.to[rpc.Post] }
    post match {
      case Some(post) => post
      case None       => null
    }
  }
    .filter(_ != null)
}

def getUserVoteState(userId: String, postId: Long)(using con: DbCon): rpc.VoteState = {
  // TODO: proper translation from db post to rpc post with magnum

  val vote = sql"""
    select vote
    from vote
    where user_id = ${userId}
    and post_id = ${postId}
  """.query[Long].run().lastOption

  val direction = vote match {
    case None => rpc.Direction.Neutral
    case Some(vote) =>
      vote match {
        case 1  => rpc.Direction.Up
        case -1 => rpc.Direction.Down
        case _  => rpc.Direction.Neutral
      }
  }
  rpc.VoteState(postId, direction)
}

def getVoteCount(postId: Long)(using con: DbCon): Long = {
  sql"""
    select count(*)
    from vote
    where post_id = ${postId}
    and vote != 0
  """.query[Long].run().last
}

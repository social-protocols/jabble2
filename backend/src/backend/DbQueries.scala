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

def getCurrentVote(userId: String, postId: Long)(using con: DbCon): Option[rpc.Vote] = {
  val vote = sql"""
    select *
    from vote
    where user_id = ${userId}
    and post_id = ${postId}
  """.query[rpc.Vote].run()
  return vote.map(_.to[rpc.Vote]).lastOption
}

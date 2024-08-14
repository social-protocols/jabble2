package backend

import com.augustnagro.magnum
import com.augustnagro.magnum.*
import io.github.arainko.ducktape.*

def getRecursiveComments(postId: Long)(using con: DbCon): Option[rpc.PostTree] = {
  val post = db.PostRepo.findById(postId)
  post.map { post =>
    val replyIds = queries.getReplyIds(postId)
    val replies  = replyIds.flatMap(getRecursiveComments)
    rpc.PostTree(post.to[rpc.Post], replies)
  }
}

def getSubtreePostIds(subrootPostId: Long)(using con: DbCon): Vector[Long] = {
  val descendantIds = queries.getDescendantIds(subrootPostId)
  Vector(subrootPostId).concat(descendantIds)
}

def getAllSubtreePosts(subrootPostId: Long)(using con: DbCon): Vector[rpc.Post] = {
  val subtreePostIds: Vector[Long] = getSubtreePostIds(subrootPostId)
  subtreePostIds.flatMap { id =>
    db.PostRepo.findById(id).map { _.to[rpc.Post] }
  }
}

def getVote(userId: String, postId: Long)(using con: DbCon): rpc.Direction = {
  queries
    .getVote(userId, postId)
    .headOption
    .map(rpc.Direction.fromDb)
    .getOrElse(rpc.Direction.Neutral)
}

def getDbPostTreeData(targetPostId: Long, userId: String)(using con: DbCon): rpc.PostTreeData = {
  rpc.PostTreeData(
    targetPostId,
    getAllSubtreePosts(targetPostId).view.map { post =>
      post.id -> rpc.PostData(
        post.id,
        getVote(userId, post.id),
        queries.getVoteCount(post.id),
        None, // TODO: actual informed probability
        None, // TODO: actual effectOnTargetPost
        post.deletedAt.isDefined,
      )
    }.toMap,
  )
}

def getTransitiveParents(postId: Long)(using con: DbCon): Vector[rpc.Post] = {
  val parentThreadWithTargetPost = sql"""
    with recursive transitive_parents as (
      select
        id
        , parent_id
        , author_id
        , content
        , created_at
        , deleted_at
        , is_private
      from post
      where id = ${postId}
      union all
      select
        p.id
        , p.parent_id
        , p.author_id
        , p.content
        , p.created_at
        , p.deleted_at
        , p.is_private
      from post p
      inner join transitive_parents tp
      on p.id = tp.parent_id
    )
    select *
    from transitive_parents
  """.query[rpc.Post].run()
  parentThreadWithTargetPost.tail.reverse
}

def insertScoreEvent(scoreEvent: db.ScoreEvent.Creator)(using con: DbCon): Unit = {
  // https://github.com/scala/scala3/issues/21383
  // val scoreEventTable = TableInfo[db.ScoreEvent.Creator, db.ScoreEvent, Long]
  // sql"""
  //   INSERT INTO $scoreEventTable ${scoreEventTable.insertColumns} values ($scoreEvent) ON CONFLICT DO NOTHING
  // """.update.run()

  sql"""
    INSERT INTO score_event (vote_event_id, vote_event_time ,post_id ,o ,o_count ,o_size ,p ,score) values ($scoreEvent) ON CONFLICT DO NOTHING
  """.update.run()
}

def insertEffectEvent(effectEvent: db.EffectEvent.Creator)(using con: DbCon): Unit = {
  // https://github.com/scala/scala3/issues/21383
  // val effectEventTable = TableInfo[db.EffectEvent.Creator, db.EffectEvent, Long]
  // sql"""
  //    INSERT INTO $effectEventTable ${effectEventTable.insertColumns} values ($effectEvent) ON CONFLICT DO NOTHING
  //  """.update.run()
  //

  sql"""
     INSERT INTO effect_event (vote_event_id, vote_event_time, post_id, comment_id, p, p_count, p_size, q, q_count, q_size, r) values ($effectEvent) ON CONFLICT DO NOTHING
   """.update.run()
}

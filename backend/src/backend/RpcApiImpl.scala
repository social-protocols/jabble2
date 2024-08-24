package backend

import cats.effect.IO
import org.http4s.Request
import org.http4s.headers.Authorization
import org.http4s.AuthScheme
import org.http4s.Credentials
import cats.implicits.*
import com.augustnagro.magnum
import com.augustnagro.magnum.*
import io.github.arainko.ducktape.*
import javax.sql.DataSource
import org.http4s.ember.client.EmberClientBuilder
import cats.effect.unsafe.implicits.global // TODO
import scala.concurrent.duration.*

import authn.backend.TokenVerifier
import authn.backend.AuthnClient
import authn.backend.AccountImport

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

private val threadPool                     = Executors.newFixedThreadPool(2)
private val dbec: ExecutionContextExecutor = ExecutionContext.fromExecutor(threadPool)

val httpClient = EmberClientBuilder.default[IO].withTimeout(44.seconds).build.allocated.map(_._1).unsafeRunSync()
val authnClient = AuthnClient[IO](
  AppConfig.fromEnv.authnClientConfig,
  httpClient = httpClient,
)
val tokenVerifier = TokenVerifier[IO](AppConfig.fromEnv.authnClientConfig.issuer, AppConfig.fromEnv.authnClientConfig.audiences)

class RpcApiImpl(ds: DataSource, request: Request[IO]) extends rpc.RpcApi {

  // Authn integration

  lazy val headers: Option[Authorization] = request.headers.get[Authorization]
  lazy val token: Option[String]          = headers.collect { case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token }

  def userAccountId: IO[Option[String]] = token.traverse(token => tokenVerifier.verify(token).map(_.accountId))
  def withUser[T](code: String => IO[T]): IO[T] = userAccountId.flatMap {
    case Some(accountId) => code(accountId)
    case None            => IO.raiseError(Exception("403 Unauthorized"))
  }

  // RpcApi implementations

  def register(username: String, password: String): IO[Boolean] = lift {
    val accountImportResult = unlift(authnClient.importAccount(AccountImport(username, password)).attempt)
    accountImportResult match {
      case Left(error) =>
        scribe.info("account creation failed", error)
        false
      case Right(accountImport) =>
        Either.catchNonFatal {
          magnum.connect(ds) {
            db.UserProfileRepo.insert(db.UserProfile.Creator(userId = accountImport.id.toString, userName = username))
          }
        } match {
          case Left(error) =>
            // if database fails, remove the just created account
            unlift(authnClient.archiveAccount(accountImport.id.toString))
            throw error
            false
          case Right(_) => true
        }
    }
  }

  def getUserProfile(): IO[Option[rpc.UserProfile]] = lift {
    val userIdOpt = unlift(userAccountId.attempt).toOption.flatten
    magnum.connect(ds) {
      userIdOpt.flatMap(userId => db.UserProfileRepo.findById(userId).map(_.to[rpc.UserProfile]))
    }
  }

  def createPost(content: String, withUpvote: Boolean): IO[Unit] = withUser { userId =>
    IO {
      magnum.connect(ds) {
        val newPost: rpc.Post =
          db.PostRepo.insertReturning(db.Post.Creator(parentId = None, authorId = userId, content = content)).to[rpc.Post]
        if (withUpvote) {
          submitVote(userId, newPost.id, rpc.Direction.Up, httpClient)
        }
      }
    }
  }

  def createReply(parentId: Long, targetPostId: Long, content: String, withUpvote: Boolean): IO[(rpc.PostTree, rpc.PostTreeData)] =
    withUser { userId =>
      IO {
        magnum.transact(ds) {
          val newPost: rpc.Post =
            db.PostRepo.insertReturning(db.Post.Creator(parentId = Some(parentId), authorId = userId, content = content)).to[rpc.Post]
          if (withUpvote) {
            submitVote(userId, newPost.id, rpc.Direction.Up, httpClient)
          }
          getPostWithScore(newPost.id) match {
            case Some(post) =>
              (
                rpc.PostTree(post, Vector.empty),
                getDbPostTreeData(targetPostId, userId),
              )
            case None => throw Exception(s"New reply to $parentId couldn't be created")
          }
        }
      }
    }

  def getPosts(): IO[Vector[rpc.Post]] = {
    IO {
      magnum.connect(ds) {
        db.PostRepo.findAll.map(_.to[rpc.Post]).sortBy(-_.createdAt)
      }
    }
  }

  def getPostTree(rootPostId: Long): IO[Option[rpc.PostTree]] = withUser { userId =>
    IO {
      magnum.transact(ds) {
        val postTreeData = getDbPostTreeData(rootPostId, userId)
        getRecursivePostTree(rootPostId, postTreeData)
      }
    }
  }

  def vote(postId: Long, targetPostId: Long, direction: rpc.Direction): IO[rpc.PostTreeData] = withUser { userId =>
    IO {
      magnum.transact(ds) {
        submitVote(userId, postId, direction, httpClient)
        getDbPostTreeData(targetPostId, userId)
      }
    }
  }

  def getPostTreeData(targetPostId: Long): IO[rpc.PostTreeData] = withUser { userId =>
    IO { magnum.transact(ds) { getDbPostTreeData(targetPostId, userId) } }
  }

  def getParentThread(targetPostId: Long): IO[Vector[rpc.Post]] = {
    IO {
      magnum.connect(ds) {
        getTransitiveParents(targetPostId)
      }
    }
  }

  def setDeletedAt(postId: Long, deletedAt: Option[Long]): IO[Unit] = withUser { userId =>
    IO {
      magnum.connect(ds) {
        checkIsAdminOrThrow(userId)
        val existingPost = db.PostRepo.findById(postId)
        existingPost.map { post =>
          db.PostRepo.update(post.copy(deletedAt = deletedAt))
        }.getOrElse(throw Exception(s"Cannot delete post: Post $postId not found"))
      }
    }
  }
}

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
import authn.backend.AuthnClientConfig
import authn.backend.AccountImport

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

private val threadPool                     = Executors.newFixedThreadPool(2)
private val dbec: ExecutionContextExecutor = ExecutionContext.fromExecutor(threadPool)

class RpcApiImpl(ds: DataSource, request: Request[IO]) extends rpc.RpcApi {

  // Authn integration
  val headers: Option[Authorization] = request.headers.get[Authorization]
  val token: Option[String]          = headers.collect { case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token }
  val httpClient                     = EmberClientBuilder.default[IO].withTimeout(44.seconds).build.allocated.map(_._1).unsafeRunSync() // TODO not forever
  val authnClient = AuthnClient[IO](
    AuthnClientConfig(
      issuer = "http://localhost:3000",
      audiences = Set("localhost"),
      username = "admin",
      password = "adminpw",
      adminURL = Some("http://localhost:3001"),
    ),
    httpClient = httpClient,
  )
  val verifier                          = TokenVerifier[IO]("http://localhost:3000", Set("localhost"))
  def userAccountId: IO[Option[String]] = token.traverse(token => verifier.verify(token).map(_.accountId))
  def withUser[T](code: String => IO[T]): IO[T] = userAccountId.flatMap {
    case Some(accountId) => code(accountId)
    case None            => IO.raiseError(Exception("403 Unauthorized"))
  }

  def register(username: String, password: String): IO[Unit] = lift {
    val accountImport = unlift(authnClient.importAccount(AccountImport(username, password)))
    unlift(
      IO {
        magnum.connect(ds) {
          db.UserProfileRepo.insert(db.UserProfile.Creator(userId = accountImport.id.toString, userName = username))
        }
      }.onError(e =>
        scribe.info("account creation failed", e)
        // if database fails, remove the just created account
        authnClient.archiveAccount(accountImport.id.toString)
      )
    )
  }

  def getUsername(): IO[String] = withUser { userId =>
    lift {
      val account = unlift(authnClient.account(userId))
      account.username
    }
  }

  def getUserProfile(): IO[rpc.UserProfile] = withUser { userId =>
    IO {
      magnum.connect(ds) {
        db.UserProfileRepo.findById(userId).map(_.to[rpc.UserProfile]).getOrElse(throw Exception(s"User $userId not found"))
      }
    }
  }

  def increment(x: Int): IO[Int] = IO.pure(x + 1)
  def incrementAuthorized(x: Int): IO[Int] = withUser { user =>
    lift {
      println(s"user $user incremented")
      x + 1
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
          (
            rpc.PostTree(newPost, Vector.empty),
            getDbPostTreeData(targetPostId, userId),
          )
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

  def getPostTree(rootPostId: Long): IO[Option[rpc.PostTree]] = {
    IO {
      magnum.transact(ds) {
        getRecursiveComments(rootPostId)
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

package backend

import com.augustnagro.magnum
import com.augustnagro.magnum.*

def checkIsAdminOrThrow(userId: String)(using con: DbCon) = {
  val userIsAdmin: Option[db.UserProfile] = db.UserProfileRepo.findById(userId)
  userIsAdmin match {
    case Some(user) => if (user.isAdmin != 1) throw new Exception(s"User $userId is not an admin")
    case None       => throw new Exception(s"User $userId not found")
  }
}

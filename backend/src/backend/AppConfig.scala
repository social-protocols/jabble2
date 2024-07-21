package backend

import org.sqlite.SQLiteDataSource

case class AppConfig(
  frontendDistributionPath: String,
  jdbcUrl: String,
) {
  val dataSource = SQLiteDataSource().tap(_.setUrl(jdbcUrl)).tap(_.setEnforceForeignKeys(true)).tap(_.setJournalMode("WAL"))
}

object AppConfig {
  def fromEnv(): AppConfig = AppConfig(
    frontendDistributionPath = sys.env("FRONTEND_DISTRIBUTION_PATH"),
    jdbcUrl = sys.env("JDBC_URL"),
  )
}

package backend

import org.sqlite.SQLiteDataSource
import authn.backend.AuthnClientConfig

case class AppConfig(
  frontendDistributionPath: String,
  jdbcUrl: String,
  authnClientConfig: AuthnClientConfig,
) {
  val dataSource = {
    val ds = SQLiteDataSource()
    ds.setUrl(jdbcUrl)
    ds.setEnforceForeignKeys(true)
    ds.setJournalMode("WAL")
    ds
  }
}

object AppConfig {
  lazy val fromEnv: AppConfig = AppConfig(
    frontendDistributionPath = sys.env("FRONTEND_DISTRIBUTION_PATH"),
    jdbcUrl = sys.env("JDBC_URL"),
    AuthnClientConfig(
      issuer = sys.env("AUTHN_URL"),
      audiences = sys.env("AUTHN_AUDIENCES").split(",").map(_.trim).toSet,
      username = sys.env("AUTHN_HTTP_AUTH_USERNAME"),
      password = sys.env("AUTHN_HTTP_AUTH_PASSWORD"),
      adminURL = sys.env.get("AUTHN_ADMIN_URL"),
    ),
  )
}

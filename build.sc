import mill._, scalalib._, scalajslib._

import $repo.`https://oss.sonatype.org/content/repositories/snapshots`
import $repo.`https://oss.sonatype.org/content/repositories/public`

import $ivy.`com.github.cornerman::mill-db-codegen:0.5.0`, dbcodegen.plugin._
import $ivy.`com.github.cornerman::mill-web-components-codegen:0.1.2`, webcodegen.plugin._

import mill.scalajslib._
import mill.scalajslib.api._

trait AppScalaModule extends ScalaModule {
  def scalaVersion = "3.4.2"
  val versions = new {
    val authn    = "0.1.3"
    val colibri  = "0.8.4"
    val outwatch = "1.0.0"
    val sloth    = "0.8.0"
  }
  def ivyDeps = Agg(
    ivy"org.typelevel::cats-effect::3.5.4",
    ivy"com.github.rssh::dotty-cps-async::0.9.21",
    ivy"com.github.rssh::cps-async-connect-cats-effect::0.9.21",
  )

  def forkArgs = Seq("-Xmx256m")
}

trait AppScalacOptions extends ScalaModule {
  val isCi = sys.env.get("CI").contains("true")

  def scalacOptions = T {
    super.scalacOptions() ++ Seq(
      "-encoding",
      "utf8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:higherKinds",
      "-Ykind-projector",
      "-Wnonunit-statement",
      "-Wunused:imports,privates,params,locals,implicits,explicits",
      // default imports in every scala file. we use the scala defaults + chaining + cps for direct syntax with lift/unlift/!
      // https://docs.scala-lang.org/overviews/compiler-options/
      "-Yimports:java.lang,scala,scala.Predef,scala.util.chaining,cps.syntax.monadless,cps.monads.catsEffect",
    ) ++ Option.when(isCi)("-Xfatal-warnings")
  }
}

trait AppScalaJSModule extends AppScalaModule with ScalaJSModule {
  def scalaJSVersion = "1.16.0"

  def scalacOptions = T {
    // vite serves source maps from the out-folder. Fix the relative path to the source files:
    // TODO: for production builds, point sourcemap to github
    super.scalacOptions() ++ Seq(s"-scalajs-mapSourceURI:${T.workspace.toIO.toURI}->../../../.")
  }
}

object frontend extends AppScalaJSModule with AppScalacOptions {
  def moduleKind       = ModuleKind.ESModule
  def moduleSplitStyle = ModuleSplitStyle.SmallModulesFor(List("frontend"))

  def moduleDeps = Seq(webcomponents, rpc.js)
  def ivyDeps = Agg(
    ivy"io.github.outwatch::outwatch::${versions.outwatch}",
    ivy"com.github.cornerman::colibri::${versions.colibri}",
    ivy"com.github.cornerman::colibri-reactive::${versions.colibri}",
    ivy"com.github.cornerman::colibri-fs2::${versions.colibri}",
    ivy"com.github.cornerman::colibri-router::${versions.colibri}",
    ivy"org.http4s::http4s-dom::0.2.11",
    ivy"com.github.cornerman::sloth-jsdom-client::${versions.sloth}",
    ivy"com.github.cornerman::keratin-authn-frontend::${versions.authn}",
    ivy"org.scala-js:scalajs-java-securerandom_sjs1_2.13:1.0.0",
  )
}

object backend extends AppScalaModule with AppScalacOptions {
  def moduleDeps = Seq(dbschema, rpc.jvm)
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.xerial:sqlite-jdbc::3.46.0.0",
    ivy"com.zaxxer:HikariCP:5.1.0",
    ivy"com.augustnagro::magnum::1.2.0", // db access
    ivy"com.github.cornerman::sloth-http4s-server::${versions.sloth}",
    ivy"org.http4s::http4s-ember-server::0.23.24",
    ivy"org.http4s::http4s-ember-client::0.23.24",
    ivy"org.http4s::http4s-dsl::0.23.24",
    ivy"com.outr::scribe-slf4j2::3.13.0",  // logging
    ivy"org.flywaydb:flyway-core::10.6.0", // migrations
    ivy"com.github.cornerman::keratin-authn-backend::${versions.authn}",
    ivy"io.github.arainko::ducktape::0.2.1",
    ivy"com.lihaoyi::fansi:0.5.0",
    ivy"com.lihaoyi::pprint:0.9.0",
  )
}

object rpc extends Module {
  trait SharedModule extends AppScalaModule with AppScalacOptions with PlatformScalaModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"com.github.cornerman::sloth::${versions.sloth}", // rpc
      ivy"com.lihaoyi::upickle::4.0.0",                    // json and msgpack
    )
  }
  object jvm extends SharedModule
  object js  extends SharedModule with AppScalaJSModule
}

object webcomponents extends AppScalaJSModule with WebCodegenModule {
  override def webcodegenCustomElements = Seq(
    webcodegen
      .CustomElements("shoelace", (os.pwd / "node_modules" / "@shoelace-style" / "shoelace" / "dist" / "custom-elements.json").toIO)
  )
  override def webcodegenTemplates = Seq(
    webcodegen.Template.Outwatch
  )
  def ivyDeps = Agg(
    ivy"io.github.outwatch::outwatch::${versions.outwatch}"
  )

}

object dbschema extends AppScalaModule with DbCodegenModule {
  def dbTemplateFile = T.source(os.pwd / "schema.scala.ssp")
  def dbSchemaFile   = T.source(os.pwd / "schema.sql")

  def dbcodegenTemplateFiles = T { Seq(dbTemplateFile()) }
  def dbcodegenJdbcUrl       = "jdbc:sqlite:file::memory:?cache=shared"
  def dbcodegenSetupTask = T.task { (db: Db) =>
    db.executeSqlFile(dbSchemaFile())
  }
  def dbcodegenTypeMapping: (java.sql.SQLType, Option[String]) => Option[String] = (sqltype, tpe) =>
    sqltype.getVendorTypeNumber.intValue() match {
      // https://www.sqlite.org/datatype3.html
      // sqlite ints can store 64bits
      case java.sql.Types.INTEGER => Some("Long")
      case _                      => tpe
    }

  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.augustnagro::magnum::1.2.0" // db access
  )
}

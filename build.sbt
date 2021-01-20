enablePlugins(ScalaJSPlugin)

lazy val scalatagVersion = "0.9.2"
lazy val scalaReflectPortableVersion = "1.0.0"

scalaVersion := "2.13.4"
name := "table"
version := "0.0.2a"
organization := "com.github.p2m2"
scalaJSUseMainModuleInitializer := true
mainClass in Compile := Some("inrae.application.TableApp")

resolvers += Resolver.bintrayRepo("hmil", "maven")
resolvers ++= Seq("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

libraryDependencies ++= Seq(
  "com.lihaoyi" %%% "scalatags" % scalatagVersion,
  "org.portable-scala" %%% "portable-scala-reflect" % scalaReflectPortableVersion,
  "org.querki" %%% "jquery-facade" % "2.0",
  "com.github.p2m2" %%% "discovery" % "0.0.2-beta.3-SNAPSHOT",
)


Global / onChangedBuildSource := ReloadOnSourceChanges

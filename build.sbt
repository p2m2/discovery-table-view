enablePlugins(ScalaJSPlugin)

lazy val scalatagVersion = "0.9.2"
lazy val scalaReflectPortableVersion = "1.0.0"

scalaVersion := "2.13.4"
name := "table"
version := "0.0.1"
organization := "com.github.p2m2"
scalaJSUseMainModuleInitializer := true
mainClass in Compile := Some("inrae.application.TableApp")

resolvers += Resolver.bintrayRepo("hmil", "maven")

libraryDependencies ++= Seq(
  "com.lihaoyi" %%% "scalatags" % scalatagVersion,
  "org.portable-scala" %%% "portable-scala-reflect" % scalaReflectPortableVersion,
  "com.github.p2m2" %%% "discovery" % "0.0.2-SNAPSHOT",
)


Global / onChangedBuildSource := ReloadOnSourceChanges

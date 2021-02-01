
lazy val scalatagVersion = "0.9.2"
lazy val scalaReflectPortableVersion = "1.0.0"

scalaVersion := "2.13.4"
name := "table"
version := "0.0.4"
organization := "com.github.p2m2"
scalaJSUseMainModuleInitializer := true
mainClass in Compile := Some("inrae.application.TableApp")

resolvers ++= Seq(
  Resolver.sonatypeRepo("public")
)

resolvers ++= Seq("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
                  "releases" at "https://oss.sonatype.org/content/repositories/public/")

libraryDependencies ++= Seq(
  "com.lihaoyi" %%% "scalatags" % scalatagVersion,
  "org.querki" %%% "jquery-facade" % "2.0",
  "com.github.p2m2" %%% "discovery" % "develop-SNAPSHOT",
)

scalaJSLinkerConfig in (Compile, fastOptJS ) ~= {
  _.withOptimizer(false)
    .withPrettyPrint(true)
    .withSourceMap(true)
}

scalaJSLinkerConfig in (Compile, fullOptJS) ~= {
  _.withSourceMap(false)
    .withModuleKind(ModuleKind.CommonJSModule)
}

enablePlugins(ScalaJSBundlerPlugin)
webpackBundlingMode := BundlingMode.LibraryAndApplication()

npmDependencies in Compile ++= Seq("jquery" -> "3.5.1")

Global / onChangedBuildSource := ReloadOnSourceChanges

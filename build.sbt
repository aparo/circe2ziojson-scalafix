lazy val V = _root_.scalafix.sbt.BuildInfo

val rulesCrossVersions = Seq(V.scala212, V.scala213)
val scala3Version = "3.1.3"

inThisBuild(
  List(
    tlBaseVersion := "0.1",
    organization := "io.megl",
    homepage := Some(url("https://github.com/hamnis/circe2zjson-scalafix")),
    startYear := Some(2022),
    licenses := Seq(License.Apache2),
    developers := List(
      Developer(
        "aparo",
        "Alberto Paro",
        "alberto.paro@gmail.com",
        url("https://github.com/aparo"),
      )
    ),
    crossScalaVersions := rulesCrossVersions,
    scalaVersion := crossScalaVersions.value.head,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
  )
)

lazy val `data` = (project in file("."))
  .aggregate(
    rules.projectRefs ++
      input.projectRefs ++
      output.projectRefs ++
      circeAnnotation.projectRefs ++
      tests.projectRefs: _*
  )
  .enablePlugins(NoPublishPlugin)

lazy val circeAnnotation = projectMatrix
  .settings(
    moduleName := "circe-annotation",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-json" % "0.3.0"
//      "io.circe" %%%"circe-generic-extras"%"0.14.2",
//      "io.circe" %%% "circe-derivation-annotations" % "0.13.0-M5"
    )
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions /*:+ scala3Version*/)

lazy val rules = projectMatrix
  .settings(
    moduleName := "dataclass-scalafix",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion,
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions)

lazy val input = projectMatrix
  .dependsOn(circeAnnotation)
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions /*:+ scala3Version*/)
  .enablePlugins(NoPublishPlugin)

lazy val output = projectMatrix
  .dependsOn(circeAnnotation)
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions /*:+ scala3Version*/)
  .enablePlugins(NoPublishPlugin)

lazy val testsAggregate = Project("tests", file("target/testsAggregate"))
  .aggregate(tests.projectRefs: _*)
  .settings(
    publish / skip := true
  )

lazy val tests = projectMatrix
  .settings(
    publish / skip := true,
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      TargetAxis
        .resolve(output, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputSourceDirectories :=
      TargetAxis
        .resolve(input, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputClasspath :=
      TargetAxis.resolve(input, Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions :=
      TargetAxis.resolve(input, Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      TargetAxis.resolve(input, Compile / scalaVersion).value,
  )
  .defaultAxes(
    rulesCrossVersions.map(VirtualAxis.scalaABIVersion) :+ VirtualAxis.jvm: _*
  )
  .customRow(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(V.scala213), VirtualAxis.jvm),
    settings = Seq(),
  )
//  .customRow(
//    scalaVersions = Seq(V.scala213),
//    axisValues = Seq(TargetAxis(scala3Version), VirtualAxis.jvm),
//    settings = Seq(),
//  )
  .customRow(
    scalaVersions = Seq(V.scala212),
    axisValues = Seq(TargetAxis(V.scala212), VirtualAxis.jvm),
    settings = Seq(),
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)

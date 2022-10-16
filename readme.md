# Scalafix rules for dataclass
[![Continuous Integration](https://github.com/aparo/circe2ziojson-scalafix/actions/workflows/ci.yml/badge.svg)](https://github.com/aparo/circe2ziojson-scalafix/actions/workflows/ci.yml) 

### Usage

The use of scalafix as a source generator is documented in [olafurpg/scalafix-codegen][1].
Since we need to produce SemanticDB for the data class source, but we want to
avoid infinite loop of generation, we need to split data classes into another subproject.

```scala
ThisBuild / scalaVersion      := "3.1.3"
ThisBuild / version           := "0.1.0-SNAPSHOT"
ThisBuild / semanticdbEnabled := true
def circe2ziojsonScalafixV = "VERSION-FROM-BADGE"
ThisBuild / scalafixDependencies += "io.megl" %% "circe2ziojson-scalafix" % circe2ziojsonScalafixV

def circe2ZioJsonGen(data: Reference) = Def.taskDyn {
  val root = (ThisBuild / baseDirectory).value.toURI.toString
  val from = (data / Compile / sourceDirectory).value
  val to = (Compile / sourceManaged).value
  val outFrom = from.toURI.toString.stripSuffix("/").stripPrefix(root)
  val outTo = to.toURI.toString.stripSuffix("/").stripPrefix(root)
  (data / Compile / compile).value
  Def.task {
    (data / Compile / scalafix)
      .toTask(s" --rules MigrateCirce2ZioJson --out-from=$outFrom --out-to=$outTo")
      .value
    (to ** "*.scala").get
  }
}

lazy val app = project
  .settings(
    Compile / sourceGenerators += circe2ZioJsonGen(definitions).taskValue,
  )

// put data classes here
lazy val definitions = project
```

Optionally apply [scalafmt](https://scalameta.org/scalafmt/) to make it somewhat readable.


### To develop rule

```
sbt ~tests/test
# edit rules/src/main/scala/fix/MigrateCirce2ZioJson.scala
```

  [1]: https://github.com/olafurpg/scalafix-codegen

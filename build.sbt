organization := "com.audienceproject"

name := "spark-dynamodb"

version := "1.1.3"

description := "Plug-and-play implementation of an Apache Spark custom data source for AWS DynamoDB."

scalaVersion := "2.12.12"

compileOrder := CompileOrder.JavaThenScala

resolvers += "DynamoDBLocal" at "https://s3-us-west-2.amazonaws.com/dynamodb-local/release"

//For M1 architecture
Global / serverConnectionType := ConnectionType.Tcp

libraryDependencies += "com.amazonaws" % "aws-java-sdk-sts" % "1.11.678"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.678"
libraryDependencies += "com.amazonaws" % "DynamoDBLocal" % "[1.11,2.0)" % "test" exclude("com.google.guava", "guava")

libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.0.0" % "provided"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"

libraryDependencies ++= {
    val log4j2Version = "2.11.1"
    Seq(
        "org.apache.logging.log4j" % "log4j-api" % log4j2Version % "test",
        "org.apache.logging.log4j" % "log4j-core" % log4j2Version % "test",
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version % "test"
    )
}

libraryDependencies += "com.almworks.sqlite4java" % "sqlite4java" % "1.0.392" % "test"

retrieveManaged := true

fork in Test := true

val libManaged = "lib_managed"
val libManagedSqlite = s"${libManaged}_sqlite4java"

javaOptions in Test ++= Seq(s"-Djava.library.path=./$libManagedSqlite", "-Daws.dynamodb.endpoint=http://localhost:8000")

/**
  * Put all sqlite4java dependencies in [[libManagedSqlite]] for easy reference when configuring java.library.path.
  */
Test / resourceGenerators += Def.task {
    import java.nio.file.{Files, Path}
    import java.util.function.Predicate
    import java.util.stream.Collectors
    import scala.collection.JavaConverters._

    def log(msg: Any): Unit = println(s"[℣₳ℒ𐎅] $msg") //stand out in the crowd

    val theOnesWeLookFor = Set(
        "libsqlite4java-linux-amd64-1.0.392.so",
        "libsqlite4java-linux-i386-1.0.392.so ",
        "libsqlite4java-osx-1.0.392.dylib     ",
        "sqlite4java-1.0.392.jar              ",
        "sqlite4java-win32-x64-1.0.392.dll    ",
        "sqlite4java-win32-x86-1.0.392.dll    "
    ).map(_.trim)

    val isOneOfTheOnes = new Predicate[Path] {
        override def test(p: Path) = theOnesWeLookFor exists (p endsWith _)
    }

    val theOnesWeCouldFind: Set[Path] = Files
        .walk(new File(libManaged).toPath)
        .filter(isOneOfTheOnes)
        .collect(Collectors.toSet[Path])
        .asScala.toSet

    theOnesWeCouldFind foreach { path =>
        log(s"found: ${path.toFile.getName}")
    }

    assert(theOnesWeCouldFind.size == theOnesWeLookFor.size)

    val libManagedSqliteDir = new File(s"$libManagedSqlite")
    sbt.IO delete libManagedSqliteDir
    sbt.IO createDirectory libManagedSqliteDir
    log(libManagedSqliteDir.getAbsolutePath)

    theOnesWeCouldFind
        .map { path =>
            val source: File = path.toFile
            val target: File = libManagedSqliteDir / source.getName
            log(s"copying from $source to $target")
            sbt.IO.copyFile(source, target)
            target
        }
        .toSeq
}.taskValue

/**
 * Maven specific settings for publishing to Maven central.
 */
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
ThisBuild / scmInfo := Some(
    ScmInfo(
        url("https://github.com/Rice456/spark-dynamodb"),
        "scm:git@github.com:Rice456/spark-dynamodb.git"
    )
)
ThisBuild / developers := List(
    Developer(
        id = "rice456",
        name = "Joe liu",
        email = "joe.liu@gummicube.com",
        url = url("https://github.com/Rice456/spark-dynamodb")
    )
)

ThisBuild / description := "spark-dynamodb"
ThisBuild / licenses := List(
    "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / homepage := Some(url("https://github.com/Rice456/spark-dynamodb"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
ThisBuild / publishArtifact in Test := false

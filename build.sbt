lazy val buildSettings = Seq(
  organization := "com.milessabin",
  scalaVersion := "2.11.8",
  crossVersion := CrossVersion.full
)

lazy val commonSettings = Seq(
  scalacOptions := Seq(
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    "bintray/non" at "http://dl.bintray.com/non/maven"
  ),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-library" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  )
)

lazy val usePluginSettings = Seq(
  scalacOptions in Compile <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
    System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

lazy val coreSettings = buildSettings ++ commonSettings

lazy val root = project.in(file("."))
  .aggregate(plugin, library)
  .dependsOn(plugin, library)
  .settings(coreSettings:_*)
  .settings(noPublishSettings)

lazy val library = project.in(file("library"))
  .settings(moduleName := "si2712fix-library")
  .settings(coreSettings)

lazy val plugin = project.in(file("plugin"))
  .settings(moduleName := "si2712fix-plugin")
  .settings(coreSettings)
  .settings(resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "si2712fix" / "embedded"))
  .settings(compileOrder := CompileOrder.ScalaThenJava)
  .settings(manipulateBytecode in Compile := {
    val previous = (manipulateBytecode in Compile).value
    fixSuperCtorCall(
      classes = (classDirectory in Compile).value,
      classpath =
        (managedClasspath in Compile).value.files ++
          (unmanagedResourceDirectories in Compile).value :+
          (classDirectory in Compile).value
    )
    previous
  })

lazy val tests = project.in(file("tests"))
  .dependsOn(library)
  .settings(moduleName := "tests")
  .settings(coreSettings)
  .settings(usePluginSettings)
  .settings(
    fullClasspath in Test := {
      val testcp = (fullClasspath in Test).value.files.map(_.getAbsolutePath).mkString(java.io.File.pathSeparatorChar.toString)
      sys.props("sbt.paths.tests.classpath") = testcp
      (fullClasspath in Test).value
    }
  )

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  homepage := Some(url("https://github.com/milessabin/si2712fix-plugin")),
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  scmInfo := Some(ScmInfo(url("https://github.com/milessabin/si2712fix-plugin"), "scm:git:git@github.com:milessabin/si2712fix-plugin.git")),
  pomExtra := (
    <developers>
      <developer>
        <id>milessabin</id>
        <name>Miles Sabin</name>
        <url>http://milessabin.com/blog</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

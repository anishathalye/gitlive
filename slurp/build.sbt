name := "slurp"

scalaVersion := "2.11.2"

scalacOptions := Seq(
  "-unchecked", "-deprecation", "-feature", "-Xfatal-warnings"
)

unmanagedClasspath in Runtime += baseDirectory.value

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  "com.typesafe" % "config" % "1.2.1",
  "org.parboiled" %% "parboiled-scala" % "1.1.6",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "com.rabbitmq" % "amqp-client" % "3.3.5",
  "com.twitter" %% "util-collection" % "6.20.0"
)

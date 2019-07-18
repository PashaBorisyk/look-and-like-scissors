import com.typesafe.sbt.packager.docker.{DockerChmodType, DockerPermissionStrategy, ExecCmd}

name := "look-and-like-scissors"

version := "0.1"
enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "com.example"


libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.6.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.1"

libraryDependencies += "com.microsoft.azure" % "azure-storage-blob" % "11.0.1"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.9"
libraryDependencies += "io.reactivex.rxjava2" % "rxjava" % "2.2.10"
libraryDependencies += "com.microsoft.azure" % "azure-client-runtime" % "1.6.9"
libraryDependencies += "com.microsoft.azure" % "azure-client-authentication" % "1.6.9"
libraryDependencies += "com.microsoft.rest.v2" % "client-runtime" % "2.0.0"

dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.MultiStage

dockerCommands ++= Seq(
   // setting the run script executable
   ExecCmd("RUN", "touch" , "opencv"),
   ExecCmd("RUN", "wget", "https://github.com/Itseez/opencv/archive/3.0.0.zip")
)

// -*- scala -*-

organization := "com.digium"

name := "con13-server"

version := "0.0.1"

scalaVersion := "2.10.2"

seq(webSettings :_*)

seq(coffeeSettings: _*)

seq(lessSettings:_*)

scalacOptions ++= Seq("-deprecation", "-unchecked")

(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (resourceManaged in Compile)(_ / "toserve" / "js")

(resourceManaged in (Compile, LessKeys.less)) <<= (resourceManaged in Compile)(_ / "toserve" / "css")

libraryDependencies ++= {
  val liftVersion = "2.5.1"
  Seq(
    "net.liftweb"       %% "lift-webkit"        % liftVersion
  )
}

libraryDependencies ++= Seq(
  "ch.qos.logback"    % "logback-classic" % "1.0.13"
)

libraryDependencies ++= Seq(
  "org.eclipse.jetty" %  "jetty-webapp" % "8.1.13.v20130916" % "container; test",
  "com.h2database"    %  "h2"           % "1.3.173"          % "runtime",
  "org.scalatest"     %% "scalatest"    % "1.9.2"            % "test"
)

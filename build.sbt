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
  "ch.qos.logback"    % "logback-classic" % "1.0.13",
  "org.apache.httpcomponents" % "httpclient" % "4.3"
)

libraryDependencies ++= Seq(
  "com.h2database"    %  "h2"           % "1.3.173"          % "runtime",
  "org.scalatest"     %% "scalatest"    % "1.9.2"            % "test"
)

libraryDependencies ++= {
  val jettyVersion = "9.0.5.v20130815"
  Seq(
    "org.eclipse.jetty"           %  "jetty-webapp"     % jettyVersion % "container; test",
    "org.eclipse.jetty.websocket" %  "websocket-client" % jettyVersion
  )
}

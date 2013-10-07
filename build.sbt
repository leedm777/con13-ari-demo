// -*- scala -*-

organization := "com.digium.con13"

name := "ari-demo"

version := "0.0.1"

scalaVersion := "2.10.3"

seq(webSettings :_*)

seq(coffeeSettings: _*)

seq(lessSettings:_*)

scalacOptions ++= Seq("-deprecation", "-unchecked")

(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (resourceManaged in Compile)(_ / "toserve" / "js")

(resourceManaged in (Compile, LessKeys.less)) <<= (resourceManaged in Compile)(_ / "toserve" / "css")

libraryDependencies ++= {
  val liftVersion = "2.5.1"
  Seq(
    "net.liftweb"   %% "lift-webkit"    % liftVersion,
    "net.liftweb"   %% "lift-json"      % liftVersion
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
  val jettyVersion = "9.0.6.v20130930"
  Seq(
    "org.eclipse.jetty"           %  "jetty-webapp"     % jettyVersion % "container; test",
    "org.eclipse.jetty"           %  "jetty-client"     % jettyVersion,
    "org.eclipse.jetty.websocket" %  "websocket-client" % jettyVersion
  )
}

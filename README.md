This project contains an application I threw together over a couple of
days to demonstrate what one could do using [ARI][] to build Asterisk
applications.

The program has almost no comments, no tests and very little runtime.
Take it for what it is. It's written with [Scala][] and [Lift][]
(which has awesome Comet support, by the way). As with all things
Scala, it's built using [sbt][]. The settings for how to connect to
Asterisk are in `./src/main/resources/props/default.props`.

To get started, use the sample config files in `./asterisk-config/` to
configure Asterisk (along with configuring phones, like using SIP or
something). Start Asterisk, which should start ARI on its webserver
using port 8088.

To start `ari-demo`:

    $ ./sbt
    <snip/>
    sbt> container:start
    [info] jetty-9.0.6.v20130930
    <snip/>
    [success] Total time: 1 s, completed Oct 6, 2013 10:48:09 PM

Now open http://localhost:8080/ to see the `ari-demo` app. From your
phone(s), dial `7000`. These channels should show up automagically on
the channel list. You can play media to the channel. You can create
bridges. You can add channels to a bridge using drag-and-drop.

As events happen on the WebSocket, they show up on the Logs at the
bottom of the page. And as you perform actions, the command and
response also show up in the logs. Click the `+` to expand the full
response.

 [ari]: https://wiki.asterisk.org/wiki/x/lYBbAQ
 [scala]: http://scala-lang.org/
 [lift]: http://liftweb.net/
 [sbt]: http://www.scala-sbt.org/

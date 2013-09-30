package com.digium.con13.comet

import net.liftweb.http.{SHtml, RenderOut, CometActor}
import net.liftweb.http.js.JsCmds
import com.digium.con13.model.Asterisk
import net.liftweb.common.Loggable

class AsteriskController extends CometActor with Loggable {
  def renderChannels =
    ".channel *" #> "Put a channel here"

  def renderBridges =
    ".bridge *" #> "Put a bridge here"


  def renderReconnectButton ={
    def reconnect() = {
      logger.info("Reconnecting")
      Asterisk.connect()
      JsCmds.Noop
    }

    "* [onclick]" #> SHtml.ajaxButton("", () => reconnect()).attribute("onclick")
  }

  def render = "#channels *" #> renderChannels &
    "#bridges *" #> renderBridges &
    ".reconnect" #> renderReconnectButton
}

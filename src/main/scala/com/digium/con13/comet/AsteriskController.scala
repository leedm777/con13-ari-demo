package com.digium.con13.comet

import net.liftweb.http.{CometListener, SHtml, CometActor}
import net.liftweb.http.js.JsCmds
import com.digium.con13.model.{Update, AsteriskStateServer, Asterisk}
import net.liftweb.common.Loggable

class AsteriskController extends CometActor with Loggable with CometListener {
  private var channels = Set.empty[String]

  protected def registerWith = AsteriskStateServer

  def renderChannel(id: String) = {
    def answer() = {
      logger.info(s"Answer($id)")
      JsCmds.Noop
    }
    ".name" #> id & ".answer" #> SHtml.ajaxButton("Answer", () => answer())
  }

  def renderChannels =
    ".channel *" #> channels.map(renderChannel)

  def renderBridges =
    ".bridge *" #> "Put a bridge here"


  def renderReconnectButton = {
    def reconnect() = {
      logger.info("Reconnecting")
      Asterisk.connect()
      JsCmds.Noop
    }

    "* [onclick]" #> SHtml.ajaxButton("", () => reconnect())
      .attribute("onclick")
  }

  override def lowPriority = {
    case Update(c) =>
      channels = c
      reRender()
  }

  def render = "#channels *" #> renderChannels &
    "#bridges *" #> renderBridges &
    ".reconnect" #> renderReconnectButton
}

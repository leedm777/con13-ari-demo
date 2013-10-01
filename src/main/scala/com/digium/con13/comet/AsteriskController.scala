package com.digium.con13.comet

import net.liftweb.http.{CometListener, SHtml, CometActor}
import net.liftweb.http.js.JsCmds
import com.digium.con13.model.{Update, AsteriskStateServer, Asterisk}
import net.liftweb.common.Loggable

class AsteriskController extends CometActor with Loggable with CometListener {
  private[this] var channels = Set.empty[String]
  private[this] var bridges = Set.empty[String]

  protected def registerWith = AsteriskStateServer

  def renderChannel(id: String) = {
    def answer() = {
      logger.info(s"Answer($id)")
      Asterisk.post(s"/channels/$id/answer")
      JsCmds.Noop
    }

    def hangup() = {
      logger.info(s"Hangup($id)")
      Asterisk.delete(s"/channels/$id")
      JsCmds.Noop
    }

    ".name *" #> id &
      ".answer *" #> SHtml.ajaxButton("Answer", () => answer()) &
      ".hangup *" #> SHtml.ajaxButton("Hangup", () => hangup())
  }

  def renderChannels =
    ".channel *" #> channels.map(renderChannel)

  def renderBridge(id: String) = {
    def delete() = {
      logger.info(s"Delete($id)")
      Asterisk.delete(s"/bridges/$id")
      JsCmds.Noop
    }

    ".name *" #> id &
      ".delete *" #> SHtml.ajaxButton("Delete", () => delete())
  }

  def renderBridges =
    ".bridge *" #> bridges.map(renderBridge)

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
    case Update(c, b) =>
      channels = c
      bridges = b
      reRender()
  }

  def render = "#channels *" #> renderChannels &
    "#bridges *" #> renderBridges &
    ".reconnect" #> renderReconnectButton
}

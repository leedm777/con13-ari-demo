package com.digium.con13.comet

import com.digium.con13.model._
import com.digium.con13.util.Tapper._
import net.liftweb.common._
import net.liftweb.http.js.JsCmds
import net.liftweb.http.{CometListener, SHtml, CometActor}
import com.digium.con13.util.JsonFormat

class AsteriskController extends CometActor with Loggable with CometListener with JsonFormat {
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
      Asterisk.delete(s"/bridges/$id").tap { inv =>
        if (inv.isSuccess) {
          AsteriskStateServer ! RemoveBridge(id)
        }
      }
      JsCmds.Noop
    }

    ".name *" #> id &
      ".delete *" #> SHtml.ajaxButton("Delete", () => delete())
  }

  def renderCreate = {
    var bridgeType = "mixing"
    def create() = {
      logger.info(s"Create")
      Asterisk.post("/bridges", "type" -> bridgeType).tap { inv =>
        if (inv.isSuccess) {
          AsteriskStateServer ! NewBridge((inv.body \ "id").extract[String])
        }
      }
      JsCmds.Noop
    }
    ".create" #> SHtml.ajaxButton("Create", () => create()) &
      ".bridge-type" #> SHtml.ajaxSelectElem("holding" :: "mixing" :: Nil, Full(bridgeType))(bridgeType = _)
  }

  def renderBridges =
    ".bridge *" #> bridges.map(renderBridge) & renderCreate

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

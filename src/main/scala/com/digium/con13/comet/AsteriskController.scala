package com.digium.con13.comet

import com.digium.con13.model._
import net.liftweb.common._
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.{CometListener, SHtml, CometActor}
import com.digium.con13.util.JsonFormat

class AsteriskController extends CometActor with Loggable with CometListener with JsonFormat {
  private[this] var channels = Iterable.empty[Channel]
  private[this] var bridges = Iterable.empty[Bridge]
  private[this] var sounds = Seq.empty[Sound]

  protected def registerWith = AsteriskStateServer

  implicit val soundPromote = SHtml.PairStringPromoter.funcPromote { s: Sound =>
    s.id
  }

  def renderChannel(chan: Channel) = {
    def invoke(fn: Channel => Unit): () => JsCmd = () => {
      fn(chan)
      JsCmds.Noop
    }

    val ansState = if (chan.canAnswer) "enabled" else "disabled"
    var sound: Box[Sound] = Empty

    def selectSound(s: Sound) = {
      logger.info(s"Selecting ${s.id}")
      sound = Full(s)
      JsCmds.Noop
    }

    def play() = {
      sound match {
        case Full(s) =>
          logger.info(s"Playing ${s.id}")
          chan.play(s)
          JsCmds.Noop
        case _ =>
          JsCmds.Alert("No sound selected")
      }
    }

    ".name [ondragstart]" #> s"con13.channelDragStart(event, '${chan.id}')" &
    ".name *+" #> chan.id &
      ".state *" #> chan.state &
      ".answer" #> SHtml.ajaxButton("Answer", invoke(_.answer()), ansState -> "true") &
      ".hangup" #> SHtml.ajaxButton("Hangup", invoke(_.hangup())) &
      ".sound" #> SHtml.ajaxSelectElem(sounds, sound)(selectSound) &
      ".play" #> SHtml.ajaxButton("Play", play _)
      
  }

  def renderChannels =
    ".channel *" #> channels.map(renderChannel)

  def renderBridge(bridge: Bridge) = {
    def delete() = {
      bridge.delete()
      JsCmds.Noop
    }

    var sound: Box[Sound] = Empty

    def selectSound(s: Sound) = {
      logger.info(s"Selecting ${s.id}")
      sound = Full(s)
      JsCmds.Noop
    }

    def play() = {
      sound match {
        case Full(s) =>
          logger.info(s"Playing ${s.id}")
          bridge.play(s)
          JsCmds.Noop
        case _ =>
          JsCmds.Alert("No sound selected")
      }
    }

    ".bridge [ondragover]" #> s"con13.bridgeDragOver(event, '${bridge.id}')" &
      ".bridge [ondrop]" #> s"con13.bridgeDrop(event, '${bridge.id}')" &
      ".bridge [id]" #> s"bridge-${bridge.id}" &
      ".name *" #> bridge.id &
      ".type *" #> bridge.bridgeType &
      ".delete" #> SHtml.ajaxButton("Delete", () => delete()) &
      ".sound" #> SHtml.ajaxSelectElem(sounds, sound)(selectSound) &
      ".play" #> SHtml.ajaxButton("Play", play _)
  }

  def renderCreate = {
    var bridgeType = "holding"
    def create() = {
      Bridge.create(bridgeType)
      JsCmds.Noop
    }
    ".create" #> SHtml.ajaxButton("Create", () => create()) &
      ".bridge-type" #> SHtml.ajaxSelectElem("holding" :: "mixing" :: Nil, Full(bridgeType))(bridgeType = _)
  }

  def renderBridges =
      ".bridge" #> bridges.map(renderBridge) & renderCreate

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
    case Update(c, b, s) =>
      channels = c
      bridges = b
      sounds = s
      reRender()
  }

  def render = "#channels *" #> renderChannels &
    "#bridges *" #> renderBridges &
    ".reconnect" #> renderReconnectButton
}

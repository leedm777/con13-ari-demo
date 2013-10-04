package com.digium.con13.model

import com.digium.con13.util.JsonFormat
import com.digium.con13.util.Tapper._
import java.net.URI
import net.liftweb.common.Loggable
import net.liftweb.json
import net.liftweb.util.Props
import org.apache.http.client.utils.URIBuilder
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.websocket.api.annotations._
import org.eclipse.jetty.websocket.api.{StatusCode, CloseStatus, Session}
import org.eclipse.jetty.websocket.client.WebSocketClient

case class Channel(id: String, state: String) extends Loggable {
  def canAnswer = state == "Ring"

  def answer() = {
    logger.info(s"Answer($id)")
    Asterisk.post(s"/channels/$id/answer")
  }

  def hangup() = {
    logger.info(s"Hangup($id)")
    Asterisk.delete(s"/channels/$id")
  }

  def play(sound: Sound) = {
    logger.info(s"Play($id, ${sound.id}")
    Asterisk.post(s"/channels/$id/play", "media" -> s"sound:${sound.id}").tap { invocation =>
      if (invocation.isSuccess) {
        AsteriskStateServer ! Playback.fromJson(invocation.body)
      }
    }
  }
}

case class Bridge(id: String, bridgeType: String) extends Loggable {
  def delete() = {
    logger.info(s"Delete($id)")
    Asterisk.delete(s"/bridges/$id")
    AsteriskStateServer ! RemoveBridge(id)
  }

  def play(sound: Sound) = {
    logger.info(s"Play($id, ${sound.id}")
    Asterisk.post(s"/bridges/$id/play", "media" -> s"sound:${sound.id}").tap { invocation =>
      if (invocation.isSuccess) {
        AsteriskStateServer ! Playback.fromJson(invocation.body)
      }
    }
  }
}

object Bridge extends JsonFormat with Loggable {
  def fromJson(parsed: json.JValue): Bridge = {
    Bridge((parsed \ "id").extract[String],
      (parsed \ "bridge_type").extract[String])
  }

  def create(bridgeType: String) = {
    logger.info(s"Create")
    Asterisk.post("/bridges", "type" -> bridgeType).tap { invocation =>
      if (invocation.isSuccess) {
        AsteriskStateServer ! fromJson(invocation.body)
      }
    }
  }

  def addChannel(bridgeId: String, channelId: String) = {
    logger.info(s"Adding $channelId to $bridgeId")
    Asterisk.post(s"/bridges/$bridgeId/addChannel", "channel" -> channelId)
  }
}

case class Sound(id: String, langs: Seq[String]) extends Ordered[Sound] {
  def compare(that: Sound): Int = id.compare(that.id)
}

object Sound extends JsonFormat {
  def fromJson(parsed: json.JValue): Sound = {
    val langs = (parsed \\ "").children.map(_.extract[String])
    Sound((parsed \ "id").extract[String], langs)
  }
}

case class Playback(id: String) extends Loggable {
  def control(op: String) = {
    logger.info(s"$id: $op")
    Asterisk.post(s"/playback/$id/control", "operation" -> op)
  }

  def stop() = {
    logger.info(s"Stopping $id")
    Asterisk.delete(s"/playback/$id")
  }
}

object Playback extends JsonFormat {
  def fromJson(parsed: json.JValue): Playback = {
    Playback((parsed \ "id").extract[String])
  }
}

object Asterisk extends Loggable with JsonFormat {
  private var session: Option[Session] = None
  val baseUrl = new URI(Props.get("asterisk.url", "http://localhost:8088/"))
  val username = Props.get("asterisk.ari.user", "ari")
  val password = Props.get("asterisk.ari.password", "ari")
  val app = Props.get("asterisk.ari.app", "con13")
  val wsClient = new WebSocketClient().tap { c =>
    c.getPolicy.setIdleTimeout(-1)
    c.start()
  }
  val client = new HttpClient().tap { c =>
    c.start()
  }

  def request(method: HttpMethod, path: String, params: (String, String)*) = {
    val uri = baseUrl.resolve(s"/ari$path")
    val req = (client.newRequest(uri).method(method) /: params) {
      case (acc, (k, v)) => acc.param(k, v)
    }
    req.param("api_key", s"$username:$password")
    val resp = req.send()

    AriInvocation(method, req.getURI, resp.getStatus, resp.getReason,
      json.parse(resp.getContentAsString)).tap {
      AsteriskLog ! _
    }
  }

  def get(path: String, params: (String, String)*) =
    request(HttpMethod.GET, path, params: _*)

  def post(path: String, params: (String, String)*) =
    request(HttpMethod.POST, path, params: _*)

  def delete(path: String, params: (String, String)*) =
    request(HttpMethod.DELETE, path, params: _*)

  def connect() {
    disconnect()

    val destUri = {
      val builder = new URIBuilder(baseUrl)
      builder.setScheme(baseUrl.getScheme.replaceFirst("^http", "ws"))
      builder.setPath("/ari/events")
      builder.addParameter("api_key", s"$username:$password")
      builder.addParameter("app", s"$app")
      builder.build()
    }

    logger.info(s"Connecting to $destUri")
    wsClient.connect(AriSocket, destUri)
  }

  def disconnect() {
    session match {
      case Some(s) =>
        logger.info("Disconnecting")
        s.close(new CloseStatus(StatusCode.NORMAL, "Reconnecting"))
      case _ =>
    }
  }

  def shutdown() {
    wsClient.stop()
  }

  private def onEvent(event: AriEvent) {
    def updateChannel(msg: json.JValue) {
      val id: String = (msg \ "id").extract[String]
      val state: String = (msg \ "state").extract[String]
      logger.info(s"Channel state change $id")
      AsteriskStateServer ! Channel(id, state)
    }
    event.eventType match {
      case "StasisStart" =>
        updateChannel(event.msg \ "channel")
      case "StasisEnd" =>
        val id: String = (event.msg \ "channel" \ "id").extract[String]
        logger.info(s"Channel left $id")
        AsteriskStateServer ! RemoveChannel(id)
      case "ChannelStateChange" =>
        updateChannel(event.msg \ "channel")
      case "PlaybackStateChange" if (event.msg \ "playback" \ "state").extract[String] == "done" =>
        val id: String = (event.msg \ "playback" \ "id").extract[String]
        logger.info(s"Playback done $id")
        AsteriskStateServer ! RemovePlayback(id)
      case _ => // don't care
    }
  }

  @WebSocket(maxMessageSize = 64 * 1024)
  object AriSocket {
    @OnWebSocketConnect
    def onConnect(s: Session) {
      logger.info("WebSocket connected")
      AsteriskLog ! AriMessage("WebSocket connected")
      session = Some(s)

      val bridges = get("/bridges").body.children.map(Bridge.fromJson)
      AsteriskStateServer ! BridgeList(bridges)

      val sounds = get("/sounds").body.children.map(Sound.fromJson)
      AsteriskStateServer ! SoundList(sounds)

      // Clear out the channel and playback lists
      AsteriskStateServer ! ChannelList(Nil)
      AsteriskStateServer ! PlaybackList(Nil)
    }

    @OnWebSocketMessage
    def onMessage(msg: String) {
      logger.debug(s"Message received: $msg")
      val event: AriEvent = AriEvent(json.parse(msg))
      AsteriskLog ! event
      onEvent(event)
    }

    @OnWebSocketClose
    def onClose(statusCode: Int, reason: String) {
      logger.info(s"WebSocket closed: $statusCode - $reason")
      AsteriskLog ! AriMessage(s"WebSocket closed ($reason)")
      session = None
    }

    @OnWebSocketError
    def onError(err: Throwable) {
      logger.error("Cannot connect to WebSocket", err)
      AsteriskLog ! AriMessage(s"WebSocket error: ${err.getMessage}")
    }
  }

}

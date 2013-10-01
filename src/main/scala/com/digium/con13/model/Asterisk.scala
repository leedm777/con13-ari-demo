package com.digium.con13.model

import java.net.URI
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.eclipse.jetty.websocket.api.annotations._
import net.liftweb.common.Loggable
import net.liftweb.util.Props
import org.eclipse.jetty.websocket.api.{StatusCode, CloseStatus, Session}
import org.apache.http.client.utils.URIBuilder
import net.liftweb.json
import com.digium.con13.util.Tapper._
import com.digium.con13.util.JsonFormat
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.http.HttpMethod

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
    val req = (client.newRequest(uri).method(method) /: params) { (acc, param) =>
      val (k, v) = param
      acc.param(k, v)
    }
    req.param("api_key", s"$username:$password")
    val resp = req.send()

    json.parse(resp.getContentAsString).tap { body =>
      AsteriskLog ! AriRequest(method, uri, resp.getStatus, resp.getReason, body)
    }
  }

  def get(path: String, params: (String, String)*)  =
    request(HttpMethod.GET, path, params: _*)

  def post(path: String, params: (String, String)*)  =
    request(HttpMethod.POST, path, params: _*)

  def delete (path: String, params: (String, String)*)  =
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
    event.eventType match {
      case "StasisStart" =>
        val id: String = (event.msg \ "channel" \ "id").extract[String]
        logger.info(s"Channel entered $id")
        AsteriskStateServer ! NewChannel(id)
      case "StasisEnd" =>
        val id: String = (event.msg \ "channel" \ "id").extract[String]
        logger.info(s"Channel left $id")
        AsteriskStateServer ! RemoveChannel(id)
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

      val bridgeFields = get("/bridges").filter {
        case json.JField("id", _) => true
        case _ => false
      }
      val bridges = bridgeFields.map(_.extract[String])
      AsteriskStateServer ! BridgeList(bridges)
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

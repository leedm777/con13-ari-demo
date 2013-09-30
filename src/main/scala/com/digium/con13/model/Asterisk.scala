package com.digium.con13.model

import java.net.URI
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.eclipse.jetty.websocket.api.annotations._
import net.liftweb.common.Loggable
import net.liftweb.util.Props
import org.eclipse.jetty.websocket.api.Session
import org.apache.http.client.utils.URIBuilder

object Asterisk extends Loggable {
  val baseUrl = new URI(Props.get("asterisk.url", "http://localhost:8088/"))
  val username = Props.get("asterisk.ari.user", "ari")
  val password = Props.get("asterisk.ari.password", "ari")
  val client = new WebSocketClient()

  def connect() {
    val destUri = {
      val builder = new URIBuilder(baseUrl)
      builder.setScheme(baseUrl.getScheme.replaceFirst("^http", "ws"))
      builder.setPath("/ari/events")
      builder.addParameter("api_key", s"$username:$password")
      builder.addParameter("app", "con13")
      builder.build()
    }

    val socket = new AriSocket()
    client.start()
    logger.info(s"Connecting to $destUri")
    client.connect(socket, destUri)
  }

  def shutdown() {
    client.stop()
  }

  @WebSocket(maxMessageSize = 64 * 1024)
  class AriSocket {
    @OnWebSocketConnect
    def onConnect(session: Session) {
      logger.info("WebSocket connected")
    }
    @OnWebSocketMessage
    def onMessage(msg: String) {
      logger.info(s"Message received: $msg")
    }

    @OnWebSocketClose
    def onClose(statusCode: Int, reason: String) {
      logger.info(s"WebSocket closed: $statusCode - $reason")
    }

    @OnWebSocketError
    def onError(err: Throwable) {
      logger.error("Cannot connect to WebSocket", err)
    }
  }
}

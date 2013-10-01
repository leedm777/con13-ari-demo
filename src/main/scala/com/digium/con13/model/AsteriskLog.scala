package com.digium.con13.model

import net.liftweb.actor.LiftActor
import net.liftweb.http.ListenerManager
import net.liftweb.json
import net.liftweb.common.Loggable
import com.digium.con13.util.JsonFormat

sealed abstract class LogItem

sealed case class AriMessage(msg: String) extends LogItem

sealed case class AriEvent(msg: json.JValue) extends LogItem with JsonFormat {
  val eventType = (msg \\ "type").extract[String]

  def eventHeader = {
    val ids = json.compact(json.render(msg \\ "id"))
    s"$eventType $ids"
  }


  override def toString: String = json.compact(json.render(msg))
}

sealed case class AriResponse(method: String, url: String, code: Int, codeText: String, body: json.JValue) extends LogItem {

  override def toString: String = s"$method $url - $codeText"
}

case class Logs(items: List[LogItem])

object AsteriskLog extends LiftActor with ListenerManager with Loggable {
  private var logs = Logs(Nil)

  protected def createUpdate = logs

  override protected def lowPriority = {
    case item: LogItem =>
      logger.debug(s"msg: $item")
      logs = Logs(item :: logs.items)
      updateListeners()
  }
}

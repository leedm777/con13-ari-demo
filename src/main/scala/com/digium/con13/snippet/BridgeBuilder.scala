package com.digium.con13.snippet

import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import net.liftweb.http.js.{JsCmds, JsCmd}
import com.digium.con13.model.Asterisk
import net.liftweb.common.Loggable

object BridgeBuilder extends Loggable {
  def render = {
    var channel = ""
    var bridge = ""

    def process(): JsCmd = {
      logger.info(s"Adding $channel to $bridge")
      Asterisk.post(s"/bridges/$bridge/addChannel", "channel" -> channel)
      JsCmds.Noop
    }

    ".channel" #> SHtml.text(channel, channel = _) &
      ".bridge" #> SHtml.text(bridge, bridge = _) &
      ".build" #> SHtml.hidden(process)
  }
}

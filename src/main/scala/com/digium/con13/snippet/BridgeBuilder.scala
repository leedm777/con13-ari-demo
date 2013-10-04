package com.digium.con13.snippet

import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._
import net.liftweb.http.js.{JsCmds, JsCmd}
import com.digium.con13.model._
import net.liftweb.common.Loggable

object BridgeBuilder extends Loggable {
  def render = {
    var channelId = ""
    var bridgeId = ""

    def process(): JsCmd = {
      Bridge.addChannel(bridgeId, channelId)
      JsCmds.Noop
    }

    ".channel" #> SHtml.text(channelId, channelId = _) &
      ".bridge" #> SHtml.text(bridgeId, bridgeId = _) &
      ".build" #> SHtml.hidden(process)
  }
}

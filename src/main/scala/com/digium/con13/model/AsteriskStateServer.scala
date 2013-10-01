package com.digium.con13.model

import net.liftweb.actor.LiftActor
import net.liftweb.common.Loggable
import net.liftweb.http.ListenerManager

case class RemoveChannel(id: String)

case class BridgeList(id: Seq[String])

case class NewBridge(id: String)

case class RemoveBridge(id: String)

case class Update(channels: Iterable[Channel], bridges: Set[String])

object AsteriskStateServer extends LiftActor with Loggable with ListenerManager {
  protected def createUpdate = Update(channels.values, bridges)

  private var channels = Map.empty[String, Channel]

  private var bridges = Set.empty[String]

  override protected def lowPriority = {
    case chan: Channel => channels += (chan.id -> chan); updateListeners()
    case RemoveChannel(id) => channels -= id; updateListeners()
    case BridgeList(bridgeIds) => bridges = bridgeIds.toSet; updateListeners()
    case NewBridge(id) => bridges += id; updateListeners()
    case RemoveBridge(id) => bridges -= id; updateListeners()
  }
}

package com.digium.con13.model

import net.liftweb.actor.LiftActor
import net.liftweb.common.Loggable
import net.liftweb.http.ListenerManager

case class NewChannel(id: String)

case class RemoveChannel(id: String)

case class BridgeList(id: Seq[String])

case class NewBridge(id: String)

case class RemoveBridge(id: String)

case class Update(channels: Set[String], bridges: Set[String])

object AsteriskStateServer extends LiftActor with Loggable with ListenerManager {
  protected def createUpdate = Update(channels, bridges)

  private var channels = Set.empty[String]

  private var bridges = Set.empty[String]

  override protected def lowPriority = {
    case NewChannel(id) => channels += id; updateListeners()
    case RemoveChannel(id) => channels -= id; updateListeners()
    case BridgeList(bridgeIds) => bridges = bridgeIds.toSet; updateListeners()
    case NewBridge(id) => bridges += id; updateListeners()
    case RemoveBridge(id) => bridges -= id; updateListeners()
  }
}

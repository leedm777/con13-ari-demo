package com.digium.con13.model

import net.liftweb.actor.LiftActor
import net.liftweb.common.Loggable
import net.liftweb.http.ListenerManager

case class RemoveChannel(id: String)

case class BridgeList(id: Seq[Bridge])

case class SoundList(id: Seq[Sound])

case class RemoveBridge(id: String)

case class Update(channels: Iterable[Channel], bridges: Iterable[Bridge], sounds: Seq[Sound])

object AsteriskStateServer extends LiftActor with Loggable with ListenerManager {
  protected def createUpdate = Update(channels.values, bridges.values, sounds)

  private var channels = Map.empty[String, Channel]

  private var bridges = Map.empty[String, Bridge]

  private var sounds = Seq.empty[Sound]

  override protected def lowPriority = {
    case chan: Channel => channels += (chan.id -> chan); updateListeners()
    case RemoveChannel(id) => channels -= id; updateListeners()

    case BridgeList(bridgeList) =>
      bridges = bridgeList.map(b => b.id -> b).toMap
      updateListeners()

    case SoundList(soundList) =>
      sounds = soundList.sorted
      updateListeners()

    case bridge: Bridge =>
      bridges += (bridge.id -> bridge)
      updateListeners()

    case RemoveBridge(id) =>
      bridges -= id
      updateListeners()
  }
}

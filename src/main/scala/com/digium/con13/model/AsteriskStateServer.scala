package com.digium.con13.model

import net.liftweb.actor.LiftActor
import net.liftweb.common.Loggable
import net.liftweb.http.ListenerManager

case class RemoveChannel(id: String)

case class RemoveBridge(id: String)

case class RemovePlayback(id: String)

case class BridgeList(bridges: Seq[Bridge])

case class SoundList(sounds: Seq[Sound])

case class ChannelList(channels: Seq[Channel])

case class PlaybackList(playbacks: Seq[Playback])

case class Update(channels: Iterable[Channel], bridges: Iterable[Bridge], sounds: Seq[Sound], playback: Iterable[Playback])

object AsteriskStateServer extends LiftActor with Loggable with ListenerManager {
  protected def createUpdate = Update(channels.values, bridges.values, sounds, playbacks.values)

  private var channels = Map.empty[String, Channel]

  private var bridges = Map.empty[String, Bridge]

  private var playbacks = Map.empty[String, Playback]

  private var sounds = Seq.empty[Sound]

  override protected def lowPriority = {
    case chan: Channel => channels += (chan.id -> chan); updateListeners()

    case bridge: Bridge =>
      bridges += (bridge.id -> bridge)
      updateListeners()

    case playback: Playback =>
      playbacks += (playback.id -> playback)
      updateListeners()

    case RemoveChannel(id) =>
      channels -= id
      updateListeners()

    case RemoveBridge(id) =>
      bridges -= id
      updateListeners()

    case RemovePlayback(id) =>
      playbacks -= id
      updateListeners()

    case SoundList(soundList) =>
      sounds = soundList.sorted
      updateListeners()

    case BridgeList(bridgeList) =>
      bridges = bridgeList.map(b => b.id -> b).toMap
      updateListeners()

    case ChannelList(channelList) =>
      channels = channelList.map(c => c.id -> c).toMap
      updateListeners()

    case PlaybackList(playbackList) =>
      playbacks = playbackList.map(p => p.id -> p).toMap
      updateListeners()
  }
}

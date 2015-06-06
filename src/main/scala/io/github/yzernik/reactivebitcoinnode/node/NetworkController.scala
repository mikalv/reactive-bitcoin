package io.github.yzernik.reactivebitcoinnode.node

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import io.github.yzernik.bitcoinscodec.messages.Addr
import io.github.yzernik.bitcoinscodec.messages.GetAddr
import io.github.yzernik.bitcoinscodec.structures.Message

object NetworkController {
  def props(blockchain: ActorRef, peerManager: ActorRef, btc: ActorRef) =
    Props(classOf[NetworkController], blockchain, peerManager, btc)

  case object Initialize
}

class NetworkController(blockchain: ActorRef, peerManager: ActorRef, btc: ActorRef)
  extends Actor with ActorLogging {
  import NetworkController._
  import context.system
  import context.dispatcher

  var preferredDownloadPeers: Vector[ActorRef] = Vector.empty

  def receive = ready

  def ready: Receive = {
    case Initialize =>
      peerManager ! PeerManager.Initialize(self)
      context.system.scheduler.schedule(0 seconds, 5 seconds, peerManager, PeerManager.UpdateConnections)
      context.become(active(true))
  }

  def active(syncing: Boolean): Receive = {
    case PeerManager.NewConnection(ref) =>
      log.info(s"Got a new connection: $ref")
      handleNewConnection(ref)
    case PeerManager.ReceivedFromPeer(msg, ref) =>
      log.info(s"network controller received from $ref other: $msg")
      handlePeerMessage(ref, syncing)(msg)
    case o =>
      log.info(s"network controller received other: $o")
  }

  private def getNetworkTime: Future[Long] = {
    implicit val timeout = Timeout(5 seconds)
    (peerManager ? PeerManager.GetNetworkTime).mapTo[Long]
  }

  private def handleNewConnection(peer: ActorRef) = {
    peerManager ! PeerManager.SendToPeer(GetAddr(), peer)
    //peerManager ! PeerManager.SendToPeer(GetHeaders(), peer)
    context.become(active(true))
  }

  def handlePeerMessage(peer: ActorRef, syncing: Boolean): PartialFunction[Message, Unit] = {
    case Addr(addrs) =>
      addrs.map { case (t, a) => a.address }.foreach { addr =>
        log.info(s"Adding address: $addr")
        peerManager ! PeerManager.AddNode(addr, false)
      }
    case _ =>
  }

}
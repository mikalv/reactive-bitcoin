package com.oohish.bitcoinakkanode.listener

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import com.oohish.bitcoinakkanode.node.APIClient
import com.oohish.bitcoinakkanode.node.BlockChain
import com.oohish.bitcoinakkanode.node.Node
import com.oohish.bitcoinakkanode.wire.NetworkParameters
import com.oohish.bitcoinakkanode.wire.PeerManager
import com.oohish.bitcoinscodec.structures.Hash

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.util.Timeout

object ListenerNode {
  def props(networkParams: NetworkParameters) =
    Props(classOf[ListenerNode], networkParams)
}

class ListenerNode(np: NetworkParameters) extends Node with APIClient with Actor with ActorLogging {

  def networkParams = np

  implicit val timeout = Timeout(1 second)

  val pm = context.actorOf(PeerManager.props(self, networkParams))

  def receive: Receive =
    nodeBehavior orElse apiClientBehavior

  override def getChainHead: Future[BlockChain.StoredBlock] =
    Future.failed(new UnsupportedOperationException())
  override def getBlockByIndex(index: Int): Future[Option[BlockChain.StoredBlock]] =
    Future.failed(new UnsupportedOperationException())
  override def getBlock(hash: Hash): Future[Option[BlockChain.StoredBlock]] =
    Future.failed(new UnsupportedOperationException())

}
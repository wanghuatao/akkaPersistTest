package com.service

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, ActorSystem, DeadLetter, Props, ReceiveTimeout}
import akka.cluster.Cluster
import akka.cluster.sharding.ShardRegion.{CurrentShardRegionState, ExtractEntityId, ExtractShardId, Passivate}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence._
import com.msg._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

case class ClusterMaster(system: ActorSystem) extends Actor {
  val logger = LoggerFactory.getLogger(this.getClass)
  val cluster = Cluster(system)
  val shardActor = createShard
  val numberOfShards = 10

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[akka.actor.DeadLetter])
    context.system.scheduler.scheduleOnce(15.second) {
      shardActor ! ShardRegion.GetShardRegionState
    }(context.dispatcher)

  }

  def receive = {
    case msg: DeadLetter =>
    case CurrentShardRegionState(shards) =>

      shards.foreach { p => logger.warn(s"CurrentShardRegionState size ${p.shardId} ${p.entityIds.size} ") }
    case e =>

      shardActor forward e
  }

  private def createShard = {
    val extractEntityId: ExtractEntityId = {
      case msg@EntityMsg(entityId, _) => (entityId, msg)

    }

    val extractShardId: ExtractShardId = {
      case EntityMsg(entityId, msg) =>
        (entityId.hashCode % numberOfShards).toString

    }

    val serviceRegion: ActorRef = ClusterSharding(system).start(
      typeName = "ShardActor",
      entityProps = Props[ShardActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)

    serviceRegion
  }

}


class ShardActor extends PersistentActor with AtLeastOnceDelivery {
  val logger = LoggerFactory.getLogger(this.getClass)
  private val entityId = self.path.name
  private val timeout = 3600.seconds
  var playerActor = context.actorOf(Props(PlayerActor()), "playerActor")

  override def preStart(): Unit = {
    context.setReceiveTimeout(timeout)

  }


  override def receiveCommand = {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)

    case Stop =>
      context.stop(self)

    case msg: EntityMsg =>
      msg.entity match {

        case m: Msg =>
          playerActor forward m

      }
  }

  override def receiveRecover: Receive = {
    case _ =>

  }


  override def persistenceId: String = s"ShardActor_$entityId"
}


package com.service

import akka.actor.Actor
import akka.persistence.AtLeastOnceDelivery.UnconfirmedWarning
import akka.persistence._
import com.msg._
import org.slf4j.LoggerFactory


case class PlayerActor() extends Actor with AtLeastOnceDelivery with SystemQuery {
  val logger = LoggerFactory.getLogger(this.getClass)
  protected val entityId = self.path.name
  private var lastSnapNr = 0l
  var entityState = EntityState()

  override def preStart(): Unit = {
       val events = queries.eventsByPersistenceId("player-" + entityId, 0, Long.MaxValue)
        events.async.runForeach { evt =>
          logger.info(s"event ${evt.event}")
        }
    //
    //        events.runForeach { evt =>
    //          logger.info(s"event ${evt.event}")
    //        }
  }


  override def receiveCommand: Receive = {

    case UnconfirmedWarning(deliveries) =>
      logger.error(s"UnconfirmedWarning ${deliveries}")
      deliveries.foreach(p => confirmDelivery(p.deliveryId))


    case SaveSnapshot =>
      deleteSnapshot(lastSnapNr)
      saveSnapshot(entityState)

    case SaveSnapshotSuccess(metadata) =>
      lastSnapNr = metadata.sequenceNr
      logger.info(s"SaveSnapshotSuccess $metadata")
    case DeleteSnapshotSuccess(metadata) =>

    case msg: EntityMsg =>

    case testCmd: TestCmd =>
      persist(TestEvent(testCmd.id)) { event =>
        handMsg(event)
        //logger.info(s"player ${self.path.name} recv $testCmd")
      }
    case any =>
      logger.error(s"not impl $any")

  }


  def handMsg(event: TestEvent, isRecover: Boolean = false) = {
    updateState(entityState.updateTestMsg(TestMsg(event.id)), isRecover)
  }


  private def updateState(s: EntityState, isRecover: Boolean = false) = {
    entityState = s
    if (!isRecover) {
      increaseEvtCountAndSnapshot()
    }

  }

  private def increaseEvtCountAndSnapshot() {
    val snapShotInterval = 20
    if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) {
      self ! SaveSnapshot
    }
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(metadata, snapshot: Any) =>
      snapshot match {
        case state: EntityState =>
          entityState = state
          logger.info(s"receiveRecover playerActor $entityId   $metadata from snapshot $snapshot")
      }

    case event: TestEvent =>
      handMsg(event, true)

    case RecoveryCompleted =>
    //logger.info(s"shard $entityId  RecoveryCompleted  lastSequenceNr $lastSequenceNr")
  }


  override def persistenceId: String = s"player-$entityId"
}


case class EntityState(var testMsg: TestMsg = null) {

  def updateTestMsg(testMsg: TestMsg) = {
    this.testMsg = testMsg
    copyValue()
  }

  def copyValue() = {
    copy(testMsg)
  }

}
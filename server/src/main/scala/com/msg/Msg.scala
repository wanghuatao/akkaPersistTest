package com.msg

trait Msg extends Serializable

case class EntityMsg(entityId: String, entity: Msg) extends Msg

case object SaveSnapshot

case class TestMsg(id: String) extends Msg


case class TestCmd(id: String) extends Msg

case class TestEvent(id: String)

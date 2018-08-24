package com.service

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import com.msg.{EntityMsg, TestMsg}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object ServerBoot extends App {
  val logger = LoggerFactory.getLogger(this.getClass)
  val system: ActorSystem = ActorSystem("server", ConfigFactory.load("application"))
  val cluster = Cluster(system)
  val selfAddress = cluster.selfAddress

  //test
  cluster.join(selfAddress)

  implicit val dispatcher = system.dispatcher
  implicit val scheduler = system.scheduler

  val master = system.actorOf(Props(ClusterMaster(system)), "master")

  (1 to 10000).foreach { f =>
    master ! EntityMsg(f.toString, TestMsg(f.toString))
  }


  logger.info(s"master path ${master.path}")
  ClusterClientReceptionist(system).registerService(master)

}

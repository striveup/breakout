package com.neo.sk.smallBreak

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.ActorRef
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.smallBreak.http.HttpService
import akka.actor.typed.scaladsl.adapter._
import akka.dispatch.MessageDispatcher

import scala.language.postfixOps

import com.neo.sk.smallBreak.core.{UserManager, RoomManager}

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:25 PM
  */
object Boot extends HttpService {

  import concurrent.duration._
  import com.neo.sk.smallBreak.common.AppSettings._


  override implicit val system = ActorSystem("small-break", config)
  // the executor should not be the default dispatcher.
  override implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  override implicit val materializer = ActorMaterializer()

  override implicit val scheduler = system.scheduler

  override implicit val timeout:Timeout = Timeout(20 seconds) // for actor asks

  val userManager = system.spawn(UserManager.create(), "userManager")
  val roomManager = system.spawn(RoomManager.create(), "roomManager")

  val log: LoggingAdapter = Logging(system, getClass)


  def main(args: Array[String]) {
    log.info("Starting.")
    Http().bindAndHandle(routes, httpInterface, httpPort)
    log.info(s"Listen to the $httpInterface:$httpPort")
    log.info("Done.")
  }






}

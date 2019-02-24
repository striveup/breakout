package com.neo.sk.smallBreak.http

import java.util.concurrent.atomic.AtomicInteger
import com.neo.sk.smallBreak.Boot.{executor, scheduler, timeout}
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, Materializer, Supervision}
import akka.util.Timeout
import com.neo.sk.smallBreak.core.UserManager
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import com.neo.sk.smallBreak.Boot.userManager
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.smallBreak.core.UserManager
import com.neo.sk.smallBreak.utils.ServiceUtils
import io.circe.Error
import io.circe.generic.auto._
/**
  * User: yuwei
  * Date: 4/2/2019
  * Time: 4:13 PM
  */
trait LinkService extends ServiceUtils with LoginService{


  val idGenerator = new AtomicInteger(1000000)

  private[this] val log = LoggerFactory.getLogger("LinkService")


  private val joinRoute = (path("play") & get) {
      pathEndOrSingleSlash {
        getFromResource("html/play.html")
      }
    }


  private val playRoute = (path("playGame") & get) {
    parameter('roomType.as[Int].?, 'pw.as[String].?, 'roomId.as[Long].?) {
      (roomType, pw, roomId) =>
        optionalUserSession {
          case Some(session) =>
            val flowFuture: Future[Flow[Message, Message, Any]] =
              userManager ? (UserManager.GetWebSocketFlow(session.playerId.toLong, session.playerName, _, roomType, pw, roomId))
            dealFutureResult(
              flowFuture.map(r => handleWebSocketMessages(r))
            )

          case None =>
            getFromResource("html/login.html")
        }
    }
  }


  val linkRoute = joinRoute ~ playRoute






}

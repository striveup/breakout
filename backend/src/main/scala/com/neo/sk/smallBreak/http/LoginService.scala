package com.neo.sk.smallBreak.http

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.smallBreak.Boot.{executor, scheduler, timeout}
import akka.actor.ActorSystem
import akka.actor.Status.Success
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, Materializer, Supervision}
import akka.util.Timeout
import com.neo.sk.smallBreak.core.UserManager
import org.slf4j.LoggerFactory
import com.neo.sk.smallBreak.model.SlickTables._

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import com.neo.sk.smallBreak.Boot.userManager
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import com.neo.sk.smallBreak.core.UserManager
import com.neo.sk.smallBreak.http.SessionBase.{UserSession, UserSessionKey}
import com.neo.sk.smallBreak.utils.ServiceUtils
import com.neo.sk.smallBreak.model.dao.userDao
import com.neo.sk.smallBreak.protocol.LoginProtocol
import io.circe.Error
import io.circe.generic.auto._

/**
  * User: yuwei
  * Date: 2019/2/13
  * Time: 15:59
  */
trait LoginService extends ServiceUtils with SessionBase{

  private[this] val log = LoggerFactory.getLogger("LoginService")


  private[this] val joinPageRoute = (path("joinPage") & get){
    pathEndOrSingleSlash {
      getFromResource("html/join.html")
    }
  }

  private val loginPageRoute = (path("login") & get){
    pathEndOrSingleSlash {
      getFromResource("html/login.html")
    }
  }

  private val registerRoute = (path("register") & get){
    pathEndOrSingleSlash {
      getFromResource("html/register.html")
    }
  }

  private[this] val travellerRoute = (path("travel") & get) {
      optionalUserSession {
        case Some(session) =>
          getFromResource("html/join.html")
        case None =>
          dealFutureResult(
            userDao.insertUser(rUser(-1, "guest", "", 0, "guest")).map {
              id =>
                val sessionMap = UserSession(id.toString, "guest")
                setUserSession(sessionMap)(getFromResource("html/join.html"))
            }
          )
      }
  }

  private val registerPost = (path("registerPost") & get) {
    parameter('name.as[String], 'pw.as[String]) {
      (name, pw) =>
        println(name)
        dealFutureResult(
          userDao.checkUser(name).flatMap {
            isExist =>
              if (!isExist) {
                userDao.insertUser(rUser(-1, name, pw, 0, name)).map{
                  id =>
                    val sessionMap = UserSession(id.toString, name)
                    setUserSession(sessionMap)(complete(LoginProtocol.RegisterRsp()))
                }
              }else {
                Future(complete(LoginProtocol.RegisterRsp(100001, "The user exists")))
              }
          }.recover {
            case e =>
              log.error(s"db wrong when register:$e")
              complete(LoginProtocol.RegisterRsp(100009, "server wrong"))
          }
        )
    }
  }

  private val loginPostRoute = (path("loginPost") & get){
    parameter('name.as[String], 'pw.as[String]) {
      (name, pw) =>
        optionalUserSession {
          case Some(value) =>
            complete(LoginProtocol.LoginRsp())
          case None =>
            dealFutureResult(
              userDao.checkPw(name, pw).map {
                u =>
                  if (u.isEmpty) {
                    complete(LoginProtocol.LoginRsp(100001, "name or pw wrong"))
                  } else {
                    val sessionMap = UserSession(u.get.toString, name)
                    setUserSession(sessionMap)(complete(LoginProtocol.LoginRsp()))
                  }
              }
            )
        }
    }
  }


  val loginRoute:Route = registerRoute ~ loginPageRoute ~ joinPageRoute~travellerRoute ~  registerPost ~ loginPostRoute

}

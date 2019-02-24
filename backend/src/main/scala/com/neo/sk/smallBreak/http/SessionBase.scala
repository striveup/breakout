package com.neo.sk.smallBreak.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.{Directive0, Directive1}
import akka.http.scaladsl.server.Directives.redirect
import akka.http.scaladsl.server.directives.BasicDirectives
import org.slf4j.LoggerFactory
import com.neo.sk.smallBreak.utils.{CirceSupport, SessionSupport}
import com.neo.sk.smallBreak.common.AppSettings

/**
  * User: yuwei
  * Date: 2019/2/13
  * Time: 18:58
  */
object SessionBase {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val SessionTypeKey = "STKey"

  /*  object UserSessionKey {
      val SESSION_TYPE = "userSession"
      val uid = "uid"
      val loginTime = "loginTime"
    }*/

  object UserSessionKey {
    val SESSION_TYPE = "userSession"
    val playerId = "playerId"
    val playerName = "playerName"
  }

  case class UserSession(
    playerId: String,
    playerName: String
  ) {
    def toSessionMap = Map(
      SessionTypeKey -> UserSessionKey.SESSION_TYPE,
      UserSessionKey.playerId -> playerId,
      UserSessionKey.playerName -> playerName,
    )
  }

  implicit class SessionTransformer(sessionMap: Map[String, String]) {
    def toUserSession: Option[UserSession] = {
      logger.debug(s"toUserSession: change map to session, ${sessionMap.mkString(",")}")
      try {
        if (sessionMap.get(SessionTypeKey).exists(_.equals(UserSessionKey.SESSION_TYPE))) {
          Some(UserSession(
            sessionMap(UserSessionKey.playerId),
            sessionMap(UserSessionKey.playerName)
          ))
        } else {
          logger.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          logger.warn(s"toUserSession: ${e.getMessage}")
          None
      }
    }
  }

}

trait SessionBase extends CirceSupport with SessionSupport {

  import SessionBase._
  import io.circe.generic.auto._

  override val sessionEncoder = SessionSupport.PlaySessionEncoder
  override val sessionConfig = AppSettings.sessionConfig

  protected def setUserSession(userSession: UserSession): Directive0 = setSession(userSession.toSessionMap)

  def authUser(f: UserSession => server.Route) = optionalUserSession {
    case Some(session) =>
      f(session)
    case None =>
      redirect("/",StatusCodes.SeeOther)
  }

  protected val optionalUserSession: Directive1[Option[UserSession]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toUserSession)
    case Left(error) =>
      logger.debug(error)
      BasicDirectives.provide(None)
  }

}

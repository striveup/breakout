package com.neo.sk.smallBreak.core

import java.util.concurrent.atomic.AtomicLong

import com.neo.sk.smallBreak.Boot.executor
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.neo.sk.smallBreak.model.dao.userDao
import io.circe.{Decoder, Encoder}
import com.neo.sk.smallBreak.Boot.executor
import org.slf4j.LoggerFactory
import com.neo.sk.smallBreak.Boot.{executor, scheduler, timeout, userManager}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.smallBreak.Boot.roomManager
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.smallBreak.shared.model.Point
import com.neo.sk.smallBreak.shared.msg.GameMessage
import com.neo.sk.smallBreak.shared.msg.GameMessage.Brick4front

/**
  * User: yuwei
  * Date: 2019/2/3
  * Time: 14:08
  */
object UserActor {

  trait Command

  case object UserLeft extends Command

  final case class FailureMessage(ex: Throwable) extends Command

  case class BrickOffList(list:List[Int]) extends Command

  case class JoinRoomSuccess(roomActor:ActorRef[RoomActor.Command], roomId:Long, roomType:Int) extends Command

  case class FrontActor(frontRef: ActorRef[GameMessage.WsMsgSource]) extends Command

  case class ObserveInit(frame:Long, bricks:Option[List[Brick4front]], rotate:Point, BallLocation:Point, BoardLocation:Point, ballSpeed:Int, boardSpeed:Int, boardDir:Point) extends Command

  case class TalkMsg(t:Int, msg:String) extends Command

  case class Key(frame:Long, code:Int, down:Boolean) extends Command

  case object PlayGame extends Command
  
  case class MsgWrap(msg: GameMessage.WsMsgServer) extends Command

  case object Observe extends Command

  case class UnknownMsg(msg:GameMessage.WsMsgFront) extends Command

  case class UserDead(solo:Int, two:Option[Int]) extends Command

  case object LeftSuccess extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(id:Long, name:String, roomType: Option[Int], pw:Option[String], roomId:Long): Behavior[Command] = {
    log.info(s"UserActor $id start......")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            init(id, name, pw, roomType, roomId)
        }
    }
  }

  private def init(id:Long, name:String, pw: Option[String] = None, roomType:Option[Int], roomId:Long) (
    implicit timer: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case FrontActor(front) =>
          idle(id, name, front,pw, roomType, roomId,None)

        case FailureMessage(e) =>
          log.debug(s"user $id ws connect fail:$e")
          Behaviors.stopped
      }
    }
  }

  private def idle(id:Long, name:String, frontActor: ActorRef[GameMessage.WsMsgSource],
    pw: Option[String] = None, roomType:Option[Int], roomId:Long,roomActor: Option[ActorRef[RoomActor.Command]])
    (
      implicit timer: TimerScheduler[Command]
    ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case t: JoinRoomSuccess =>
          frontActor ! GameMessage.RoomMsg(t.roomId, t.roomType)
          userManager ! UserManager.JoinGame(id, ctx.self)
          idle(id, name, frontActor, pw, roomType, roomId,Some(t.roomActor))

        case TalkMsg(t,m) =>
          roomActor.get ! RoomActor.TalkMsg(id,t, m)
          Behaviors.same

        case UnknownMsg(m) =>
          Behaviors.same

        case PlayGame =>
          userDao.getNick(id).map {
            nick=>
              frontActor ! GameMessage.UserMsg(id, nick)
              if(roomActor.isDefined){
                roomActor.get ! RoomActor.UserChangeRoom(id)
              }
              roomManager ! RoomManager.JoinRoom(id, ctx.self, roomType, pw, roomId, nick)
          }
          Behaviors.same

        case MsgWrap(m) =>
          frontActor ! m
          Behaviors.same

        case Key(frame, code, down) =>
          roomActor.get ! RoomActor.Key(frame,code ,down)
          Behaviors.same

        case t:UserDead =>
          roomActor.get ! RoomActor.UserDead(id, t.solo, t.two)
          Behaviors.same

        case Observe =>
          roomActor.get ! RoomActor.Observe(id)
          Behaviors.same

        case t:ObserveInit =>
          roomActor.get ! RoomActor.ObserveInit(t.frame, id, t.bricks, t.rotate, t.BallLocation, t.BoardLocation, t.ballSpeed, t.boardSpeed,t.boardDir)
          Behaviors.same

        case BrickOffList(list) =>
          roomActor.get ! RoomActor.BrickOffList(list, id)
          Behaviors.same

        case UserLeft =>
          userManager ! UserManager.UserLeft(id)
          roomActor.get ! RoomActor.UserLeft(id, ctx.self)
          Behaviors.same

        case LeftSuccess =>
          Behaviors.stopped


      }
    }
  }

  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = UserLeft,
    onFailureMessage = FailureMessage
  )

  def flow(userActor: ActorRef[Command])(implicit decoder: Decoder[GameMessage.WsMsgFront]): Flow[GameMessage.WsMsgFront, GameMessage.WsMsgSource, Any] = {
    val in =
      Flow[GameMessage.WsMsgFront]
        .map {
          case GameMessage.TalkMsg(t,m) =>
            TalkMsg(t, m)

          case GameMessage.PlayGame =>
            PlayGame

          case GameMessage.BrickOffList4Front(list) =>
            BrickOffList(list)

          case t: GameMessage.UserDead =>
            UserDead(t.soloScore, t.totalScore)

          case GameMessage.UserLeft =>
            UserLeft

          case GameMessage.ObserveGame =>
            Observe

          case GameMessage.Key4Front(frame, code, down) =>
            Key(frame, code, down)

          case t:GameMessage.ObserveInit4Front =>
            ObserveInit(t.frame, t.bricks,t.rotate,t.BallLocation,t.BoardLocation,t.ballSpeed, t.boardSpeed, t.boardDir)

          case x =>
            UnknownMsg(x)
        }
        .to(sink(userActor))

    val out =
      ActorSource.actorRef[GameMessage.WsMsgSource](
        completionMatcher = {
          case GameMessage.CompleteMsgServer =>
        },
        failureMatcher = {
          case GameMessage.FailMsgServer(ex) => ex
        },
        bufferSize = 64,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue { frontActor =>
          userActor ! FrontActor(frontActor)
      }

    Flow.fromSinkAndSource(in, out)
  }

}

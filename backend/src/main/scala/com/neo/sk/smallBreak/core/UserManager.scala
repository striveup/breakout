package com.neo.sk.smallBreak.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.neo.sk.smallBreak.shared.msg.GameMessage
import org.slf4j.LoggerFactory
import io.circe.{Decoder, Encoder}

import scala.collection._
import scala.language.implicitConversions
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._
import io.circe.generic.auto._
import net.sf.ehcache.transaction.xa.commands.Command

/**
  * User: yuwei
  * Date: 2019/2/3
  * Time: 14:10
  */
object UserManager {

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  case class UserLeft(id:Long) extends Command

  case class TalkMsg(id:Long, t:Int, m:String) extends Command

  case class JoinGame(id:Long, user:ActorRef[UserActor.Command]) extends Command

  final case class GetWebSocketFlow(id:Long, name:String,replyTo:ActorRef[Flow[Message,Message,Any]], roomType: Option[Int], pw:Option[String], roomId:Option[Long]) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.info(s"UserManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            idle(mutable.HashMap[Long, ActorRef[UserActor.Command]]())
        }
    }
  }

  private def idle(userMap:mutable.HashMap[Long, ActorRef[UserActor.Command]])
    (
      implicit timer: TimerScheduler[Command]
    ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case t:GetWebSocketFlow =>
          val userActor = getUserActor(ctx, t.id, t.name, t.roomType, t.pw, t.roomId.getOrElse(0l))
          t.replyTo ! getWebSocketFlow(userActor)

          Behaviors.same

        case UserLeft(id) =>
          userMap.remove(id)
          Behaviors.same

        case JoinGame(id, user) =>
          userMap.put(id, user)
          Behaviors.same

        case t:TalkMsg =>
          userMap.foreach{
            u=>
              if(u._1 != t.id){
                u._2 ! UserActor.MsgWrap(GameMessage.TalkMsg4Server(t.t, t.m))
              }
          }

          Behaviors.same

        case ChildDead(name, ref) =>
          log.info(s"====== $name is dead ========")
          Behaviors.same

      }
    }
  }

  private def getWebSocketFlow(userActor: ActorRef[UserActor.Command]): Flow[Message, Message, Any] = {
    Flow[Message]
      .collect {
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          GameMessage.TextInfo(msg)


        case BinaryMessage.Strict(bMsg) =>
          //decode process.

          val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
          val msg =
            bytesDecode[GameMessage.WsMsgFront](buffer) match {
              case Right(v) => v
              case Left(e) =>
                println(s"decode error: ${e.message}")
                GameMessage.TextInfo("decode error")
            }
          msg
        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }
      .via(UserActor.flow(userActor)) // ... and route them through the chatFlow ...
      .map { //... pack outgoing messages into WS JSON messages ...
      case message: GameMessage.WsMsgServer =>
        val sendBuffer = new MiddleBufferInJvm(409600)
        val msg = ByteString(message.asInstanceOf[GameMessage.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
        BinaryMessage.Strict(msg)

      case _ =>
        TextMessage.apply("")
    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin
  }


  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }


  def getUserActor(ctx: ActorContext[Command], id:Long, name:String, roomType: Option[Int], pw:Option[String], roomId:Long): ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(id, name,roomType, pw, roomId),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }
}

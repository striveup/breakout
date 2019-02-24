package com.neo.sk.smallBreak.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable
import com.neo.sk.smallBreak.common.Constant.RoomType
/**
  * User: yuwei
  * Date: 2019/2/6
  * Time: 17:55
  */
object RoomManager {

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command
  case class CreateRoom(userId:Long, user: ActorRef[UserActor.Command], roomType:Int, pw:Option[String], nick:String) extends Command
  case class RoomDead(id:Long) extends Command
  case class JoinRoom(userId:Long, user: ActorRef[UserActor.Command], roomType:Option[Int], pw:Option[String], roomId:Long, nick:String) extends Command
  private val log = LoggerFactory.getLogger(this.getClass)
  private val idGenerator = new AtomicLong(1000l)
  def create(): Behavior[Command] = {
    log.info(s"UserManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            idle(mutable.HashMap[Long, (ActorRef[RoomActor.Command], Option[String], Int, Int)]()) //pw, roomType, playerNum
        }
    }
  }

  private def idle(roomMap:mutable.HashMap[Long,(ActorRef[RoomActor.Command],Option[String] ,Int, Int)])(
    implicit timer: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case CreateRoom(userId, user, rt, p, nick) =>
          val id = idGenerator.getAndIncrement()
          log.warn("create room " + id)
          val roomActor = getRoomActor(ctx, id, rt)
          roomMap.put(id,(roomActor, p, rt, 1))
          roomActor ! RoomActor.JoinRoom(userId, user, nick)
          Behaviors.same

        case t: JoinRoom =>
          if(t.roomType.isEmpty){
            val roomActor = getRoomActor(ctx, t.roomId, 9)
            roomMap.put(t.roomId,(roomActor, t.pw, roomMap(t.roomId)._3, 2))
            roomActor ! RoomActor.JoinRoom(t.userId, t.user, t.nick)
          }else {
            if(t.pw.isEmpty) {
              t.roomType.get match {
                case RoomType.soloUnLimit | RoomType.soloUpGrade =>
                  ctx.self ! CreateRoom(t.userId, t.user, t.roomType.get, t.pw, t.nick)

                case RoomType.twoUpGrade | RoomType.twoUnLimit | RoomType.twoPk =>
                  val room = roomMap.find(s => s._2._3 == t.roomType.get && s._2._2.isEmpty && s._2._4 < 2)
                  if (room.isEmpty) {
                    ctx.self ! CreateRoom(t.userId, t.user, t.roomType.get, t.pw, t.nick)
                  } else {
                    val roomActor = getRoomActor(ctx, room.get._1, t.roomType.get)
                    roomMap.put(room.get._1,(roomActor, room.get._2._2, room.get._2._3, 2))
                    roomActor ! RoomActor.JoinRoom(t.userId, t.user, t.nick)
                  }
              }
            }else{
              ctx.self ! CreateRoom(t.userId, t.user, t.roomType.get, t.pw, t.nick)
            }
          }
          Behaviors.same

        case ChildDead(name, ref) =>
          log.info(s"====== $name is dead ========")
          Behaviors.same

        case RoomDead(id) =>
          roomMap.remove(id)
          Behaviors.same

      }
    }
  }

  def getRoomActor(ctx: ActorContext[Command], id:Long, roomType: Int): ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RoomActor.create(id, roomType),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[RoomActor.Command]
  }
}

package com.neo.sk.smallBreak.core


import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.StashBuffer
import com.neo.sk.smallBreak.Boot.executor
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import org.slf4j.LoggerFactory
import com.neo.sk.smallBreak.game.GridOnServer
import com.neo.sk.smallBreak.shared.msg.GameMessage.{Brick4front, WsMsgServer}
import com.neo.sk.smallBreak.shared.model.{Constant, Point}
import com.neo.sk.smallBreak.shared.msg.GameMessage
import com.neo.sk.smallBreak.common.Constant._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import com.neo.sk.smallBreak.Boot.{roomManager, userManager}
import com.neo.sk.smallBreak.core.UserActor.Command
/**
  * User: yuwei
  * Date: 2019/2/6
  * Time: 17:52
  */
object RoomActor {

  trait Command
  private final case object BehaviorChangeKey
  case class JoinRoom(userId:Long, userActor: ActorRef[UserActor.Command], nick:String) extends Command

  case class DispatchMsg(msg:WsMsgServer) extends Command

  case class TalkMsg(id:Long,t:Int, msg:String) extends Command

  case class UserDead(id:Long, solo:Int, two:Option[Int]) extends Command

  case object RoomDead extends Command
  case class Key(frame:Long, code:Int, down:Boolean) extends Command

  case class UserLeft(id:Long, user:ActorRef[UserActor.Command]) extends Command
  private val log = LoggerFactory.getLogger(this.getClass)


  final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  case class TimeOut(msg: String) extends Command

  case class ObserveInit(frame:Long, id:Long, bricks:Option[List[Brick4front]], rotate:Point, BallLocation:Point, BoardLocation:Point, ballSpeed:Int, boardSpeed:Int, boardDir:Point) extends Command

  case object Update extends Command

  case object HeartBeat extends Command

  case class BrickOffList(list: List[Int], userId:Long) extends Command

  case object Timer4Update

  case class Observe(id:Long) extends Command

  case object Timer4HeartBeat extends Command

  case class UpdateBricks(id:Long) extends Command

  case class TooLongUnDo(id: Long) extends Command

  case class UserChangeRoom(id:Long) extends Command

  case class Timer4UserDead(id:Long)

  case object Timer4RoomDead

  private[this] def switchBehavior(ctx: ActorContext[Command],
    behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(id: Long, roomType:Int): Behavior[Command] = {
    Behaviors.setup[Command] {
      ctx =>
        log.warn(s"room-$id is created------------")
        Behaviors.withTimers[Command] {
          implicit timer =>
            timer.startPeriodicTimer(Timer4HeartBeat, HeartBeat, (Constant.frameInter * 30).milli)
            idle(0l, mutable.HashMap[Long, (ActorRef[UserActor.Command], Boolean, String)](), id, roomType, mutable.HashMap[Long, GridOnServer]()) //isDead, nick
        }
    }
  }

  private def idle(frameCount:Long, userMap: mutable.HashMap[Long, (ActorRef[UserActor.Command], Boolean, String)],
    roomId:Long, roomType:Int, gridMap:mutable.HashMap[Long, GridOnServer])(
    implicit timer: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case JoinRoom(userId, userActor, nick) =>
          userMap.put(userId, (userActor, false, nick))
          if(gridMap.isEmpty){
            val grid = new GridOnServer(ctx.self,userId)
            grid.init()
            gridMap.put(userId, grid)
          }else if(gridMap.size == 1 && roomType == RoomType.twoPk){
            val grid = new GridOnServer(ctx.self, userId)
            grid.init()
            gridMap.put(userId, grid)
          }
          if(gridMap.size == 1){
            timer.startPeriodicTimer(Timer4Update, Update, Constant.frameInter.milli)
          }
          userActor ! UserActor.JoinRoomSuccess(ctx.self, roomId, roomType)
          if(roomType == RoomType.twoPk && gridMap.size == 2) {
            val initMsg = userMap.map{
              u=> (u._1, gridMap(u._1).getInitParams(frameCount))
            }

            userMap.values.foreach{u=>
              u._1 ! UserActor.MsgWrap(GameMessage.InitMsg4Pk(initMsg.toList))
            }
          }
          if(roomType == RoomType.soloUnLimit ){
            userActor ! UserActor.MsgWrap(gridMap.values.head.getInitParams(frameCount))
          }else if(roomType == RoomType.twoUnLimit){
            if(userMap.size == 2){
              userMap.values.foreach{u=>
                u._1 ! UserActor.MsgWrap(gridMap.values.head.getInitParams(frameCount))
              }
            }
          }
          val nicks = userMap.map{
            u => (u._1, u._2._3)
          }.toList
          userMap.values.foreach{u=>
            u._1 ! UserActor.MsgWrap(GameMessage.Nicks(nicks))
          }
          Behaviors.same

        case DispatchMsg(m) =>
          userMap.values.foreach(u => u._1 ! UserActor.MsgWrap(m))
          Behaviors.same

        case t:TalkMsg =>
          if(t.t == Constant.MsgType.broadCast){
            userManager ! UserManager.TalkMsg(t.id, t.t, t.msg)
          }else {
            dispatch2Others(t.id, UserActor.MsgWrap(GameMessage.TalkMsg4Server(t.t, t.msg)), userMap)
          }
          Behaviors.same

        case t:UserChangeRoom =>
          userMap.remove(t.id)
          Behaviors.same

        case t: ObserveInit =>
          val otherPlayer = userMap.find(_._1 != t.id)
          if(otherPlayer.isDefined){
            otherPlayer.get._2._1 ! UserActor.MsgWrap(GameMessage.ObserveInit4Server(frameCount, t.frame, t.bricks, t.rotate, t.BallLocation, t.BoardLocation, t.ballSpeed, t.boardSpeed, t.boardDir))
          }
          Behaviors.same

        case Key(frame, code, down) =>
          val otherPlayer = userMap.find(_._2._2)
          if(otherPlayer.isDefined){
            otherPlayer.get._2._1 ! UserActor.MsgWrap(GameMessage.Key4Server(frame, code, down))
          }
          Behaviors.same

        case t: UserLeft =>
          userMap.remove(t.id)
          t.user ! UserActor.LeftSuccess
          if(roomType != RoomType.soloUnLimit){
            gridMap.remove(t.id)
          }else if(userMap.isEmpty) {
            gridMap.remove(t.id)
          }
          timer.cancel(Timer4UserDead(t.id))
          dispatch2Others(t.id, UserActor.MsgWrap(GameMessage.OtherLeft), userMap)
          Behaviors.same

        case UserDead(id, solo, two) =>
          userMap.put(id, (userMap(id)._1, true, userMap(id)._3))
          if(userMap.exists(!_._2._2)){
            userMap(id)._1 ! UserActor.MsgWrap(GameMessage.DeadButUnFinish(ScoreRecord.highSolo, ScoreRecord.highTwo))
            dispatch2Others(id, UserActor.MsgWrap(GameMessage.OtherDead),userMap)

          }else{
            ctx.self ! DispatchMsg(GameMessage.DeadAndFinish(ScoreRecord.highSolo, ScoreRecord.highTwo))
            roomManager ! RoomManager.RoomDead(id)
            timer.startSingleTimer(Timer4RoomDead, RoomDead, 3.minutes)
            Behaviors.same
          }
          ScoreRecord.highSolo = Math.max(ScoreRecord.highSolo, solo)
          ScoreRecord.highTwo = Math.max(ScoreRecord.highTwo, two.getOrElse(0))
          Behaviors.same
        case t:Observe =>
          val otherPlayer = userMap.find(_._1 != t.id )
          if(otherPlayer.isDefined){
            otherPlayer.get._2._1 ! UserActor.MsgWrap(GameMessage.Observe)
          }
          Behaviors.same

        case RoomDead =>
          userMap.values.foreach{
            u=> u._1 ! UserActor.LeftSuccess
          }
          Behaviors.stopped

        case HeartBeat =>
          ctx.self ! DispatchMsg(GameMessage.HeartBeat)
          Behaviors.same

        case Update =>
          if(frameCount % 4 == 0) {
            gridMap.values.foreach { g =>
              g.update()
            }
          }
          if(frameCount % 20 == 0) {
            ctx.self ! DispatchMsg(GameMessage.Sync(frameCount))
          }
          idle(frameCount+1, userMap, roomId, roomType, gridMap)

        case UpdateBricks(id) =>
          userMap.values.foreach(u => u._1 ! UserActor.MsgWrap(gridMap(id).getBricks()))
          Behaviors.same

        case TooLongUnDo(id) =>
          if(userMap.get(id).isDefined) {
            if (!userMap(id)._2) {
              userMap(id)._1 ! UserActor.UserLeft
            }
          }
          Behaviors.same

        case BrickOffList(list, id) =>
          timer.cancel(Timer4UserDead(id))
          timer.startSingleTimer(Timer4UserDead(id), TooLongUnDo(id), 2.minutes)
          if(roomType == RoomType.twoPk) {
            gridMap(id).removeBricks(list)
          }else{
            gridMap.values.head.removeBricks(list)
          }
          dispatch2Others(id, UserActor.MsgWrap(GameMessage.BrickOffList4Server(list)), userMap)
          Behaviors.same

      }
    }
  }

  def dispatch2Others(id:Long, msg: UserActor.MsgWrap, userMap: mutable.HashMap[Long,( ActorRef[UserActor.Command], Boolean, String)]) = {
    userMap.foreach{ u =>
      if(u._1 != id) {
        u._2._1 ! msg
      }
    }
  }

}

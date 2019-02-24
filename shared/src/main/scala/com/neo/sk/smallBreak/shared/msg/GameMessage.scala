package com.neo.sk.smallBreak.shared.msg

import com.neo.sk.smallBreak.shared.Element.Brick
import com.neo.sk.smallBreak.shared.model.Point

/**
  * User: yuwei
  * Date: 2019/2/4
  * Time: 11:40
  */
object GameMessage {

  sealed trait Message
  sealed trait WsMsgFrontSource extends Message
  case object CompleteMsgFrontServer extends WsMsgFrontSource
  case class FailMsgFrontServer(ex: Exception) extends WsMsgFrontSource
  sealed trait WsMsgFront extends WsMsgFrontSource

  case class TextInfo(txt:String) extends WsMsgFront
  case class TalkMsg(t:Int, msg:String) extends WsMsgFront
  case object PlayGame extends WsMsgFront
  case class BrickOffList4Front(list:List[Int]) extends WsMsgFront
  case class UserDead(soloScore:Int, totalScore:Option[Int]) extends WsMsgFront
  case object ReStart extends WsMsgFront
  case object UserLeft extends WsMsgFront
  case object ObserveGame extends WsMsgFront
  case class ObserveInit4Front(frame:Long, bricks:Option[List[Brick4front]] = None, rotate:Point, BallLocation:Point, BoardLocation:Point, ballSpeed:Int, boardSpeed:Int, boardDir:Point) extends WsMsgFront
  case class Key4Front(frame:Long, code:Int, down:Boolean) extends WsMsgFront

  sealed trait WsMsgSource extends Message

  case object CompleteMsgServer extends WsMsgSource

  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgServer extends WsMsgSource

  case class WsMsgServerWrap(msg:Array[Byte]) extends WsMsgServer

  case class InitMsg(frame:Long, bricks:List[Brick4front], rotate:Point) extends WsMsgServer

  case class InitMsg4Pk(bricks:List[(Long, InitMsg)]) extends WsMsgServer

  case class BrickOffList4Server(list:List[Int]) extends WsMsgServer

  case class Brick4front(id:Int, atr: Int, position:Point, row:Int, col:Int)

  case class UpdateBricks(id:Long, bricks:List[Brick4front]) extends WsMsgServer

  case object HeartBeat extends WsMsgServer

  case class RoomMsg(roomId:Long,  roomType:Int) extends WsMsgServer

  case class UserMsg(id:Long, nick:String) extends WsMsgServer

  case class Nicks(nicks: List[(Long,String)]) extends WsMsgServer

  case class DeadButUnFinish(soloScore:Int, twoScore:Int) extends WsMsgServer

  case class DeadAndFinish(soloScore:Int, twoScore:Int) extends WsMsgServer

  case object OtherDead extends WsMsgServer

  case object OtherLeft extends WsMsgServer

  case object Observe extends WsMsgServer

  case class Sync(frame: Long) extends WsMsgServer

  case class TalkMsg4Server(t:Int, msg:String) extends WsMsgServer

  case class ObserveInit4Server(frameCur:Long, frame: Long, bricks:Option[List[Brick4front]] = None, rotate:Point, BallLocation:Point, BoardLocation:Point, ballSpeed:Int, boardSpeed:Int, boardDir:Point) extends WsMsgServer

  case class Key4Server(frame:Long, code:Int, down:Boolean) extends WsMsgServer
}

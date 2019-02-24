package com.neo.sk.smallBreak

import com.neo.sk.smallBreak.NetGameHolder._
import com.neo.sk.smallBreak.shared.Element.{Ball, Board, Brick}
import com.neo.sk.smallBreak.shared.Grid
import com.neo.sk.smallBreak.shared.model.Constant
import com.neo.sk.smallBreak.shared.msg.GameMessage._
import com.neo.sk.smallBreak.shared.model.Constant._
import com.neo.sk.smallBreak.shared.msg.GameMessage
import org.scalajs.dom

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * User: yuwei
  * Date: 2019/2/9
  * Time: 18:33
  */
class GridOnFront extends Grid {

  val actionMap = mutable.HashMap[Long, GameMessage.Message]()
  val brickOffList = ListBuffer[Int]()
  val ball = new Ball()
  val board = new Board()
  var score = 0

  def init(msg: InitMsg): Unit = {
    brickOffList.clear()
    if(msg.bricks.nonEmpty) {
      brickMap.clear()
      msg.bricks.foreach {
        b =>
          val brick = Brick(b.id, b.atr, b.row, b.col)
          brick.position = b.position
          brickMap.put(b.id, brick)
      }
    }
    ball.direction = msg.rotate
  }


  def removeBricks() : Unit ={
    brickOffList.foreach { b =>
      if (brickMap.remove(b).isDefined) {
        score += score4Each
        if(isObserve && NetGameHolder.roomType == RoomType.twoPk){
          otherScore = score
        }
        if (score <= 800) {
          if (score % 50 == 0) {
            BallSpeed.level1 = Math.min(BallSpeed.level1 + 2, BallSpeed.level2)
            BoardSpeed.boardSpeed1 = Math.min(BoardSpeed.boardSpeed1 + 2, BoardSpeed.boardSpeed2)
          }
        } else {
          if (score % 100 == 0) {
            BallSpeed.level1 = Math.min(BallSpeed.level1 + 2, BallSpeed.level2)
            BoardSpeed.boardSpeed1 = Math.min(BoardSpeed.boardSpeed1 + 2, BoardSpeed.boardSpeed2)
          }
        }
      }
    }
    brickOffList.clear()
  }

  def updateCallBack(brickOff:Int) : Unit = {
    val brick = brickMap.get(brickOff)
    if(brick.isDefined){
      brickOffList.append(brickOff)
      val r = brick.get.row
      val c = brick.get.column
      brick.get.attribute match {

        case BrickType.normal =>

        case BrickType.bomb =>
          brickMap.values.filter(t=> math.abs(t.row - r) <=1 && math.abs(t.column - c) <= 1 ).foreach{t=>
            if(t.id != brickOff){
              if(t.attribute == BrickType.normal){
                brickOffList.append(t.id)
              }else if(!brickOffList.contains(t.id)){
                updateCallBack(t.id)
              }
            }
          }


        case BrickType.column =>
          brickMap.values.filter(_.column == c).foreach {
            t =>
              if (t.id != brickOff) {
                if(t.attribute == BrickType.normal){
                  brickOffList.append(t.id)
                }else if(!brickOffList.contains(t.id)){
                  updateCallBack(t.id)
                }
              }
          }

        case BrickType.row =>
          brickMap.values.filter(_.row == r).foreach {
            t =>
              if (t.id != brickOff) {
                if(t.attribute == BrickType.normal){
                  brickOffList.append(t.id)
                }else if(!brickOffList.contains(t.id)){
                  updateCallBack(t.id)
                }
              }
          }

      }
    }

  }

  def addActionMap(frame:Long, action: GameMessage.Message) = {
    actionMap.put(frame, action)
    actionMap.remove(frame - 10)
  }

  override def update(): Unit = {
    frameCount += 1
    actionMap.get(frameCount).foreach{
      case GameMessage.Key4Server(f, c, down) =>
        println("==========")
        keyState(c) = down
        board.updateDirection(keyState)
        if(isObserved){
          sendMag2Server(GameMessage.Key4Front(frameCount + Constant.delayFrame, c, down))
        }

      case t:GameMessage.ObserveInit4Server =>
        Constant.BoardSpeed.boardSpeed1 = t.boardSpeed
        Constant.BallSpeed.level1 = t.ballSpeed
        init(GameMessage.InitMsg(gridOnFront.frameCount, t.bricks.getOrElse(List()), t.rotate))
        ball.position = t.BallLocation
        board.position = t.BoardLocation
        board.direction = t.boardDir
        if(!isObserve) {
          msgView.clearMsg()
          lastFrameTime = System.currentTimeMillis()
          score = NetGameHolder.otherScore
          dom.window.requestAnimationFrame(drawLoop(this))
        }
        isObserve = true

      case t:GameMessage.ObserveInit4Front =>
        NetGameHolder.sendMag2Server(t.copy(frame = frameCount + delayFrame))
    }
      removeBricks()
  }

  def updateBricks(bricks:List[Brick4front]): Unit ={
    brickMap.clear()
    bricks.foreach{
      b =>
        val brick = Brick(b.id,b.atr, b.row, b.col)
        brick.position = b.position
        brickMap.put(b.id, brick)
    }
  }

  def getInitParams(t:Int) = {
   if(t==1){
      GameMessage.ObserveInit4Front(frameCount + delayFrame, Some(brickMap.values.map(b => Brick4front(b.id, b.attribute, b.position, b.row, b.column)).toList), ball.direction, ball.position,
        board.position, BallSpeed.level1, BoardSpeed.boardSpeed1, board.direction)
    }else {
      GameMessage.ObserveInit4Front(frameCount + delayFrame, None, ball.direction, ball.position,
        board.position, BallSpeed.level1, BoardSpeed.boardSpeed1, board.direction)
    }
  }


}

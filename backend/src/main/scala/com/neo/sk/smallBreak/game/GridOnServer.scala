package com.neo.sk.smallBreak.game

import com.neo.sk.smallBreak.shared.Element._
import com.neo.sk.smallBreak.shared.Grid
import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.smallBreak.shared.model.Constant._
import com.neo.sk.smallBreak.shared.model.Point
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.smallBreak.shared.msg.GameMessage._

import scala.collection.mutable
import com.neo.sk.smallBreak.core.RoomActor
import com.neo.sk.smallBreak.shared.msg.GameMessage

import scala.collection.mutable.ListBuffer
import scala.util.Random
/**
  * User: yuwei
  * Date: 2019/2/5
  * Time: 15:22
  */
class GridOnServer(roomActor:ActorRef[RoomActor.Command], uId:Long) extends Grid {

  var score = 0l

  val rand = new Random()

  val getBrickId = new AtomicInteger(1)


  def init(): Unit = {
    for(r <- 1 to row){
      for(c <- 1 to column){
        val id = getBrickId.getAndIncrement()
        val atr = getRandomAtr()
        val brick = Brick(id, atr, r, c )
        brick.position = Point((c - 1) * brickW + brickW/2 ,blankH + (row - r) * brickH + brickH/2.0)
        brickMap.put(id, brick)
      }
    }
  }

  def getInitParams(frame:Long) = {
    val rotateArc = (280 + rand.nextInt(70))*math.Pi/180
    InitMsg(frame, brickMap.values.map(b=> Brick4front(b.id, b.attribute, b.position, b.row, b.column)).toList, Point(math.cos(rotateArc), math.sin(rotateArc)))
  }

  override def update(): Unit = {
    if(isNeedAddBrick()){
      lowestRow += 1
      addBricks()
      roomActor ! RoomActor.UpdateBricks(uId)
    }
  }

  def removeBricks(bricks:List[Int]): Unit ={
    bricks.foreach{b =>
      if(brickMap.remove(b).isDefined){
        this.score += score4Each
      }
    }
  }

  def isNeedAddBrick(): Boolean ={
    !brickMap.values.exists(_.row == lowestRow)
  }

  def addBricks() = {
    rowIndex += 1
    brickMap.values.foreach{
      b => b.position = Point(b.position.x, b.position.y + brickH)
    }
    for(c <- 1 to column){
      val id = getBrickId.getAndIncrement()
      val brick = Brick(id, getRandomAtr(), rowIndex, c)
      brick.position = Point((c - 1) * brickW + brickW/2.0, blankH + brickH/2.0f)
      brickMap.put(id, brick)
    }
  }

  def getBricks() = {
    GameMessage.UpdateBricks(uId, brickMap.values.map(b=> Brick4front(b.id, b.attribute, b.position, b.row, b.column)).toList)
  }


  def getRandomAtr() = {
    val t = rand.nextInt(100)
    if(t < 4){
      2
    }else if(t < 8){
      3
    }else if(t < 12){
      4
    }else{
      1
    }
  }





}

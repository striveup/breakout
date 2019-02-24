package com.neo.sk.smallBreak

import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import com.neo.sk.smallBreak.shared.model.Constant._
import scala.scalajs.js
import com.neo.sk.smallBreak.shared.Element
/**
  * User: yuwei
  * Date: 2019/2/17
  * Time: 11:46
  */
class DrawScoreView(ctx: dom.CanvasRenderingContext2D) {

  val pig = dom.document.getElementById("pig").asInstanceOf[HTMLElement]
  val bomb = dom.document.getElementById("bomb").asInstanceOf[HTMLElement]
  val row = dom.document.getElementById("row").asInstanceOf[HTMLElement]
  val col = dom.document.getElementById("col").asInstanceOf[HTMLElement]

  private def setStyle(): Unit ={
    ctx.font = "15px 黑体"
    ctx.fillStyle = "Black"
  }
  def drawScore(nick:String, score:Long):Unit ={
    setStyle()
    if(nick != null)
    ctx.fillText(s"$nick : $score",10,30)
  }

  def drawScore(nick1:String, score1:Long, nick2:String, score2:Long, roomType:Int): Unit ={
    setStyle()
    if(nick1 != null) {
      ctx.fillText(s"$nick1 : $score1", 10, 30)
    }
    if(nick2 != "") {
      ctx.fillText(s"$nick2 : $score2", 10, 55)
    }
    if(roomType != RoomType.twoPk){
      ctx.fillText(s"total : ${score2 + score1}",10,80)
    }
  }

  def drawOtherBrick(bricks: List[Element.Brick]) = {
    val scaleW = scoreViewW * 1.0 / borderW
    bricks.foreach {
      b =>
        val img = b.attribute match {
          case BrickType.normal => pig
          case BrickType.row => row
          case BrickType.column => col
          case BrickType.bomb => bomb
        }
        val leftPoint = b.getRectangle()._1
        ctx.drawImage(img, leftPoint.x * scaleW, leftPoint.y * scaleW + scoreH, brickW * scaleW, brickH * scaleW)
    }
  }

  def scoreClear(): Unit ={
    ctx.clearRect(0, 0, scoreViewW, scoreViewH)
  }
}

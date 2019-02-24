package com.neo.sk.smallBreak

import com.neo.sk.smallBreak.shared.Element._
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, Image, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import com.neo.sk.smallBreak.shared.model.Constant._

import scala.scalajs.js
/**
  * User: yuwei
  * Date: 2019/2/9
  * Time: 18:32
  */
class DrawGameView(ctx: dom.CanvasRenderingContext2D) {

  val pig = dom.document.getElementById("pig").asInstanceOf[HTMLElement]
  val bomb = dom.document.getElementById("bomb").asInstanceOf[HTMLElement]
  val row = dom.document.getElementById("row").asInstanceOf[HTMLElement]
  val col = dom.document.getElementById("col").asInstanceOf[HTMLElement]


  def drawBricks(bricks:List[Brick]) = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, borderW, borderH)
    bricks.foreach {
      b =>
        val img = b.attribute match {
          case BrickType.normal => pig
          case BrickType.row => row
          case BrickType.column => col
          case BrickType.bomb => bomb
        }
        val leftPoint = b.getRectangle()._1
        ctx.drawImage(img, leftPoint.x, leftPoint.y, brickW, brickH )
    }
  }

  def drawBoard(board:Board) = {
    ctx.fillStyle = Color.Yellow.toString()
    val leftPoint = board.getRectangle()._1
    ctx.fillRect(leftPoint.x, leftPoint.y, boardW, boardH)
  }

  def drawBall(ball:Ball): Unit ={
    ctx.fillStyle = Color.Red.toString()
    ctx.beginPath()
    ctx.arc(ball.position.x, ball.position.y, ballR, 0, Math.PI * 2, true)
    ctx.closePath()
    ctx.fill()
  }


}

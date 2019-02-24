package com.neo.sk.smallBreak.shared
import com.neo.sk.smallBreak.shared.model.Point
import com.neo.sk.smallBreak.shared.model.Constant._
import util.control.Breaks._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks

/**
  * User: yuwei
  * Date: 2019/2/5
  * Time: 11:28
  */
object Element {


  trait RecElement{
    var position = Point(0, 0)
    protected val w : Double
    protected val h : Int

    def getRectangle(): (Point, Point) = {
      (Point(position.x - w/2.0, position.y - h/2.0),
        Point(position.x + w/2.0, position.y + h/2.0))
    }
  }

  //attribute 1:普通， 2：炸弹， 3，去row， 4，去column
  case class Brick(id:Int, attribute:Int, row:Int, column: Int) extends RecElement{
    override val w = brickW
    override val h = brickH
  }


  class Ball() extends RecElement {
    var direction = Point(0, 0)
    override val w = ballR
    override protected val h: Int = ballR
    position = Point(boardW/2, borderH - (boardH + ballR) )

    def getDirection(againstPart:String): Unit ={
      againstPart match {
        case "top" => direction = Point(direction.x, -direction.y)
        case "right" => direction = Point(-direction.x, direction.y)
        case "left" => direction = Point(-direction.x, direction.y)
        case "bottom" => direction = Point(direction.x, -direction.y)
      }
    }

    def isIntersectBrick(brick: (Point, Point)):Boolean={
      val (blx, bly, brx, bry) = (brick._1.x, brick._1.y, brick._2.x, brick._2.y)
      val thisRec = getRectangle()
      val (thlx, thly, thrx, thry) = (thisRec._1.x, thisRec._1.y, thisRec._2.x, thisRec._2.y)

      val result = thlx <= brx && thly <= bry && thrx >= blx && thry >= bly
      if(result){
        if(thrx >= blx && thrx <= brx) {
          getDirection("right")
//          position = Point(2 * (thlx - ballR) - position.x, position.y)
        }
        if(thlx >= blx && thlx <= brx){
          getDirection("left")
//          position = Point(2 * (thrx + ballR) - position.x, position.y)
        }
        if(thly >= bly && thly <= bry) {
          getDirection("top")
//          position = Point(position.x, (thry + ballR) * 2 - position.y)
        }
        if(thry >= bly && thry <= bry) {
          getDirection("bottom")
//          position = Point(position.x, (thly - ballR) * 2 - position.y)
        }
      }
      result
    }

    def isIntersectBoard(board:(Point, Point)) : (Boolean, Boolean) = {
      var isDead = false
      val (blx, bly, brx, bry) = (board._1.x, board._1.y - 2, board._2.x, board._2.y)
      val thisRec = getRectangle()
      val (thlx, thly, thrx, thry) = (thisRec._1.x, thisRec._1.y, thisRec._2.x, thisRec._2.y)
      val result = thlx <= brx && thly <= bry && thrx >= blx && thry >= bly
      if(result){

        if(thry >= bly && thry <= bry) {
          getDirection("bottom")
          //          position = Point(position.x, (thly - ballR) * 2 - position.y)
        }else{
          isDead = true
        }
      }
      (result, isDead)
    }

    def isIntersectBorder() : (Boolean, Boolean) = { //是否碰撞， 是否死亡
      if(borderH - position.y < ballR) (true, true)  //dead
      else if(position.x < ballR){
        getDirection("left")
        position = Point(2 * ballR - position.x, position.y)
        (true, false)
      }else if(borderW - position.x < ballR){
        getDirection("right")
        position = Point(2 * (borderW - ballR) - position.x, position.y)
        (true, false)
      }else if(position.y < ballR){
        getDirection("top")
        position = Point(position.x, 2 * ballR - position.y)
        (true, false)
      }else{
        (false, false)
      }
    }

    def updatePosition(bricks: List[Brick],board: Board, scale: Float, callBack: Int=> Unit) :Boolean = {
      var isDead = false
      breakable(
        for (i <- 5 to BallSpeed.level1 by 2) {
          position = position + direction * 2 * scale
          val boardResult = isIntersectBoard(board.getRectangle())
          if (boardResult._2) {
            isDead = true
            break()
          } else {
            if (!boardResult._1) {
              val borderResult = isIntersectBorder()
              if (borderResult._2) {
                isDead = true
                break()
              } else {
                if (!borderResult._1) {
                  breakable(
                    for (b <- bricks) {
                      if (isIntersectBrick(b.getRectangle())) {
                        callBack(b.id)
                        break()
                      }
                    }
                  )
                }
              }
            }
          }
        }
      )
      isDead
    }


  }


  class Board() extends RecElement {
    position = Point(boardW/2.0, borderH - boardH/2.0)
    override protected val h: Int = boardH
    override protected val w = boardW
    var direction = Point(0, 0)

    def updatePosition(scale: Float) = {
      position = position  + direction * scale * BoardSpeed.boardSpeed1
      val thisRec = getRectangle()
      val (thlx, thly, thrx, thry) = (thisRec._1.x, thisRec._1.y, thisRec._2.x, thisRec._2.y)
      if(thlx < 0) position = Point(boardW/2.0, position.y)
      else if(thrx > borderW) position = Point(borderW - boardW/2.0, position.y)
      if(thly < borderH - boardMoveH) position = Point(position.x, borderH - boardMoveH + boardH/2.0)
      else if(thry > borderH) position = Point(position.x, borderH - boardH/2.0)
    }

    def updateDirection(keyState:ListBuffer[Boolean]) = {
      direction = if(keyState(0) && keyState(1)){
        Point(math.cos(Math.PI*5/4), math.sin(Math.PI*5/4))
      }else if(keyState(0) && keyState(3)){
        Point(math.cos(Math.PI*3/4), math.sin(Math.PI*3/4))
      }else if(keyState(2) && keyState(1)){
        Point(math.cos(Math.PI*7/4), math.sin(Math.PI*7/4))
      }else if(keyState(2) && keyState(3)){
        Point(math.cos(Math.PI/4), math.sin(Math.PI/4))
      }else if(keyState(0)){
        Point(-1, 0)
      }else if(keyState(1)){
        Point(0, -1)
      }else if(keyState(2)){
        Point(1, 0)
      }else if(keyState(3)){
        Point(0, 1)
      }else{
        Point(0, 0)
      }

    }

  }

}

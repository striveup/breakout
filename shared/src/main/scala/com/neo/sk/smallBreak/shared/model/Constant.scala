package com.neo.sk.smallBreak.shared.model

/**
  * User: yuwei
  * Date: 2019/2/5
  * Time: 11:35
  */
object Constant {

  val borderW = 500
  val borderH = 500

  val row = 5
  val column = 14

  val brickW:Double = borderW / column.toDouble
  val brickH = 25

  val ballR = 6

  val delayFrame = 8
  val frameInter = 50

  val blankH = 100

  val boardW: Double = 100
  val boardH = 8
  val score4Each = 5

  object BallSpeed{
    val speed = 20
    var level1 = 20
    val level2 = 80
  }

  object BoardSpeed {
    val speed = 30
    var boardSpeed1 = 30
    val boardSpeed2 = 60
  }

  val boardMoveH = 150

  val scoreViewW = 250

  val scoreViewH = 250

  object RoomType{
    val soloUpGrade = 1
    val soloUnLimit = 2
    val twoUpGrade = 3
    val twoUnLimit = 4
    val twoPk = 5
  }

  object BrickType{
    val normal = 1
    val bomb = 2
    val row = 3
    val column = 4
  }

  object MsgType{
    val normal = 1
    val broadCast = 2
    val shit = 3
    val heart = 4
  }

  val scoreH = 120

}

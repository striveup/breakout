package com.neo.sk.smallBreak.shared



import com.neo.sk.smallBreak.shared.model.Constant._
import com.neo.sk.smallBreak.shared.Element._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * User: yuwei
  * Date: 2019/2/5
  * Time: 10:51
  */
trait Grid {

  var frameCount = 0l
  var rowIndex = row
  var lowestRow = 1
  val brickMap = mutable.HashMap[Int, Brick]()
  var keyState = ListBuffer(false, false, false, false) //四个方向按键的状态， 按下为ture

  def update(): Unit



}

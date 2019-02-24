package com.neo.sk.smallBreak

import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._
import com.neo.sk.smallBreak.shared.model.Constant._
import scala.scalajs.js

/**
  * User: yuwei
  * Date: 2019/2/14
  * Time: 22:17
  */
class DrawMsgView(ctx: dom.CanvasRenderingContext2D) {

  def drawSolo(score:Int, highScore:Int) = {
    clearMsg()
    setStyle()
    ctx.fillText("真悲伤， 你输了", 40, 100)
    if(score < highScore){
      ctx.fillText(s"你也没有打破什么记录", 40, 120)
    }else{
      ctx.fillText(s"不过你打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"你的分数: $score", 40, 140)
    ctx.fillText(s"历史最高分: $highScore", 40, 160)
    ctx.fillText("按Esc返回, 按Enter重玩 ",40,180)
  }
  def drawPkLose(score:Int, highScore:Int) = {
    clearMsg()
    setStyle()
    ctx.fillText("Ooops, 没接住", 40, 100)
    if(score < highScore){
      ctx.fillText(s"你也没有打破什么记录", 40, 120)
    }else{
      ctx.fillText(s"不过你打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"你的分数: $score", 40, 140)
    ctx.fillText(s"历史最高分: $highScore", 40, 160)
    ctx.fillText("按Esc返回, 按V观战队友 ",40,180)
  }

  def drawCorLose(score:Int, highScore:Int, highTwo:Int) = {
    clearMsg()
    setStyle()
    ctx.fillText("Ooops, 没接住", 40, 100)
    if(score < highScore){
      ctx.fillText(s"你也没有打破什么记录", 40, 120)
    }else{
      ctx.fillText(s"不过你打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"你的分数: $score", 40, 140)
    ctx.fillText(s"历史个人最高分: $highScore", 40, 160)
    ctx.fillText(s"历史团队最高分：$highTwo", 40, 180)
    ctx.fillText("按Esc返回, 按V观战队友 ",40,200)
  }

  def drawPkWin(score:Int, highScore:Int) = {
    clearMsg()
    setStyle()
    ctx.fillText("知道不？ 你赢了！", 40, 100)
    if (score < highScore) {
      ctx.fillText(s"但是你也没有打破什么记录", 40, 120)
    } else {
      ctx.fillText(s"更厉害滴！你打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"你的分数: $score", 40, 140)
    ctx.fillText(s"历史个人最高分: $highScore", 40, 160)
    ctx.fillText(" 按Esc返回, 按Enter重玩 ", 40, 180)
  }

  def drawPkLoseOberseve(score:Int, highScore:Int) = {
    clearMsg()
    setStyle()
    ctx.fillText("瞅啥？ 人家赢了！", 40, 100)
    if(score < highScore){
      ctx.fillText(s"虽然人家没有打破什么记录", 40, 120)
    }else{
      ctx.fillText(s"更厉害滴！人家打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"人家分数: $score", 40, 140)
    ctx.fillText(s"历史个人最高分: $highScore", 40, 160)
    ctx.fillText("按Esc返回, 按Enter重玩 ", 40, 180)
  }

  def drawCorWin(score:Int, highScore:Int, highTwo:Int, total:Int)={
    clearMsg()
    setStyle()
    ctx.fillText("Ooops, 没接住", 40, 100)
    if(score > highScore){
      ctx.fillText(s"你打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"你的分数: $score", 40, 140)
    ctx.fillText(s"历史个人最高分: $highScore", 40, 160)
    if(total > highTwo){
      ctx.fillText(s"你们打破了团队最高分记录！", 40, 180)
    }
    ctx.fillText(s"你们总分：$total", 40, 200)
    ctx.fillText(s"历史团队最高分：$highTwo", 40, 220)
    ctx.fillText("按Esc返回, 按Enter重玩 ",40,240)
  }

  def drawCorObserve(score:Int, highScore:Int, highTwo:Int, total:Int) = {
    clearMsg()
    setStyle()
    ctx.fillText("Ooops, 没接住", 40, 100)
    if(score > highScore){
      ctx.fillText(s"你亲耐滴队友打破了个人成绩记录", 40, 120)
    }
    ctx.fillText(s"他的分数: $score", 40, 140)
    ctx.fillText(s"历史个人最高分: $highScore", 40, 160)
    if(total > highTwo){
      ctx.fillText(s"你们打破了团队最高分记录！", 40, 180)
    }
    ctx.fillText(s"你们总分：$total", 40, 200)
    ctx.fillText(s"历史团队最高分：$highTwo", 40, 220)
    ctx.fillText("按Esc返回, 按Enter重玩 ",40,240)
  }


  def drawLoading() = {
    clearMsg()
    setStyle()
    ctx.fillText("Loading... ",40,100)
  }

  def drawWaiting4Othor() = {
    clearMsg()
    setStyle()
    ctx.fillText("等待其他玩家加入... ",40,100)
  }

  def clearMsg() ={
    ctx.clearRect(0, 0, borderW, borderH)
  }


  private def setStyle() = {
    ctx.fillStyle = "rgba(192,192,192,0.5)"
    ctx.fillRect(0, 0, borderW, borderH)
    ctx.font = "20px 黑体"
    ctx.fillStyle = "Black"
  }

  def otherDead() = {
    clearMsg()
    ctx.font = "20px 黑体"
    ctx.fillText("对方死亡", 100, 50)
  }
  def otherLeft() ={
    clearMsg()
    ctx.font = "20px 黑体"
    ctx.fillText("对反已离开房间", 100, 50)
  }

  def tooLong() = {
    clearMsg()
    ctx.font = "20px 黑体"
    ctx.fillText("长时间未操作， 请返回重新加入", 100, 50)
  }

}

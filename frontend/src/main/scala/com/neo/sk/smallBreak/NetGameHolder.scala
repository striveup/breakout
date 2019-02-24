package com.neo.sk.smallBreak

import com.neo.sk.smallBreak.shared.msg.GameMessage
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Document => _, _}
import org.scalajs.dom.raw._

import scala.scalajs.js.typedarray.ArrayBuffer
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.{MiddleBufferInJs, decoder}
import io.circe.generic.auto._
import io.circe.parser._
import com.neo.sk.smallBreak.shared.model.Constant
import com.neo.sk.smallBreak.shared.model.Constant.RoomType
import com.neo.sk.smallBreak.shared.msg.GameMessage.{Brick4front, OtherLeft}
import org.scalajs.dom.Blob

import scala.collection.mutable
import scala.scalajs.js

/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 12:45 PM
  */
object NetGameHolder extends js.JSApp {


  private[this] val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]

  private[this] val msgCanvas = dom.document.getElementById("MsgView").asInstanceOf[Canvas]

  private[this] val scoreCanvas = dom.document.getElementById("scoreView").asInstanceOf[Canvas]

  canvas.width = Constant.borderW
  canvas.height = Constant.borderH
  msgCanvas.width = Constant.borderW
  msgCanvas.height = Constant.borderH
  scoreCanvas.width = Constant.scoreViewW
  scoreCanvas.height = Constant.scoreViewH
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val msgCtx = msgCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val scoreCtx = scoreCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  val gameView = new DrawGameView(ctx)
  val msgView = new DrawMsgView(msgCtx)
  val scoreView = new DrawScoreView(scoreCtx)
  var playTimer = -1
  var observingTimer = -1
  var observedTimer = -1
  var gridOnFront = new GridOnFront()
  var nextAnimation = 0.0
  var lastFrameTime = System.currentTimeMillis()
  var gameStream:WebSocket  = _
  val sendBuffer = new MiddleBufferInJs(82920) //sender buffer
  var isDead = false
  var isFinish = false
  var isObserve = false
  var isObserved = false
  var roomType = 1
  private var roomId:Long = _
  private var myId:Long = _
  private var myNick: String = _
  private var otherNick: String = ""
  private var grid4Other: GridOnFront= _
  var otherScore = 0
  var observeCount = 0l
  var over = false


  @scala.scalajs.js.annotation.JSExport
  override def main(): Unit = {
    val search = dom.window.location.search
    joinGame(search)
    dom.window.requestAnimationFrame(drawScore())
  }

  def drawLoop(grid:GridOnFront):  Double => Unit = { _ =>
    if(!isDead || isObserve ) {
      nextAnimation = dom.window.requestAnimationFrame(drawLoop(grid))
    }else{
      if(!isObserve) {
        sendMag2Server(GameMessage.UserDead(grid.score, if(roomType == RoomType.twoUnLimit) Some(grid.score + otherScore) else None))
      }
      dom.window.setTimeout(()=> {dom.window.clearInterval(playTimer); playTimer = -1}, Constant.frameInter * 3)
//      if(observeTimer >= 0 ) {
//        dom.window.clearInterval(observeTimer)
//        observeTimer = -1
//      }
    }
    val current = System.currentTimeMillis()
    val scale = (current - lastFrameTime) / (Constant.frameInter.toFloat * 2)
    lastFrameTime = current
    val tmpIsDead = grid.ball.updatePosition(grid.brickMap.values.toList, grid.board, scale, grid.updateCallBack)
    if(!isDead) {
      isDead = tmpIsDead
    }
    grid.board.updatePosition(scale)
    gameView.drawBricks(grid.brickMap.values.toList)
    gameView.drawBall(grid.ball)
    gameView.drawBoard(grid.board)
  }

  def drawScore(): Double => Unit = { _ =>
    nextAnimation = dom.window.requestAnimationFrame(drawScore())
    scoreView.scoreClear()
    if(roomType == RoomType.twoPk){
      scoreView.drawScore(myNick, gridOnFront.score, otherNick, otherScore, roomType)
      scoreView.drawOtherBrick(grid4Other.brickMap.values.toList)
    }else if(roomType == RoomType.twoUnLimit){
      scoreView.drawScore(myNick, gridOnFront.score , otherNick, otherScore, roomType)
    }else{
      scoreView.drawScore(myNick, gridOnFront.score)
    }
  }


  def gameLoop(grid:GridOnFront): Unit ={
    if(grid.brickOffList.nonEmpty  && !isObserve ){
      sendMag2Server(GameMessage.BrickOffList4Front(grid.brickOffList.toList))
    }
    grid.update()
  }

  def keyEvent(grid:GridOnFront, down:Boolean, keyLocation: Int): Unit ={
    grid.addActionMap(grid.frameCount + 1, GameMessage.Key4Server(grid.frameCount + 1, keyLocation, down))
  }

  def sendMag2Server(msg: GameMessage.WsMsgFront) = {
    msg.fillMiddleBuffer(sendBuffer)
    val sendMsg: ArrayBuffer = sendBuffer.result()
    gameStream.send(sendMsg)
  }

  def reStart() = {
    isDead = false
    isObserve = false
    observeCount = 0l
    isObserved = false
    gridOnFront = new GridOnFront()
    grid4Other = null
    otherScore = 0
    dom.window.clearInterval(playTimer)
    dom.window.clearInterval(observingTimer)
    dom.window.clearInterval(observedTimer)
    Constant.BoardSpeed.boardSpeed1 = Constant.BoardSpeed.speed
    Constant.BallSpeed.level1 = Constant.BallSpeed.speed
    msgView.drawLoading()
    val msg:GameMessage.WsMsgFront = GameMessage.PlayGame
    msg.fillMiddleBuffer(sendBuffer) //encode msg
    val ab: ArrayBuffer = sendBuffer.result() //get encoded data.
    gameStream.send(ab) // send data.
    dom.window.requestAnimationFrame(drawLoop(gridOnFront))
  }

  canvas.onmousedown = {e: dom.MouseEvent =>
    e.preventDefault()
    bindListenCtx()
  }

  msgCanvas.onmousedown = {e: dom.MouseEvent =>
    e.preventDefault()
    bindListenCtx()
  }
  def bindListenCtx(): Unit ={
    canvas.focus()
    canvas.onkeydown = { e: dom.KeyboardEvent =>
     val key =  e.keyCode match {
        case KeyCode.Left =>
          0
        case KeyCode.Up =>
          1
        case KeyCode.Right =>
          2
        case KeyCode.Down =>
          3

        case KeyCode.Escape=>
          if(isDead) {
            sendMag2Server(GameMessage.UserLeft)
            dom.window.location.href = "joinPage"
          }
          5

        case KeyCode.Enter =>
          if(isDead ) {
            reStart()
          }
          5

        case KeyCode.V =>
          if(isDead) {
            grid4Other = new GridOnFront()
            sendMag2Server(GameMessage.ObserveGame)
          }
          5
        case _ => 5
      }
      if(key != 5 && !isDead){
        keyEvent(gridOnFront,true, key)
      }

    }
    canvas.onkeyup = { e: dom.KeyboardEvent =>
      val key = e.keyCode match {
        case KeyCode.Left =>
          0
        case KeyCode.Up =>
          1
        case KeyCode.Right =>
          2
        case KeyCode.Down =>
          3
        case _ => 5
      }
      if(key != 5 && !isDead) {
        keyEvent(gridOnFront, false, key)
//        if(isObserved){
//          sendMag2Server(GameMessage.Key4Front(gridOnFront.frameCount + Constant.delayFrame, key, false))
//        }
      }
    }
  }



  def joinGame(search: String): Unit = {
    gameStream = new WebSocket(getWebSocketUri(dom.document, search))
    gameStream.onopen = { (event0: Event) =>
      msgView.drawLoading()
      val msg:GameMessage.WsMsgFront = GameMessage.PlayGame
      msg.fillMiddleBuffer(sendBuffer) //encode msg
      val ab: ArrayBuffer = sendBuffer.result() //get encoded data.
      gameStream.send(ab) // send data.
      event0
    }

    gameStream.onerror = { (event: ErrorEvent) =>}




    gameStream.onmessage = { (event: MessageEvent) =>
      //val wsMsg = read[Protocol.GameMessage](event.data.toString)
      event.data match {
        case blobMsg: Blob =>
          val fr = new FileReader()
          fr.readAsArrayBuffer(blobMsg)
          fr.onloadend = { _: Event =>
            val buf = fr.result.asInstanceOf[ArrayBuffer]
            val middleDataInJs = new MiddleBufferInJs(buf) //put data into MiddleBuffer
          val encodedData: Either[decoder.DecoderFailure, GameMessage.WsMsgServer] =
            bytesDecode[GameMessage.WsMsgServer](middleDataInJs) // get encoded data.
            //            GameView.canvas.focus()
            encodedData match {
              case Right(data) =>
                data match {
                  case t: GameMessage.InitMsg =>
                    gridOnFront.init(t)
                    gridOnFront.frameCount = t.frame
                    lastFrameTime = System.currentTimeMillis()
                    bindListenCtx()
                    msgView.clearMsg()
                    dom.window.requestAnimationFrame(drawLoop(gridOnFront))
                    playTimer = dom.window.setInterval(() => gameLoop(gridOnFront), Constant.frameInter)

                  case t: GameMessage.Sync =>
                    gridOnFront.frameCount = t.frame
                    if(grid4Other != null){
                      grid4Other.frameCount = t.frame
                    }

                  case t: GameMessage.BrickOffList4Server =>
                    if(roomType == Constant.RoomType.twoPk ){
                      if(!isObserve) {
                        t.list.foreach { b =>
                          if (grid4Other.brickMap.get(b).isDefined) {
                            otherScore += Constant.score4Each
                          }
                          grid4Other.brickMap.remove(b)
                        }
                      }
                    }else {
                      t.list.foreach { b =>
                        if(gridOnFront.brickMap.get(b).isDefined){
                          otherScore += Constant.score4Each
                        }
                        gridOnFront.brickMap.remove(b)
                      }
                    }

                  case t: GameMessage.UpdateBricks =>
                    if(t.id == myId) {
                      updateBrick(gridOnFront, t.bricks)

                    }else{
                      updateBrick(grid4Other, t.bricks)
                    }

                  case GameMessage.HeartBeat =>

                  case t:GameMessage.DeadButUnFinish =>
                    if(roomType == RoomType.twoPk){
                      msgView.drawPkLose(gridOnFront.score, t.soloScore)
                    }else if(roomType == RoomType.twoUnLimit){
                      msgView.drawCorLose(gridOnFront.score, t.soloScore, t.twoScore)
                    }


                  case t:GameMessage.DeadAndFinish =>
                    if(roomType == RoomType.soloUnLimit){
                      msgView.drawSolo(gridOnFront.score, t.soloScore)
                    }else if(roomType == RoomType.twoUnLimit){
                      if(isObserve){
                        msgView.drawCorObserve(otherScore, t.soloScore,t.twoScore, gridOnFront.score + otherScore)
                      }else{
                        msgView.drawCorWin(gridOnFront.score, t.soloScore, t.twoScore, gridOnFront.score + otherScore)
                      }
                    }else if(roomType == RoomType.twoPk){
                      if(isObserve){
                        msgView.drawPkLoseOberseve(otherScore, t.soloScore)
                      }else{
                        msgView.drawPkWin(gridOnFront.score, t.soloScore)
                      }
                    }
                    if(isObserve){
                      dom.window.clearInterval(observingTimer)
                      isObserve = false
                    }
                    if(isObserved){
                      dom.window.clearInterval(observedTimer)
                      isObserved = false
                    }


                  case GameMessage.TalkMsg4Server(t, msg) =>
                    getMsg(otherNick,t, msg)

                  case GameMessage.OtherDead =>
                    if(!isDead) {
                      msgView.otherDead()
                      dom.window.setTimeout(() => msgView.clearMsg(), 3000)
                    }

                  case GameMessage.InitMsg4Pk(bricks) =>
                    bricks.foreach{
                      b=>
                        if(b._1 == myId){
                          gridOnFront.init(b._2)
                        }else{
                          grid4Other = new GridOnFront()
                          grid4Other.init(b._2)
                        }
                    }
                    lastFrameTime = System.currentTimeMillis()
                    bindListenCtx()
                    msgView.clearMsg()
                    dom.window.requestAnimationFrame(drawLoop(gridOnFront))
                    playTimer = dom.window.setInterval(() => gameLoop(gridOnFront), Constant.frameInter)

                  case GameMessage.Nicks(nicks) =>
                    nicks.foreach{
                      n => if(n._1 != myId){
                        otherNick = n._2
                      }
                    }

                  case GameMessage.Observe => //被观

                    observedTimer = dom.window.setInterval(() =>
                      if(!isDead) {
                        if(observeCount % 5 == 0) {
                          gridOnFront.addActionMap(gridOnFront.frameCount + 1, gridOnFront.getInitParams(1))
                        }
//                        }else{
//                          gridOnFront.addActionMap(gridOnFront.frameCount + 1,gridOnFront.getInitParams(2))
//                        }
                        observeCount += 1
                      }, Constant.frameInter * 20)
                    isObserved= true
                  case OtherLeft =>
                    msgView.otherLeft()
                    if(isObserved){
                      isObserved = false
                      dom.window.clearInterval(observedTimer)
                    }
                    dom.window.setTimeout(() => msgView.clearMsg(), 3000)

                  case t:GameMessage.ObserveInit4Server =>
                    grid4Other.frameCount = t.frameCur
                    grid4Other.addActionMap(t.frame, t)
                    if(!isObserve) {
                      observingTimer = dom.window.setInterval(() => gameLoop(grid4Other), Constant.frameInter)
                    }

                  case t:GameMessage.Key4Server =>
                    println(t)
                    grid4Other.addActionMap(t.frame, t)

                  case GameMessage.UserMsg(uId, nick) =>
                    myId = uId
                    myNick = nick
                    if((search.contains("roomType=5") || search.contains("roomType=4"))&& otherNick == ""){
                      msgView.drawWaiting4Othor()
                    }

                  case GameMessage.RoomMsg(rId, rt) =>
                    roomId = rId
                    roomType = rt
                }
              case Left(e) =>
                println(s"got error: ${e.message}")

            }
          }

      }
    }


    gameStream.onclose = { (event: Event) =>
      msgView.tooLong()
    }

  }

  def getWebSocketUri(document: Document, search:String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/smallBreak/playGame" + search
  }


  val submitBtn = dom.document.getElementById("sub_btn").asInstanceOf[Input]
  val sayText = dom.document.getElementById("saytext").asInstanceOf[Input]
  val show = dom.document.getElementById("show")
  val shitImg = dom.document.getElementById("shit").asInstanceOf[Image]
  val heartImg = dom.document.getElementById("heart").asInstanceOf[Image]
  val broadBtn = dom.document.getElementById("broad").asInstanceOf[Input]

  submitBtn.onclick = {e:dom.MouseEvent =>
    e.preventDefault()
    val msg = regexChange(sayText.value)
    if(msg != ""){
      val p = dom.document.createElement("p")
      p.innerHTML = "我 ：" + msg
      show.appendChild(p)
      show.scrollTop = show.scrollHeight
      NetGameHolder.sendMag2Server(GameMessage.TalkMsg(Constant.MsgType.normal, msg))
      sayText.value = ""
    }
  }

  broadBtn.onclick = {e:dom.MouseEvent =>
    e.preventDefault()
    val msg = regexChange(sayText.value)
    if(msg != ""){
      val p = dom.document.createElement("p")
      p.setAttribute("color", "red")
      p.innerHTML = "广播 ：" + msg
      show.appendChild(p)
      show.scrollTop = show.scrollHeight
      NetGameHolder.sendMag2Server(GameMessage.TalkMsg(Constant.MsgType.broadCast, myNick+": " + msg))
      sayText.value = ""
    }
  }

  shitImg.onclick = {e:dom.MouseEvent =>
    e.preventDefault()
    val p = dom.document.createElement("p")
    p.setAttribute("color", "yellow")
    p.innerHTML = "给对方丢了一坨屎"
    show.appendChild(p)
    show.scrollTop = show.scrollHeight
    NetGameHolder.sendMag2Server(GameMessage.TalkMsg(Constant.MsgType.shit, ""))
    sayText.value = ""
  }
  heartImg.onclick = {e:dom.MouseEvent =>
    e.preventDefault()
    val p = dom.document.createElement("p")
    p.setAttribute("color", "yellow")
    p.innerHTML = "给对方点了个赞"
    show.appendChild(p)
    show.scrollTop = show.scrollHeight
    NetGameHolder.sendMag2Server(GameMessage.TalkMsg(Constant.MsgType.heart, ""))
    sayText.value = ""
  }

  def regexChange(str:String) = {
    val reg = "\\[em_([0-9]*)\\]"
    str.replaceAll(reg, "<img src=\"/smallBreak/static/arclist/$1.gif\"/>")
  }

  def getMsg(nick:String,t:Int, msg:String) = {
    val p = dom.document.createElement("p")
    if(t == Constant.MsgType.normal) {
      p.innerHTML = nick + " ：" + msg
    }else if(t == Constant.MsgType.broadCast){
      p.setAttribute("color", "red")
      p.innerHTML = msg
    }else if(t == Constant.MsgType.heart){
      p.setAttribute("color", "yellow")
      p.innerHTML = "你打的太好啦！！ヾ(*´▽‘*)ﾉ"
    }else{
      p.setAttribute("color", "yellow")
      p.innerHTML = "你打的和屎一样(╯#-_-)╯"
    }
    show.appendChild(p)
    show.scrollTop = show.scrollHeight
  }

  def updateBrick(grid:GridOnFront, bricks:List[Brick4front]): Unit ={
    if(grid.ball.position.y > (Constant.brickH * Constant.row + Constant.brickH)){
      grid.updateBricks(bricks)
    }else{
      dom.window.setTimeout(() => updateBrick(grid, bricks), 1000)
    }
  }

}

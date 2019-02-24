package com.neo.sk.smallBreak.protocol

/**
  * User: yuwei
  * Date: 2019/2/13
  * Time: 17:26
  */
object LoginProtocol {

  trait CommonRsp{
    val errCode:Int
    val msg:String
  }
  case class RegisterRsp(errCode:Int = 0, msg:String = "ok") extends CommonRsp

  case class LoginRsp(errCode:Int = 0, msg:String = "ok") extends CommonRsp

}

package com.neo.sk.smallBreak.model.dao


import com.neo.sk.smallBreak.model.SlickTables._
import com.neo.sk.smallBreak.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.smallBreak.Boot.executor
/**
  * User: yuwei
  * Date: 2019/2/13
  * Time: 17:14
  */
object userDao {

  def checkUser(name:String)= {
    db.run(tUser.filter(_.name === name).exists.result)
  }

  def getNick(id:Long) = {
    db.run(tUser.filter(_.id === id).map(_.nickname).result.head)
  }

  def insertUser(user:rUser) = {
    db.run(tUser.returning(tUser.map(_.id)) += user)
  }

  def deleteUser(id:Long)={
    db.run(tUser.filter(_.id === id).delete)
  }

  def getAllUsers() = {
    db.run(tUser.result)
  }

  def updateNick(nick:String, id:Long) = {
    db.run(tUser.filter(_.id === id).map(_.nickname).update(nick))
  }

  def updatePw(id:Long, pw:String) = {
    db.run(tUser.filter(_.id === id).map(_.pw).update(pw))
  }

  def checkPw(name:String, pw:String) ={
    db.run(tUser.filter(t=>t.name === name && t.pw === pw).map(_.id).result.headOption)
  }
}

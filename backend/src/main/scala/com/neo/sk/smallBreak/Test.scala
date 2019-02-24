package com.neo.sk.smallBreak

/**
  * User: yuwei
  * Date: 2019/2/19
  * Time: 20:54
  */
object Test {

  def main(args: Array[String]): Unit = {
    val str = "[em_1]"
    val reg = "\\[em_([0-9]*)\\]"
    println(str.replaceAll(reg, "<img src=\"/smallBreak/static/arclist/$1.gif\"/>"))
  }

}

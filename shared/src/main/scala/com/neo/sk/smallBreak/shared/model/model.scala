package com.neo.sk.smallBreak.shared

/**
  * User: yuwei
  * Date: 2019/2/5
  * Time: 11:34
  */
package object model {
  case class Point(x: Double, y: Double) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def %(other: Point) = Point(x % other.x, y % other.y)

    def <(other: Point) = x < other.x && y < other.y

    def >(other: Point) = x > other.x && y > other.y

    def /(value: Point) = Point(x / value.x, y / value.y)

    def /(value: Float) = Point(x / value, y / value)

    def *(value: Float) = Point(x * value, y * value)

    def *(other: Point) = x * other.x + y * other.y

    def length = Math.sqrt(lengthSquared)

    def lengthSquared = x * x + y * y

    def distance(other: Point) = Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))

    def within(a: Point, b: Point, extra: Point = Point(0, 0)) = {
      import math.{max, min}
      x >= min(a.x, b.x) - extra.x &&
      x < max(a.x, b.x) + extra.y &&
      y >= min(a.y, b.y) - extra.x &&
      y < max(a.y, b.y) + extra.y
    }

    def rotate(theta: Float) = {
      val (cos, sin) = (Math.cos(theta), math.sin(theta))
      Point(cos * x - sin * y, sin * x + cos * y)
    }

    def getTheta(center: Point): Double = {
      math.atan2(y - center.y, x - center.x)
    }

    def getAngle(center: Point): Byte = {
      val y_i=y - center.y
      val x_i=x - center.x
      val angle=math.atan2(y_i, x_i)/3.14*180
      if(y_i<=0){
        ((math.round(angle)/3)%120).toByte
      }else{
        (((360-math.round(angle))/3)%120).toByte
      }
    }

    def in(view: Point, extra: Point) = {
      x >= (0 - extra.x) && y >= (0 - extra.y) && x <= (view.x + extra.x) && y <= (view.y + extra.y)
    }


  }

}

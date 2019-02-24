package com.neo.sk.smallBreak.shared

/**
  *
  * @author liuZhiwei
  *         2016年8月6日 下午3:48:38
  */
object Test {
  /**
    * 是否有 横断<br/>
    * 参数为四个点的坐标
    *
    * @param px1
    * @param py1
    * @param px2
    * @param py2
    * @param px3
    * @param py3
    * @param px4
    * @param py4
    * @return
    */
  def isIntersect(px1: Double, py1: Double, px2: Double, py2: Double, px3: Double, py3: Double, px4: Double, py4: Double): Boolean = {
    var flag = false
    val d = (px2 - px1) * (py4 - py3) - (py2 - py1) * (px4 - px3)
    if (d != 0) {
      val r = ((py1 - py3) * (px4 - px3) - (px1 - px3) * (py4 - py3)) / d
      val s = ((py1 - py3) * (px2 - px1) - (px1 - px3) * (py2 - py1)) / d
      if ((r >= 0) && (r <= 1) && (s >= 0) && (s <= 1)) flag = true
    }
    flag
  }

  /**
    * 目标点是否在目标边上边上<br/>
    *
    * @param px0 目标点的经度坐标
    * @param py0 目标点的纬度坐标
    * @param px1 目标线的起点(终点)经度坐标
    * @param py1 目标线的起点(终点)纬度坐标
    * @param px2 目标线的终点(起点)经度坐标
    * @param py2 目标线的终点(起点)纬度坐标
    * @return
    */
  def isPointOnLine(px0: Double, py0: Double, px1: Double, py1: Double, px2: Double, py2: Double): Boolean = {
    var flag = false
    val ESP = 1e-9 //无限小的正数
    if ((Math.abs(Multiply(px0, py0, px1, py1, px2, py2)) < ESP) && ((px0 - px1) * (px0 - px2) <= 0) && ((py0 - py1) * (py0 - py2) <= 0)) flag = true
    flag
  }

  def Multiply(px0: Double, py0: Double, px1: Double, py1: Double, px2: Double, py2: Double): Double = (px1 - px0) * (py2 - py0) - (px2 - px0) * (py1 - py0)

  /**
    * 判断目标点是否在多边形内(由多个点组成)<br/>
    *
    * @param px        目标点的经度坐标
    * @param py        目标点的纬度坐标
    * @param polygonXA 多边形的经度坐标集合
    * @param polygonYA 多边形的纬度坐标集合
    * @return
    */
  def isPointInPolygon(px: Double, py: Double, polygonXA: List[Double], polygonYA: List[Double]): Boolean = {
    var isInside = false
    val ESP = 1e-9
    var count = 0
    var linePoint1x = .0
    var linePoint1y = .0
    val linePoint2x = 180
    var linePoint2y = .0
    linePoint1x = px
    linePoint1y = py
    linePoint2y = py
    var i = 0
    while ( {
      i < polygonXA.size - 1
    }) {
      val cx1 = polygonXA(i)
      val cy1 = polygonYA(i)
      val cx2 = polygonXA(i + 1)
      val cy2 = polygonYA(i + 1)
      //如果目标点在任何一条线上
      if (isPointOnLine(px, py, cx1, cy1, cx2, cy2)) return true
      //如果线段的长度无限小(趋于零)那么这两点实际是重合的，不足以构成一条线段
//      if (Math.abs(cy2 - cy1) < ESP) continue //todo: continue is not supported
      //第一个点是否在以目标点为基础衍生的平行纬度线
      if (isPointOnLine(cx1, cy1, linePoint1x, linePoint1y, linePoint2x, linePoint2y)) { //第二个点在第一个的下方,靠近赤道纬度为零(最小纬度)
        if (cy1 > cy2) {
          count += 1; count - 1
        }
      }
      else { //第二个点是否在以目标点为基础衍生的平行纬度线
        if (isPointOnLine(cx2, cy2, linePoint1x, linePoint1y, linePoint2x, linePoint2y)) { //第二个点在第一个的上方,靠近极点(南极或北极)纬度为90(最大纬度)
          if (cy2 > cy1) {
            count += 1; count - 1
          }
        }
        else { //由两点组成的线段是否和以目标点为基础衍生的平行纬度线相交
          if (isIntersect(cx1, cy1, cx2, cy2, linePoint1x, linePoint1y, linePoint2x, linePoint2y)) count += 1
        }
      }

      {
        i += 1; i - 1
      }
    }
    if (count % 2 == 1) isInside = true
    isInside
  }
}

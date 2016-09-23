package com.huntdreams.rf.util

import org.apache.spark.ml.linalg.Vectors

/**
  * Util
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/22/16 4:48 PM.
  */
object Util extends Serializable {

  /**
    * Convert String or java.lang.Number to Double.
    *
    * @param x String or java.lang.Number
    * @return Double
    */
  def toDoubleDynamic(x: Any) = x match {
    case s: String => s.toDouble
    case jn: java.lang.Number => jn.doubleValue()
    case _ => throw new ClassCastException("cannot cast to double")
  }

  /**
    * Convert any seq to spark Vector.
    *
    * @param x any
    * @tparam T Vector
    * @return
    */
  def anySeqToSparkVector[T](x: Any) = x match {
    case a: Array[T] => Vectors.dense(a.map(toDoubleDynamic))
    case s: Seq[Any] => Vectors.dense(s.toArray.map(toDoubleDynamic))
    case v: Vector[Any] => v
    case _ => throw new ClassCastException("unsupported class")
  }

  /**
    * Convert a String to Int, if exception is thrown,
    * a default value will be returned.
    *
    * @param s String to convert
    * @param default The default value to be returned.
    * @return
    */
  def toInt(s: String, default: Int): Int = {
    try {
      s.toInt
    } catch {
      case e: Exception => default
    }
  }
}

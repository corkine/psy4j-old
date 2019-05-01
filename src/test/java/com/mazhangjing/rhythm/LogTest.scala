package com.mazhangjing.rhythm

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable
import scala.util.matching.Regex

class LogTest extends FlatSpec with Matchers {
  val log: String = """
                      |2019-04-26 添加了 Sound 纯音的  AudioFunction、AudioMaker 接口，
                      |提供了 SimpleAudioFUnctionMakerTOneUtilsImpl 实现
                      |2019-04-27 添加了日志系统
                    """.stripMargin
  val emptyLog:String = "dsffe"
  val date: Regex = """(\d{4}-\d{2}-\d{2})""".r

  "a Reg" should "work well on array result with result" in {
    val list = date.findAllIn(log).toBuffer.reverse
    val lastDate = if (list.nonEmpty) list.head else "NaN"
    assert("2019-04-27" == lastDate,"最后的日期应该是 27 号")
    assertResult("NaN") {
      val reverse = date.findAllIn(emptyLog).toBuffer.reverse
      val all = if (reverse.nonEmpty) reverse.head else "NaN"
      all
    }
  }

  it should "work well on empty result headOption with regex" in {
    val array = date.findAllIn(log).toArray
    assertResult("2019-04-27") {
      array.reverse.headOption match {
        case None => "NaN"
        case Some(time) => time
      }
    }
    assertResult("NaN") {
      date.findAllIn(emptyLog).toArray.reverse.headOption match {
        case None => "NaN"
        case Some(time) => time
      }
    }
  }

  it should "work well on a version String" in {
    val log: String = """
                        |1.0.0 2019-04-26 添加了 Sound 纯音的  AudioFunction、AudioMaker 接口，
                        |      提供了 SimpleAudioFUnctionMakerTOneUtilsImpl 实现。
                        |1.0.1 2019-04-27 添加了日志系统
                      """.stripMargin
    val version = """(\d+\.\d+\.\d+)""".r
    val str = version.findAllIn(log).toArray.mkString(", ")
    println(str)
  }

  "a Buffer that Empty" should "get the last with exception" in {
    val a = mutable.Buffer[String]()
    intercept[NoSuchElementException] {
      val head = a.reverse.head
      println(head)
    }
    val right = if (a.nonEmpty) a.reverse.head else ""
    assert("" == right)
  }
}

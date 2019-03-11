package com.mazhangjing.lab.data

import java.io.{FileOutputStream, FileWriter, IOException, PrintStream}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, FileVisitor, Files, Path}

import scala.io.Source

/**
  * Psy4J 数据收集工具类
 *
  * @since 1.2.5
  * @note 2019-03-06 撰写本工具类
  *       2019-03-11 修正 getSD 为计算样本而非总体标准差
  */
object DataUtils {
  /**
    * 求标准差
    * 标准差等于每个值减去平均数的平方之和除N，开平方
    */
  val getSD: Array[Double] => Double = data => {
    val length = data.length
    val sum = data.sum
    val avg = sum / length
    val sd = //标准差等于每个数减去平均数，之后求平方，累加，除以 N，开平方
      math.sqrt(data.map(each => {
        val abs = each - avg
        abs * abs
      }).sum / (length - 1))
    sd
  }

  /**
    * 过滤 N 个标准差之内的数据
    */
  def filterInSD(n: Int)
                (data: Array[Double], sd: Double): Array[Double] = {
    val sum = data.sum
    val avg = sum / data.length
    data.filter(i => i < avg + n * sd)
      .filter(i => i > avg - n * sd)
  }

  /**
    * 根据条件遍历文件夹，执行操作
    *
    * @param rootFolder   根目录
    * @param filter       文件过滤器
    * @param op           对过滤后的文件执行操作
    * @param printErrorTo 打印错误到何处，可选 System.err or System.out 或者你想要的流中
    * @return
    */
  def walkAndProcess(rootFolder: Path,
                     filter: Path => Boolean,
                     printErrorTo: PrintStream = System.out)(op: Path => Unit): Path = {
    Files.walkFileTree(rootFolder, new FileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = FileVisitResult.CONTINUE

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        file match {
          case logFile if filter(logFile) =>
            try {
              op(logFile)
            } catch {
              case e: Exception => e.printStackTrace(printErrorTo)
            }
          case _ =>
        }
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
    })
  }

  /**
    * 从文件按照编码读取数据，按照 lineToData 策略转换成为数据结构，之后进行操作
    *
    * @param file    文件
    * @param coding  文件编码
    * @param count   在执行转换前的操作
    * @param convert 执行数据结构转换
    * @param op      对数据结构进行操作
    * @tparam T 数据结构类型
    */
  def doWithFile[T](file: Path,
                    coding: String = "GBK",
                    count: Stream[String] => Unit = null,
                    convert: String => Option[T]
                   )(op: Stream[T] => Unit): Unit = {
    val f = Source.fromFile(file.toFile, coding).getLines().toStream
    if (count != null) count(f)
    val ts = f.filterNot(_.isEmpty).map(convert).collect {
      case Some(t) => t
    }
    op(ts)
  }

  /**
    * 打印 System.out 到文件，如果 real 为否，则不打印，直接输出到默认位置
    *
    * @param file 需要保存输出的文件
    * @param real 是否测试条件 - 需要在控制台看到输出
    * @param op   执行的操作
    */
  def printToFile(file: Path, real: Boolean = true)(op: => Unit): Unit = {
    if (!real) op
    else {
      val old = System.out
      val newOut = new PrintStream(new FileOutputStream(file.toFile))
      System.setOut(newOut)
      op
      newOut.close()
      System.setOut(old)
    }
  }

  /**
    * 将一个 StringBuilder 保存到指定文件中
    * @param file 保存到的文件，覆盖
    * @param op 操作，应该返回一个 StringBuilder
    */
  def saveTo(file: Path)(op: => StringBuilder): Unit = {
    val f = new FileWriter(file.toFile)
    try {
      val sb = op
      f.write(sb.toString())
      f.flush()
    } catch {
      case e: Exception => e.printStackTrace(System.err)
    } finally {
      f.close()
    }
  }
}

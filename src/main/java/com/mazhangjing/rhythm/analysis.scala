package com.mazhangjing.rhythm

import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant

import com.mazhangjing.lab.data.DataUtils
import com.mazhangjing.rhythm.SequenceProcessFactory.newInstance
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

trait SequenceProcess {
  //需要批处理的文件夹
  var handleFiles: Array[File]
  //如何将 record_csv 和 obj 结合起来的策略，注意，可能存在多个 record_csv
  val handleGroup: (File,File) => Boolean
  //遍历文件夹，将多个 record_csv 和 obj 结合起来的动作
  val groupSeeker: (Array[File], (File,File) => Boolean) => Array[(File, Array[File])]
  //将 obj 解析为 Data
  val dataParser: File => ExperimentData
  //将 csv 解析为 Instant
  var csvParser: Array[File] => Array[(Instant, Int)]
  //将 obj 和 record_csv 数据对齐
  var groupAlign: (ExperimentData, Array[(Instant, Int)]) => ExperimentData
  def doProcess(): Array[ExperimentData] =  {
    if (handleFiles == null || handleFiles.length == 0)
      throw new RuntimeException("不存在需要处理的数据")
    if (handleGroup == null || groupSeeker == null || dataParser == null)
      throw new RuntimeException("参数传递不正确")
    val groups = groupSeeker(handleFiles, handleGroup)
    val result = ArrayBuffer[ExperimentData]()
    groups.foreach(group => {
      val data = dataParser(group._1)
      val csvs = csvParser(group._2)
      val res = groupAlign(data, csvs)
      result.append(res)
    })
    result.toArray
  }
/*  def doSingleProcess(objectFile: File, csvFiles: Array[File]): ExperimentData = {
    if (!handleFolder.toFile.exists()) throw new RuntimeException("不存在此文件夹")
    if (handleGroup == null || groupSeeker == null || dataParser == null)
      throw new RuntimeException("参数传递不正确")
    ()
    groups.foreach(group => {
      val data = dataParser(group._1)
      val csvs = csvParser(group._2)
      val res = groupAlign(data, csvs)
    })
  }*/
}

//数据分析会自动对 record.csv 结尾、.obj 结尾的文件进行分析
//自动拼合 11111_record.csv、11111_2_record.csv、11111_mac_record.csv 和 11111.obj
//但不会处理 11111.csv (作为 .obj 的临时查看文件查看)
class SequenceProcessImpl(private[this] val useFakeRecords: Boolean = false) extends SequenceProcess {

  private val logger = LoggerFactory.getLogger(classOf[SequenceProcessImpl])

  override var handleFiles: Array[File] = _

  override val handleGroup: (File, File) => Boolean = (obj, csv) => {
    if (csv.getName.endsWith("record.csv") && obj.getName.endsWith(".obj")) {
      csv.getName.replace(".csv","").split("_").head ==
      obj.getName.replace(".obj","").split("_").head
    } else {  false }
  }

  override val groupSeeker: (Array[File], (File, File) => Boolean) => Array[(File, Array[File])] = (findFrom, fileGet) => {
    def processAndFind(findFrom: Array[File], op: File => Boolean): Array[File] = {
      //val result = ArrayBuffer[File]()
      /*Files.walkFileTree(path,  new FileVisitor[Path] {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          FileVisitResult.CONTINUE
        }

        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          //println("Visiting " + file)
          if (op(file.toFile)) result.append(file.toFile)
          FileVisitResult.CONTINUE
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
          FileVisitResult.TERMINATE
        }
      })*/
      findFrom.filter(op)
      //result.toArray
    }
    val result = ArrayBuffer[(File, Array[File])]()
    val objs = processAndFind(findFrom, file => file.getName.endsWith(".obj"))
    objs.foreach(obj => {
      val csvs = processAndFind(findFrom, file => fileGet(obj, file))
      result.append((obj, csvs))
    })
    result.toArray
  }

  override val dataParser: File => ExperimentData = in => {
    //解析 ExperimentData 数据
    ExperimentData.loadWithObject(in.toPath)
  }

  override var csvParser: Array[File] => Array[(Instant, Int)] = in => {
    val result = ArrayBuffer[(Instant,Option[Int])]()
    in.foreach(file => {
      try {
        val reader = Source.fromFile(file,"UTF-8").getLines().toArray
        val tuples = reader.filter(line => !line.isEmpty && line.contains(",")).collect {
          case li =>
            val str = li.split(", ")
            val ins = Instant.parse(str.head.trim)
            val key = SequenceProcessFactory.parseStrToNumber(str(1).trim)
            (ins, key)
        }
        result ++= tuples
      } catch {
        case e: Throwable => e.printStackTrace(System.err)
      }
    })
    result.collect {
      case (ins, Some(int)) => (ins, int)
    }.toArray
  }

  override var groupAlign: (ExperimentData, Array[(Instant, Int)]) => ExperimentData = (oldData, arrays) => {
    //为每一个 ExperimentData 遍历一次 Instant Array，如果有找到的，则赋值，否则，则跳过
    logger.info("GroupAlign Data Now...")
    val trials = oldData.trialData
    val list = arrays.toList.reverse //逆序，如果找到两个则取时间最后那个

    import collection.JavaConverters._
    val setSize = trials.asScala.toSet.size
    if (trials.size - setSize > 10) logger.warn("GroupAlign Find Duplicate Lines in Trials - Number is " + (trials.size() - setSize))

    var badCount = 0
    if (useFakeRecords) logger.warn("Fake Records In GroupAlign Now.....")

    trials.forEach(trial => {
      //对于数组长度为 0 的 Records，且已经开启 fakeRecord
      if (useFakeRecords) {
        trial.answer = trial.pointNumber
      } else {//对于包含不为 0 长度 Array 的 Records
        try {
          list.find(time => {
            time._1.isAfter(trial.startInstant) && time._1.isBefore(trial.endInstant)
          }) match {
            case None =>
              trial.answer = -1
              badCount += 1
            case Some(time) => trial.answer = time._2
          }
        } catch {
          case e: Throwable => e.printStackTrace(System.err); trial.answer = -1; badCount += 1
        }
      }
    })
    logger.info(s"Alignment Done, Bad Trial Count: $badCount - For Trial Total Number ${trials.size()}")
    oldData
  }
}

object SequenceProcessFactory {

  private[this] val logger = LoggerFactory.getLogger("SequenceProcessFactory")

  private[this] def initSearchFolder(folder:Path): Array[File] = {
    val result = ArrayBuffer[File]()
    Files.walkFileTree(folder,  new FileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        FileVisitResult.CONTINUE
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        result.append(file.toFile)
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        FileVisitResult.TERMINATE
      }
    })
    result.toArray
  }
  val parseStrToNumber: String => Option[Int] = in => {
    try {
      if (in.toUpperCase().contains("DIGIT") || in.toUpperCase().contains("NUMPAD")) {
        val int = in.replace("DIGIT","").replace("NUMPAD","").trim.toInt
        Option(int)
      } else None
    } catch {
      case e: Throwable => e.printStackTrace(System.err); None
    }
  }

  private[this] val newInstance: (Array[File], Boolean) => SequenceProcess = (files, useFakeRecords) => {
    val impl = new SequenceProcessImpl(useFakeRecords)
    impl.handleFiles = files
    impl
  }

  private[this] def newInstance(files: Array[File],
                                adjustTimeSeconds: Int,
                                optionalAdjustTimeSecondsOp: File => Option[Int],
                                fakeRecords: Boolean = false): SequenceProcess = {
    val process = newInstance(files, fakeRecords)
    process.csvParser = in => {
      val result = ArrayBuffer[(Instant,Option[Int])]()
      in.foreach(file => {
        try {
          val time =
            if (optionalAdjustTimeSecondsOp == null) adjustTimeSeconds
            else optionalAdjustTimeSecondsOp(file) match {
              case None => adjustTimeSeconds
              case Some(s) => s
            }
          logger.info(s"adjust time to $time for ${file.getName}")
          val reader = Source.fromFile(file,"UTF-8").getLines().toArray
          val tuples = reader.filter(line => !line.isEmpty && line.contains(",")).collect {
            case li =>
              val str = li.split(", ")
              val ins = {
                val normal = Instant.parse(str.head.trim)
                normal.plusSeconds(time.toLong)
              }
              val key = parseStrToNumber(str(1).trim)
              (ins, key)
          }
          result ++= tuples
        } catch {
          case e: Throwable => e.printStackTrace(System.err)
        }
      })
      result.collect {
        case (ins, Some(int)) => (ins, int)
      }.toArray
    }
    process
  }

  def printToCSV(experimentDataArray: Array[ExperimentData], path: Path): Unit = {
    //ID, HZ, CONDITION, POINT_NUMBER, ANSWER, RT, CORRECT
    val dataToLine: ExperimentData => String = data => {
      val hz = data.prefHz
      val id = data.userId
      val trials = data.trialData
      val sb = new mutable.StringBuilder()
      import collection.JavaConverters._
      trials.asScala.toSet.filter(trial => !trial.isExercise && trial.answer != -1 && trial.answer != 0).foreach(trial => {
        sb.append(id).append(", ")
        sb.append(hz).append(", ")
        sb.append(trial.condition).append(", ")
        sb.append(trial.pointNumber).append(", ")
        sb.append(trial.answer).append(", ")
        sb.append(trial.actionTime).append(", ")
        //sb.append(trial.recordInstant).append(", ")
        sb.append(if (trial.answer == trial.pointNumber) "1" else "0").append("\n")
      })
      sb.toString
    }

    DataUtils.saveTo(path) {
      val builder = new mutable.StringBuilder()
      builder.append("ID, HZ, CONDITION, POINT_NUMBER, ANSWER, RT, CORRECT\n")
      experimentDataArray.foreach(data => {
        val str = dataToLine(data)
        builder.append(str)
      })
      builder
    }
  }

  def newInstance(files: Array[File]): SequenceProcess = newInstance(files,false)

  def newInstanceWithFakeRecords(files: Array[File]): SequenceProcess = newInstance(files, 0, null, fakeRecords = true)

  def newInstance(files: Array[File],
                  adjustTimeSeconds: Int): SequenceProcess =
    newInstance(files, adjustTimeSeconds, null)

  def newInstanceWithFakeRecords(files: Array[File],
                                 adjustTimeSeconds: Int): SequenceProcess =
    newInstance(files, adjustTimeSeconds, null, fakeRecords = true)

  def newInstance(path: Path): SequenceProcess = {
    val impl = new SequenceProcessImpl()
    impl.handleFiles = initSearchFolder(path)
    impl
  }

  def newInstance(path: Path, adjustTimeSeconds: Int): SequenceProcess = newInstance(path, adjustTimeSeconds, null)

  def newInstance(path: Path, adjustTimeSeconds: Int,
                  optionalAdjustTimeSecondsOp: File => Option[Int]): SequenceProcess = newInstance(initSearchFolder(path), adjustTimeSeconds, optionalAdjustTimeSecondsOp)

  def newInstanceWithFakeRecords(path: Path, adjustTimeSeconds: Int,
                                 optionalAdjustTimeSecondsOp: File => Option[Int],
                                 useFakeRecords: Boolean): SequenceProcess = newInstance(initSearchFolder(path), adjustTimeSeconds, optionalAdjustTimeSecondsOp, fakeRecords = true)
}

/**
  * 本 Scala 脚本用来将 Trial 转换为 Condition 格式（对各个 Trial 进行了极端去除、标准差过滤和分组），合并所有被试试次以供 SPSS 分析
  * 其中 Condition 格式提供了一个被试在所有条件下的所有 Trial 的分组信息。
  * 其中 printToCSVWithSPSS 将所有被试的所有 Trial 合并并且导出为 CSV 格式。
  */
object DataProcess {
  val logger: Logger = LoggerFactory.getLogger("DataProcess")
  def main(args: Array[String]): Unit = {
    import java.nio.file.Paths
    import com.mazhangjing.lab.data.DataUtils
    import scala.collection.mutable

    //传入的是没有过滤过极端值、去除三个标准差的数值
    class Condition(val unfilteredData: Array[TrialData]) {
      //获取每种条件的去除极端数据\过滤三个标准差后的数据
      private[this] val filteredData: Array[TrialData] = {
        unfilteredData.collect {
          case i if i.actionTime < 2000 & i.actionTime > 200 => i
        }
      }
      private[this] val in3SDAndFuckFake: Array[Double] => Array[Double] = in => {
        val sd = DataUtils.getSimpleSD(in)
        val mean = in.sum / in.length
        val inSD: Double => Boolean = in => in > mean - 3 * sd && in < mean + 3 * sd
        in.collect {
          case i if inSD(i) && i != 0 => i
        }
      }
      private[this] val fakeTrialData: TrialData = {
        val f = new TrialData(0,0)
        f.actionTime = 0; f
      }

      var c1n2: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 1 && d.pointNumber == 2).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c1n3: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 1 && d.pointNumber == 3).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c1n4: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 1 && d.pointNumber == 4).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c1n5: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 1 && d.pointNumber == 5).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c1n6: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 1 && d.pointNumber == 6).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))

      var c2n2: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 2 && d.pointNumber == 2).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c2n3: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 2 && d.pointNumber == 3).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c2n4: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 2 && d.pointNumber == 4).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c2n5: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 2 && d.pointNumber == 5).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c2n6: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 2 && d.pointNumber == 6).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))

      var c3n2: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 3 && d.pointNumber == 2).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c3n3: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 3 && d.pointNumber == 3).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c3n4: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 3 && d.pointNumber == 4).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c3n5: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 3 && d.pointNumber == 5).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c3n6: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 3 && d.pointNumber == 6).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))

      var c4n2: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 4 && d.pointNumber == 2).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c4n3: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 4 && d.pointNumber == 3).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c4n4: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 4 && d.pointNumber == 4).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c4n5: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 4 && d.pointNumber == 5).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c4n6: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 4 && d.pointNumber == 6).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))

      var c5n2: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 5 && d.pointNumber == 2).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c5n3: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 5 && d.pointNumber == 3).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c5n4: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 5 && d.pointNumber == 4).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c5n5: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 5 && d.pointNumber == 5).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
      var c5n6: Array[Double] = in3SDAndFuckFake(filteredData.groupBy(d => d.condition == 5 && d.pointNumber == 6).getOrElse(true, Array(fakeTrialData)).map(_.actionTime.toDouble))
    }

    //对所有的被试数据进行正确过滤，并且得到条件统计并且输出为文件
    def printToCSVWithSPSS(peoples: Array[ExperimentData]): Unit = {
      println(peoples.map(_.trialData))
      val stats = peoples.map(experimentData => {
        //对于每一个被试，过滤掉非正确的 TrialData
        val allTrialData = experimentData.trialData
        import collection.JavaConverters._
        val rightTrialData = allTrialData.asScala.collect {
          case data if data.answer == data.pointNumber => data
        }
        //使用构建类过滤极端值、去除三个标准差数据
        new Condition(rightTrialData.toArray)
      })
      implicit class SuperArray(in: Array[Double]) {
        def index(i:Int):String = {
          if (in.length > i) {
            in(i).toString
          } else ""
        }
        def avg: Double = {
          in.sum / in.length
        }
      }
      //将结果输出为 csv 文件以备分析
      val c1n2 = stats.map(_.c1n2.avg)
      val c1n3 = stats.map(_.c1n3.avg)
      val c1n4 = stats.map(_.c1n4.avg)
      val c1n5 = stats.map(_.c1n5.avg)
      val c1n6 = stats.map(_.c1n6.avg)
      val c2n2 = stats.map(_.c2n2.avg)
      val c2n3 = stats.map(_.c2n3.avg)
      val c2n4 = stats.map(_.c2n4.avg)
      val c2n5 = stats.map(_.c2n5.avg)
      val c2n6 = stats.map(_.c2n6.avg)
      val c3n2 = stats.map(_.c3n2.avg)
      val c3n3 = stats.map(_.c3n3.avg)
      val c3n4 = stats.map(_.c3n4.avg)
      val c3n5 = stats.map(_.c3n5.avg)
      val c3n6 = stats.map(_.c3n6.avg)
      val c4n2 = stats.map(_.c4n2.avg)
      val c4n3 = stats.map(_.c4n3.avg)
      val c4n4 = stats.map(_.c4n4.avg)
      val c4n5 = stats.map(_.c4n5.avg)
      val c4n6 = stats.map(_.c4n6.avg)
      val c5n2 = stats.map(_.c5n2.avg)
      val c5n3 = stats.map(_.c5n3.avg)
      val c5n4 = stats.map(_.c5n4.avg)
      val c5n5 = stats.map(_.c5n5.avg)
      val c5n6 = stats.map(_.c5n6.avg)

      val c1n23 = c1n2.zip(c1n3).map(a => (a._1 + a._2)/2)
      val c1n45 = c1n4.zip(c1n5).map(a => (a._1 + a._2)/2)
      val c2n23 = c2n2.zip(c2n3).map(a => (a._1 + a._2)/2)
      val c2n45 = c2n4.zip(c2n5).map(a => (a._1 + a._2)/2)
      val c3n23 = c3n2.zip(c3n3).map(a => (a._1 + a._2)/2)
      val c3n45 = c3n4.zip(c3n5).map(a => (a._1 + a._2)/2)
      val c4n23 = c4n2.zip(c4n3).map(a => (a._1 + a._2)/2)
      val c4n45 = c4n4.zip(c4n5).map(a => (a._1 + a._2)/2)
      val c5n23 = c5n2.zip(c5n3).map(a => (a._1 + a._2)/2)
      val c5n45 = c5n4.zip(c5n5).map(a => (a._1 + a._2)/2)

      val c1 = c1n23.zip(c1n45).map(a => (a._1 + a._2)/2)
      val c2 = c2n23.zip(c2n45).map(a => (a._1 + a._2)/2)
      val c3 = c3n23.zip(c3n45).map(a => (a._1 + a._2)/2)
      val c4 = c4n23.zip(c4n45).map(a => (a._1 + a._2)/2)
      val c5 = c5n23.zip(c5n45).map(a => (a._1 + a._2)/2)

      val sameLineCheck: Array[Array[Double]] => Boolean = in => {
        val first = in.head.length
        in.exists(_.length != first)
      }

      if (sameLineCheck(Array[Array[Double]](
        c1n2,c1n3,c1n4,c1n5,c1n6,
        c2n2,c2n3,c2n4,c2n5,c2n6,
        c3n2,c3n3,c3n4,c3n5,c3n6,
        c4n2,c4n3,c4n4,c4n5,c4n6,
        c5n2,c5n3,c5n4,c5n5,c5n6))) throw new RuntimeException("经过计算返回的行不齐，无法进行进一步计算。")

      val builder = new mutable.StringBuilder()
      builder.append("C1N1, C1N2, C1N3, C1N4, C1N5, C2N1, C2N2, C2N3, C2N4, C2N5, C3N1, C3N2, C3N3, C3N4, C3N5, C4N1, C4N2, C4N3, C4N4, C4N5, C5N1, C5N2, C5N3, C5N4, C5N5, C1N12, C1N34, C2N12, C2N34, C3N12, C3N34, C4N12, C4N34, C5N12, C5N34, C1, C2, C3, C4, C5\n")
      for (i <- c1n2.indices) {
        builder.append(c1n2.index(i)).append(", ")
        builder.append(c1n3.index(i)).append(", ")
        builder.append(c1n4.index(i)).append(", ")
        builder.append(c1n5.index(i)).append(", ")
        builder.append(c1n6.index(i)).append(", ")
        builder.append(c2n2.index(i)).append(", ")
        builder.append(c2n3.index(i)).append(", ")
        builder.append(c2n4.index(i)).append(", ")
        builder.append(c2n5.index(i)).append(", ")
        builder.append(c2n6.index(i)).append(", ")
        builder.append(c3n2.index(i)).append(", ")
        builder.append(c3n3.index(i)).append(", ")
        builder.append(c3n4.index(i)).append(", ")
        builder.append(c3n5.index(i)).append(", ")
        builder.append(c3n6.index(i)).append(", ")
        builder.append(c4n2.index(i)).append(", ")
        builder.append(c4n3.index(i)).append(", ")
        builder.append(c4n4.index(i)).append(", ")
        builder.append(c4n5.index(i)).append(", ")
        builder.append(c4n6.index(i)).append(", ")
        builder.append(c5n2.index(i)).append(", ")
        builder.append(c5n3.index(i)).append(", ")
        builder.append(c5n4.index(i)).append(", ")
        builder.append(c5n5.index(i)).append(", ")
        builder.append(c5n6.index(i)).append(", ")

        builder.append(c1n23.index(i)).append(", ")
        builder.append(c1n45.index(i)).append(", ")
        builder.append(c2n23.index(i)).append(", ")
        builder.append(c2n45.index(i)).append(", ")
        builder.append(c3n23.index(i)).append(", ")
        builder.append(c3n45.index(i)).append(", ")
        builder.append(c4n23.index(i)).append(", ")
        builder.append(c4n45.index(i)).append(", ")
        builder.append(c5n23.index(i)).append(", ")
        builder.append(c5n45.index(i)).append(", ")

        builder.append(c1.index(i)).append(", ")
        builder.append(c2.index(i)).append(", ")
        builder.append(c3.index(i)).append(", ")
        builder.append(c4.index(i)).append(", ")
        builder.append(c5.index(i)).append("\n")
      }
      DataUtils.saveTo(Paths.get("result_factors.csv")) {
        builder
      }
    }

    val path = Paths.get("/Users/corkine/工作文件夹/Psy4J/src/test/mzj_data_process/new_far_test")
    val process = SequenceProcessFactory.newInstance(path, 0, file => {
      val getInt: (File, String) => Int = (file, mark) => file.getName
        .toUpperCase.split("_")(1).replace(mark, "").trim.toInt
      if (file.getName.toUpperCase.contains("MAC")) {
        logger.info(s"For file ${file.getName}, Adjust Time for MAC Mark to 0")
        Option(0)
      } else if (file.getName.toUpperCase.contains("EARLY")) {
        val v = getInt(file, "EARLY")
        logger.info(s"For file ${file.getName}, Adjust Time for EARLY$v Mark to -$v")
        Option(-1 * v)
      } else if (file.getName.toUpperCase.contains("LATE")) {
        val v = getInt(file, "LATE")
        logger.info(s"For file ${file.getName}, Adjust Time for LATE$v Mark to $v")
        Option(1 * v)
      } else None
    })
    val datas = process.doProcess()
    printToCSVWithSPSS(datas)
  }
}

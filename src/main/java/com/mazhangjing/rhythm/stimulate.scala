package com.mazhangjing.rhythm

import java.io._
import java.nio.file.{Path, Paths}
import java.time.{Instant, LocalDateTime}
import java.util
import java.util.UUID

import MzjExperiment._
import com.sun.xml.internal.ws.developer.Serialization
import javafx.scene.paint.Color
import javafx.scene.shape.{Circle, Rectangle}
import org.yaml.snakeyaml.Yaml

import scala.beans.BeanProperty
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
//节律条件、点的个数
class TrialData(@BeanProperty var condition: Int, @BeanProperty var pointNumber: Int)
  extends Serializable {
  @BeanProperty var isExercise = false
  @BeanProperty var actionTime: Long = _ //反应时
  @BeanProperty var taskCondition: String = _ //感数、计数
  @BeanProperty var recordInstant: Instant = _
  @BeanProperty var startInstant: Instant = _
  @BeanProperty var endInstant: Instant = _
  @BeanProperty var answer: Int = _

  override def toString: String = {
    s"[TrialData] Task $taskCondition, Condition $condition, PointNumber $pointNumber, ActionTime $actionTime"
  }
}

class ExperimentData extends Serializable {
  @BeanProperty var prefHz: Double = _
  @BeanProperty var trialData: util.List[TrialData] = new util.ArrayList[TrialData]()
  @BeanProperty var userName: String = _
  @BeanProperty var userId: Int = Random.nextInt(10000) + 23333
  @BeanProperty var gender: Int = _
  @BeanProperty var information: String = _

  override def toString: String = {
    s"[ExperimentData] Name: $userName - Id: $userId - PrefHz $prefHz, Gender: $gender, DataSize ${trialData.size()} - $trialData"
  }
}

object ExperimentData {
  import collection.JavaConverters._
  def persistToYAML(file: Path, data: ExperimentData): Unit = {
    val yaml = new Yaml()
    val where = new PrintWriter(file.toFile)
    val expWrapper = new ExperimentDataWrapper
    expWrapper.userName = data.userName
    expWrapper.userId = data.userId
    expWrapper.gender = data.gender
    expWrapper.information = data.information
    val trialWrapper = data.trialData.asScala.map(td => {
      val a = new TrialDataWrapper()
      a.condition = td.condition
      a.pointNumber = td.pointNumber
      a.taskCondition = td.taskCondition
      a.actionTime = td.actionTime
      a.recordInstant = Option(td.recordInstant).getOrElse("").toString
      a.startInstant = Option(td.startInstant).getOrElse("").toString
      a.endInstant = Option(td.endInstant).getOrElse("").toString
      a
    })
    expWrapper.trialData = trialWrapper.asJava
    yaml.dump(expWrapper, where)
    where.close()
  }
  def loadWithYML(file: Path): ExperimentData =  {
    val yaml = new Yaml()
    val in = new FileReader(file.toFile)
    val wrapper = yaml.loadAs(in, classOf[ExperimentDataWrapper])
    val data = new ExperimentData()
    data.userName = wrapper.userName
    data.userId = wrapper.userId
    data.gender = wrapper.gender
    data.information = wrapper.information
    val datas = wrapper.trialData.asScala.map(tWrapper => {
      val t = new TrialData(tWrapper.condition, tWrapper.pointNumber)
      t.actionTime = tWrapper.actionTime
      t.taskCondition = tWrapper.taskCondition
      t.recordInstant = if (tWrapper.recordInstant.isEmpty) null else Instant.parse(tWrapper.recordInstant)
      t.startInstant = if (tWrapper.startInstant.isEmpty) null else Instant.parse(tWrapper.startInstant)
      t.endInstant = if (tWrapper.endInstant.isEmpty) null else Instant.parse(tWrapper.endInstant)
      t
    })
    data.trialData = datas.asJava
    data
  }
  def persistToCSV(file: Path, data: ExperimentData): Unit = {
    //META-INFO 开头以 ||| 表示，其余正常输出 CSV
    val sb = new StringBuilder()
    sb.append("|||USERNAME: " + data.userName + "\n")
    sb.append("|||USERID: " + data.userId + "\n")
    sb.append("|||GENDER: " + data.gender + "\n")
    sb.append("|||INFORMATION: " + data.information + "\n")
    sb.append("|||PREF_HZ: " + data.prefHz + "\n")
    sb.append("CONDITION, POINT_NUMBER, ACTION_TIME, TASK_CONDITION, RECORD_INSTANT, START_INSTANT, END_INSTANT, ANSWER, IS_PRE\n")
    data.trialData.forEach(trialData => {
      sb.append(trialData.condition + ", ")
      sb.append(trialData.pointNumber + ", ")
      sb.append(trialData.actionTime + ", ")
      sb.append(trialData.taskCondition + ", ")
      sb.append(trialData.recordInstant + ", ")
      sb.append(trialData.startInstant + ", ")
      sb.append(trialData.endInstant + ", ")
      sb.append(trialData.answer + ", ")
      sb.append(trialData.isExercise)
      sb.append("\n")
    })
    val out = new PrintWriter(file.toFile)
    out.print(sb.toString())
    out.flush()
    out.close()
  }
  def persistToObject(file: Path, data: ExperimentData): Unit = {
    val fileStream = new FileOutputStream(file.toFile)
    val stream = new ObjectOutputStream(fileStream)
    stream.writeObject(data)
    stream.flush()
    stream.close()
    fileStream.close()
  }
  def loadWithObject(file: Path): ExperimentData = {
    val fileInputStream = new FileInputStream(file.toFile)
    val stream = new ObjectInputStream(fileInputStream)
    val data = stream.readObject().asInstanceOf[ExperimentData]
    stream.close()
    fileInputStream.close()
    data
  }
}

//方便在 Java 和 Scala 读写，保持类名稳定，
//因此不作为 persistWithYML 内部类，也不作为 ExperimentData 内部类
class TrialDataWrapper {
  @BeanProperty var condition: Int = _
  @BeanProperty var pointNumber: Int = _
  @BeanProperty var actionTime: Long = _ //反应时
  @BeanProperty var taskCondition: String = _ //感数、计数
  @BeanProperty var recordInstant: String = _
  @BeanProperty var startInstant: String = _
  @BeanProperty var endInstant: String = _

  override def toString: String = {
    s"$taskCondition, $endInstant, $condition "
  }
}

class ExperimentDataWrapper {
  @BeanProperty var trialData: util.List[TrialDataWrapper] = new util.ArrayList[TrialDataWrapper]()
  @BeanProperty var userName: String = _
  @BeanProperty var userId: Int = Random.nextInt(100) + 23333
  @BeanProperty var gender: Int = _
  @BeanProperty var information: String = _

  override def toString: String = {
    s"$trialData, $userName, $userId"
  }
}

class Points(allPointNumber: Int = 10, blackPointNumber: Int, pointRadius: Int = 12,
             XRange: (Int, Int), YRange: (Int, Int), isReal: Boolean = true, marginPixel: Int = 5) {
  var points: ArrayBuffer[Circle] = ArrayBuffer[Circle]()
  //先处理位置问题
  val centerXRange: (Int, Int) = (XRange._1 + pointRadius, XRange._2 - pointRadius)
  val centerYRange: (Int, Int) = (YRange._1 + pointRadius, YRange._2 - pointRadius)
  1 to allPointNumber foreach( _ => {
    val circle = new Circle()
    circle.setRadius(pointRadius)
    circle.setFill(Color.WHITE)
    if (isReal) {
      var x = 0; var y = 0
      var conflict = true
      while (conflict) {
        x = Random.nextInt(centerXRange._2 - centerXRange._1) + centerXRange._1
        y = Random.nextInt(centerYRange._2 - centerYRange._1) + centerYRange._1
        conflict = points.exists(point => {
          val pX = point.getCenterX
          val pY = point.getCenterY
          Math.abs(pX - x) < pointRadius + marginPixel || Math.abs(pY - y) < pointRadius + marginPixel
          //Math.sqrt(Math.pow(Math.abs(y - pY), 2) * Math.pow(Math.abs(x - pX),2)) < pointRadius
        })
      }
      assert(x != 0); assert(y != 0)
      circle.setCenterX(x)
      circle.setCenterY(y)
    } else {
      circle.setCenterX(20)
      circle.setCenterY(20)
    }
    points.append(circle)
  })
  1 to blackPointNumber foreach(i => {
    points(i).setFill(Color.BLACK)
  })
  points = Random.shuffle(points)
}

object TestPoints {
  def main(args: Array[String]): Unit = {
    val points = new Points(10,3,10,(0,1000),(0,1000)).points
    println(points)
  }
}

//如果要使用 YAML，最好使用 JavaBean，实在不行，要用 Scala Bean，构造器可以，但是所有需要保存的属性都不能写成 val
//此外，必须为其添加 @BeanProperty 注解
object TestYAML {
  def main(args: Array[String]): Unit = {
    val experimentData = new ExperimentData()
    experimentData.setUserId(233)
    experimentData.setUserName("Corkine")
    experimentData.setInformation("Nothing")
    experimentData.setGender(1)
    val trialdata = new TrialData(1,23)
    trialdata.setActionTime(23333333333L)
    trialdata.setStartInstant(Instant.now())
    trialdata.setEndInstant(Instant.now())
    experimentData.getTrialData.add(trialdata)
    val name = experimentData.userName
    val id = experimentData.userId
    val str = name + "_" + id + ".yml"
    val path = Paths.get(str)
    ExperimentData.persistToYAML(path, experimentData)
    val data = ExperimentData.loadWithYML(path)
    println(data)
    ExperimentData.persistToCSV(Paths.get("corkine_233.csv"), data)
    ExperimentData.persistToObject(Paths.get("corkine_233.obj"),data)
    val data2 = ExperimentData.loadWithObject(Paths.get("corkine_233.obj"))
    println(data2)
  }
}
package com.mazhangjing.rhythm

import java.nio.file.Paths
import java.time.Instant

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
package com.mazhangjing.lab

import javafx.scene.Scene

/**
  * 相比较 Screen 的抽象，此特质继承了 Screen，添加了为隐式值准备的两个函数，方便代码开发
  * 可以在 Java 中直接继承 Screen，或者在 Scala 中直接继承 Screen 抽象类、或者在 Scala
  * 中继承 ScreenAdaptor 特质以获得更为简洁的语法
  */
trait ScreenAdaptor extends Screen {
  implicit def exp: Experiment = getExperiment
  implicit def sce: Scene = getScene
}

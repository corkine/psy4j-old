package com.mazhangjing.demo

import java.util

import com.mazhangjing.lab.LabUtils._
import com.mazhangjing.lab._
import javafx.geometry.Pos
import javafx.scene.input.KeyCode
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text}

class CmExperiment extends Experiment {
  /** 你应该在此实现并且传递一组 Trial 类型的数据到 trials 变量中。 */
  override protected def initExperiment(): Unit = {
    this.trials.add(new TrialA().initTrial())
  }

  /** 实现此方法来通过 getTrial 方法获取 Trial 对象，通过 getUserData 来保存数据到文件。一般应该对GUI关闭事件添加监听器完成此步骤 */
  override def saveData(): Unit = {
    println("On Saving Data")
  }
}

class TrialA extends Trial {
  /** 你应该实现这个方法，用来保存一组 Screen 对象到 screens 中，保存 Trial 的信息到 information 中。 */
  override def initTrial(): Trial = {
    val fixScreen = ScreenBuilder.named("Fixed Screen").showIn(1000).setScene(() => {
      val pane = new BorderPane()
      val fix = new Text("+")
      fix.setFont(Font.font(50))
      pane.setCenter(fix)
      pane
    }).build()
    val stiScreen = ScreenBuilder.named("Sti Screen").showIn(5000).setScene(() => {
      val box = new HBox()
      box.setAlignment(Pos.CENTER)
      box.setSpacing(200)
      val left = new Text("StiA"); left.setFont(Font.font(50))
      val right = new Text("StiB"); right.setFont(Font.font(50))
      box.getChildren.addAll(left,right)
      box
    }).beforeShowThen((exp, sce) => {
      println(s"Before Screen Show You can get $exp and $sce")
    }).afterShowThen((exp, sce) => {
      println(s"After Screen Show You can get $exp and $sce")
    }).ifEventThen((event, exp, sce) => {
      println("Now I have Event In My Sti Screen")
      ifKeyButton(KeyCode.F) {
        (exp, sce) => {
          println("Now I get KeyCode F, go Next Now")
          goNextScreenUnSafe(exp)
        }
      }(event, exp, sce)
      ifKeyButton(KeyCode.J) {
        (exp, sce) => {
          val box = new HBox()
          box.setAlignment(Pos.CENTER)
          val x = new Text("x"); x.setFont(Font.font(50)); x.setFill(Color.RED)
          box.getChildren.add(x)
          sce.setRoot(box)
        }
      }(event, exp, sce)
    }).build()
    screens.add(fixScreen)
    screens.add(stiScreen)
    this
  }
}

class CmExpRunner extends ExpRunner {
  /**
    * 在其中初始化 eventMakerSet， openedEventSet， experiment，title，version，logs，fullScreen
    */
  override def initExpRunner(): Unit = {
    setEventMakerSet(null)
    val set = new util.HashSet[OpenedEvent]()
    set.add(OpenedEvent.KEY_PRESSED)
    setOpenedEventSet(set)
    setExperimentClassName("com.mazhangjing.demo.CmExperiment")
    setTitle("DEMO TITLE")
    setVersion("0.0.1")
    setFullScreen(true)
  }
}
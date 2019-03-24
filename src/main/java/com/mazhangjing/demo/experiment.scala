package com.mazhangjing.demo

import java.util

import com.mazhangjing.lab.LabUtils._
import com.mazhangjing.lab._
import javafx.event.Event
import javafx.geometry.Pos
import javafx.scene.Scene
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
    }).ifEventThen((event, exp, sce, _) => {
      implicit val getEvent:Event = event
      implicit val experiment:Experiment = exp
      implicit val scene:Scene = sce
      println("Now I have Event In My Sti Screen")
      ifKeyButton(KeyCode.F) {
        (exp, _) => {
          println("Now I get KeyCode F, go Next Now")
          goNextScreenUnSafe(exp)
        }
      }
      ifKeyButton(KeyCode.J) {
        (_, sce) => {
          val box = new HBox()
          box.setAlignment(Pos.CENTER)
          val x = new Text("x"); x.setFont(Font.font(50)); x.setFill(Color.RED)
          box.getChildren.add(x)
          sce.setRoot(box)
        }
      }
    }).build()
    screens.add(fixScreen)
    screens.add(stiScreen)
    screens.add(fixScreen) //这里重复使用了 fixScreen，没有关系，因为绝对不会在一个 Scene 中 root 为两个 Screen 出现
    screens.add(new SingleScreen().initScreen())
    this
  }
}

/**
  * 如果一个 Screen 实例过大，比如需要对很多事件进行处理，需要进行复杂的交互，那么使用单独的 Screen 类管理更为方便
  * 如果使用 Scala，直接继承 ScreenAdaptor 特质，其提供了几个便于 LabUtils 类函数使用的隐式值，除此之外，和 Screen
  * 抽象类完全相同。如果使用 Java，那么直接继承 Screen 即可，因为 Java 中的 implicit 均会被转换成入参。
  */
class SingleScreen extends ScreenAdaptor {

  /** 你应该在子类自行实现 Screen 的 initScreen 方法，为 layout 和 duration 变量赋值，设置子类状态 */
  override def initScreen(): Screen = {
    duration = 5000
    infomation = "SingleScreen"
    val pane = new BorderPane()
    val text = new Text("Hello, World"); text.setFont(Font.font(60)); text.setFill(Color.RED)
    pane.setCenter(text)
    layout = pane
    this
  }

  /**
    * 你应该在这里处理当呈现 Screen 时的鼠标和键盘事件，并且使用 terminal 变量标记此类已经完成使命。
    *
    * @param event      JavaFX 的 Event 事件
    * @param experiment Experiment 类型变量，你可以调用 .terminal 来获得 terminal 以控制定时器行为，你可以调用 .setData 来保存结果
    * @param scene      Scene 类型变量，你可以调用它来绘制 GUI，用于给被试反馈*/
  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = {
    ifKeyButton(KeyCode.A, event) {
      println("A Pressed")
      goNextScreenUnSafe //使用了隐式 def 值，是否为空呢？ 否
    }
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
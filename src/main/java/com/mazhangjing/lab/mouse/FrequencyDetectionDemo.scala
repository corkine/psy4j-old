package com.mazhangjing.lab.mouse

import java.util.concurrent.TimeUnit

import javafx.application.Application
import javafx.beans.property.{SimpleBooleanProperty, SimpleDoubleProperty, SimpleIntegerProperty, SimpleStringProperty}
import javafx.concurrent.Task
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.layout.{HBox, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.{Font, Text}
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

class FrequencyDetectionDemo extends Application {

  val freq = new SimpleDoubleProperty(0.0)

  var showWarn = new SimpleBooleanProperty(false)

  var inAngel = false

  var warnWillDisappearSoon = false

  val isClocking = new SimpleBooleanProperty(false)

  var count = new SimpleIntegerProperty(0)

  var start = 0.0

  val root: Parent = {
    val f = new HBox()
    f.setAlignment(Pos.CENTER_LEFT)
    f.setPadding(new Insets(0,0,0,40))
    f.setSpacing(20)
    val angle = new Rectangle()
    angle.setFill(Color.DARKGRAY)
    angle.setWidth(80)
    angle.setHeight(100)
    angle.setArcHeight(10)
    angle.setArcWidth(10)
    isClocking.addListener(e => {
      //如果开始计时，那么设置 Angel 为绿色，否者设置为红色。
      //每次改变状态，都重置开始时间，重置计数器
      if (isClocking.get()) angle.setFill(Color.GREEN)
      else angle.setFill(Color.GRAY)
      start = System.nanoTime()
      count.set(0)
    })
    count.addListener(e => {
      //当计数发生变化，那么如果在计数，则重新计算频率，否则将频率设置为 0
      if (isClocking.get())
        freq.set(count.get()/((System.nanoTime() - start)/1000000000))
      else
        freq.set(0.0)
    })
    val text = new Text()
    text.setFont(Font.font("STHeiti", 15))
    text.setText("请将鼠标指针移入灰色方框，并且保持不动，之后按照自然频率敲击触摸板。")
    val newText = new Text("按下空格开始，再次按下空格停止")
    newText.setLineSpacing(7)
    newText.setFont(Font.font("STHeiti",13))
    newText.setFill(Color.DARKGRAY)
    text.setLineSpacing(7)
    text.setWrappingWidth(300)
    val info = new Label("频率： Hz")
    val warn = new Label("请保持鼠标位置不动！"); warn.setTextFill(Color.RED)
    warn.visibleProperty().bind(showWarn)
    showWarn.addListener(e => {
      //当显示为 true 的时候才处理
      if (showWarn.get()) {
        //println("ShowWarn Changed and current is true")
        if (warnWillDisappearSoon) {} else {
          //先阻断其余事件
          //println("WarnNot WIll Disappear soon")
          warnWillDisappearSoon = true
          val task = new Task[Double]() {
            override def call(): Double = {
              TimeUnit.SECONDS.sleep(3)
              //在 3s 后消失此红色提示
              showWarn.set(false)
              warnWillDisappearSoon = false
              0.0
            }
          }
          new Thread(task).start()
        }
      }
    })
    info.textProperty().bind(new SimpleStringProperty("频率：").concat(freq.asString("%.3f")).concat(" Hz"))
    val vbox = new VBox()
    vbox.setSpacing(10)
    vbox.setAlignment(Pos.CENTER_LEFT)
    val hbox = new HBox()
    hbox.getChildren.addAll(info, warn)
    hbox.setSpacing(10)
    vbox.getChildren.addAll(text, newText, hbox)
    angle.setOnMouseMoved(e => {
      if (!inAngel) inAngel = true
      else {
        //println("In move, show Warn now...")
        //触发 Listener 调用
        showWarn.set(false)
        showWarn.set(true)
      }
      e.consume()
    })
    angle.setOnMouseClicked(e => {
      //如果在计数，那么设置计数器，反之什么也不做
      if (isClocking.get()) count.set(count.get() + 1)
      e.consume()
    })

    f.getChildren.addAll(angle, vbox)
    f
  }

  val scene: Scene = new Scene(root, 500, 300)

  scene.setOnKeyReleased(e => {
    e.getCode match {
      case KeyCode.SPACE =>
        if (isClocking.get()) isClocking.set(false)
        else isClocking.set(true)
      case _ =>
    }
  })

  override def start(stage: Stage): Unit = {
    stage.setTitle("Frequency Detection - A Part of Psy4J")
    stage.setScene(scene)
    stage.show()
  }
}

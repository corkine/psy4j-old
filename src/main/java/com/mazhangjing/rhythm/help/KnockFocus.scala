package com.mazhangjing.rhythm.help

import java.util.concurrent.TimeUnit

import javafx.application.{Application, Platform}
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.stage.Stage

import scala.util.Random

class KnockFocus extends Application {

  //一个中间变量，避免 UI 组件和多个线程之间的耦合
  //同时这种属性可以被多个监听者监听，具有扩展性
  val text = new SimpleStringProperty("+")

  val task: Runnable = () => {
    while (true) {
      TimeUnit.MILLISECONDS.sleep(Random.nextInt(2000) + 100)
      Platform.runLater(() => {
        //尽可能的不要使用变量，含义不仅仅是不要使用 var，var 可被在多个地方改变
        //其中一个隐含的说明是，不要过多的依赖函数闭包
        //这样的话，task 可以被很方便的重构，移动到任意位置
        if (text.get() == "+") text.set("")
        else text.set("+")
      })
    }
  }

  override def start(primaryStage: Stage): Unit = {
    val label = new Label()
    label.setFont(Font.font(60))
    label.setTextFill(Color.RED)
    label.textProperty().bind(text)
    val g = new HBox()
    g.setAlignment(Pos.CENTER)
    g.getChildren.addAll(label)
    primaryStage.setTitle("Helper")
    primaryStage.setScene(new Scene(g, 400, 300))
    primaryStage.setFullScreen(true)
    primaryStage.setFullScreenExitHint("")
    primaryStage.show()

    new Thread(task).start()
  }
}

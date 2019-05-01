package com.mazhangjing.demo

import javafx.application.Application
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Pos
import javafx.scene.control.{Button, ColorPicker, Label, TextField}
import javafx.scene.layout.{GridPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.{Circle, Rectangle}
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

import scala.collection.mutable

class SimpleCircle extends Application {

  val circleRadiusForArea: Double => Double = area => math.sqrt(area / math.Pi)
  val squareRadiusForArea: Double => Double = area => math.sqrt(area)
  val circleRadiusForRound: Double => Double = round => round / math.Pi / 2
  val squareRadiusForRound: Double => Double = round => round / 4
  val circleRound: Double => Double = radius => radius * 2 * math.Pi

  val color: mutable.Buffer[Color] = mutable.Buffer()
  val area = new SimpleIntegerProperty(15000)

  def getScene: Scene = {
    val root: Parent = {
      val gridPane = new GridPane()
      gridPane.setAlignment(Pos.CENTER)
      val area = this.area.getValue.toDouble
      val round = circleRound(circleRadiusForArea(area))
      for (i <- color.indices) {
        gridPane.add(getSameAreaShape(area, round, circle =>
          circle.setFill(color(i)), rect => rect.setFill(color(i))), 0,i,1,1)
      }
      gridPane.setVgap(40)
      gridPane
    }
    val scene = new Scene(root, 900, 800)
    scene
  }

  val getIntroScene: Scene = {
    val root: Parent = {
      val gridPane = new GridPane()
      gridPane.setAlignment(Pos.CENTER)
      gridPane.setHgap(15)
      gridPane.setVgap(15)
      val picker = new ColorPicker()
      val btn = new Button("确定")
      btn.setOnAction(_ => {
        color.clear()
        color.append(picker.getValue)
        val n = new Stage()
        n.setScene(getScene)
        n.show()
      })
      val h = new HBox(); h.setSpacing(10)
      h.getChildren.addAll(picker, btn)
      gridPane.add(h, 0,0)
      val text = new TextField()
      text.setPromptText("输入你想要自定义的面积")
      text.textProperty().addListener((e, o, n) => {
        if (!text.getText.isEmpty) area.set(text.getText.toInt)
      })
      gridPane.add(text, 0,1)
      gridPane
    }
    new Scene(root, 400, 300)
  }

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("Demo")
    primaryStage.setScene(getIntroScene)
    primaryStage.show()
  }

  /**
    * 生成指定面积的圆和正方形
    * @param area 面积大小，单位像素
    */
  private def getSameAreaShape(area: Double, round: Double, circleOp: Circle => Unit = null,
                               rectOp: Rectangle => Unit = null): Parent = {
    val hbox = new HBox()
    hbox.setAlignment(Pos.CENTER)
    hbox.setSpacing(60)
    val circle = new Circle()
    circle.setRadius(circleRadiusForArea(area))
    val rect = new Rectangle()
    rect.setHeight(squareRadiusForArea(area))
    rect.setWidth(squareRadiusForArea(area))
    val rect2 = new Rectangle()
    val height = squareRadiusForRound(round)
    rect2.setHeight(height)
    rect2.setWidth(height)
    if (circleOp != null) circleOp(circle)
    if (rectOp != null) {
      rectOp(rect); rectOp(rect2)
    }
    hbox.getChildren.addAll(circle, rect, rect2)
    hbox
  }
}

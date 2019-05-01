package com.mazhangjing.rhythm.help

import com.mazhangjing.rhythm.ExperimentData
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.event.{Event, EventType}
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.{DragEvent, Dragboard}
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font

object Utils {

  /**
    * 用來解決 JavaFx addEventHandler 时的泛型问题
    * @param node 为 javafx.scene.Node 添加一个用于派发事件的方法
    */
  implicit class SuperNode(node: Node) {
    def handle[T <: Event](eventType: EventType[T], op: T => Unit): Unit =
      node.addEventHandler(eventType, (event: T) => op(event))
  }

  implicit class SuperProperty[T](observableValue: ObservableValue[T]) {
    def addListener(op: => Unit): Unit = {
      val changedListener: ChangeListener[T] = (e, o, n) => op
      observableValue.addListener(changedListener)
    }
  }

  def getDragPane(doWithDragBoardOver: (Dragboard, DragEvent) => Unit,
                  doWithDragBoardDropped: (Dragboard, DragEvent) => Unit): StackPane = {
    val label = new Label("⤵")
    label.setTextFill(Color.WHITE)
    label.setFont(Font.font(70))

    val border = new Rectangle()
    border.setFill(Color.DARKGRAY)
    border.setArcHeight(60)
    border.setArcWidth(60)
    border.setWidth(150)
    border.setHeight(border.getWidth)

    val st = new StackPane()
    st.getChildren.addAll(border, label)

    st.setOnDragOver((event: DragEvent) => {
      label.setText("◉")
      val dragboard = event.getDragboard
      doWithDragBoardOver(dragboard, event)
    })

    st.setOnDragExited((ev: DragEvent) => {
      label.setText("︎⤵")
    })

    st.setOnDragDropped((event: DragEvent) => {
      val dragboard = event.getDragboard
      doWithDragBoardDropped(dragboard, event)
    })

    st
  }

  def getStatus(experimentData: ExperimentData, withStatus: Boolean = true): String = {
    import collection.JavaConverters._
    val id = experimentData.userId
    val name = experimentData.userName
    val hz = experimentData.prefHz
    val allSize = experimentData.trialData.size()
    val data = experimentData.trialData.asScala
    val condition1 = data.count(_.condition == 1)
    val condition2 = data.count(_.condition == 2)
    val condition3 = data.count(_.condition == 3)
    val condition4 = data.count(_.condition == 4)
    val condition5 = data.count(_.condition == 5)
    val cNRight: Int => (Int, Int, Int) = i => (
      data.count(d => (d.answer == d.pointNumber) && d.condition == i),
      data.count(d => d.answer != d.pointNumber && d.answer != 0 && d.answer != -1 && d.condition == i),
      data.count(d => (d.answer == 0 || d.answer == -1) && d.condition == i)
    )
    if (withStatus) {
      val rData = data.filter(d => d.answer != 0 && d.answer != -1)
      val usefulSize = rData.size
      val nUsefulSize = data.size - rData.size
      val avgActionTime = rData.map(_.actionTime).sum * 1.0/rData.size
      val avgRatio = rData.count(d => d.answer == d.pointNumber) * 1.0/rData.size

      s"For $id, $name, and PrefHz: $hz, Total $allSize，Used $usefulSize, nUsed $nUsefulSize。\n" +
        s"Condition1: $condition1, r/n/m${cNRight(1)}, Condition2: $condition2, r${cNRight(2)}, " +
        s"Condition3: $condition3, r${cNRight(3)}, Condition4: $condition4, r${cNRight(4)}, Condition5: $condition5, r${cNRight(5)}\n" +
        s"AVG_ACTION_TIME: $avgActionTime, AVG_RIGT_RATIO: $avgRatio\n\n"
    } else {
      s"For $id, $name, and PrefHz: $hz, Total $allSize\n" +
        s"Condition1: $condition1, Condition2: $condition2, " +
        s"Condition3: $condition3, Condition4: $condition4, " +
        s"Condition5: $condition5"
    }
  }
}
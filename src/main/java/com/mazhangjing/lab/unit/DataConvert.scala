package com.mazhangjing.lab.unit

import java.awt.GraphicsEnvironment

import javafx.application.Application
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.layout.{GridPane, HBox, VBox}
import javafx.scene.{Parent, Scene}
import javafx.stage.{Screen, Stage}

class DataConvert extends Application {

  val convertPtD = new Button("Convert Pixel to Deg")

  val convertDtP = new Button("Convert Deg to Pixel")

  val screen = iScreen(0.0,0.0,0.0)

  val screenPX = new TextField()

  val currentScreenInfo = new Label("当前屏幕信息")

  val screenCM = new TextField()

  val peopleToScreenCM = new TextField()

  val objectPx = new TextField()

  val objectDeg = new TextField()

  val root: Parent = {
    val box = new VBox()
    val pane = new GridPane()

    screenCM.setPromptText("比如 30cm 输入 30")
    screenPX.setPromptText("比如 1024px 输入 1024")
    peopleToScreenCM.setPromptText("比如 57cm 输入 57")

    pane.add(new Label("屏幕的长边的厘米数"), 0,0)
    pane.add(screenCM, 1,0)
    pane.add(new Label("屏幕的长边的像素数"), 0,1)
    pane.add(screenPX, 1,1)
    pane.add(new Label("人距离屏幕的距离（厘米）"), 0,2)
    pane.add(peopleToScreenCM, 1,2)
    pane.add(new Label("此屏幕的信息"), 0, 3)
    pane.add(currentScreenInfo, 1,3)
    pane.add(new Label("屏幕上物体的像素数"),0,4)
    pane.add(objectPx,1,4)
    pane.add(new Label("屏幕上物体的度数"), 0,5)
    pane.add(objectDeg, 1, 5)
    pane.setVgap(13)
    pane.setAlignment(Pos.CENTER)

    val hbox = new HBox(); hbox.getChildren.addAll(convertPtD, convertDtP)
    hbox.setSpacing(10); hbox.setAlignment(Pos.CENTER)
    pane.add(hbox, 1, 7)
    box.getChildren.addAll(pane)
    box.setAlignment(Pos.CENTER)
    box.setSpacing(20)
    box.setPadding(new Insets(0,30,0,30))

    convertPtD.setOnAction(_ => {
      if (screenCM.getText.isEmpty ||
        screenPX.getText.isEmpty ||
        peopleToScreenCM.getText.isEmpty ||
        objectPx.getText.isEmpty) {
        objectPx.setPromptText("请输入需要转换成度数的像素数")
        objectDeg.setPromptText("")
        objectPx.clear()
      } else {
        val s_cm = screenCM.getText.trim.toDouble
        val s_px = screenPX.getText.trim.toDouble
        val p_to_s_cm = peopleToScreenCM.getText.trim.toDouble
        val o_px = objectPx.getText.trim.toDouble
        objectDeg.setText(screen.init(s_px, s_cm, p_to_s_cm).pxInScreenToDeg(o_px).toString)}
    })
    convertDtP.setOnAction(_ => {
      if (screenCM.getText.isEmpty ||
        screenPX.getText.isEmpty ||
        peopleToScreenCM.getText.isEmpty ||
        objectDeg.getText.isEmpty) {
        objectDeg.setPromptText("请输入需要转换成像素的度数")
        objectPx.setPromptText("")
        objectDeg.clear()
      } else {
        val s_cm = screenCM.getText.trim.toDouble
        val s_px = screenPX.getText.trim.toDouble
        val p_to_s_cm = peopleToScreenCM.getText.trim.toDouble
        val o_deg = objectDeg.getText.trim.toDouble
        objectPx.setText(screen.init(s_px, s_cm, p_to_s_cm).degToScreenPx(o_deg).toString)}
    })
    objectPx.setOnKeyPressed(e => {
      objectDeg.clear()
    })
    objectDeg.setOnKeyPressed(e => {
      objectPx.clear()
    })
    objectPx.setOnMousePressed(e => {
      objectDeg.clear()
      objectDeg.setPromptText("")
      objectPx.selectAll()
    })
    objectDeg.setOnMousePressed(e => {
      objectPx.clear()
      objectPx.setPromptText("")
      objectDeg.selectAll()
    })
    objectPx.textProperty().addListener((e,o,n) => {
      if (!n.isEmpty && objectPx.isFocused) {
        if (screenCM.getText.isEmpty ||
          screenPX.getText.isEmpty ||
          peopleToScreenCM.getText.isEmpty) {} else {
          val s_cm = screenCM.getText.trim.toDouble
          val s_px = screenPX.getText.trim.toDouble
          val p_to_s_cm = peopleToScreenCM.getText.trim.toDouble
          val o_px = objectPx.getText.trim.toDouble
          objectDeg.setText(screen.init(s_px, s_cm, p_to_s_cm).pxInScreenToDeg(o_px).toString)
        }
      }
    })
    objectDeg.textProperty().addListener((e,o,n) => {
      if (!n.isEmpty && objectDeg.isFocused) {
        if (screenCM.getText.isEmpty ||
          screenPX.getText.isEmpty ||
          peopleToScreenCM.getText.isEmpty) {} else {
          val s_cm = screenCM.getText.trim.toDouble
          val s_px = screenPX.getText.trim.toDouble
          val p_to_s_cm = peopleToScreenCM.getText.trim.toDouble
          val o_deg = objectDeg.getText.trim.toDouble
          objectPx.setText(screen.init(s_px, s_cm, p_to_s_cm).degToScreenPx(o_deg).toString)
        }
      }
    })
    box
  }

  val scene = new Scene(root, 600, 400)

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("像素和角度转换 - A Part of Psy4J - 0.1.3")
    primaryStage.setScene(scene)
    initBeforeShow()
    primaryStage.show()
  }

  def initBeforeShow(): Unit = {
    val bounds = Screen.getPrimary.getVisualBounds
    val dpi = Screen.getPrimary.getDpi
    val (dep, rate) = getFreshRate
    currentScreenInfo.setText("Width × Height : " + bounds.getWidth + " × " + bounds.getHeight + " px\n\n" +
      "DPI : " + dpi.toString + ", Width（Computed）： " + ((bounds.getWidth/dpi) * 2.54).formatted("%.3f") + " cm\n\n" +
      "BitDepth: " + dep + " bit , FreshRate: " + rate + " hz")
    screenPX.setText(bounds.getWidth.toString)
    screenCM.setText(((bounds.getWidth/dpi) * 2.54).formatted("%.3f"))
    peopleToScreenCM.setText("45")
    objectPx.setText("56")
    peopleToScreenCM.requestFocus()
    peopleToScreenCM.selectAll()
  }

  def getFreshRate: (Int, Int) = {
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment
    val device = env.getDefaultScreenDevice
    val mode = device.getDisplayMode
    val bit = mode.getBitDepth
    val rate = mode.getRefreshRate
    (bit, rate)
  }
}

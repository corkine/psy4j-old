package com.mazhangjing.rhythm

import java.io.{BufferedReader, FileReader, PrintWriter}
import java.nio.file.{Path, Paths}
import java.time.Instant
import java.util.concurrent.TimeUnit

import com.mazhangjing.lab.data.DataUtils
import javafx.application.{Application, Platform}
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, TextField, TextInputDialog}
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text}
import javafx.scene.{Parent, Scene}
import javafx.stage.{Modality, Stage}

import scala.collection.mutable.ArrayBuffer

class Recorder extends Application {

  val newBtn = new Button("新建记录")

  val introRoot: VBox = {
    //Root 必须保持和 Scene、Stage 一样大
    //如果被托管，那么 prefXXX 没作用
    val pane = new VBox(); pane.setAlignment(Pos.CENTER); pane.setSpacing(10)
    pane.getChildren.addAll(newBtn)
    //pane.setStyle("-fx-border-color: red")
    pane
  }

  val introScene = new Scene(introRoot, 400, 300)

  val key = new SimpleStringProperty("Waiting...")

  var saving = false

  val data = new ArrayBuffer[(Instant,String)]()

  val waiting = new TextField("5")

  val edit = new Label("检测到修改")

  val save = new Label("正在保存")

  val enter = new Button("开始")

  val root: Parent = {
    val pane = new BorderPane()
    val info = new Text()
    val funcBox = new HBox(); funcBox.setAlignment(Pos.CENTER); funcBox.setSpacing(15)
    pane.setTop(info)
    val keyPressed = new Label("")
    keyPressed.textProperty().bind(key)
    keyPressed.setTextFill(Color.RED)
    keyPressed.setFont(Font.font(80))
    pane.setCenter(keyPressed)
    pane.setBottom(funcBox)
    val waitingLabel = new Label("按下后等待秒数自动保存：")
    val waitBox = new HBox(); waitBox.setSpacing(5); waitBox.setAlignment(Pos.CENTER)
    info.textProperty().bind(new SimpleStringProperty("在英文输入下，记录按键，按键后 ")
    .concat(waiting.textProperty()).concat(" 秒内可撤销，最终会记录最后的按键。\n" +
      "按下 Enter 记录为空，按下数字记录对应数字"))
    waitBox.getChildren.addAll(waitingLabel, waiting)
    edit.setTextFill(Color.RED)
    edit.setVisible(false)
    save.setTextFill(Color.GREEN)
    save.setVisible(false)
    funcBox.getChildren.addAll(waitBox, enter, edit, save)
    BorderPane.setAlignment(info, Pos.CENTER)
    BorderPane.setMargin(info, new Insets(20,20,20,20))
    BorderPane.setMargin(funcBox, new Insets(20,20,20,20))
    pane
  }

  val scene = new Scene(root, 600, 400)

  override def start(mainStage: Stage): Unit = {
    mainStage.setTitle("Recorder")
    mainStage.setScene(introScene)
    newBtn.prefWidthProperty().bind(introScene.widthProperty().divide(2))
    newBtn.prefHeightProperty().bind(Bindings.selectDouble(newBtn.getParent,"height").divide(4))
    newBtn.setOnAction(e => {
      val stage = new Stage()
      stage.setScene(scene)
      stage.initOwner(mainStage)
      stage.initModality(Modality.APPLICATION_MODAL)
      stage.setOnCloseRequest(e => {
        println("Closing Stage Now...")
        val alert = new TextInputDialog()
        alert.setContentText("输入被试编号")
        alert.setHeaderText("将保存被试回答到 编号.csv 中")
        alert.showAndWait()
        val info = alert.getEditor.getText().trim
        if (!info.isEmpty) doSave(info + "_record.csv")
      })
      stage.show()
    })
    scene.setOnKeyPressed(e => {
      key.set(e.getCode.toString)
      //如果没有启动保存进程，则启动，反之，则不启动，算作更改
      if (!saving) {
        edit.setVisible(false)
        save.setVisible(true)
        saving = true
        val current = (Instant.now(), e.getCode)
        val task = new Task[Double] {
          override def call(): Double = {
            //n s 后自动保存
            TimeUnit.SECONDS.sleep(waiting.getText().trim.toInt)
            //如果检测到更改，则以最后修改为准
            val real = (current._1, key.get())
            data.append(real)
            println("Saved " + real)
            saving = false
            Platform.runLater(() => {
              key.set("Waiting...")
              save.setVisible(false)
              edit.setVisible(false)
            })
            0.0
          }
        }
        new Thread(task).start()
      } else edit.setVisible(true)
    })
    waiting.textProperty().addListener(e => {
      enter.requestFocus()
    })
    mainStage.show()
  }

  private def doSave(name: String): Unit = {
    DataUtils.saveTo(Paths.get(name)) {
      val sb = new StringBuilder
      data.foreach(i => {
        sb.append(i._1.toString).append(", ").append(i._2).append("\n")
      })
      sb
    }
  }
}

class RecorderDataProcess {

  def processWithDataAndRecord(data: Path, record: Path): Unit = {
    val dta = ExperimentData.loadWithObject(data)
    val reader = new FileReader(record.toFile)
    val buffered = new BufferedReader(reader)
    val rec:ArrayBuffer[(Instant, Integer)] = new ArrayBuffer[(Instant, Integer)]()
    buffered.lines().forEach(line => {
      if (!line.isEmpty) {
        val s = line.split(", ")
        val instant = Instant.parse(s(0))
        var int_ = -1
        try {
          int_ = s(1) match {
            case "ENTER" => -1
            case other => other.replace("DIGIT","").trim.toInt
          }
        } catch {
          case e:Throwable => e.printStackTrace(System.err)
        }
        rec.append((instant, int_))
      }
    })
    val trialDatas = dta.getTrialData
    trialDatas.forEach(trial => {
      val tuple = rec.find(line => {
        line._1.isAfter(trial.startInstant) && line._1.isBefore(trial.endInstant)
      }).getOrElse((null, null))
      trial.answer = tuple._2
    })
    println(dta)
    ExperimentData.persistToCSV(Paths.get(dta.userId + "_Final.csv"), dta)
  }
}
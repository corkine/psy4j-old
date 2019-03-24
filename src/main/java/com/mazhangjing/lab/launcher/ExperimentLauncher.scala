package com.mazhangjing.lab.launcher

import java.io.{File, FileInputStream}
import java.util.concurrent.TimeUnit

import javafx.application.Application
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.layout.{HBox, VBox}
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import org.yaml.snakeyaml.Yaml

class ExperimentLauncher extends Application {

  var config: Config = _

  var root: Parent = _

  override def init(): Unit = {
    val stream =
      new FileInputStream(new File(System.getProperty("user.dir") + File.separator + "launch.yaml"))
    config = new Yaml().loadAs(stream, classOf[Config])
    stream.close()
  }

  override def start(stage: Stage): Unit = {
    stage.setTitle("Experiment Launcher")
    initConfig()
    stage.setScene(new Scene(root,800,600))
    stage.show()
  }

  def initConfig(): Unit = {
    root = {
      val vbox = new VBox(); vbox.setSpacing(10)
      vbox.setAlignment(Pos.CENTER)
      config.commandList.forEach((id, bean) => {
        val hbox = new HBox()
        val nameLabel = new Label(bean.getName)
        nameLabel.setMinWidth(100)
        nameLabel.autosize()
        val commandText = new TextField(bean.getCommand)
        commandText.setPrefWidth(350)
        val runBtn = new Button("Run Command")
        runBtn.setOnAction(e => {
          val command = commandText.getText.trim
          runCommand(command)
        })
        hbox.getChildren.addAll(nameLabel, commandText, runBtn)
        hbox.setAlignment(Pos.CENTER)
        hbox.setSpacing(5)
        vbox.getChildren.add(hbox)
      })
      vbox
    }
  }

  def runCommand(command: String): Unit = {
    println("Running Command " + command)
    new Task[Double]() {
      override def call(): Double = {
        println("In Task Now")
        Runtime.getRuntime.exec("" + command)
        TimeUnit.SECONDS.sleep(3)
        System.exit(0)
        0.0
      }
    }.run()
  }
}

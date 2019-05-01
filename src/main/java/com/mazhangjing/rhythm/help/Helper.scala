package com.mazhangjing.rhythm.help

import java.io.File
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime}
import java.util.concurrent.TimeUnit

import com.mazhangjing.lab.data.DataUtils
import com.mazhangjing.lab.mouse.FrequencyPane
import com.mazhangjing.lab.sound.{SimpleAudioFunctionMakerToneUtilsImpl, ToneUtils}
import com.mazhangjing.rhythm.help.Helper.{Processor, Recorder, Timer}
import com.mazhangjing.rhythm.{ExperimentData, Log, SequenceProcessFactory}
import javafx.application.{Application, Platform}
import javafx.beans.binding.Bindings
import javafx.beans.property.{SimpleBooleanProperty, SimpleIntegerProperty, SimpleStringProperty}
import javafx.concurrent.Task
import javafx.geometry.{Insets, Pos}
import javafx.scene.control._
import javafx.scene.input.DragEvent
import javafx.scene.layout.{BorderPane, HBox, StackPane, VBox}
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.{Font, Text, TextAlignment}
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Helper extends Application {

  val newRecordBtn = new Button("新建记录")

  val processBtn = new Button("数据对齐")

  val timeBtn = new Button("时间校准")

  val freqBtn = new Button("频率测试")

  val introRoot: VBox = {
    //Root 必须保持和 Scene、Stage 一样大
    //如果被托管，那么 prefXXX 没作用
    val pane = new VBox(); pane.setAlignment(Pos.CENTER); pane.setSpacing(10)
    pane.getChildren.addAll(newRecordBtn, processBtn, timeBtn, freqBtn)
    pane
  }

  val introScene = new Scene(introRoot, 400, 300)

  private[this] def bindWidthHeightWithBtn(btn: Button, scene:Scene)
                            (divideWidth: Int = 2, divideHeight: Int = 8)
                            (action: => Unit): Unit = {
    btn.prefWidthProperty().bind(scene.widthProperty().divide(divideWidth))
    btn.prefHeightProperty().bind(Bindings.selectDouble(btn.getParent,"height").divide(divideHeight))
    btn.setOnAction(_ => {
      action
    })
  }

  def initBtnBind(mainStage:Stage): Unit = {
    bindWidthHeightWithBtn(newRecordBtn, introScene)() {
      val controller = new Recorder(mainStage)
      controller.stage.show()
    }
    bindWidthHeightWithBtn(processBtn, introScene)() {
      val controller = new Processor(mainStage)
      controller.stage.show()
    }
    bindWidthHeightWithBtn(timeBtn, introScene)() {
      val controller = new Timer(mainStage)
      controller.stage.show()
    }
    bindWidthHeightWithBtn(freqBtn, introScene)() {
      new FrequencyPane().decorateStage(mainStage).show()
    }
  }

  override def start(mainStage: Stage): Unit = {
    mainStage.setTitle(s"Rhythm Experiment Helper - ${Log.version}")
    mainStage.setScene(introScene)
    initBtnBind(mainStage)
    mainStage.show()
  }
}

object Helper {
  class Timer(mainStage:Stage) {

    var endMark = false

    val time = new SimpleStringProperty("00:00:00")

    val tone: ToneUtils = new SimpleAudioFunctionMakerToneUtilsImpl

    val task: Task[Double] = {
      val t = new Task[Double]{
        override def call(): Double = {
          while (!endMark) {
            Platform.runLater(() => {
              val now = LocalDateTime.now()
              time.set(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
              if (now.getSecond == 0 ||
                now.getSecond == 30) beepWhenTime()
            })
            TimeUnit.SECONDS.sleep(1)
          }
          0.0
        }
      }
      t
    }

    def beepWhenTime(): Unit = {
      new Thread(() => {
        tone.playForDuration(300,5000,5000,0)
      }).start()
    }

    def getRoot: Parent = {
      val hbox = new HBox()
      hbox.setAlignment(Pos.CENTER)
      val timeLabel = new Label()
      timeLabel.textProperty().bind(time)
      timeLabel.setFont(Font.font(50))
      timeLabel.setTextFill(Color.GREEN)
      hbox.getChildren.add(timeLabel)
      hbox
    }

    val scene = new Scene(getRoot, 400, 300)

    def stage: Stage = {
      val stage = new Stage()
      if (mainStage != null) stage.initOwner(mainStage)
      stage.setTitle("Timer")
      stage.setScene(scene)
      stage.setOnCloseRequest(_ => {
        endMark = true
      })
      new Thread(task).start()
      stage
    }
  }
  class Recorder(mainStage:Stage) {
    val key = new SimpleStringProperty("Waiting...")
    var saving = false
    val waiting = new TextField("5")
    val edit = new Label("检测到修改")
    val save = new Label("正在保存")
    val enter = new Button("开始")
    val data = new ArrayBuffer[(Instant,String)]()

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
    val stage: Stage = initDataStage(mainStage, scene)

    var fileName: String = showAlertAndGetName

    stage.setTitle(fileName)

    initController()

    def showAlertAndGetName: String = {
      val alert = new TextInputDialog()
      alert.setContentText("输入被试编号")
      alert.setHeaderText("将保存被试回答到 编号.csv 中")
      alert.showAndWait()
      alert.getEditor.getText().trim match {
        case i if !i.isEmpty => i + "_record.csv"
        case _ => "NoName_record.csv"
      }
    }

    def initDataStage(mainStage: Stage, scene: Scene): Stage = {
      val stage = new Stage()
      stage.setScene(scene)
      stage.initOwner(mainStage)
      //stage.initModality(Modality.APPLICATION_MODAL)
      stage.setOnCloseRequest(e => {
        println("Closing Stage Now...")
        doSave(fileName)
      })
      stage
    }

    def initController(): Unit = {
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
                stage.setTitle(fileName + " [ Collected - " + data.length + " ]")
              })
              0.0
            }
          }
          new Thread(task).start()
        } else edit.setVisible(true)
      })
      import Utils._
      waiting.textProperty().addListener {
        enter.requestFocus()
      }
    }

    def doSave(name: String): Unit = {
      DataUtils.saveTo(Paths.get(name)) {
        val sb = new StringBuilder
        data.foreach(i => {
          sb.append(i._1.toString).append(", ").append(i._2).append("\n")
        })
        sb
      }
    }
  }
  class Processor(mainStage:Stage) {

    import collection.JavaConverters._

    val data: mutable.Set[File] = mutable.HashSet[File]()

    val dataInfo = new SimpleStringProperty("No Files")

    val info = new SimpleStringProperty("Information")

    val adjustTime = new SimpleIntegerProperty(0)

    val printToFolder = new SimpleBooleanProperty(false)

    val useFakeRecords = new SimpleBooleanProperty(true)

    def task(useFakeRecords: Boolean): Task[Double] = {
      val t = new Task[Double]() {
        override def call(): Double = {
          println("Adjust Time with " + adjustTime.get())
          val impl =
            if (useFakeRecords) SequenceProcessFactory.newInstanceWithFakeRecords(data.toArray, adjustTime.get())
            else SequenceProcessFactory.newInstance(data.toArray, adjustTime.get())
          val datas = impl.doProcess()
          Platform.runLater(() => {
            if (printToFolder.get()) saveToFile(datas)
            showStatInfo(datas)
          })
          0.0
        }
      }
      t.exceptionProperty().addListener((_,_,n) => {
        n.printStackTrace(System.err)
      })
      t
    }

    val root: Parent = {
      val box = new BorderPane()
      val cBox = new VBox(); cBox.setAlignment(Pos.CENTER); cBox.setSpacing(20)
      val label = new Label("↓")
      label.setTextFill(Color.WHITE)
      val border = new Rectangle()
      border.setFill(Color.DARKGRAY)
      //border.widthProperty().bind(label.widthProperty().add(80))
      //border.heightProperty().bind(label.heightProperty().add(40))
      border.setArcHeight(60)
      border.setArcWidth(60)
      //border.setX(-40)
      //border.setY(-20)
      border.setWidth(150)
      border.setHeight(border.getWidth)
      val st = new StackPane()
      st.getChildren.addAll(border, label)

      val btn = new Hyperlink("Do Process")
      val adj = new TextField()
      adj.setPromptText("输入需要调整的秒数")
      adj.textProperty().addListener((_, _, n) => {
        if (!n.isEmpty && n != "-") {
          try {
            adjustTime.set(n.toInt)
          } catch {
            case e: Throwable => e.printStackTrace(System.err)
          }
        }
      })

      val fBox = new VBox(); fBox.setAlignment(Pos.CENTER); fBox.setSpacing(10)
      fBox.setFillWidth(false)

      val output = new CheckBox("输出为原始记录")
      val useFakeRecord = new CheckBox("缺失记录设为真")
      useFakeRecord.selectedProperty().bindBidirectional(useFakeRecords)
      output.selectedProperty().bindBidirectional(printToFolder)
      fBox.getChildren.addAll(adj, output, useFakeRecord, btn)
      cBox.getChildren.addAll(st, fBox)

      label.setFont(Font.font(70))
      val text = new Text()
      text.setWrappingWidth(500)
      text.textProperty().bind(dataInfo)
      st.setOnDragOver((event: DragEvent) => {
        label.setText("◉")
        import javafx.scene.input.TransferMode

        val dragboard = event.getDragboard
        if (dragboard.hasFiles) {
          val files = dragboard.getFiles
          if (files.asScala.forall(file => file.getAbsolutePath.endsWith(".csv") || file.getAbsolutePath.endsWith(".obj"))) {
            event.acceptTransferModes(TransferMode.COPY)
          }
        }
      })

      st.setOnDragExited((ev: DragEvent) => {
        label.setText("︎↓")
      })

      st.setOnDragDropped((event: DragEvent) => {
        val dragboard = event.getDragboard
        if (event.isAccepted) {
          val files = dragboard.getFiles
          data.clear()
          data ++= files.asScala
          dataInfo.set(data.map(in => in.getName).mkString(", "))
        }
      })

      btn.setOnAction(_ => {
        if (data != null && data.nonEmpty) {
          new Thread(task(useFakeRecords.get())).start()
        }
      })

      text.setTextAlignment(TextAlignment.CENTER)
      val bBox = new HBox()
      bBox.setAlignment(Pos.CENTER)
      bBox.getChildren.add(text)
      BorderPane.setMargin(bBox, new Insets(0,0,30,0))
      box.setCenter(cBox)
      box.setBottom(bBox)
      box
    }

    val stage: Stage = {
      val s = new Stage()
      s.initOwner(mainStage)
      s.setTitle("Processor")
      val scene = new Scene(root, 600, 600)
      s.setScene(scene)
      s
    }

    private def saveToFile(datas: Array[ExperimentData]): Unit = {
      SequenceProcessFactory.printToCSV(datas, Paths.get("all_result.csv"))
    }

    private def showStatInfo(datas: Array[ExperimentData]): Unit = {
      val stage = new Stage()
      val root = new BorderPane()
      val area = new TextArea("")
      val info = new mutable.StringBuilder()
      datas.foreach(data => {
        val str = Utils.getStatus(data)
        info.append(str)
      })
      area.setText(info.toString())
      area.setEditable(false)
      area.setWrapText(true)
      area.prefWidthProperty().bind(stage.widthProperty())
      area.prefHeightProperty().bind(stage.heightProperty())
      root.setCenter(area)
      val scene = new Scene(root, 600, 400)
      scene.getStylesheets.add("noborder.css")
      stage.setScene(scene)
      stage.show()
    }
  }
}
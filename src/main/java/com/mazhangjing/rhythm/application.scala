package com.mazhangjing.rhythm

import java.nio.file.Paths
import java.util

import com.mazhangjing.lab._
import com.mazhangjing.lab.sound.{Capture, SimpleAudioFunctionMakerToneUtilsImpl, SimpleLongToneUtilsImpl, SimpleShortToneUtilsImpl}
import com.mazhangjing.rhythm
import com.mazhangjing.rhythm.MzjExperiment._
import com.mazhangjing.rhythm.help.{Helper, Utils}
import javafx.application.{Application, Platform}
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.Event
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout.{GridPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.util.Random

class VoiceDetectEventMaker(exp: Experiment, scene: Scene) extends EventMaker(exp, scene) {

  val logger: Logger = LoggerFactory.getLogger(classOf[VoiceDetectEventMaker])

  override def run(): Unit = {
    logger.debug("VoiceDetectEventMaker Called now....")
    val capture = new Capture()
    import com.mazhangjing.rhythm.help.Utils._
    capture.messageProperty().addListener {
      val value = capture.getMessage.toDouble
      if (value != 0.0) {
        logger.debug("Receive Sound Event Now..., Current Sound Signal is " + value)
        if (exp.getScreen != null) exp.getScreen.eventHandler(new Event(Event.ANY), exp, scene)
      }
    }
    new Thread(capture).start()
  }
}

class MzjApplication extends Application {

  class ExperimentModifier {
    val conditionChooseSet: collection.mutable.Set[Int]= new mutable.HashSet[Int]()
    def setEstimate(): Unit = {
      JUST_ESTIMATE = true
      JUST_COUNTING = false
    }
    def setCounting(): Unit = {
      JUST_ESTIMATE = false
      JUST_COUNTING = true
    }
    def setAll(): Unit = {
      JUST_ESTIMATE = false
      JUST_COUNTING = false
    }
    def setTest(): Unit = {
      IS_DEBUG = true
      IS_FAST_DEBUG = false
    }
    def setFastTest(): Unit = {
      IS_DEBUG = true
      IS_FAST_DEBUG = true
    }
    def setReal(): Unit = {
      IS_DEBUG = false
      IS_FAST_DEBUG = false
    }
    def setConditionChooseSet(selected: Boolean, conditionNumber: Int): Unit = {
      if (selected) conditionChooseSet.add(conditionNumber)
      else conditionChooseSet.remove(conditionNumber)
    }
    def setSoundImplMode(n: String): Unit = {
      n.toUpperCase match {
        case i if i.contains("SHORT") => SOUND_IMPL_CLASS = classOf[SimpleShortToneUtilsImpl]
        case i if i.contains("LONG") => SOUND_IMPL_CLASS = classOf[SimpleLongToneUtilsImpl]
        case i if i.contains("AUDIO") => SOUND_IMPL_CLASS = classOf[SimpleAudioFunctionMakerToneUtilsImpl]
      }
    }
    def setHz(hz: Double): Unit = {
      SUBJECT_HZ = hz
    }
    def setExperimentData(experimentData: ExperimentData): Unit = {
      MzjExperiment.initExperiment(experimentData, conditionChooseSet.toSet)
    }
    def setTrialRepeat(number: Int): Unit = {
      TRIAL_COUNT = number
    }
    def getTrialRepeat: String = {
      TRIAL_COUNT.toString
    }
  }

  class ExperimentController {
    import com.mazhangjing.rhythm.help.Utils._

    def initControlWithExp(gridPane: GridPane, rowIndex: Int): Unit = {
      val ecGroup = new ToggleGroup
      val estimateBtn = new ToggleButton("感数")
      val countingBtn = new ToggleButton("计数")
      val allBtn = new ToggleButton("随机全部")
      ecGroup.getToggles.addAll(estimateBtn, countingBtn, allBtn)
      gridPane.add(new Label("实验"),0,rowIndex)
      gridPane.add({
        val b1 = new HBox()
        b1.getChildren.addAll(estimateBtn, countingBtn, allBtn)
        b1.setSpacing(5); b1}, 1,rowIndex)

      ecGroup.selectedToggleProperty().addListener {
        if (ecGroup.getSelectedToggle == estimateBtn) {
          modifier.setEstimate()
        } else if (ecGroup.getSelectedToggle == countingBtn) {
          modifier.setCounting()
        } else if (ecGroup.getSelectedToggle == allBtn) {
          modifier.setAll()
        }
      }

      ecGroup.selectToggle(allBtn)
    }

    def initControlWithDebug(gridPane: GridPane, rowIndex: Int): Unit = {
      val trialGroup = new ToggleGroup
      val testBtn = new ToggleButton("测试")
      val fastTestBtn = new ToggleButton("快速测试")
      val realBtn = new ToggleButton("正式实验")
      trialGroup.getToggles.addAll(testBtn, fastTestBtn, realBtn)
      gridPane.add(new Label("调试"),0,rowIndex)
      gridPane.add({
        val t = new HBox()
        t.getChildren.addAll(testBtn, fastTestBtn, realBtn)
        t.setSpacing(5); t}, 1,rowIndex)

      trialGroup.selectedToggleProperty().addListener {
        val selected = trialGroup.getSelectedToggle
        if (selected == testBtn) {
          modifier.setTest()
        } else if (selected == fastTestBtn) {
          modifier.setFastTest()
        } else if (selected == realBtn) {
          modifier.setReal()
        }
      }

      trialGroup.selectToggle(realBtn)
    }

    def initControlWithCondition(gridPane: GridPane, rowIndex: Int): Unit = {
      val c1 = new ToggleButton("基线条件")
      val c2 = new ToggleButton("节律高频条件")
      val c3 = new ToggleButton("节律中频条件")
      val c4 = new ToggleButton("节律低频条件")
      val c5 = new ToggleButton("无节律条件")
      val c6 = new Button("全部")
      gridPane.add(new Label("条件"),0,rowIndex)
      gridPane.add({
        val t = new HBox()
        t.getChildren.addAll(c1,c2,c3,c4,c5,c6)
        t.setSpacing(5); t},1,rowIndex)

      c1.selectedProperty().addListener((_, _, n) => modifier.setConditionChooseSet(n,1))
      c2.selectedProperty().addListener((_, _, n) => modifier.setConditionChooseSet(n,2))
      c3.selectedProperty().addListener((_, _, n) => modifier.setConditionChooseSet(n,3))
      c4.selectedProperty().addListener((_, _, n) => modifier.setConditionChooseSet(n,4))
      c5.selectedProperty().addListener((_, _, n) => modifier.setConditionChooseSet(n,5))
      c6.setOnAction(_ => {
        if (c1.isSelected && c2.isSelected && c3.isSelected
          && c4.isSelected && c5.isSelected) {
          c1.setSelected(false)
          c2.setSelected(false)
          c3.setSelected(false)
          c4.setSelected(false)
          c5.setSelected(false)
        } else {
          c1.setSelected(true)
          c2.setSelected(true)
          c3.setSelected(true)
          c4.setSelected(true)
          c5.setSelected(true)
        }
      })
    }

    def initControlWithSound(gridPane: GridPane, rowIndex: Int): Unit = {
      val soundChoose = {
        val t = new ChoiceBox[String]()
        t.getItems.add("SimpleShortToneUtilsImpl")
        t.getItems.add("SimpleLongToneUtilsImpl")
        t.getItems.add("SimpleAudioFunctionMakerToneUtilsImpl"); t}
      gridPane.add(new Label("声音"), 0, rowIndex)
      gridPane.add(soundChoose, 1,rowIndex)

      soundChoose.getSelectionModel.selectedItemProperty().addListener((_, _, n) => {
        modifier.setSoundImplMode(n)
      })

      soundChoose.getSelectionModel.selectLast()
    }

    def initControlWithTrialRepeat(gridPane: GridPane, rowIndex: Int): Unit = {
      val label = new Label("重复")
      label.setAccessibleHelp("每个 Trial 重复次数")
      val text = new TextField()
      text.setPromptText(modifier.getTrialRepeat)
      text.textProperty().addListener((_,_,n) => {
        if (!n.isEmpty) {
          val value = n.toInt
          modifier.setTrialRepeat(value)
        }
      })
      gridPane.add(label, 0, rowIndex)
      gridPane.add(text, 1,rowIndex)
      GridPane.setFillWidth(text,false)
    }

    def getControlPane(gridPane: GridPane): GridPane = {
      val condition1 = new Label("配置面板")
      gridPane.add(condition1, 0,0)

      initControlWithExp(gridPane, 1)
      initControlWithDebug(gridPane, 2)
      initControlWithCondition(gridPane, 3)
      initControlWithTrialRepeat(gridPane, 4)
      initControlWithSound(gridPane, 5)
      //gridPane.setStyle("-fx-border-color:red")
      //gridPane.setGridLinesVisible(true)
      gridPane
    }
  }

  class DataController {
    val name = new TextField()
    val gender = new ChoiceBox[String]()
    val id = new TextField()
    val hz = new TextField()
    val info = new TextField()

    val detailString: SimpleStringProperty =
      new SimpleStringProperty("如果是分开做的实验，先输入编号并点击按钮以从 编号.obj 文件中读取原本的信息")

    var experimentData:ExperimentData = _

    def getInformationPane(gridPane: GridPane): GridPane =  {
      name.setPromptText("姓名")
      name.setText("NoName")
      gender.setItems({
        val a = FXCollections.observableArrayList[String]()
        a.addAll("女","男")
        a
      })
      gender.getSelectionModel.selectFirst()
      id.setPromptText("编号")
      id.setText((Random.nextInt(10000) + 23333).toString)
      hz.setText("1.5")
      hz.setPromptText("最佳频率")
      info.setPromptText("备注")

      gridPane.add(new Label("被试信息"), 0,0)
      gridPane.add(new Label("姓名"), 0,1)
      gridPane.add(new Label("性别"),0,2)
      gridPane.add(new Label("编号"),0,3)
      gridPane.add(new Label("频率"),0,5)
      gridPane.add(new Label("备注"),0,6)
      gridPane.add(name,1,1)
      gridPane.add(gender,1,2)
      val detail = new Text("")
      detail.textProperty().bind(detailString)
      detail.setFill(Color.DARKGRAY)
      val load = new Button("从 编号.obj 中加载")
      val hbox = new HBox(); hbox.setSpacing(5)
      hbox.getChildren.addAll(id, load)
      gridPane.add(hbox,1,3)
      gridPane.add(detail, 1,4)
      gridPane.add(hz,1,5)
      gridPane.add(info,1,6)

      load.textProperty().bind(
        new SimpleStringProperty("从 ").concat(id.textProperty()).concat(".obj 加载"))
      load.setOnAction(_ => {
        Platform.runLater(() => {
          doLoadExistData()
        })
      })

      val shuffleBtn = new Hyperlink("随机平衡")
      val timeChecker = new Hyperlink("时间校准")

      shuffleBtn.setOnAction(_ => {
        val alert = new Alert(AlertType.INFORMATION)
        alert.setHeaderText("Lucky Order")
        val ints = Random.shuffle(List.range(1,6))
        alert.setContentText(ints.mkString(" - "))
        alert.showAndWait()
      })
      timeChecker.setOnAction(_ => {
        val timer = new rhythm.help.Helper.Timer(null)
        timer.stage.show()
      })

      val cmd = new HBox(); cmd.setSpacing(13)
      cmd.getChildren.addAll(start, isFullScreen, shuffleBtn, timeChecker)
      cmd.setAlignment(Pos.CENTER_LEFT)
      GridPane.setConstraints(cmd,0,7,2,1)
      GridPane.setMargin(cmd,new Insets(30,0,0,0))
      gridPane.getChildren.add(cmd)

      //gridPane.setStyle("-fx-border-color: pink")
      //gridPane.setGridLinesVisible(true)
      gridPane
    }

    private[this] def getDetailInfo(experimentData: ExperimentData): String = {
      if (experimentData.trialData != null &&
        !experimentData.trialData.isEmpty)
        Utils.getStatus(experimentData, withStatus = false)
      else ""
    }

    private[this] def doLoadExistData(): Unit = {
      try {
        val data = ExperimentData.loadWithObject(Paths.get(id.getText() + ".obj"))
        this.experimentData = data
        name.setText(this.experimentData.userName)
        id.setText(this.experimentData.userId.toString)
        hz.setText(this.experimentData.prefHz.toString)
        gender.getSelectionModel.select(this.experimentData.gender)
        info.setText(this.experimentData.information)
        getDetailInfo(experimentData) match {
          case i if !i.isEmpty => detailString.set(i)
          case _ =>
        }
      } catch {
        case e: Throwable =>
          this.experimentData = null
          val alert = new Alert(AlertType.ERROR)
          alert.setHeaderText("加载出现问题")
          alert.setContentText("无法完成从指定文件进行的加载，请检查文件名和路径，以及读写权限")
          e.printStackTrace(System.err)
          alert.showAndWait()
      }
    }

    def initDataBeforeRunExperiment(): Unit = {
      if (experimentData == null) {
        //是新实验
        experimentData = new ExperimentData()
        experimentData.userName = name.getText().trim
        experimentData.gender = gender.getSelectionModel.getSelectedIndex
        experimentData.userId = id.getText().toInt
        experimentData.information = info.getText().trim
        experimentData.prefHz = hz.getText().trim.toDouble
        modifier.setHz(experimentData.prefHz)
      } else {
        //是旧实验
        //如果是旧实验，仅允许添加 Information，但是不允许变更姓名、性别、ID 和 prefHZ
        experimentData.information = info.getText().trim
        modifier.setHz(experimentData.prefHz)
      }
      modifier.setExperimentData(experimentData)
    }
  }

  val isFullScreen: CheckBox = {
    val t = new CheckBox("全屏显示")
    t.setSelected(true); t
  }
  val start = new Button("RUN EXPERIMENT")

  val modifier = new ExperimentModifier

  val controlController = new ExperimentController

  val dataController = new DataController

  def getRoot: Parent = {
    def initPane: GridPane = {
      val gridPane = new GridPane()
      gridPane.setAlignment(Pos.CENTER_LEFT)
      gridPane.setHgap(5)
      gridPane.setVgap(10)
      gridPane
    }
    val pane1 = dataController.getInformationPane(initPane)
    val pane2 = controlController.getControlPane(initPane)
    val vbox = new VBox()
    vbox.setSpacing(20)
    vbox.setAlignment(Pos.CENTER)
    vbox.getChildren.addAll(pane2,pane1)
    vbox.setPadding(new Insets(0,0,0,60))
    vbox
  }

  val configureScene = new Scene(getRoot, 700, 663)

  override def start(stage: Stage): Unit = {
    stage.setTitle(s"Rhythm Experiment Configure - ${Log.version}")
    stage.setScene(configureScene)
    start.setOnAction(_ => {
      dataController.initDataBeforeRunExperiment() //此时 experimentData 才准备好

      val runner: ExpRunner = new ExpRunner {
        override def initExpRunner(): Unit = {
          val makers = new util.HashSet[String]()
          makers.add("com.mazhangjing.rhythm.VoiceDetectEventMaker")
          setEventMakerSet(makers)
          val set = new util.HashSet[OpenedEvent]()
          set.add(OpenedEvent.KEY_PRESSED)
          setOpenedEventSet(set)
          setExperimentClassName("com.mazhangjing.rhythm.MzjExperiment")
          setVersion("0.0.1")
          setFullScreen(isFullScreen.isSelected)
        }
      }
      val experimentStage = new Stage()
      val helper = new SimpleExperimentHelperImpl(runner)
      helper.initStage(experimentStage)
      experimentStage.setTitle("Rhythm Experiment Runner")
      experimentStage.show()
    })

    stage.show()
  }
}

object Log {
  val log: String = """
      |1.0.0 2019-04-26 添加了 Sound 纯音的  AudioFunction、AudioMaker 接口，
      |      提供了 SimpleAudioFUnctionMakerTOneUtilsImpl 实现。
      |1.0.1 2019-04-27 添加了日志系统，练习了正则表达式。确定了在 Windows 上声音呈现的正常表现。
      |1.0.2 2019-04-27 添加了 csv 的姓名记录。
      |1.0.3 2019-04-28 重构了 Recorder 程序，重构了 MzjApplication 程序
      |1.0.4 2019-04-29 重构了 Processor 程序，将其整理到 Recorder 程序中，思考了关于 GUI 程序的重构问题
      |      继续重构了 MzjApplication 程序，将两个没有逻辑关系的 Pane 区分开，放入两个 Controller 中。
      |1.0.5 2019-04-29 修复了 ExperimentData object 对象 UUID 改变导致无法读取数据的问题，
      |      重构了 MzjApplication 中的 SOUND_IMPL_CLASS 避免在 MzjApplication、MzjExperiment、Trial 中三次更改
      |      （现在 Trial 读取 MzjExp 的方法，而 MzjApp 则往 MzjExp 中写 className，好莱坞原则）
      |1.0.6 2019-04-30 更新了自定义 Trial 数目的设置。更改了高频、低频跨度设置。
      |1.0.7 2019-04-30 修正了休息最短呈现时间问题。添加了时间校准程序。
      |1.0.8 2019-05-01 修正了数据分析程序对于键盘记录快于语音报告的结果导致对齐的错误，现在 DataProcess object 处理
      |      会自动根据文件名 early、late 自动进行时间处理。对于 Helper.Process、SequenceImpl、Factory 类，保持原有 API。
      |1.0.9 2019-05-01 添加了不需要记录即可自动伪造对齐的 API。
    """.stripMargin

  def version: String = {
    val version = """(\d+\.\d+\.\d+)""".r
    val array = version.findAllIn(log).toArray
    array.reverse.headOption match {
      case None => "NaN"
      case Some(v) => v
    }
  }
}

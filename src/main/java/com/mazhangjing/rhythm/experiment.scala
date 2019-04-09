package com.mazhangjing.rhythm

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.{Duration, Instant}
import java.util
import java.util.concurrent.TimeUnit

import com.mazhangjing.lab.LabUtils._
import com.mazhangjing.lab._
import com.mazhangjing.lab.sound.{Capture, WaveUtil}
import com.mazhangjing.rhythm.MzjExperiment._
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.{Event, EventType}
import javafx.geometry.{Insets, Pos}
import javafx.scene._
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.input.KeyCode
import javafx.scene.layout.{BorderPane, GridPane, HBox, VBox}
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text, TextAlignment}
import javafx.stage.Stage
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object MzjExperiment {
  var IS_DEBUG = true //关闭后，严格执行实验，开启后，且开启 FAST_DEBUG，规则最为宽松，且关闭 FAST_DEBUG，仅仅是允许键盘替代语音
  var IS_FAST_DEBUG = true //开启后，规则最为宽松，关闭后，相比较正式实验，仅允许键盘替代语音反应计数
  var TRIAL_COUNT = 20 //真实的 Trial 重复数
  var TEST_TRIAL_COUNT = 2 //测试的 Trial 重复数
  val PEOPLE_TO_SCREEN_CM = 57 //人距离屏幕的 CM 数
  val INTRO_MAX_KEEP = 2000000 //指导语最长超时
  val USE_RANDOM_TRIAL_ORDER = true //真实模式随机计数和感数 Block
  var JUST_COUNT_FIRST = false //调试模式固定先计数，再感数
  val RED_CROSS_KEEP_TIME: () => Int = () => Random.nextInt(500) + 500 //500 - 1000 红色十字呈现的时长
  var ALL_CROSS_KEEP_TIME = 8000 //声音-注视点刺激呈现的时长
  var SOUND_TIME_MS = 20 //单个声音呈现的时长
  val SOUND_RANDOM_MAX_SPACE_MS = 130 //随机无节律声音间隔的差值，根据文献设置
  val SUBJECT_HZ = 3.01 //被试的自然频率
  val SUBJECT_HEIGHT_HZ_RATIO = 1.25
  val SUBJECT_LOWER_HZ_RATIO = 0.75
  val COUNTING_MAX_KEEP_TIME_MS = 100000 //计数阶段刺激最长呈现时长
  val STIMULATE_KEEP_TIME_MS = 200 //感数阶段刺激呈现时长
  val COUNTING_ACTION_SCREEN_KEEP_TIME_MS = 2000
  val ESTIMATE_ACTION_SCREEN_KEEP_TIME_MS = 4000 //感数阶段最长允许反应时间，之后进入下一个试次
  val MASK_SCREEN_KEEP_TIME_MS = 150 //掩蔽刺激呈现时长
  val CIRCLE_RADIUS_PIXEL = 12 //点的半径，像素个数
  val WIDTH_RATIO = 0.5 //点的范围所在的屏幕宽度的比例
  val HEIGHT_RATIO = 0.5 //点的范围所在的屏幕高度的比例
  val MAX_REST_KEEP_TIME = 100000 //最长休息时间，最短时间由类自行控制
  val BOUND_WIDTH: (Int, Int) = getBound(WIDTH_RATIO, HEIGHT_RATIO)._1
  val BOUND_HEIGHT: (Int, Int) = getBound(WIDTH_RATIO, HEIGHT_RATIO)._2

  var JUST_ESTIMATE = false
  var JUST_COUNTING = false
  var DO_MULTIPLE_CONDITION_NUMBERS: Set[Int] = Set(1,2,3,4,5)
  //-1 全做 1 基线 2 高频 3 中频 4 低频 5 无节律
  //1 Baseline, 2、3、4 Rhythm High,Normal,Low, 5 noRhythm

  val COUNTING = "COUNTING"
  val ESTIMATE = "ESTIMATE"

  val CONDITION2: Set[Int] = (2 to 6).toSet
  val CONDITION2_TEST_CHOOSE_NUMBER = 3

  var experimentData: ExperimentData = _

  def initExperiment(experimentData: ExperimentData, conditionSet: Set[Int]): Unit = {
    if (IS_DEBUG && IS_FAST_DEBUG) {
      TRIAL_COUNT = 4
      JUST_COUNT_FIRST = false
      //保证训练时长长于随机红色十字最长时长
      ALL_CROSS_KEEP_TIME = 1500
    }
    this.experimentData = experimentData
    this.DO_MULTIPLE_CONDITION_NUMBERS = conditionSet
    println("Condition is " + this.DO_MULTIPLE_CONDITION_NUMBERS.mkString(", "))
  }

  /**
    * 获取呈现刺激点的长和宽的范围
    * @param widthRatio 希望使用的中心的宽的比例
    * @param heightRatio 希望使用的中心的长的比例
    * @return
    */
  private def getBound(widthRatio: Double, heightRatio: Double): ((Int, Int),(Int, Int)) = {
    val width = javafx.stage.Screen.getPrimary.getVisualBounds.getWidth.toInt
    val height = javafx.stage.Screen.getPrimary.getVisualBounds.getHeight.toInt
    val realWidth = width * widthRatio
    val realHeight = height * heightRatio
    ((((width - realWidth)/2).toInt, ((width - realWidth)/2 + realWidth/2).toInt),
      (((height - realHeight)/2).toInt, ((height - realHeight)/2 + realHeight/2).toInt))
  }

  /**
    * 根据 Condition - 节律、无节律、基线等五种情况、Condition2 - 呈现的数字个数、randomRepeat 每个 Trial 重复次数、
    * op 给一个 TrialData 用于记录实验结果
    * @param condition 节律、无节律、基线等五种情况
    * @param numberCondition 呈现的数字个数
    * @param randomGetNumber 如果是练习试次，则随机呈现点的范围
    * @param randomRepeat 每个 Trial 的重复数
    * @param op 可以在此生成一个 Trial，并且将 TrialData 注入其中
    */
  def withConditionAndRepeat(condition: Set[Int])
                            (numberCondition: Set[Int], randomGetNumber: Int = -1)
                            (randomRepeat: Int)
                            (op: (TrialData, Boolean) => Unit): Unit = {
    val buffer = new ArrayBuffer[TrialData]()
    condition foreach ( condition1 => {
      //如果是正式实验，全部数字
      if (randomGetNumber == -1) {
        numberCondition foreach( condition2 => {
          val data = new TrialData(condition1, condition2)
          1 to randomRepeat foreach( _ => {
            buffer.append(data)
          })
        })
      } else {
        //如果是练习，则随机选择 randomGetNumber 个数字
        val ints = Random.shuffle(numberCondition).toBuffer.slice(0, randomGetNumber)
        ints foreach( condition2 => {
          val data = new TrialData(condition1, condition2)
          1 to randomRepeat foreach( _ => {
            buffer.append(data)
          })
        })
      }
    })
    val randomArray = Random.shuffle(buffer).toArray
    //将基线、节律、非节律区分成 Block 并随机 - 先分组，再随机组的 Key，最后将组的 Value 合并到一起
    val allConditionData = randomArray.groupBy(_.condition)
    val conditionRandomSet = Random.shuffle(allConditionData.keySet)
    val blockBuffer = conditionRandomSet
      .foldLeft(new ArrayBuffer[TrialData]())((arrays, i) => arrays ++= allConditionData(i))

    //如果是多个条件，则中间插入休息
    if (DO_MULTIPLE_CONDITION_NUMBERS.size > 1) {
      //将休息 Screen 插入到 singleBlockSize 可以整除的前一个，除了第一个和最后一个
      val singleBlockSize = allConditionData.head._2.length
      for (i <- blockBuffer.indices) {
        if (i != blockBuffer.size && i != 0 && i % singleBlockSize == 0) {
          op(blockBuffer(i), true)
        } else {
          op(blockBuffer(i), false)
        }
      }
    } else {
      //如果是单条件，则不休息
      blockBuffer.foreach(block => op(block, false))
    }
  }
}

class MzjExperiment extends Experiment {

  private val logger = LoggerFactory.getLogger(classOf[MzjExperiment])

  private final val learn_count = "在本实验中，首先呈现一个黑色 \"+\" ，请调整并保持对于黑色 \"+\" 的注视。\n\n" +
    "接下来你可能听到一串声音，声音持续一段时间后， \"+\" 将变红，当 \"+\" 消失后，会呈现一组黑色和白色组成的点阵，\n\n" +
    "请尽可能快的统计出现的所有点的个数，然后立刻大声、清楚的报告。\n\n接下来开始练习。"

  private final val real_count = "在本实验中，首先呈现一个黑色 \"+\" ，请调整并保持对于黑色  \"+\" 的注视。\n\n" +
    "接下来你可能听到一串声音，声音持续一段时间后， \"+\" 将变红，当 \"+\" 消失后，会呈现一组黑色和白色组成的点阵，\n\n" +
    "请尽可能快的统计出现的所有点的个数，然后立刻大声、清楚的报告。\n\n接下来开始正式实验。"

  private final val learn_estimate = "在本实验中，首先呈现一个黑色 \"+\" ，请调整并保持对于黑色 \"+\" 的注视。\n\n" +
    "接下来你可能听到一串声音，声音持续一段时间后， \"+\" 将变红，当 \"+\" 消失后，会呈现一组黑色和白色组成的点阵，\n\n" +
    "请尽可能快的估计出现的所有点的个数，如果不确定，则进行猜测，然后在出现问号后立刻大声、清楚的报告。\n\n接下来开始练习。"

  private final val real_estimate = "在本实验中，首先呈现一个黑色 \"+\" ，请调整并保持对于黑色 \"+\" 的注视。\n\n" +
    "接下来你可能听到一串声音，声音持续一段时间后， \"+\" 将变红，当 \"+\" 消失后，会呈现一组黑色和白色组成的点阵，\n\n" +
    "请尽可能快的估计出现的所有点的个数，如果不确定，则进行猜测，然后在出现问号后大声、清楚的报告。\n\n接下来开始正式实验。"


  //包含 CountingTrial 和 EstimateTrial 两种 Block
  override protected def initExperiment(): Unit = {
    val width = javafx.stage.Screen.getPrimary.getVisualBounds.getWidth.toInt
    val height = javafx.stage.Screen.getPrimary.getVisualBounds.getHeight.toInt
    println(s"Width $width, Height $height")

    val countFirst = Random.nextBoolean()
    //如果指定 COUNT_FIRST，那么就用指定，否则使用随机
    if (JUST_COUNT_FIRST || countFirst) {
      if (JUST_COUNTING) {
        initCountingTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      } else if (JUST_ESTIMATE) {
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      } else {
        initCountingTrial(DO_MULTIPLE_CONDITION_NUMBERS)
        trials.add(new RestTrial(60).initTrial())
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      }
    } else {
      if (JUST_COUNTING) {
        initCountingTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      } else if (JUST_ESTIMATE) {
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      } else {
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
        trials.add(new RestTrial(60).initTrial())
        initCountingTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      }
    }
  }


  //对 Condition2 即呈现的数字、指导语、练习、正式试验进行选择 - 计数
  //同时添加 trial 到 trials 队列
  private def initCountingTrial(conditions: Set[Int]): Unit = {
    //实验一 计数
    //指导语和练习
    trials.add(new IntroTrial(learn_count).initTrial())
    withConditionAndRepeat(conditions)(CONDITION2,CONDITION2_TEST_CHOOSE_NUMBER)(TEST_TRIAL_COUNT)((data, _) => {
      data.taskCondition = COUNTING
      data.isExercise = true
      trials.add(new CountingTrial(data, experimentData, false).initTrial())
    })
    //指导语和正式实验
    trials.add(new IntroTrial(real_count).initTrial())
    withConditionAndRepeat(conditions)(CONDITION2)(TRIAL_COUNT)((data, rest) => {
      data.taskCondition = COUNTING
      trials.add(new CountingTrial(data, experimentData, rest).initTrial())
    })
  }

  //对 Condition2 即呈现的数字、指导语、练习、正式试验进行选择 - 感数
  //同时添加 trial 到 trials 队列
  private def initEstimateTrial(conditions: Set[Int]): Unit = {
    //实验二 感数
    //指导语和练习
    trials.add(new IntroTrial(learn_estimate).initTrial())
    withConditionAndRepeat(conditions)(CONDITION2,CONDITION2_TEST_CHOOSE_NUMBER)(TEST_TRIAL_COUNT)((data, _) => {
      data.taskCondition = ESTIMATE
      data.isExercise = true
      trials.add(new EstimateTrial(data, experimentData, false).initTrial())
    })
    //指导语和正式实验
    trials.add(new IntroTrial(real_estimate).initTrial())
    withConditionAndRepeat(conditions)(CONDITION2)(TRIAL_COUNT)((data, rest) => {
      data.taskCondition = ESTIMATE
      trials.add(new EstimateTrial(data, experimentData, rest).initTrial())
    })
  }

  /**
    * 保存数据到 obj 二进制文件中
    */
  override def saveData(): Unit = {
    logger.info("Saving Experiment")
    println(experimentData)
    val id = experimentData.userId
    ExperimentData.persistToObject(Paths.get(id + ".obj"), experimentData)
    ExperimentData.persistToCSV(Paths.get(id + ".csv"), experimentData)
    try {
      logger.info("Moving Log data to id.log now...")
      val oldFile = Paths.get(System.getProperty("user.dir") + File.separator + "log" + File.separator + "logFile.log").toFile
      val newFile = Paths.get(System.getProperty("user.dir") + File.separator + "log" + File.separator + id + ".log")
      if (oldFile.exists()) {
        Files.copy(oldFile.toPath, newFile, StandardCopyOption.REPLACE_EXISTING)
      }
    } catch {
      case e: Throwable => e.printStackTrace(System.err)
    }
  }
}

/**
  * 计数 Trial 的定义，其中包含声音训练、刺激屏幕和结束后的等待屏幕
  * @param trialData 需要注入一个 Trial 的数据回调类
  * @param experimentData 在某个合适的时候，将 TrialData 写入到 ExperimentData 中
  */
class CountingTrial(val trialData: TrialData, val experimentData: ExperimentData, val needRestScreen: Boolean) extends Trial {

  override def initTrial(): Trial = {
    //学习、测验、等待，其中测验用于处理回调，保存数据，data 中刺激开始的时候在 TaskScreen 开始的时候
    val learn_screen = new VoiceTrainScreen(trialData, experimentData)
    val task_screen = new TaskScreen(true,false, trialData, experimentData)
    val action_wait_screen = ScreenBuilder.named("试次等待屏幕")
      .showIn(COUNTING_ACTION_SCREEN_KEEP_TIME_MS).setScene({
      val root = new HBox()
      root.setAlignment(Pos.CENTER)
      val text = new Text("...")
      text.setFont(Font.font(30))
      root.getChildren.addAll(text)
      root.setCursor(Cursor.NONE)
      root
    }).afterShowThen((_,_) => {
      //在此处定义终止时刻
      trialData.endInstant = Instant.now()
    }).build()
    screens.add(learn_screen.initScreen())
    screens.add(task_screen.initScreen())
    screens.add(action_wait_screen.initScreen())
    if (needRestScreen) screens.add(new RestScreen().initScreen())
    information = "Counting Trial"
    this
  }
}

/**
  * 感数 Trial 的定义
  * @param trialData 提供的关于刺激的信息
  * @param experimentData 提供的用于写会刺激集合的信息
  */
class EstimateTrial(val trialData: TrialData, val experimentData: ExperimentData, val needRestScreen: Boolean) extends Trial {

  override def initTrial(): Trial = {
    //练习、刺激、掩蔽、反应
    //区分计数，这里的开始时刻是在 VoiceTrainScreen 开始，在 ActionWait 记录（反应时是从呈现刺激到反应的时间），在 ActionWait 终止
    val learn_screen = new VoiceTrainScreen(trialData, experimentData)
    val task_screen = new TaskScreen(false,true, trialData, experimentData)
    val mask_screen = ScreenBuilder.named("掩蔽刺激屏幕").setScene(ShelterUtils.getRandomShelter).showIn(MASK_SCREEN_KEEP_TIME_MS).build()
    val action_wait_screen = new EstimateActionRequireScreen(trialData, experimentData)
    screens.add(learn_screen.initScreen())
    screens.add(task_screen.initScreen())
    screens.add(mask_screen.initScreen())
    screens.add(action_wait_screen.initScreen())
    if (needRestScreen) screens.add(new RestScreen().initScreen())
    information = "Estimate Trial"
    this
  }
}

/**
  * 声音播放屏幕，根据基线、节律、非节律等 Condition1 的条件来决定不同的行为
  * 在呈现此屏幕的时候，开始播放声音，定义一个 TrialData 的开始时刻 startInstant
  * @param trialData 保存实验刺激关键信息
  */
class VoiceTrainScreen(val trialData: TrialData, val experimentData: ExperimentData) extends ScreenAdaptor {

  val condition: Int = trialData.condition

  val showAndUntilToRedCrossTime: Int = ALL_CROSS_KEEP_TIME - RED_CROSS_KEEP_TIME()

  val cross = new Text("+")

  private def playIt(condition: Int): Unit = {
    val runnable = new Runnable {
      override def run(): Unit = {
        if (condition == 2)
          WaveUtil.playForDuration(
            (experimentData.prefHz * SUBJECT_HEIGHT_HZ_RATIO).toInt,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            ((1000 - SUBJECT_HZ * SOUND_TIME_MS)/SUBJECT_HZ).toInt)
        else if (condition == 3)
          WaveUtil.playForDuration(
            experimentData.prefHz,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            ((1000 - SUBJECT_HZ * SOUND_TIME_MS)/SUBJECT_HZ).toInt)
        else if (condition == 4)
          WaveUtil.playForDuration(
            (experimentData.prefHz * SUBJECT_LOWER_HZ_RATIO).toInt,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            ((1000 - SUBJECT_HZ * SOUND_TIME_MS)/SUBJECT_HZ).toInt)
        else if (condition == 5)
          WaveUtil.playForDuration(
            experimentData.prefHz,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            0,
            SOUND_RANDOM_MAX_SPACE_MS)
      }
    }
    new Thread(runnable).start()
  }

  override def callWhenShowScreen(): Unit = {
    trialData.startInstant = Instant.now()
    //根据条件不同设置节律、非节律、无声音三种条件
    playIt(condition)
    //设置定时器在特定时间将 Cross 变成红色
    LabUtils.doAfter(showAndUntilToRedCrossTime) {
      cross.setFill(Color.RED)
    }
  }

  override def initScreen(): Screen = {
    duration = ALL_CROSS_KEEP_TIME
    val root = new HBox(); root.setAlignment(Pos.CENTER)
    cross.setFont(Font.font(100))
    cross.setFill(Color.BLACK)
    root.getChildren.add(cross)
    layout = root
    layout.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;")
    information = {
      if (condition == 1) {
        "基线条件屏幕"
      } else if (condition == 5) {
        "非节律条件屏幕"
      } else if (condition == 2) {
        "节律 Low 条件屏幕"
      } else if (condition == 3) {
        "节律 Normal 条件屏幕"
      } else if (condition == 4) {
        "节律 Height 条件屏幕"
      } else {
        ""
      }
    }
    root.setCursor(Cursor.NONE)
    this
  }

  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = { }
}

class IntroTrial(val intro:String) extends Trial {

  override def initTrial(): Trial = {
    screens.add(new IntroScreen(intro).initScreen())
    information = "指导语试次"
    this
  }
}

class IntroScreen(val intro:String) extends ScreenAdaptor {

  val text = new Text()

  override def callWhenShowScreen(): Unit = {
    text.setWrappingWidth(getScene.getWidth / 2)
  }

  override def initScreen(): Screen = {
    val a = new BorderPane()
    text.setFont(Font.font("STHeiti",30))
    text.setText(intro)
    text.setLineSpacing(10)
    val next = new Text()
    next.setText("点击空格开始实验")
    next.setFont(Font.font("STKaiti", 20))
    next.setFill(Color.DARKGRAY)
    a.setCenter(text)
    a.setBottom(next)
    BorderPane.setAlignment(next,Pos.CENTER)
    BorderPane.setMargin(next, new Insets(0,0,30,0))
    layout = a
    duration = INTRO_MAX_KEEP
    information = "指导语屏幕"
    layout.setCursor(Cursor.NONE)
    this
  }

  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = {
    ifKeyButton(KeyCode.SPACE, event) {
      goNextScreenSafe
    }
  }
}

/**
  * 任务屏幕，根据计数、感数的不同决定不同的策略，在此屏幕展示时定义开始时间，在此屏幕结束后，定义计数的反应时刻、反应时
  * 但是对于感数任务，则不定义反应，因为尚未开始报告
  * TrialData 中定义了需要呈现的点的个数信息和节律非节律信息
  * @param isCounting 是否计数
  * @param isStimulate 是否感数
  * @param trialData 实验数据
  * @param experimentData 写入实验数据的最终数据集
  */
class TaskScreen(val isCounting: Boolean = false, val isStimulate: Boolean = false,
                 val trialData: TrialData, val experimentData: ExperimentData) extends ScreenAdaptor {
  var receivedMoveEvent = false
  var showThisScreenNanoTime = 0L

  override def callWhenShowScreen(): Unit = {
    showThisScreenNanoTime = System.nanoTime()
  }

  override def initScreen(): Screen = {
    if (isCounting) duration = COUNTING_MAX_KEEP_TIME_MS
    else if (isStimulate) duration = STIMULATE_KEEP_TIME_MS
    val root = new BorderPane()
    val group = new Group()
    val points = new Points(trialData.pointNumber,
      Random.nextInt(trialData.pointNumber),
      CIRCLE_RADIUS_PIXEL,
      BOUND_WIDTH, BOUND_HEIGHT).points
    group.getChildren.addAll(points:_*)
    root.setCenter(group)
    layout = root
    layout.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;")
    information = if (isCounting) "计数任务屏幕" else "感数任务屏幕"
    layout.setCursor(Cursor.NONE)
    this
  }

  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = {
    //如果是计数，那么记录反应时、记录时刻，并且将数据添加到 ExperimentData 中
    if (isCounting && event.getEventType.toString == "EVENT" && !receivedMoveEvent) {
      receivedMoveEvent = true
      trialData.actionTime = (System.nanoTime() - showThisScreenNanoTime)/1000000
      trialData.recordInstant = Instant.now()
      logger.info("Get Voice Result and Go Now..." + trialData)
      experimentData.trialData.add(trialData)
      goNextScreenSafe
    }
    if (isCounting && IS_DEBUG && !receivedMoveEvent) {
      receivedMoveEvent = true
      trialData.actionTime = (System.nanoTime() - showThisScreenNanoTime)/1000000
      trialData.recordInstant = Instant.now()
      logger.info("WARN - KEY_STIMULATE - Get Result and Go Now..." + trialData)
      experimentData.trialData.add(trialData)
      goNextScreenSafe
    }
  }
}

/**
  * 对于感数 Trial，要求被试在此刺激语音接受报告，在此处定义反应时刻、计算反应时（呈现刺激屏幕+掩蔽刺激屏幕+此屏幕时长）
  * @param trialData 提供此 Trial 的条件信息
  * @param experimentData 用户回调写会 Trial 的数据集
  */
class EstimateActionRequireScreen(val trialData: TrialData, val experimentData: ExperimentData) extends ScreenAdaptor {
  var receivedMoveEvent = false
  var showThisScreenNanoTime = 0L

  override def initScreen(): Screen = {
    duration = ESTIMATE_ACTION_SCREEN_KEEP_TIME_MS
    information = "等待反应屏幕"
    val root = new HBox()
    root.setAlignment(Pos.CENTER)
    val text = new Text("Number ?")
    text.setFont(Font.font(50))
    root.getChildren.addAll(text)
    root.setStyle("-fx-background-color: transparent;-fx-border-color: transparent;")
    layout = root
    layout.setCursor(Cursor.NONE)
    this
  }

  override def callWhenShowScreen(): Unit = {
    showThisScreenNanoTime = System.nanoTime()
  }

  override def callWhenLeavingScreen(): Unit = {
    trialData.actionTime =
      (System.nanoTime() - showThisScreenNanoTime)/1000000 + MASK_SCREEN_KEEP_TIME_MS + STIMULATE_KEEP_TIME_MS
    trialData.recordInstant = Instant.now()
    logger.info("Get Trial Data " + trialData)
    experimentData.trialData.add(trialData)
    trialData.endInstant = Instant.now()
  }

  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = {
    if (event.getEventType.toString == "EVENT" && !receivedMoveEvent) {
      receivedMoveEvent = true
      logger.info("Get Voice Result and Go Now...")
      goNextScreenSafe
    }
  }
}

class RestScreen(val minRestTimeSeconds: Int = 0) extends ScreenAdaptor {

  var showScreenTime: Instant = _

  val info = "请休息，休息结束后，请按空格键继续实验。"

  val text = new Text()

  val infoText = new Text()

  var isDisappearing = false

  override def callWhenShowScreen(): Unit = {
    showScreenTime = Instant.now()
  }

  override def initScreen(): Screen = {
    val r = new VBox()
    r.setAlignment(Pos.CENTER)
    text.setText(info)
    text.setTextAlignment(TextAlignment.CENTER)
    text.setFont(Font.font("STKaiti",40))
    infoText.setTextAlignment(TextAlignment.CENTER)
    infoText.setFont(Font.font("STKaiti",30))
    infoText.setFill(Color.DARKGRAY)
    r.setSpacing(20)
    r.getChildren.addAll(text, infoText)
    layout = r
    duration = MAX_REST_KEEP_TIME
    information = if (minRestTimeSeconds == 0) "可控休息屏幕" else "强制最短休息屏幕"
    layout.setCursor(Cursor.NONE)
    this
  }

  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = {
    ifKeyButton(KeyCode.SPACE, event) {
      if (minRestTimeSeconds != 0) {
        //如果是强制最短休息屏幕，并且现在时间已经超过规定时间，则允许
        val timeDiff = Duration.between(showScreenTime, Instant.now()).abs().getSeconds
        if (timeDiff > minRestTimeSeconds) {
          goNextScreenSafe
        } else {
          infoText.setText("最少还有 " + (minRestTimeSeconds - timeDiff) + " 秒")
          infoText.setVisible(true)
          if (isDisappearing) {
            //什么都不做
          } else {
            //让刺激消失
            isDisappearing = true
            val task = new Task[String]() {
              override def call(): String = {
                TimeUnit.SECONDS.sleep(3)
                infoText.setVisible(false)
                isDisappearing = false
                ""
              }
            }
            new Thread(task).start()
          }
        }
      } else {
        //如果没有强制休息，则继续
        goNextScreenSafe
      }
    }
  }
}

class RestTrial(val minRestSeconds:Int = 10) extends Trial {
  override def initTrial(): Trial = {
    screens.add(new RestScreen(minRestSeconds).initScreen())
    information = "Rest Trial"
    this
  }
}

class VoiceDetectEventMaker(exp: Experiment, scene: Scene) extends EventMaker(exp, scene) {

  val logger: Logger = LoggerFactory.getLogger(classOf[VoiceDetectEventMaker])

  override def run(): Unit = {
    logger.debug("VoiceDetectEventMaker Called now....")
    val capture = new Capture()
    import Utils._
    capture.messageProperty().addListener {
      val value = capture.getMessage.toDouble
      if (value != 0.0) {
        logger.debug("Receive Sound Event Now..., Current Sound Signal is " + value)
        exp.getScreen.eventHandler(new Event(Event.ANY), exp, scene)
      }
    }
    new Thread(capture).start()
  }
}

class MzjApplication extends Application {

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
      setFullScreen(true)
    }
  }

  val estimateBtn = new ToggleButton("感数")
  val countingBtn = new ToggleButton("计数")
  val allBtn = new ToggleButton("随机全部")
  val testBtn = new ToggleButton("测试")
  val fastTestBtn = new ToggleButton("快速测试")
  val realBtn = new ToggleButton("正式实验")
  val c1 = new ToggleButton("基线条件")
  val c2 = new ToggleButton("节律高频条件")
  val c3 = new ToggleButton("节律中频条件")
  val c4 = new ToggleButton("节律低频条件")
  val c5 = new ToggleButton("无节律条件")
  val c6 = new Button("全部")
  val shuffleBtn = new Hyperlink("Tell Me a Screct")

  val start = new Button("RUN EXPERIMENT")

  val name = new TextField()
  val gender = new ChoiceBox[String]()
  val id = new TextField()
  val hz = new TextField()
  val info = new TextField()

  var experimentData:ExperimentData = _

  val set: collection.mutable.Set[Int]= new mutable.HashSet[Int]()

  val root: Parent =  {
    val gridPane = new GridPane()
    val condition1 = new Label("配置面板")
    val ecGroup = new ToggleGroup
    ecGroup.getToggles.addAll(estimateBtn, countingBtn, allBtn)
    val trialGroup = new ToggleGroup
    trialGroup.getToggles.addAll(testBtn, fastTestBtn, realBtn)
    gridPane.add(condition1, 0,0)
    gridPane.add(new Label("实验"),0,1)
    val b1 = new HBox(); b1.getChildren.addAll(estimateBtn, countingBtn, allBtn); b1.setSpacing(5)
    gridPane.add(b1, 1,1)
    gridPane.add(new Label("调试"),0,2)
    val b2 = new HBox(); b2.getChildren.addAll(testBtn, fastTestBtn, realBtn); b2.setSpacing(5)
    gridPane.add(b2, 1,2)
    gridPane.add(new Label("条件"),0,3)
    val b3 = new HBox(); b3.getChildren.addAll(c1,c2,c3,c4,c5,c6); b3.setSpacing(5)

    import MzjExperiment._
    import Utils._

    ecGroup.selectedToggleProperty().addListener {
      if (ecGroup.getSelectedToggle == estimateBtn) {
        JUST_ESTIMATE = true
        JUST_COUNTING = false
      } else if (ecGroup.getSelectedToggle == countingBtn) {
        JUST_ESTIMATE = false
        JUST_COUNTING = true
      } else if (ecGroup.getSelectedToggle == allBtn) {
        JUST_ESTIMATE = false
        JUST_COUNTING = false
      }
    }

    trialGroup.selectedToggleProperty().addListener {
      val selected = trialGroup.getSelectedToggle
      if (selected == testBtn) {
        IS_DEBUG = true
        IS_FAST_DEBUG = false
      } else if (selected == fastTestBtn) {
        IS_DEBUG = true
        IS_FAST_DEBUG = true
      } else if (selected == realBtn) {
        IS_DEBUG = false
        IS_FAST_DEBUG = false
      }
    }

    c1.selectedProperty().addListener(_ => set.add(1))
    c2.selectedProperty().addListener(_ => set.add(2))
    c3.selectedProperty().addListener(_ => set.add(3))
    c4.selectedProperty().addListener(_ => set.add(4))
    c5.selectedProperty().addListener(_ => set.add(5))

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

    ecGroup.selectToggle(allBtn)
    trialGroup.selectToggle(realBtn)

    gridPane.add(b3,1,3)
    val cmd = new HBox(); cmd.setSpacing(8); cmd.getChildren.addAll(start, shuffleBtn)
    GridPane.setConstraints(cmd,0,11,2,2)
    GridPane.setMargin(cmd,new Insets(30,0,0,0))
    gridPane.getChildren.add(cmd)
    gridPane.setAlignment(Pos.CENTER)
    gridPane.setHgap(5)
    gridPane.setVgap(10)

    val userLabel = new Label("被试信息")
    name.setPromptText("姓名")
    name.setText("NoName")
    gender.setItems({
      val a = FXCollections.observableArrayList[String]()
      a.addAll("女","男")
      a
    }); gender.getSelectionModel.selectFirst()
    id.setPromptText("编号")
    id.setText((Random.nextInt(10000) + 23333).toString)
    hz.setText("2000")
    hz.setPromptText("最佳频率")
    info.setPromptText("备注")

    gridPane.add(userLabel, 0,4)
    GridPane.setMargin(userLabel, new Insets(10,0,0,0))
    gridPane.add(new Label("姓名"), 0,5)
    gridPane.add(new Label("性别"),0,6)
    gridPane.add(new Label("编号"),0,7)
    gridPane.add(new Label("频率"),0,8)
    gridPane.add(new Label("备注"),0,9)
    gridPane.add(name,1,5)
    gridPane.add(gender,1,6)
    val detail = new Text("如果是分开做的实验，先输入编号并点击按钮以从 编号.obj 文件中读取原本的信息")
    detail.setFill(Color.DARKGRAY)
    val load = new Button("从 编号.obj 中加载")
    val hbox = new HBox(); hbox.setSpacing(5)
    hbox.getChildren.addAll(id, load)
    gridPane.add(hbox,1,7)
    gridPane.add(hz,1,8)
    gridPane.add(info,1,9)
    gridPane.add(detail, 1,10)

    load.textProperty().bind(
      new SimpleStringProperty("从 ").concat(id.textProperty()).concat(".obj 加载"))
    load.setOnAction(_ => {
      try {
        val data = ExperimentData.loadWithObject(Paths.get(id.getText() + ".obj"))
        this.experimentData = data
        name.setText(this.experimentData.userName)
        id.setText(this.experimentData.userId.toString)
        hz.setText(this.experimentData.prefHz.toString)
        gender.getSelectionModel.select(this.experimentData.gender)
        info.setText(this.experimentData.information)
      } catch {
        case e: Throwable =>
          this.experimentData = null
          val alert = new Alert(AlertType.ERROR)
          alert.setHeaderText("加载出现问题")
          alert.setContentText("无法完成从指定文件进行的加载，请检查文件名和路径，以及读写权限")
          e.printStackTrace(System.err)
          alert.showAndWait()
      }
    })
    gridPane
  }

  val configureScene = new Scene(root, 700, 600)

  override def start(stage: Stage): Unit = {
    stage.setTitle("Rhythm Experiment Configure")
    stage.setScene(configureScene)

    start.setOnAction(_ => {
      if (experimentData == null) {
        //是新实验
        experimentData = new ExperimentData()
        experimentData.userName = name.getText().trim
        experimentData.gender = gender.getSelectionModel.getSelectedIndex
        experimentData.userId = id.getText().toInt
        experimentData.information = info.getText().trim
        experimentData.prefHz = hz.getText().trim.toDouble
      } else {
        //是旧实验
        //如果是旧实验，仅允许添加 Information，但是不允许变更姓名、性别、ID 和 prefHZ
        experimentData.information = info.getText().trim
      }
      MzjExperiment.initExperiment(experimentData, set.toSet)
      val experimentStage = new Stage()
      val helper = new SimpleExperimentHelperImpl(runner)
      helper.initStage(experimentStage)
      experimentStage.setTitle("Rhythm Experiment Runner")
      experimentStage.show()
    })

    shuffleBtn.setOnAction(_ => {
      val alert = new Alert(AlertType.INFORMATION)
      alert.setHeaderText("Lucky Order")
      val ints = Random.shuffle(List.range(1,6))
      alert.setContentText(ints.mkString(" - "))
      alert.showAndWait()
    })

    stage.show()
  }
}

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
}
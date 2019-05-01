package com.mazhangjing.rhythm

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.time.{Duration, Instant}
import java.util
import java.util.concurrent.TimeUnit

import com.mazhangjing.lab.LabUtils._
import com.mazhangjing.lab._
import com.mazhangjing.lab.sound._
import com.mazhangjing.lab.unit.iScreen
import com.mazhangjing.rhythm.MzjExperiment._
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.FXCollections
import javafx.concurrent.Task
import javafx.event.{Event, EventType}
import javafx.geometry.{Insets, Pos, Rectangle2D}
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

/**
  * 在 2019-04-15 以及之前的实验中，Log 日志中的 节律 Low 指的是 节律 Height，反之亦然。2019-04-15 修正了此 Bug。
  * 2019年04月15日 修复了 Configure 点选后取消点选的 BUG
  */
object MzjExperiment {
  val screenWidthPx: Double = javafx.stage.Screen.getPrimary.getVisualBounds.getWidth
  val screenHeightPx: Double = javafx.stage.Screen.getPrimary.getVisualBounds.getHeight
  val screenWidthCm: Double = (javafx.stage.Screen.getPrimary.getVisualBounds.getWidth/javafx.stage.Screen.getPrimary.getDpi) * 2.54
  val peopleToScreenCm = 57
  val is = iScreen(screenWidthPx, screenWidthCm, peopleToScreenCm)

  var IS_DEBUG = true //关闭后，严格执行实验，开启后，且开启 FAST_DEBUG，规则最为宽松，且关闭 FAST_DEBUG，仅仅是允许键盘替代语音
  var IS_FAST_DEBUG = true //开启后，规则最为宽松，关闭后，相比较正式实验，仅允许键盘替代语音反应计数
  var TRIAL_COUNT = 20 //真实的 Trial 重复数
  var TEST_TRIAL_COUNT = 1 //测试的 Trial 重复数
  val PEOPLE_TO_SCREEN_CM = 57 //人距离屏幕的 CM 数
  val INTRO_MAX_KEEP = 2000000 //指导语最长超时
  val USE_RANDOM_TRIAL_ORDER = true //真实模式随机计数和感数 Block
  var JUST_COUNT_FIRST = false //调试模式固定先计数，再感数
  val RED_CROSS_KEEP_TIME: () => Int = () => Random.nextInt(500) + 500 //500 - 1000 红色十字呈现的时长
  var ALL_CROSS_KEEP_TIME = 8000 //声音-注视点刺激呈现的时长
  var SOUND_TIME_MS = 20 //单个声音呈现的时长
  val SOUND_RANDOM_MAX_SPACE_MS = 130 //随机无节律声音间隔的差值，根据文献设置
  var SUBJECT_HZ = 0.0 //被试的自然频率
  val SUBJECT_HEIGHT_HZ_RATIO = 1.5//1.25
  val SUBJECT_LOWER_HZ_RATIO = 0.5//0.75
  val SOUND_HZ = 300 //声音的频率
  val COUNTING_MAX_KEEP_TIME_MS = 100000 //计数阶段刺激最长呈现时长
  val STIMULATE_KEEP_TIME_MS = 200 //感数阶段刺激呈现时长
  val COUNTING_ACTION_SCREEN_KEEP_TIME_MS = 1000
  val ESTIMATE_ACTION_SCREEN_KEEP_TIME_MS = 4000 //感数阶段最长允许反应时间，之后进入下一个试次
  val MASK_SCREEN_KEEP_TIME_MS = 150 //掩蔽刺激呈现时长
  val CIRCLE_RADIUS_PIXEL: Double = is.degToScreenPx(1.2) //点的半径，像素个数
  val WIDTH_RATIO = 0.5 //点的范围所在的屏幕宽度的比例
  val HEIGHT_RATIO = 0.5 //点的范围所在的屏幕高度的比例

  val MAX_REST_KEEP_TIME_MS = 10000000 //最长休息时间，最短时间由类自行控制
  val SIMPLE_REST_MIN_SECONDS = 60
  val EXPERIMENT_MIN_REST_SECONDS = 120

  val BOUND_WIDTH: (Double, Double) = getBound(WIDTH_RATIO, HEIGHT_RATIO)._1
  val BOUND_HEIGHT: (Double, Double) = getBound(WIDTH_RATIO, HEIGHT_RATIO)._2
  var SOUND_IMPL_CLASS: Class[_] = _

  def getSoundImpl: ToneUtils = {
    if (SOUND_IMPL_CLASS != null) {
      SOUND_IMPL_CLASS.newInstance().asInstanceOf[ToneUtils]
    } else throw new RuntimeException("没有可选的 SoundImpl 实现")
  }

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
      if (TRIAL_COUNT > 2) TRIAL_COUNT = 2
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
  private def getBound(widthRatio: Double, heightRatio: Double): ((Double, Double),(Double, Double)) = {
    val needBorderWidthPx: Double = is.degToScreenPx(16)
    (((screenWidthPx - needBorderWidthPx)/2, (screenWidthPx - needBorderWidthPx)/2 + needBorderWidthPx),
      ((screenHeightPx - needBorderWidthPx)/2, (screenHeightPx - needBorderWidthPx)/2 + needBorderWidthPx))
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
          1 to randomRepeat foreach( _ => {
            val data = new TrialData(condition1, condition2)
            buffer.append(data)
          })
        })
      } else {
        //如果是练习，则随机选择 randomGetNumber 个数字
        val ints = Random.shuffle(numberCondition).toBuffer.slice(0, randomGetNumber)
        ints foreach( condition2 => {
          1 to randomRepeat foreach( _ => {
            val data = new TrialData(condition1, condition2)
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
        trials.add(new RestTrial().initTrial())
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      }
    } else {
      if (JUST_COUNTING) {
        initCountingTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      } else if (JUST_ESTIMATE) {
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
      } else {
        initEstimateTrial(DO_MULTIPLE_CONDITION_NUMBERS)
        trials.add(new RestTrial().initTrial())
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
    val name = experimentData.userName match {
      case null => "NoName"
      case other => other
    }
    ExperimentData.persistToObject(Paths.get(id + ".obj"), experimentData)
    ExperimentData.persistToCSV(Paths.get(id + "_" + name + ".csv"), experimentData)
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
    if (needRestScreen) screens.add(new RestScreen(SIMPLE_REST_MIN_SECONDS).initScreen())
    //学习、测验、等待，其中测验用于处理回调，保存数据，data 中刺激开始的时候在 TaskScreen 开始的时候
    val learn_screen = new VoiceTrainScreen(trialData, experimentData)
    val task_screen = new TaskScreen(true,false, trialData, experimentData)
    val action_wait_screen = new ScreenBuilder().named("试次等待屏幕")
      .showIn(COUNTING_ACTION_SCREEN_KEEP_TIME_MS).setScene({
      val root = new HBox()
      root.setAlignment(Pos.CENTER)
      val text = new Text("")
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
    if (needRestScreen) screens.add(new RestScreen(SIMPLE_REST_MIN_SECONDS).initScreen())
    //练习、刺激、掩蔽、反应
    //区分计数，这里的开始时刻是在 VoiceTrainScreen 开始，在 ActionWait 记录（反应时是从呈现刺激到反应的时间），在 ActionWait 终止
    val learn_screen = new VoiceTrainScreen(trialData, experimentData)
    val task_screen = new TaskScreen(false,true, trialData, experimentData)
    val mask_screen = new ScreenBuilder().named("掩蔽刺激屏幕").setScene(ShelterUtils.getRandomShelter).showIn(MASK_SCREEN_KEEP_TIME_MS).build()
    val action_wait_screen = new EstimateActionRequireScreen(trialData, experimentData)
    screens.add(learn_screen.initScreen())
    screens.add(task_screen.initScreen())
    screens.add(mask_screen.initScreen())
    screens.add(action_wait_screen.initScreen())
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

  val toneUtils: ToneUtils = getSoundImpl

  private def playIt(condition: Int): Unit = {
    val runnable = new Runnable {
      override def run(): Unit = {
        if (condition == 2)
          toneUtils.playForDuration(
            SOUND_HZ,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            ((1000 - SUBJECT_HZ * SOUND_TIME_MS)/(SUBJECT_HZ * SUBJECT_HEIGHT_HZ_RATIO)).toInt)
        else if (condition == 3)
          toneUtils.playForDuration(
            SOUND_HZ,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            ((1000 - SUBJECT_HZ * SOUND_TIME_MS)/SUBJECT_HZ).toInt)
        else if (condition == 4)
          toneUtils.playForDuration(
            SOUND_HZ,
            SOUND_TIME_MS,
            showAndUntilToRedCrossTime,
            ((1000 - SUBJECT_HZ * SOUND_TIME_MS)/(SUBJECT_HZ * SUBJECT_LOWER_HZ_RATIO)).toInt)
        else if (condition == 5)
          toneUtils.playForDuration(
            SOUND_HZ,
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
    cross.setFont(Font.font(30))
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
        "节律 Height 条件屏幕"
      } else if (condition == 3) {
        "节律 Normal 条件屏幕"
      } else if (condition == 4) {
        "节律 Low 条件屏幕"
      } else {
        ""
      }
    }
    root.setCursor(Cursor.NONE)
    this
  }

  override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit = { }
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
    val points = new Points(
      trialData.pointNumber,
      Random.nextInt(trialData.pointNumber),
      CIRCLE_RADIUS_PIXEL.toInt,
      BOUND_WIDTH, BOUND_HEIGHT,
      true,
      is.pxInScreenToDeg(2).toInt).points
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
      //import collection.JavaConverters._
      /*if (experimentData.trialData.asScala.exists(_.actionTime == trialData.actionTime)) {
        println(experimentData.trialData)
        println(trialData)
      }*/
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

class RestScreen(val minRestTimeSeconds: Int) extends ScreenAdaptor {

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
    duration = MAX_REST_KEEP_TIME_MS
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

class RestTrial extends Trial {
  override def initTrial(): Trial = {
    screens.add(new RestScreen(EXPERIMENT_MIN_REST_SECONDS).initScreen())
    information = "Rest Trial"
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

class IntroTrial(val intro:String) extends Trial {

  override def initTrial(): Trial = {
    screens.add(new IntroScreen(intro).initScreen())
    information = "指导语试次"
    this
  }
}
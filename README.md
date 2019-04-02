# Psy4J

> A cognitive science package based on object-oriented programming ideas. The program is driven by the JavaFx framework and the JVM platform.

Psy4J means **Psychology ToolKit For Java Virtual Machine Platform**，The program is written in Java 8 and Scala 2.12.6 and built on the JavaFx 2 GUI platform.


## Structure

![](http://static2.mazhangjing.com/20190218/4f4630d_psy4j.png)

## Update Log

### Version 1.2.10

添加了 WaveUtil，提供快速获取连续间隔以及随机间隔的，指定频率数、呈现长度、空白长度、总共呈现长度的纯音刺激的 API。

添加了 ExperimentHelper 接口，并且将 Main.java 和 Application 类解耦，将原本 Main.java 中的代码使用 SimpleExperimentHelperImpl 提供实现。

这种设计可以允许在任何的 Application 中创建一个 ExperimentHelper，其中 ExperimentHelper 包含了所有的 Experiment 信息，以及其调用策略，通过调用 `ExperimentHelper#initStage` 为 Application 创建一个 Scene，并将其和 Stage 绑定。

如下是现在 Psy4J 的使用步骤：

```scala
class AppDemo extends Application {
  val runner = new ExpRunner {
    override def initExpRunner(): Unit = {
      setEventMakerSet(null)
      val set = new util.HashSet[OpenedEvent]()
      set.add(OpenedEvent.KEY_PRESSED)
      setOpenedEventSet(set)
      setExperimentClassName("com.mazhangjing.demo.CmExperiment")
      setTitle("DEMO TITLE")
      setVersion("0.0.1")
      setFullScreen(true)
    }
  }
  override def start(stage: Stage): Unit = {
    val helper: ExperimentHelper = new SimpleExperimentHelperImpl(runner)
    helper.initStage(stage)
    stage.setTitle("Hello")
    stage.show()
  }
}
```

可以看到，现在的 Psy4J 更接近一个简单的工具，而不是一个必须调用的很重的 Application —— 这种更接近创建一个简单的 JavaFx GUI 界面的写法允许将 Experiment 嵌入任意的 JavaFx 程序中。

### Version 1.2.9

修正了 Main.java 中呈现 Screen 时调用 callBefore 和 callAfter 时发生错误的处理方式。

### Version 1.2.8

修正了 ScreenBuilder 中无法快速的处理事件 —— 执行跳转的问题。

添加了实用工具类：角度和像素转换工具。添加了实用工具类：根据 YAML 配置自动执行命令的启动器

![](http://static2.mazhangjing.com/20190324/21f10b6_cm_image2019-03-2417.19.11.png)

### Version 1.2.7

增强了 DataUtils 类的功能，修正了一些细节问题。

将 `getSD` 拆分为 `getSimpleSD` 和 `getTotalSD`， 将 `walkAndProcess` 拆分为 `walkAndProcess` 以及 `walkAndProcessAround`。

后者当遍历完一个文件夹后，统一执行操作，将 `doWithFile` 拆分为 `doWithLine` 和 `doWithTable`，前者将 `Stream[String]` 转换为 `Stream[D]`，之后，后者相比前者，又进行了 `Stream[D]` 到 `S` 的转换。

因为在实际中发现，或许除了 `Data` 表示每行的数据之外，还需要 `Subject` 定义一些对于整张表的方法，比如对每行数据进行的分组策略，每个被试的实验条件信息等。

下面是一个使用 DataUtils 处理，且没有 Subject 层的示例文档：

```scala
object DataBatch {

  /**
    * 将每行转换成对应的数据结构 - 必须为 12 行数据，如果为 11 行，则伪造最后一行为全部正确
    *
    * @param line 如果每行不齐，则返回伪造数据，用于最后的对齐，反之，则装载真正数据
    * @return 伪造或者真实的数据，通过 real 属性辨别
    */
  def lineToData(line: String): Option[Data] = 
    if (line.contains("ID, SHOW_TIME,")) return None
    var s = line.split(",").filterNot(_.isEmpty).map(_.trim).map(_.replace("$",""))
    if (s.length != 12) {
      println("Get Wrong Line => " + line)
      if (s.length == 11) s = fakeLine(s)
      else return Option(fakeData)
    }
    val data = Data(
      s(0), s(1).toLong, s(2).toLong, s(3).toLong, s(4).toLong, s(5).toLong, s(6).toLong,
      if (s(7).toInt == 0) false else true, s(8).toLong,
      if (s(9).toInt == 0) false else true, s(10), if (s(11).toInt == 0) false else true
    )
    Option(finalCheck(data))
  }

  def dataToSubject(in: Stream[Data]): Subject = Subject(in.toArray)

  var duration_time_bad_line = 0

  def finalCheck(in:Data):Data = {
    if (in.duration_time_ms < 250) {
      duration_time_bad_line += 1
      in.real = false
      in
    }
    else in
  }

  def fakeLine(s: Array[String]): Array[String] = {
    println(s"Fake Line to ${s.mkString(",") + ",1"}")
    s ++ Array("1")
  }

  def fakeData: Data = Data("0",0L,0L,0L,0L,0L,0L,size_is_big = false,0L, sti_is_left = false,"", answer = false, real = false)
  
  case class Subject(lines: Array[Data])

  def invoke(path: Path, isPreExp:Boolean = false): Path =
    walkAndProcess(path, _.toFile.toString.endsWith(".csv"))(
      f => {
        implicit val sb: StringBuilder = new StringBuilder
        doWithTable(f,
          check = c => {
            println(s"Total Line is ${c.filterNot(_.isEmpty).size}")
            sb.append(s"Total Line is ${c.filterNot(_.isEmpty).size}\n")
          },
          convert = lineToData,
          collect = dataToSubject)(
          s => {
            val i = s.lines
            if (isPreExp) i.foreach(_.isPreExp = true)
            val all_removed_data_length = i.filterNot(_.real).toList.size
            val b = i.toArray.filter(_.real)
            val a = b.filter(_.correct)
            val correct_wrong_count = b.length - a.length
            if (a.forall(_.isPreExp)) {
              val left_late = a.filter(d => d.sti_is_left && d.order_is_late).map(_.duration_time_ms.toDouble)
              val left_early = a.filter(d => d.sti_is_left && !d.order_is_late).map(_.duration_time_ms.toDouble)
              val right_late = a.filter(d => !d.sti_is_left && d.order_is_late).map(_.duration_time_ms.toDouble)
              val right_early = a.filter(d => !d.sti_is_left && !d.order_is_late).map(_.duration_time_ms.toDouble)

              val ll_res = filterInSD(2)(left_late, getSD(left_late))
              val le_res = filterInSD(2)(left_early, getSD(left_early))
              val rl_res = filterInSD(2)(right_late, getSD(right_late))
              val re_res = filterInSD(2)(right_early, getSD(right_early))

              sb.append(s"CHECK_BY = JI_OU\n")
              sb.append(s"For LEFT_LATE: ${ll_res.sum / ll_res.length} ms, SD is ${getSD(ll_res)} ms\n")
              sb.append(s"For LEFT_EARLY: ${le_res.sum / le_res.length} ms, SD is ${getSD(le_res)} ms\n")
              sb.append(s"For RIGHT_LATE: ${rl_res.sum / rl_res.length} ms, SD is ${getSD(rl_res)} ms\n")
              sb.append(s"For RIGHT_EARLY: ${re_res.sum / re_res.length} ms, SD is ${getSD(re_res)} ms\n")
              sb.append("\n\n\n")

            } else if (a.forall(_.check_by == "ORDER")) {
              val left_late = a.filter(d => d.sti_is_left && d.order_is_late).map(_.duration_time_ms.toDouble)
              val left_early = a.filter(d => d.sti_is_left && !d.order_is_late).map(_.duration_time_ms.toDouble)
              val right_late = a.filter(d => !d.sti_is_left && d.order_is_late).map(_.duration_time_ms.toDouble)
              val right_early = a.filter(d => !d.sti_is_left && !d.order_is_late).map(_.duration_time_ms.toDouble)

              val ll_res = filterInSD(2)(left_late, getSD(left_late))
              val le_res = filterInSD(2)(left_early, getSD(left_early))
              val rl_res = filterInSD(2)(right_late, getSD(right_late))
              val re_res = filterInSD(2)(right_early, getSD(right_early))

              sb.append(s"CHECK_BY = ORDER\n")
              sb.append(s"For LEFT_LATE: ${ll_res.sum / ll_res.length} ms, SD is ${getSD(ll_res)} ms\n")
              sb.append(s"For LEFT_EARLY: ${le_res.sum / le_res.length} ms, SD is ${getSD(le_res)} ms\n")
              sb.append(s"For RIGHT_LATE: ${rl_res.sum / rl_res.length} ms, SD is ${getSD(rl_res)} ms\n")
              sb.append(s"For RIGHT_EARLY: ${re_res.sum / re_res.length} ms, SD is ${getSD(re_res)} ms\n")
              sb.append("\n\n\n")
            } else if (a.forall(_.check_by == "SIZE")) {
              val big_left = a.groupBy(d => d.sti_is_left && d.size_is_big)(true).map(_.duration_time_ms.toDouble)
              val big_right = a.groupBy(d => !d.sti_is_left && d.size_is_big)(true).map(_.duration_time_ms.toDouble)
              val small_left = a.groupBy(d => d.sti_is_left && !d.size_is_big)(true).map(_.duration_time_ms.toDouble)
              val small_right = a.groupBy(d => !d.sti_is_left && !d.size_is_big)(true).map(_.duration_time_ms.toDouble)

              val bl_res = filterInSD(2)(big_left, getSD(big_left))
              val br_res = filterInSD(2)(big_right, getSD(big_right))
              val sl_res = filterInSD(2)(small_left, getSD(small_left))
              val sr_res = filterInSD(2)(small_right, getSD(small_right))
              
              sb.append(s"CHECK_BY = SIZE\n")
              sb.append(s"For BIG_LEFT: ${bl_res.sum / bl_res.length} ms, SD is ${getSD(bl_res)} ms\n")
              sb.append(s"For BIG_RIGHT: ${br_res.sum / br_res.length} ms, SD is ${getSD(br_res)} ms\n")
              sb.append(s"For SMALL_LEFT: ${sl_res.sum / sl_res.length} ms, SD is ${getSD(sl_res)} ms\n")
              sb.append(s"For SMALL_RIGHT: ${sr_res.sum / sr_res.length} ms, SD is ${getSD(sr_res)} ms\n")
              sb.append("\n\n\n")
            } else { }

            val newFile = Paths.get(f.getFileName.toString.replace(".csv", "") + "_final.csv")
            saveTo(newFile) {
              sb.append("ID, SHOW_TIME, ACTION_TIME, SHOW_TIME_MS, DURATION_TIME_MS," +
                "STAND_SIZE, ACTION_SIZE, SIZE_IS_BIG, ACTION_ORDER, STI_IS_LEFT," +
                "CHECK_BY, ANSWER, ORDER_IS_LATE, STAND_ANSWER, CORRECT").append("\n")
              i.foreach(data => {
                import DataConvert._
                sb.append(data.id).append(", ")
                sb.append(data.show_time).append(", ")
                sb.append(data.action_time).append(", ")
                sb.append(data.show_time_ms).append(", ")

                data.duration_time_ms.inSb
                data.stand_size.inSb
                data.action_size.inSb
                data.size_is_big.str.inSb
                data.action_order.inSb
                data.sti_is_left.str.inSb
                data.check_by.inSb
                data.answer.str.inSb
                data.order_is_late.str.inSb
                data.stand_answer.str.inSb
                data.correct.str.endLineInSb
              })
              sb
            }
        })
    })

  def runInJava(isPreExp:Boolean = false): Unit = {
    printToFile(Paths.get("result.log")) {
      invoke(Paths.get("."), isPreExp = isPreExp)
    }
  }
}

/**
  * 数据结构
  */
case class Data(
                 id: String,
                 show_time: Long,
                 action_time: Long,
                 show_time_ms: Long,
                 duration_time_ms: Long,
                 stand_size: Long,
                 action_size: Long,
                 size_is_big: Boolean,
                 action_order: Long,
                 sti_is_left: Boolean,
                 check_by: String,
                 answer: Boolean,
                 var real:Boolean = true,
                 var isPreExp:Boolean = false
               ) {
  /**
    * 对于 ORDER 判断条件的正确答案
    * @return
    */
  def order_is_late: Boolean = {
      if (action_order == 0 || action_order == 1) false
      else if (action_order == 3 || action_order == 4) true
      else throw new RuntimeException(s"ACTION_ORDER 列错误 -> $action_order")
  }

  /**
    * 对于 PRE_EXP 预实验的正确答案
    * @return
    */
  def stand_answer: Boolean = {
    //如果是预实验，那么 奇数返回 false，偶数返回 true
    if (action_order == 0 || action_order == 4) false else true
  }

  /**
    * 根据不同条件的正确答案，结合主试的输入答案，计算出是否正确
    * @return 比较结果
    */
  def correct: Boolean = {
    if (isPreExp) {
      if (stand_answer && answer) true
      else if (!stand_answer && !answer) true
      else false
    } else if (check_by == "SIZE") {
      if (size_is_big && answer) true
      else if (!size_is_big && !answer) true
      else false
    } else if (check_by == "ORDER") {
      if (order_is_late && answer) true
      else if (!order_is_late && !answer) true
      else false
    } else false
  }
}
```

### Version 1.2.6

添加了语音处理类、语音检测使用工具。

修正了 DataUtils getSD 计算时使用的是总体数据，而非样本数据的问题。

2019-03-11 Corkine Ma @ CCNU

### Version 1.2.5

添加了数据处理的 DataUtils 工具类和 DataConvert 数据转换类（均使用 Scala 实现，不提供 Java API，虽然也可以使用 —— 因为使用了一些 Scala 的特殊语法来简化开发，比如隐式类型转换、柯里化、表达式即值等特性）。

此工具类可以用于快速的从一堆文件中，对每个文件直接读取到某种数据结构，然后对数据集合进行处理，之后打印到某个流中。

只用提供数据结构，然后定义从文件的每行向数据结构的转换方法即可。此外，可以在转换之前，在读取之后，进行一些统计性的工作。当转换完成 —— 转换为数据集合后，可以对数据进行各种类型的操作，程序

提供了计算标准差、过滤 N 个标准差的简便方法。此外，也提供了用于快速遍历某个文件夹的快速函数，提供了快速打印输出到文件中保存的快速函数。

2019-03-06 Corkine Ma @ CCNU

### Version 1.2.4

在当前的版本中，使用 Scala 混合 Java 实现了 Psy4J。相比较之前的版本，现在版本只用声明一个文件即可，请参照 com.mazhangjing.demo/experiment 中的例子，创建一个 Experiment 的实现，然后使用 LabUtils 类提供的 Builder 方法创建若干个包裹 Screen 的 Trial，并且将其注入到 Experiment 中，如果需要保存数据，在 Experiment 的方法中提供实现即可。

程序总是运行 Main.java，因此，你需要通过 classpath 下的 invoke.properties 声明需要反射创建的一个叫做 ExpRunner 的实现，这个实现提供了你的实验信息，包括 title、log、experiment 的 className（用于反射创建），以及 EventMaker。

EventMaker 是版本新加入的类，此类实现后，由 ExpRunner 交付给 Main.java，然后后者会通过反射构建 EventMaker 实例，并且将其放入单独的线程中运行。此实例可以通过 Main 注入的 Experiment 实例来操纵 Screen 的 eventHandler 回调方法，提供比如语音检测之类的功能。

2019-02-22 Corkine Ma @ CCNU

### Version 1.0.0

一组带有高精度定时器的类在 com.mazhangjing.lab 中实现。使用者可以参照 com.mazhangjing.zsw 中的结构层次，继承 Screen、Trial、Experiment，然后将其拼装在一起即可。参照 Main.java 中的内容，可以驱动你的 Experiment 运行。

程序并未提供实验刺激的注入方法，没有提供 CSS 默认样式，没有提供日志的记录框架，这保证了程序设计的灵活性，同时隐藏了最为复杂，同时抽象复用率最高的部分 —— 序列调用、定时器自动跳转、阻断跳转、自定义动作和反应。

你需要在 Screen 实现的 initScreen 中提供 duration 超时，以及 layout 呈现的静态结构，你可以在此方法中定义各种组件的 Event，比如按钮、点击、移动等等。Screen 可以由 Trial 的 screens 容器管理，后者在 initTrials 方法中设置，而 Trial 序列由 Experiment 的 trials 属性和 initExperiment 方法管理。在此处注入你的刺激，组建你的 Trial 和 Screen。

Screen 默认注入了 Experiment 和 Scene 对象，这可以方便你预先定义结构的时候，利用这些运行时对象提供独特的行为。除了 initScreen 方法，eventHandler 方法提供了鼠标移入、按键按下、鼠标点击的全局事件注册，你可以根据性能和需要在 Main.java 中处理这部分事件的注册。在这里可以方便的对全局按键进行检测，同时对用户鼠标移动做出反应，比如，取消定时，观察用户的动作。

所有的静态变量存储在枚举 SET.java 中，所有的刺激生成逻辑在 StiFactory.java 中，这些部分由你自己实现。com.mazhangjing.zsw 包中提供了一些实现的示例。

如果你需要保存一些关键信息，可以使用 Experiment 的 setData 或者 setGlobalData 方法，前者保存在 Trial 中，后者保存在 Experiment 中，程序默认在退出界面后调用 saveData 方法进行数据保存，你可以在这里处理数据的序列化。com.mazhangjing.zsw 中提供了一种基于 Log4J 的实现。这部分也由你自己定义。

2018-12-24 Corkine Ma @ CCNU


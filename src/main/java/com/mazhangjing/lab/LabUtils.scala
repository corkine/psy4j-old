/***
  * @version 1.2.3 2019-02-19 LabUtils 添加了 ifMouseButton、ifKeyPress 的精简过入参的方法
  *          1.2.4 2019-03-24 修正了一个逻辑上的问题 —— 在 ScreenBuilder 的 eventHandler 中不能方便的跳转到下一屏幕的问题
  */
package com.mazhangjing.lab

import java.util.{Timer, TimerTask}

import javafx.event.Event
import javafx.scene.input.{KeyCode, KeyEvent, MouseButton, MouseEvent}
import javafx.scene.{Parent, Scene}

import scala.collection.mutable

/**
  * ScreenBuilder 用来使用 Builder 模式快速创建只含有静态内容、固定时间的 Screen，其也可以处理鼠标点击、移动、
  * 按键等预定义事件，可以在被调用之前、之后进行快速的设置
  * （发生在主线程，因此 beforeShowThen 和 afterShowThen 要尽可能轻量，不要在此创建对象）
  *
  * @usecase  ScreenBuilder 可以用在 Trial 中，警告：在 Trial 中不能依靠 Trial 的 getExperiment 等函数设置入参，因为调用 build 的时候
  *           此函数的当前值会被获取，且入参会被作为闭包，固定为空，这导致了严重的错误。
  * @usecase  ScreenBuilder 可以用在 Experiment 中，这时候，可以任意使用外部闭包，比如使用 this 指代 Experiment 对象
  * @usecase  ScreenBuilder
  *             .named("信息收集")
  *             .showIn(SET.INFO_SET_MS.getValue())
  *             .setScene(() -> {
  *               VBox box = new VBox();
  *               Button ok = new Button("确定");
  *               box.getChildren().addAll(ok);
  *               ok.setOnAction(event -> {
  *                 this.terminal.set(1);
  *                 this.setGlobalData(String.format("%s_%s_%s",id.getText(),name.getText(),sex.getText()));
  *               });
  *               return box;
  *             })
  *             .beforeShowThen((exp, scene) -> {
  *               System.out.println("exp = " + exp);
  *               System.out.println("scene = " + scene);
  *               return null;
  *             })
  *             .ifEventThen((event, exp, scene) -> {
  *               System.out.println("event.getEventType() = " + event.getEventType());
  *               return null;
  *             })
  *             .build())
  */
object ScreenBuilder extends Builder[Screen] {
  private var name = ""
  private var showTime = 0
  private var parent: Parent = _
  private var eventAction = mutable.Buffer[(Event, Experiment, Scene, Screen) => Unit]()
  private var preShowAction = mutable.Buffer[(Experiment, Scene) => Unit]()
  private var afterShowAction = mutable.Buffer[(Experiment, Scene) => Unit]()

  /**
    * 给 Screen 命名，在日志中方便识别
    * @param name 字符串名称，比如"刺激展示屏幕"
    * @return ScreenBuilder
    */
  def named(name:String): ScreenBuilder.type = {
    this.name = name; this
  }

  /**
    * Screen 展示的时长，单位为毫秒
    * @param duration 时长，毫秒
    * @return ScreenBuilder
    */
  def showIn(duration:Int): ScreenBuilder.type = {
    this.showTime = duration; this
  }

  /**
    * Screen 展示的内容，可以为静态或者动态
    * @usecase 需要注意，如果展示动态内容，需要调用 Experiment 对象，在 Experiment 中使用 this
    *          注意，在 Trial 实例中，不能使用像是 getExperiment 获取 Experiment 实例，因为此举动会形成闭包
    *          setScene 会在 build 调用时调用此处内容进行设置，此时，闭包中的入参可能为空。
    * @param layout JavaFx 的 Scene 内容接口对象，比如 HBox BorderPane 实例等
    * @return ScreenBuilder
    */
  def setScene(layout: Parent): ScreenBuilder.type = {
    this.parent = layout; this
  }

  /**
    * Screen 展示的内容，可以为静态或者动态（JavaAPI，方便 Java 8 的 Lambda 函数作为入参）
    * @usecase 需要注意，如果展示动态内容，需要调用 Experiment 对象，在 Experiment 中使用 this
    *          注意，在 Trial 实例中，不能使用像是 getExperiment 获取 Experiment 实例，因为此举动会形成闭包
    *          setScene 会在 build 调用时调用此处内容进行设置，此时，闭包中的入参可能为空。
    * @param opLayout JavaFx 的 Scene 内容对象接口，比如 HBox BorderPane 实例等
    * @return ScreenBuilder
    */
  def setScene(opLayout: () => Parent): ScreenBuilder.type = {
    this.parent = opLayout(); this
  }
  //不能调用带参函数，因为调用时外部依赖形成闭包，而这时 Exp 不存在，导致 Scene、Experiment 为空
  //在 Experiment 中使用时，直接用 this 替代即可
  //这是一个调用，这时还不存在 Experiment 值，因此对于函数而言，只能在运行时动态调用，但这又影响了效率...
  //而 initScreen 必须在运行前创建完毕，这决定了不能使用 FP 实现闭包，而要直接从外部获取引用
  /*def setScreenWithExperiment(opLayout: Experiment => Parent): ScreenBuilder.type = {
    if (this.parent != null) throw new IllegalStateException("已经设置过了 Parent")
    this.parentGetFunction = opLayout; this
  }*/
  /**
    * Screen 接受到 GUI 事件的回调函数，动态调用，切勿在此处放置大量需要耗时操作，否则会造成独立事件线程阻塞
    * @param eventAction JavaAPI，接受事件、当前实验、当前场景三个参数，进行操作，返回 null。
    * @return ScreenBuilder
    */
  def ifEventThen(eventAction: (Event, Experiment, Scene, Screen) => Unit): ScreenBuilder.type = {
    this.eventAction += eventAction; this
  }

  /**
    * ScreenQueue 在调用此 Screen 前执行的准备工作，动态调用，调用此处代码需要注意线程阻塞问题。
    * 此处调用时，指针已经移动到当前 Screen 处，layout 属性已经被取出，且设置，不可更改，
    * 但是计时器尚未设置（Platform.runLater 队列的下一个执行），因此可以在此处动态重置计时器。也可以用来执行比如日志等其他操作。
    * @param doAction JavaAPI，接受当前实验、当前场景两个参数，进行操作，返回 null。
    * @return ScreenBuilder
    */
  def beforeShowThen(doAction: (Experiment, Scene) => Unit): ScreenBuilder.type = {
    this.preShowAction += doAction; this
  }

  /**
    * 【此 API 尚未被实现】ScreenQueue 在调用此 Screen 后执行的准备工作，动态调用，调用此处代码需要注意线程阻塞问题。
    * @return ScreenBuilder
    * @deprecated 此 API 尚未被实现
    */
  def afterShowThen(doAction: (Experiment, Scene) => Unit): ScreenBuilder.type = {
    this.afterShowAction += doAction; this
  }

  /**
    * 调用此方法以返回构建器构造的 Screen 对象，自动调用其 initScreen 方法
    * @usecase 需要注意，如果展示动态内容，需要调用 Experiment 对象，在 Experiment 中使用 this
    *          注意，在 Trial 实例中，不能使用像是 getExperiment 获取 Experiment 实例，因为此举动会形成闭包
    *          在 build 调用时调用 setScene 进行设置，此时，闭包中的入参可能为空。
    * @throws IllegalArgumentException 必须调用 showIn 和 setScene 方法设置时间和样式，否则抛出此异常
    * @return 构造好的 Screen 实例
    */
  @throws[IllegalStateException]
  override def build(): Screen = {
    if (showTime == 0 || parent == null)
      throw new IllegalStateException("创建 Screen 定义错误，可能是未设置呈现时长或者呈现样式")
    val result = new Screen {
      //对于原来的工厂模式，因为 Screen 中定义的 initScreen 在 Trial 中定义，而 Trial.initTrial 在 Experiment 的
      //initExperiment 中定义，而后者在注入每个 Screen Experiment 依赖后才调用，因此整体不会出错
      //而使用建造者模式，提前返回了 Screen、Trial 对象，因此，其对象的构造时，调用 getExperiment 返回的必定是空值，
      //也就造成了程序无法正常运行。

      //这些调用时发生在构造之后的，因此，其中如果含有函数，函数不能提前被 Call，否则入参可能为空
      //因此删除了 setScreenWithExperiment，转而使用外部提供的引用，注意，在 Trial 中不能使用 getExperiment 替代，
      //因为 Trial 也创建在 Exp 之前，而在 Experiment 中可以用 this 替代。

      //说白了这是一个 OOP 和 FP 的冲突，也是方法和函数的冲突，方法可以动态定义，依赖类中其它方法，而函数则必须静态定义，
      //但是其优点是可以像值一样传递，非常轻便，而方法则必须寄居在一个类中（太过于沉重）。
      override def initScreen(): Screen = {
        this.information = name
        this.duration = showTime
        this.layout = parent
        this
      }
      //这些调用是动态的，因此不会出错。
      override def callWhenShowScreen(): Unit =
        preShowAction.foreach(function => function(getExperiment, getScene))
      override def callWhenLeavingScreen(): Unit =
        afterShowAction.foreach(f => f(getExperiment, getScene))
      override def eventHandler(event: Event, experiment: Experiment, scene: Scene): Unit =
        eventAction.foreach(func => func(event, experiment, scene, this))
    }
    result.initScreen()
  }
}

/**
  * TrialBuilder 用来使用 Builder 方法创建 Trial 实例
  *
  * @usecase  TrialBuilder.named("测试试次")
  *               .addScreen(
  *                   ScreenBuilder
  *                   .named("准备开始屏幕")
  *                   .showIn(SET.START_CLICK_MS.getValue())
  *                   .setScene(() -> {
  *                      HBox box = new HBox();
  *                      box.setAlignment(Pos.BOTTOM_CENTER);
  *                      Button btn = new Button("START");
  *                      box.getChildren().addAll(btn);
  *                      btn.setOnAction(event -> this.terminal.set(1));
  *                      return box; })
  *                   .build())
  *                .addScreen(new TestStiHeadScreen(array).initScreen())
  *                .addScreen(new TestStiBackScreen(array).initScreen())
  *                .addScreen(
  *                     ScreenBuilder.named("空白屏幕")
  *                     .showIn(SET.ANS_BLANK_MS.getValue())
  *                     .setScene(new HBox())
  *                     .build())
  *           .build()
  */
object TrialBuilder extends Builder[Trial] {
  import scala.collection.JavaConverters._
  private var name = ""
  private val screensInTrail = mutable.Buffer[Screen]()

  /**
    * 给当前 Trial 设置 information 属性，比如 "刺激展示序列"
    * @param name 名称字符串
    * @return TrialBuilder
    */
  def named(name:String): TrialBuilder.type = {
    this.name = name; this
  }

  /**
    * 给当前 Trial 设置 Screen，此 Screen 会被添加到 Trial screens List 的末尾
    * @param screen 试次包含的 Screen 对象
    * @return TrialBuilder
    */
  def addScreen(screen: Screen): TrialBuilder.type = {
    this.screensInTrail += screen; this
  }

  /**
    * 给当前的 Trial 设置 Screen，此 Screen 会被添加到 Trial screens List 的末尾
    * @param opToScreen 试次包含的 Screen 对象， JavaAPI
    * @return TrialBuilder
    */
  def addScreen(opToScreen: () => Screen): TrialBuilder.type = {
    this.screensInTrail += opToScreen(); this
  }

  /**
    * 给当前的 Trial 设置多个 Screen，其会被逐个按顺序添加到 Trial screens List 的末尾
    * @param screens 试次包含的多个 Screen 对象， JavaAPI
    * @return TrialBuilder
    */
  def addScreens(screens: java.util.List[Screen]): TrialBuilder.type = {
    this.screensInTrail ++= screens.asScala; this
  }

  /**
    * 构建 Trial 对象
    * @throws IllegalStateException 必须调用 addScreen 或者 addScreens 方法设置 screens List，List 不能为空
    * @return 构造好的 Trial 实例
    */
  override def build(): Trial = {
    if (screensInTrail.isEmpty)
      throw new IllegalStateException("创建的 Trial 中的 Screens 不能为空")
    val result = new Trial {
      override def initTrial(): Trial = {
        this.information = name
        this.screens = screensInTrail.asJava
        this
      }
    }
    result.initTrial()
  }
}

/**
  * LabUtils 包括 Java API 和 Scala API，其中，Scala API 提供了 implicit 的入参设置，而 Java API 则优化了满足 Java
  * 语法的合理的入参顺序（虽然 implicit 的 Scala API 也可以使用，不过用起来不太自然）。
  * 提供了对于 Mouse Move、Click、KeyPress 的判断。
  * 提供了替代直接操纵 experiment.terminal 的 goNextScreen 方法，包括快速但是线程不安全的版本和线程安全的版本。
  * 提供了 stopGlobalTimerGoNext 停止全局计时器的方法。
  * 提供了 doInScreenAction 立刻执行的、线程安全的操纵全局计时器和 GUI 的方法。
  * 提供了 doAfter 的延迟执行的、线程安全的操纵全局计时器和 GUI 的方法。
  */
object LabUtils {

  val isCurrentScreen: (Experiment, Screen) => Boolean =
    (exp,  scr) => if (exp.getScreen == scr) true else false

  /***
    * Java API: 如果检测到鼠标按键 testButton，那么执行操作 op
    *
    * @param testButton MouseButton 类，NONE 表示鼠标移动，需要实验 Form 类支持此事件传递
    *                   MouseButton.PRIMARY 表示鼠标左键
    *                   MouseButton.SECONDARY 表示鼠标右键
    * @param event 侦听到的事件
    * @param exp 当前实验
    * @param scene 当前场景
    * @param op 需要执行的操作
    */
  def ifMouseButton(testButton: MouseButton, event: Event, exp: Experiment, scene: Scene)
                   (op: (Experiment, Scene) => Unit): Unit = event match {
    case mouseEvent: MouseEvent if mouseEvent.getButton == testButton => op(exp, scene)
    case _ => //防止为 null
  }

  /***
    * Scala API, Implicit 版本：如果检测到鼠标按键 testButton，那么执行操作 op
    *
    * @param testButton MouseButton 类，NONE 表示鼠标移动，需要实验 Form 类支持此事件传递
    *                   MouseButton.PRIMARY 表示鼠标左键
    *                   MouseButton.SECONDARY 表示鼠标右键
    * @param event 侦听到的事件
    * @param exp 当前实验
    * @param scene 当前场景
    * @param op 需要执行的操作
    */
  def ifMouseButton(testButton: MouseButton)
                   (op: (Experiment, Scene) => Unit)
                   (implicit event: Event, exp: Experiment, scene: Scene): Unit =
    ifMouseButton(testButton, event, exp, scene)(op)

  /***
    * Scala API, 闭包版本：如果检测到鼠标按键 testButton，那么执行操作 op
    *
    * @param testButton MouseButton 类，NONE 表示鼠标移动，需要实验 Form 类支持此事件传递
    *                   MouseButton.PRIMARY 表示鼠标左键
    *                   MouseButton.SECONDARY 表示鼠标右键
    * @param event 侦听到的事件
    * @param op 需要执行的操作
    */
  def ifMouseButton(testButton: MouseButton, event: Event)
                   (op: => Unit): Unit = event match {
    case mouseEvent: MouseEvent if mouseEvent.getButton == testButton => op
    case _ => //防止为 null
  }

  /**
    * Java API: 如果检测到键盘按键 testButton，那么执行操作 op
    * @param testButton KeyCode 类，比如 KeyCode.SPACE 空格键
    * @param event 侦听到的事件
    * @param exp 当前实验
    * @param scene 当前场景
    * @param op 需要执行的操作
    */
  def ifKeyButton(testButton: KeyCode, event: Event, exp: Experiment, scene: Scene)
                 (op: (Experiment, Scene) => Unit): Unit = event match {
    case keyEvent: KeyEvent if keyEvent.getCode == testButton => op(exp, scene)
    case _ =>
  }

  /**
    * Scala API, Implicit 版本：如果检测到键盘按键 testButton，那么执行操作 op
    * @param testButton KeyCode 类，比如 KeyCode.SPACE 空格键
    * @param event 侦听到的事件
    * @param exp 当前实验
    * @param scene 当前场景
    * @param op 需要执行的操作
    */
  def ifKeyButton(testButton: KeyCode)
                 (op: (Experiment, Scene) => Unit)
                 (implicit event: Event, exp: Experiment, scene: Scene): Unit =
    ifKeyButton(testButton,event,exp,scene)(op)

  /**
    * Scala API, 闭包版本：如果检测到键盘按键 testButton，那么执行操作 op
    * @param testButton KeyCode 类，比如 KeyCode.SPACE 空格键
    * @param event 侦听到的事件
    * @param op 需要执行的操作
    */
  def ifKeyButton(testButton: KeyCode, event: Event)
                 (op: => Unit): Unit = event match {
    case keyEvent: KeyEvent if keyEvent.getCode == testButton => op
    case _ =>
  }

  /***
    * 调用 Experiment 对象指针，通知 Queue 前往下一个 Screen
    * 此操作会清除 Queue 的全局定时器，但是不会清除已经启动、安排的 Timer，因此是不安全的
    * 请最好不要在 Psy4J 的 Screen 中使用 Timer 进行定时改变 Screen 的 GUI 界面
    * @param experiment 当前实验对象
    */
  def goNextScreenUnSafe(implicit experiment: Experiment): Unit = {
    experiment.terminal.set(0)
    experiment.terminal.set(1)
  }

  /***
    * 调用 Experiment 对象指针，通知 Queue 前往下一个 Screen
    * 此操作会清除 Queue 的全局定时器，但是不会清除已经启动、安排的 Timer，但是，因为添加了对于当前呈现屏幕的判断，
    * 因此，如果此操作在 Timer 中定时调用，那么如果 Queue 定时器已经改变当前屏幕指针，那么此方法将会不执行
    * 请最好不要在 Psy4J 的 Screen 中使用 Timer 进行定时改变 Screen 的 GUI 界面
    * @param experiment 当前实验对象
    */
  def goNextScreenSafe(implicit experiment: Experiment, currentScreen: Screen): Unit = {
    if (isCurrentScreen(experiment,currentScreen)) goNextScreenUnSafe(experiment)
  }

  /**
    * Java API: 在此方法中进行 GUI 的更改是安全的，因为代码在执行之前，会首先判断当前屏幕是否为指针指向的屏幕，如果不是，则不执行
    * 此方法可用在 Timer 设置的定时器中，定时改变 GUI 界面
    * 请最好不要在 Psy4J 的 Screen 中使用 Timer 进行定时改变 Screen 的 GUI 界面
    * @param op 操作，带有 Experiment 入参，以便于执行当前指针指向的 Screen 所调用的代码（而不是可能过期的 Screen 对象）
    * @param experiment 当前实验对象
    * @param currentScreen 执行代码所依赖的 Screen 对象
    */
  def doInScreenAction(experiment: Experiment, currentScreen: Screen,
                       op : (Experiment, Screen) => Unit): Unit = {
    if (isCurrentScreen(experiment, currentScreen)) op(experiment, experiment.getScreen)
  }

  /**
    * Scala API: 在此方法中进行 GUI 的更改是安全的，因为代码在执行之前，会首先判断当前屏幕是否为指针指向的屏幕，如果不是，则不执行
    * 此方法可用在 Timer 设置的定时器中，定时改变 GUI 界面
    * 请最好不要在 Psy4J 的 Screen 中使用 Timer 进行定时改变 Screen 的 GUI 界面
    * @param op 操作
    * @param experiment 当前实验对象
    * @param currentScreen 执行代码所依赖的 Screen 对象
    */
  def doInScreenAction(op : => Unit)(implicit experiment: Experiment, currentScreen: Screen): Unit = {
    if (isCurrentScreen(experiment, currentScreen)) op
  }

  /**
    * 停止当前的全局计时器行为
    * 注意，此函数调用不影响自己设定的 Timer 定时器的行为
    * 请最好不要在 Psy4J 的 Screen 中使用 Timer 进行定时改变 Screen 的 GUI 界面
    * @param experiment 当前实验对象
    */
  def stopGlobalTimerGoNext(implicit experiment: Experiment): Unit = experiment.terminal.set(-999)

  /**
    * Java API：在指定的毫秒数后，执行 doTask 定义的操作
    * 此方法和自己定义的 Timer 不同，它是线程安全的，如果当前的 Experiment 指向的对象不是传入的 Screen 对象，那么不执行任何操作
    * 反之，执行 doTask 操作，在 delay 的毫秒数之后。
    * @param delay 在此指定毫秒数执行操作
    * @param doTask 执行此操作
    * @param experiment 当前 Experiment 对象
    * @param screen 调用此方法时，而不是定时器到期后的 Experiment 指向的 Screen 对象
    */
  def doAfter(delay: Long, experiment: Experiment, screen: Screen)(doTask: () => Unit): Unit = {
    val timer = new Timer()
    val timerTask = new TimerTask {
      override def run(): Unit = if (isCurrentScreen(experiment, screen)) doTask()
    }
    timer.schedule(timerTask, delay)
  }

  /**
    * Scala API: 在指定的毫秒数后，执行 doTask 定义的操作
    * 此方法和自己定义的 Timer 不同，它是线程安全的，如果当前的 Experiment 指向的对象不是传入的 Screen 对象，那么不执行任何操作
    * 反之，执行 doTask 操作，在 delay 的毫秒数之后。
    * @param delay 在此指定毫秒数执行操作
    * @param doTask 执行此操作
    * @param experiment 当前 Experiment 对象
    * @param screen 调用此方法时，而不是定时器到期后的 Experiment 指向的 Screen 对象
    */
  def doAfter(delay: Long)(doTask : => Unit)(implicit experiment: Experiment, screen: Screen): Unit = {
    doAfter(delay, experiment, screen)(() => doTask)
  }
}



# Psy4J

> A cognitive science package based on object-oriented programming ideas. The program is driven by the JavaFx framework and the JVM platform.

## Version 1.2.4

在当前的版本中，使用 Scala 混合 Java 实现了 Psy4J。相比较之前的版本，现在版本只用声明一个文件即可，请参照 com.mazhangjing.demo/experiment 中的例子，创建一个 Experiment 的实现，然后使用 LabUtils 类提供的 Builder 方法创建若干个包裹 Screen 的 Trial，并且将其注入到 Experiment 中，如果需要保存数据，在 Experiment 的方法中提供实现即可。

程序总是运行 Main.java，因此，你需要通过 classpath 下的 invoke.properties 声明需要反射创建的一个叫做 ExpRunner 的实现，这个实现提供了你的实验信息，包括 title、log、experiment 的 className（用于反射创建），以及 EventMaker。

EventMaker 是版本新加入的类，此类实现后，由 ExpRunner 交付给 Main.java，然后后者会通过反射构建 EventMaker 实例，并且将其放入单独的线程中运行。此实例可以通过 Main 注入的 Experiment 实例来操纵 Screen 的 eventHandler 回调方法，提供比如语音检测之类的功能。

2019-02-22 Corkine Ma @ CCNU

## Version 1.0.0

一组带有高精度定时器的类在 com.mazhangjing.lab 中实现。使用者可以参照 com.mazhangjing.zsw 中的结构层次，继承 Screen、Trial、Experiment，然后将其拼装在一起即可。参照 Main.java 中的内容，可以驱动你的 Experiment 运行。

程序并未提供实验刺激的注入方法，没有提供 CSS 默认样式，没有提供日志的记录框架，这保证了程序设计的灵活性，同时隐藏了最为复杂，同时抽象复用率最高的部分 —— 序列调用、定时器自动跳转、阻断跳转、自定义动作和反应。

你需要在 Screen 实现的 initScreen 中提供 duration 超时，以及 layout 呈现的静态结构，你可以在此方法中定义各种组件的 Event，比如按钮、点击、移动等等。Screen 可以由 Trial 的 screens 容器管理，后者在 initTrials 方法中设置，而 Trial 序列由 Experiment 的 trials 属性和 initExperiment 方法管理。在此处注入你的刺激，组建你的 Trial 和 Screen。

Screen 默认注入了 Experiment 和 Scene 对象，这可以方便你预先定义结构的时候，利用这些运行时对象提供独特的行为。除了 initScreen 方法，eventHandler 方法提供了鼠标移入、按键按下、鼠标点击的全局事件注册，你可以根据性能和需要在 Main.java 中处理这部分事件的注册。在这里可以方便的对全局按键进行检测，同时对用户鼠标移动做出反应，比如，取消定时，观察用户的动作。

所有的静态变量存储在枚举 SET.java 中，所有的刺激生成逻辑在 StiFactory.java 中，这些部分由你自己实现。com.mazhangjing.zsw 包中提供了一些实现的示例。

如果你需要保存一些关键信息，可以使用 Experiment 的 setData 或者 setGlobalData 方法，前者保存在 Trial 中，后者保存在 Experiment 中，程序默认在退出界面后调用 saveData 方法进行数据保存，你可以在这里处理数据的序列化。com.mazhangjing.zsw 中提供了一种基于 Log4J 的实现。这部分也由你自己定义。

2018-12-24 Corkine Ma @ CCNU


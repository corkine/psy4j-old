# Psy4J

> A cognitive science package based on object-oriented programming ideas. The program is driven by the JavaFx framework and the JVM platform.

一组带有高精度定时器的类在 com.mazhangjing.lab 中实现。使用者可以参照 com.mazhangjing.zsw 中的结构层次，继承 Screen、Trial、Experiment，然后将其拼装在一起即可。参照 Main.java 中的内容，可以驱动你的 Experiment 运行。

程序并未提供实验刺激的注入方法，没有提供 CSS 默认样式，没有提供日志的记录框架，这保证了程序设计的灵活性，同时隐藏了最为复杂，同时抽象复用率最高的部分 —— 序列调用、定时器自动跳转、阻断跳转、自定义动作和反应。

你需要在 Screen 实现的 initScreen 中提供 duration 超时，以及 layout 呈现的静态结构，你可以在此方法中定义各种组件的 Event，比如按钮、点击、移动等等。Screen 可以由 Trial 的 screens 容器管理，后者在 initTrials 方法中设置，而 Trial 序列由 Experiment 的 trials 属性和 initExperiment 方法管理。在此处注入你的刺激，组建你的 Trial 和 Screen。

Screen 默认注入了 Experiment 和 Scene 对象，这可以方便你预先定义结构的时候，利用这些运行时对象提供独特的行为。除了 initScreen 方法，eventHandler 方法提供了鼠标移入、按键按下、鼠标点击的全局事件注册，你可以根据性能和需要在 Main.java 中处理这部分事件的注册。在这里可以方便的对全局按键进行检测，同时对用户鼠标移动做出反应，比如，取消定时，观察用户的动作。

所有的静态变量存储在枚举 SET.java 中，所有的刺激生成逻辑在 StiFactory.java 中，这些部分由你自己实现。com.mazhangjing.zsw 包中提供了一些实现的示例。

如果你需要保存一些关键信息，可以使用 Experiment 的 setData 或者 setGlobalData 方法，前者保存在 Trial 中，后者保存在 Experiment 中，程序默认在退出界面后调用 saveData 方法进行数据保存，你可以在这里处理数据的序列化。com.mazhangjing.zsw 中提供了一种基于 Log4J 的实现。这部分也由你自己定义。

2018-12-24 Corkine Ma @ CCNU
package com.mazhangjing.lab;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import jdk.nashorn.internal.objects.annotations.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Screen 类用来抽象实验程序中的一个静态屏幕显示的内容。</p>
 * <p>你需要继承这个抽象类，并且实现 initScreen 方法，在这个方法中对实例变量 duration 和 layout 进行赋值（默认为null），设置子类的状态。</p>
 * <p>你同样需要实现 eventHandler 方法，在这个方法中，你将可以处理JavaFX的 Event 类型参数，比如拦截和监听键盘、鼠标事件。之后你可以对 Scene 返回
 * GUI 组件以对被试反应进行反馈。你可以在这个方法中通过调用 terminal 变量（在GUI程序中，对 terminal 对象添加监听来做出对 Screen 完结时的反应，当
 * terminal 从 0 变化到 1 时，会自动翻页），通过避免 terminal 从 0 变化到 1 ，以避免定时器自动换页，然后执行你的“即兴”程序。
 * 你也可以在自己的反馈结束后恢复 terminal 为 0 和 1，以恢复定时器。</p>
 * <p>这个类通常是被用作和GUI程序直接交互的类，其实例一般由 Experiment 类产生。这个类可供调用的方法有：getDuration() 以及 getLayout()。</p>
 * <p>你可以通过添加更多的私有实例变量来指明更为复杂的Screen特征，提前构造好它们，然后使用 setLayout 和 eventHandler 进行静态和行为发生时的展示。
 * 因为这个类使用“交给子类”设计模式、“观察者”设计模式，因此能够最大限度的让程序保持弹性。</p>
 *
 * @author <a href='http://www.mazhangjing.com'>Corkine Ma</a>
 * @author Marvin Studio @ Central China Normal University
 * @version 1.1
 * */
public abstract class Screen {

    /*一个失败的尝试：使用 Builder 模式简化 Screen 开发
    问题在于：Screen 是抽象类，而 Java 不能将函数作为值进行传递，因此不能通过先构造一个 Screen，然后再注入函数/属性的 Builder 模式。
    以及，也不能先保存一个函数，然后再注入一个匿名 Screen 中。
    private static class Builder implements com.mazhangjing.lab.Builder<Screen> {
        private Integer duration;
        private String information;
        public Screen named(String name) {
        }
        @Override public Screen build() {
            return null;
        }
    }*/

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private Experiment experiment;

    private Scene scene;

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        System.out.println("Setting screen now...");
        this.experiment = experiment;
    }

    public void callWhenShowScreen() {}

    public void callWhenLeavingScreen() {}

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    /**Screen 持续的时间，单位为 ms*/
    public Integer duration;

    protected String data;

    /**Screen 的 GUI 布局*/
    public Parent layout;

    /**Screen 用来说明其功能的字符串，使用 toString 可以获得*/
    public String infomation = "Screen";

    /**你应该在子类自行实现 Screen 的 initScreen 方法，为 layout 和 duration 变量赋值，设置子类状态*/
    public abstract Screen initScreen();

    public String getData() { return data; }

    public void setData(String data) { this.data = data; }

    /**
     * 你应该在这里处理当呈现 Screen 时的鼠标和键盘事件，并且使用 terminal 变量标记此类已经完成使命。
     * @param event JavaFX 的 Event 事件
     * @param experiment Experiment 类型变量，你可以调用 .terminal 来获得 terminal 以控制定时器行为，你可以调用 .setData 来保存结果
     * @param scene Scene 类型变量，你可以调用它来绘制 GUI，用于给被试反馈*/
    public abstract void eventHandler(Event event, Experiment experiment, Scene scene);

    /**@return duration 实例变量的值，单位为ms*/
    public final Integer getDuration() {
        return this.duration;
    }

    /**获取 layout 实例变量的值
     * @return Screen 的布局类*/
    public final Parent getLayout() { return this.layout; }

    public String toString() {
        return infomation;
    }

    /*public static void main(String[] args){
        class TestScreen extends Screen {
            @Override
            public Screen initScreen() {
                this.layout = new FlowPane();
                this.duration = 500;
                return this;
            }
            @Override
            public void eventHandler(Event event, Experiment experiment, Scene scene) {
                System.out.print(event.getTarget().toString());
            }
        }
        SimpleIntegerProperty terminal = new SimpleIntegerProperty(0);
        Screen screen = new TestScreen();
        System.out.print(screen.getDuration().toString());
        System.out.print(screen.getLayout().toString());
    }*/
}

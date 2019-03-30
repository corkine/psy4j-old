package com.mazhangjing.lab;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Psy4J 依赖的基于 JavaFx 的 GUI 框架，框架通过反射从 classpath 下的 invoke.properties 文件中获取 ExpRunner 实现类，然后
 * 从中或许实验的 Experiment className、各个 EventMaker 的 className，然后通过反射的方式分别创建 Experiment 对象，EventMaker
 * 对象，对于每个 EventMaker 对象，注入 Experiment 和 Scene 对象，然后在一个独立的线程中运行，EventMaker 可以通过 Experiment
 * 对象获取当前 Screen，然后对其进行 eventHandler 调用传递事件，比如语音的检测等等。
 *
 * @apiNote Main 继承自 JavaFX 的 Application 类，属于客户端代码，大部分方法均为私有。通过传递构造好的 Experiment 对象来产生合适的行为，请勿继承或者装饰此类。
 * Main 类主要包含了Experiment实验对象，当前的 Screen 对象，一个用于定时切换 Screen 的 ScheduledThreadPoolExecutor 对象，一个用于 Screen 对象在其子类终止计时器行为的
 * SimpleIntegerProperty 类型对象，其被称作 terminal，此对象从 Experiment 内置的 terminal 变量中获得。
 *
 * @apiNote 当 executor 达到当前 Screen 的延迟时间，则调用 terminal，改变其值为 1，之后内置的 terminal 监听器会自动切换下一个 Screen，并且重置 terminal 的值为0
 * 。所有切换操作均通过 terminal 的监听器通过调用 nextScreen 方法进行传递。
 *
 * @apiNote 此外，我们实现了对于 Scene 的键盘和鼠标监听，你可以在自己的 Screen 子类中的 eventHandler 中设置相关的相应操作，这些监听会自动传递到当前呈现的Screen中
 * 进行相应，比如，你可以通过给 terminal 设置一个别的值来中断当前计时器切换行为，然后在执行完操作后再依次设置为0、1，重新恢复计时器行为。你可以在这里
 * 绘制一个Label或者对话框的同时而不暂停计时器，用于呈现被试的选择。这些行为使用“交给子类”设计模式，你可以充分能利用 eventHandler 方法进行自定义行为，
 * 而不用修改 Form 类的代码。
 *
 * @version 1.2.4
 * @author Corkine, MaZhangJing
 *         2019年03月30日 修正了 setScreen 的方法。
 */
public class Main extends Application {

    private ExpRunner runner;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Experiment experiment;

    private Screen currentScreen;

    private final ScheduledThreadPoolExecutor executor;

    private static SimpleIntegerProperty terminal;

    private final Runnable changeTask;

    private final Scene scene = new Scene(drawWelcomeContent(),400,300);

    private static final String COPYRIGHT =
            "The copyright of this software belongs to the Virtual Behavior Laboratory of the School of Psychology, Central China Normal University. " +
            "Any unauthorized copy of the program or copy of the code will be legally held liable.\n" +
            "The software is based on the Java platform design and Java TM is a registered trademark of Oracle Corporation.\n" +
            "The software is based on the Java FX framework and Java FX is based on the GNU v2 distribution protocol.\n" +
            "The software is based on the PSY4J framework design. PSY4J is the work of Corkine Ma, which allows binary packages and source code to " +
            "be used, but the source code must not be tampered with in any way and closed source.\n" +
            "Contact: psy4j@mazhangjing.com and Support site: www.mazhangjing.com" +
            " © Marvin Studio 2018 - 2019";

    /**
     * 在此处通过对 classpath 下的 invoke.properties 中的 expRunnerClassName 获取，之后通过反射创建 ExpRunner 对象，将其注入到自己的属性中
     * 之后，通过其来继续注入 Experiment 对象、初始化 currentScreen、设置单线程的全局计时器、注册 terminal 观察者。
     */
    public Main() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        //通过反射创建 ExpRunner
        initRunner();
        //注入 Experiment 对象
        this.experiment = ((Experiment) Class.forName(this.runner.getExperimentClassName()).newInstance());
        //注入依赖
        experiment.initScreensAndTrials(scene);
        //设置状态、单任务定时器策略，定时器执行的具体行为
        currentScreen = experiment.getScreen();
        terminal = experiment.terminal;
        terminal.addListener((observable, oldV, newV)->{
            if (oldV.intValue() == 0 && newV.intValue() == 1) {
                nextScreen();
                terminal.set(0); //不能写在changeScreen中，否则会递归导致失败
            }
        });
        changeTask = () -> terminal.set(1);
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
    }

    private void initRunner() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("invoke.properties"));
        String expRunnerClassName = properties.getProperty("expRunnerClassName");
        this.runner = (ExpRunner) Class.forName(expRunnerClassName).newInstance();
        logger.debug("Get Runner From Invoke: invoke.properties " + runner);
    }

    /**
     * 前往下一个 Screen，一般由 terminal 操纵
     */
    private void nextScreen() { experiment.release(); setScreen(); }

    /**
     * 执行上一个 Screen 离开时的回调函数，获取队列下一个 Screen，设置其全局定时器、GUI 界面，执行其开始时的回调函数
     */
    private void setScreen() {
        //首先处理页面遗留问题
        try {
            currentScreen.callWhenLeavingScreen();
        } catch (Exception e) { logger.warn(e.getMessage()); }
        //之后重载页面
        currentScreen = experiment.getScreen();
        logger.debug("Current Screen is " + currentScreen);
        //绘制GUI
        if (currentScreen == null) drawEndContent();
        else {
            Platform.runLater(() -> {
                scene.setRoot(currentScreen.layout);
                try {
                    currentScreen.callWhenShowScreen();
                } catch (Exception e) { logger.warn(e.getMessage()); }
            });
            //添加时间监听器
            Platform.runLater(()-> setTimer(currentScreen.duration));
        }
    }

    /**
     * 全局计时器的设定
     * @param duration 当前 Screen 在全局定时器中需要执行的最长毫秒数
     */
    private void setTimer(Integer duration) {
        executor.getQueue().clear();
        logger.debug("RunTimer with max " + duration + "ms to act");
        executor.schedule(changeTask, duration, TimeUnit.MILLISECONDS);
    }

    /**
     * 绘制欢迎页面，其中包含版本、更新日志等信息，以及一个 "Start Experiment" 用于开始的按钮。
     * @return JavaFx GUI 组件
     */
    private Parent drawWelcomeContent() {
        BorderPane root = new BorderPane();
        Button start = new Button("Start Experiment");
        start.setEffect(new DropShadow(10, Color.GREY));
        start.setFont(Font.font(20));
        root.setCenter(start);
        Text copy = new Text(COPYRIGHT);
        root.setBottom(copy);
        start.setOnAction((event -> setScreen()));
        return root;
    }

    /**
     * 绘制结束页面
     */
    private void drawEndContent() {
        executor.shutdownNow(); //关闭定时器
        FlowPane root = new FlowPane(); root.setAlignment(Pos.CENTER);
        Label label = new Label("End of Experiment");
        label.setFont(Font.font(30));
        root.getChildren().add(label);
        scene.setRoot(root);
    }

    /**
     * 注册 EventHandler 的帮助函数
     * @param event 传递给 Screen 的 Event 事件
     */
    private void addEventHandler(Event event) {
        if (currentScreen != null) currentScreen.eventHandler(event,experiment,scene);
    }

    private List<EventMaker> invokeEventMaker(Set<String> set, Experiment experiment, Scene scene) {
        List<EventMaker> makers = new ArrayList<>();
        for (String makerName : set) {
            logger.debug("Invoke EventMaker from EventMaker ClassName " + makerName);
            try {
                Class<?> aClass = Class.forName(makerName);
                Constructor<?> constructor = aClass.getConstructor(Experiment.class, Scene.class);
                EventMaker eventMaker = (EventMaker) constructor.newInstance(experiment, scene);
                makers.add(eventMaker);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return makers;
    }

    /**
     * 加载 JavaFx GUI 资源，展示 GUI 界面，在此处设置 EventHandler。
     * @param primaryStage JavaFx 主场景
     */
    @Override @SuppressWarnings("unchecked")
    public void start(Stage primaryStage) {
        primaryStage.setScene(scene);
        Set<OpenedEvent> set = runner.getOpenedEventSet();
        //根据 Runner 的设置来注册需要传递给 Screen 的 JavaFx 内置事件
        if (set.contains(OpenedEvent.KEY_PRESSED)) {
            logger.debug("Reg OpenedEvent " + OpenedEvent.KEY_PRESSED);
            scene.addEventHandler(KeyEvent.KEY_PRESSED,(EventHandler) this::addEventHandler);
        }
        if (set.contains(OpenedEvent.MOUSE_CLICK)) {
            logger.debug("Reg OpenedEvent " + OpenedEvent.MOUSE_CLICK);
            scene.addEventHandler(MouseEvent.MOUSE_CLICKED, (EventHandler) this::addEventHandler);
        }
        if (set.contains(OpenedEvent.MOUSE_MOVE)) {
            logger.debug("Reg OpenedEvent " + OpenedEvent.MOUSE_MOVE);
            scene.addEventHandler(MouseEvent.MOUSE_MOVED, (EventHandler) this::addEventHandler);
        }
        //触发和运行各个 EventMaker 子线程
        if (runner.getEventMakerSet() != null && runner.getEventMakerSet().size() != 0) {
            Set<String> eventMakerSet = runner.getEventMakerSet();
            List<EventMaker> makers = invokeEventMaker(eventMakerSet, experiment, scene);
            makers.forEach(maker -> {
                logger.info("EventMaker " + maker + " is Running on new Thread now...");
                new Thread(maker).start();
            });
        }
        //设置是否全屏显示
        if (runner.getFullScreen()) {
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setFullScreenExitKeyCombination(KeyCombination.valueOf("F11"));
        }
        //设置外观样式
        primaryStage.setWidth(900.0);
        primaryStage.setHeight(600.0);
        primaryStage.setTitle(String.format("%s - %s - Powered by PSY4J - CM ❤️ OpenSource",runner.getTitle(), runner.getVersion()));
        //设置样式表文件
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getClassLoader().getResource("style.css")).toExternalForm());

        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            experiment.saveData();
            System.exit(0);
        });
    }

    public static void main(String[] args) { launch(args); }
}

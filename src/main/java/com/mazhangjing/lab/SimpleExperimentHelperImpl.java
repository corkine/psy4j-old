package com.mazhangjing.lab;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
 * @apiNote SimpleExperimentHelperImpl 继承自 JavaFX 的 Application 类，属于客户端代码，大部分方法均为私有。通过传递构造好的 Experiment 对象来产生合适的行为，请勿继承或者装饰此类。
 * SimpleExperimentHelperImpl 类主要包含了Experiment实验对象，当前的 Screen 对象，一个用于定时切换 Screen 的 ScheduledThreadPoolExecutor 对象，一个用于 Screen 对象在其子类终止计时器行为的
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
 *         2019年4月4日 修正了 initExperiment 的问题 - 在父类构造器中导致在子类 Experiment 调用 initExperiment 中引用子类字段和方法为空的问题
 *         现在 Experiment、Trial、Screen 和 xxxBuilder 都需要自行 initXXX
 */
public class SimpleExperimentHelperImpl implements ExperimentHelper {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ExpRunner runner;

    private Experiment experiment;

    private ScheduledThreadPoolExecutor executor;

    private Runnable changeTask;

    private static SimpleIntegerProperty terminal;

    private final Scene scene = new Scene(drawWelcomeContent(),400,300);

    private Screen currentScreen;

    private boolean isInited = false;

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
    public SimpleExperimentHelperImpl(ExpRunner runner)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        initExperiment(runner, null);
    }

    public SimpleExperimentHelperImpl(Stage stage, String runnerProps)
            throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        initExperiment(null, runnerProps);
    }

    /**
     * 初始化实验、控制器、任务管理器
     * @param runner ExpRunner，提供需要监测的事件
     * @param runnerProps Props 文件地址，用于反射创建 ExpRunner
     * @throws ClassNotFoundException 没有从 Props 文件找到 ExpRunner
     * @throws InstantiationException 无法从 Props 文件初始化 ExpRunner
     * @throws IllegalAccessException 无法访问 ExpRunner 实例
     * @throws IOException 无法读取 Props 文件
     */
    private void initExperiment(ExpRunner runner, String runnerProps)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        //通过反射创建 ExpRunner
        if (runner == null) this.runner = initRunner(runnerProps);
        else this.runner = runner;
        //注入 Experiment 对象
        this.experiment = ((Experiment) Class.forName(this.runner.getExperimentClassName()).newInstance());
        //初始化 Experiment 组件
        this.experiment.initExperiment();
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

    private ExpRunner initRunner(String runnerProps)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream(runnerProps));
        String expRunnerClassName = properties.getProperty("expRunnerClassName");
        logger.debug("Get Runner From Invoke: invoke.properties " + runner);
        return (ExpRunner) Class.forName(expRunnerClassName).newInstance();
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
        } catch (Exception e) {
            logger.warn(e.toString());
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            logger.warn(writer.toString());
            try {
                writer.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            printWriter.close();
        }
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
                } catch (Exception e) {
                    logger.warn(e.toString());
                    StringWriter writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    e.printStackTrace(printWriter);
                    logger.warn(writer.toString());
                    try {
                        writer.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    printWriter.close();
                }
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
     * 在此处注册 EventHandler，并启动相关线程
     * @param runner ExpRunner 提供 OpenedEvent 集合
     * @param scene Scene 提供需要注册来自的 Scene
     */
    @SuppressWarnings("unchecked")
    private void initEventHandler(ExpRunner runner, Scene scene) {
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
    }


    /**
     * 返回 Helper 最核心的 Scene 对象
     * 在 initExperiment 处理过 Scene 后，同时为 Scene 注册 EventHandler，并启动相关线程。
     * 同时为 Stage 设置默认的 Scene，并且设置一些 Stage 的方法，比如样式表、尺寸大小、关闭行为、全屏行为，但是不显示
     * @param stage 需要装饰的 Stage
     */
    @Override public void initStage(Stage stage) {
        initEventHandler(runner, scene);
        initStageImpl(stage);
        stage.setScene(scene);
    }

    @Override
    public Experiment getExperiment() {
        if (isInited) return this.experiment;
        else throw new RuntimeException("请首先调用 initStage 完成类的初始化");
    }

    @Override
    public Scene getScene() {
        if (isInited) return this.scene;
        else throw new RuntimeException("请首先调用 initStage 完成类的初始化");
    }

    /**
     * 加载 JavaFx GUI 资源，准备 GUI 界面（Scene，样式表，尺寸大小，关闭行为，全屏行为），但是不显示
     * 可以继承和重写
     * @param stage Stage 场景
     */
    protected void initStageImpl(Stage stage) {
        //设置是否全屏显示
        if (runner.getFullScreen()) {
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("F11"));
        }
        //设置外观样式
        stage.setWidth(900.0);
        stage.setHeight(600.0);
        stage.setTitle(String.format("%s - %s - Powered by PSY4J - CM ❤️ OpenSource",runner.getTitle(), runner.getVersion()));
        //设置样式表文件
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getClassLoader().getResource("style.css")).toExternalForm());
        scene.getRoot().setStyle("-fx-background-color: transparent;-fx-border-color: transparent;");
        scene.setFill(Color.GRAY);
        stage.setOnCloseRequest(event -> {
            experiment.saveData();
            System.exit(0);
        });
        isInited = true;
    }
}

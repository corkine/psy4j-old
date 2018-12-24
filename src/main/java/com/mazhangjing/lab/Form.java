package com.mazhangjing.lab;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>Form 类主要用于展示 GUI 界面。</p>
 * <p>通过在 Form 类的构造器中传递实验对象来初始化 GUI 界面。</p>
 * <p>Form 类继承自 JavaFX 的 Application 类，属于客户端代码，大部分方法均为私有。通过传递构造好的 Experiment 对象来产生合适的行为，请勿继承或者装饰此类。</p>
 * <p>Form 类主要包含了Experiment实验对象，当前的Screen对象，一个用于定时切换Screen的ScheduledThreadPoolExecutor对象，一个用于Screen对象在其子类终止计时器行为的
 * SimpleIntegerProperty 类型对象，其被称作 terminal，此对象从 Experiment 内置的 terminal 变量中获得。
 * 当 executor 达到当前Screen的延迟时间，则调用 terminal，改变其值为1，之后内置的 terminal 监听器会自动切换下一个 Screen，并且重置 terminal 的值为0
 * 。所有切换操作均通过 terminal 的监听器通过调用 nextScreen 方法进行传递。</p>
 * <p>此外，我们实现了对于 Scene 的键盘和鼠标监听，你可以在自己的 Screen 子类中的 eventHandler 中设置相关的相应操作，这些监听会自动传递到当前呈现的Screen中
 * 进行相应，比如，你可以通过给 terminal 设置一个别的值来中断当前计时器切换行为，然后在执行完操作后再依次设置为0、1，重新恢复计时器行为。你可以在这里
 * 绘制一个Label或者对话框的同时而不暂停计时器，用于呈现被试的选择。这些行为使用“交给子类”设计模式，你可以充分能利用 eventHandler 方法进行自定义行为，
 * 而不用修改 Form 类的代码。</p>
 *
 * @author <a href='http://www.mazhangjing.com'>Corkine Ma</a>
 * @author Marvin Studio @ Central China Normal University
 * @version 1.1
 * */
public class Form extends Application {

    private Experiment experiment;

    private Screen currentScreen;

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    {
        executor.setRemoveOnCancelPolicy(true);
    }

    private Runnable CHANGE_TASK = new Runnable() { @Override public void run() { terminal.set(1); }};

    private static SimpleIntegerProperty terminal;

    private final Scene scene = new Scene(drawWelcomeContent(),400,300);

    /**此构造器会传递一个示例 Experiment 对象*/
    public Form() {
        this(new Experiment() {
            @Override
            protected void initExperiment() {

            }

            @Override
            public void saveData() {

            }
        });
    }

    /**在此处传递合适的 Experiment 对象以设置 Form 的行为。
     * @param experiment Experiment类型的实验对象，在其中应设置好Trials和Screens，用于在Form类中依次呈现。*/
    public Form(Experiment experiment){
        this.experiment = experiment;
        //注入依赖
        experiment.initScreensAndTrials(scene);
        currentScreen = experiment.getScreen();
        terminal = experiment.terminal;
        terminal.addListener((observable, oldV, newV)->{
            if (oldV.intValue() == 0 && newV.intValue() == 1) {
                nextScreen();
                terminal.set(0); //不能写在changeScreen中，否则会递归导致失败
            }
        });
    }

    private void nextScreen() {
        experiment.release();
        setScreen();
    }

    private void setScreen() {
        //绘制GUI，添加时间监听器
        currentScreen = experiment.getScreen();
        if (currentScreen == null) showEndScene();
        else {
            Platform.runLater(() -> scene.setRoot(currentScreen.layout));
            Platform.runLater(()-> setTimer(currentScreen.duration));
        }
    }

    private void setTimer(Integer duration) {
        System.out.println("RunTimer with duration " + duration);
        if (!executor.getQueue().isEmpty()) {
            executor.remove(CHANGE_TASK); System.out.println("Remove old timer!!!");
        }
        executor.schedule(CHANGE_TASK, duration, TimeUnit.MILLISECONDS);
    }

    private Parent drawWelcomeContent() {
        FlowPane root = new FlowPane();
        Button start = new Button("Start Experiment");
        start.setEffect(new DropShadow(10, Color.GREY));
        start.setFont(Font.font(20));
        root.getChildren().add(start);
        root.setAlignment(Pos.CENTER);
        start.setOnAction((event -> setScreen()));
        return root;
    }

    private void showEndScene() {
        executor.shutdownNow();
        FlowPane root = new FlowPane(); root.setAlignment(Pos.CENTER);
        Label label = new Label("End of Experiment");
        label.setFont(Font.font(30));
        root.getChildren().add(label);
        scene.setRoot(root);
    }

    /**程序主执行位点
     * @param primaryStage 舞台，等同于“窗口”。*/
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setScene(scene);
        scene.addEventHandler(MouseEvent.MOUSE_CLICKED, (EventHandler) event -> {
            addEventHandler(event);
        });
        scene.addEventHandler(KeyEvent.KEY_PRESSED,(EventHandler) event -> {
            addEventHandler(event);
        });
        //primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");
        //primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setTitle("Psychology Lab ToolKit");
        scene.getStylesheets().add(Form.class.getResource("style.css").toExternalForm());
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            experiment.saveData();
            System.exit(0);
        });
    }

    private void addEventHandler(Event event) {
        if (currentScreen != null) {
            currentScreen.eventHandler(event,experiment,scene);
        } else {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/*
class Test {
    static class MyScreen extends Screen {
        FlowPane pane = new FlowPane();
        {
            pane.setAlignment(Pos.CENTER);
            pane.getChildren().add(new Label("Haha"));
        }
        @Override
        public Screen initScreen() {
            duration = 3000;
            layout = new FlowPane();
            ((FlowPane) layout).setAlignment(Pos.CENTER);
            ((FlowPane) layout).getChildren().add(new Label("Screen1"));
            Button btn = new Button("Click Me!");
            ((FlowPane) layout).getChildren().add(btn);
            return this;
        }

        @Override
        public void eventHandler(Event event, Experiment experiment, Scene scene) {
            SimpleIntegerProperty terminal = experiment.terminal;
            if (event.getEventType() == MouseEvent.MOUSE_CLICKED) {
                System.out.println("Get Mouse clicked");
                scene.setRoot(pane);
                event.consume();
            } else if (event.getEventType() == KeyEvent.KEY_PRESSED) {
                System.out.println("Get Key Pressed");
                terminal.set(-1); //终止计时器行为
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setOnCloseRequest((event1 -> {
                    terminal.set(0); terminal.set(1); //重新唤醒计时器行为
                }));
                alert.showAndWait();
                event.consume();
            } else event.consume();
        }
    }
    static class MyScreen2 extends Screen {
        @Override
        public Screen initScreen() {
            duration = 3000;
            layout = new FlowPane();
            ((FlowPane) layout).setAlignment(Pos.CENTER);
            ((FlowPane) layout).getChildren().add(new Label("Screen2"));
            return this;
        }

        @Override
        public void eventHandler(Event event, Experiment experiment, Scene scene) {

        }
    }
    static class MyTrial extends Trial {
        @Override
        public Trial initTrial() {
            screens.add(new MyScreen().initScreen());
            screens.add(new MyScreen2().initScreen());
            return this;
        }
    }
    static class MyExperiment extends Experiment {
        @Override
        protected void initExperiment() {
            trials.add(new Test.MyTrial().initTrial());
        }

        @Override
        public void saveData() {
            System.out.println("Saving data");
        }
    }
    public static void main(String[] args){
        SimpleIntegerProperty terminal = new SimpleIntegerProperty(0);
        Experiment experiment = new MyExperiment();
    }
}
*/




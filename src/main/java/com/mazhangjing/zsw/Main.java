package com.mazhangjing.zsw;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Log;
import com.mazhangjing.lab.Screen;
import com.mazhangjing.zsw.experiment.ZswExperiment;
import com.mazhangjing.zsw.experiment.ZswLog;
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

import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends Application {

    private final Log log = new ZswLog();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Experiment experiment;

    private Screen currentScreen;

    private final ScheduledThreadPoolExecutor executor;

    private static SimpleIntegerProperty terminal;

    private final Runnable changeTask;

    private final Scene scene = new Scene(drawWelcomeContent(),400,300);

    /**在此处初始化合适的 Experiment 对象以设置 Form 的行为。*/
    public Main() {
        this(new ZswExperiment());
    }

    /**在此处传递合适的 Experiment 对象以设置 Form 的行为。
     * 此构造器方法用于设定当前对象的状态，比如注入的实验对象、当前的屏幕对象。
     * 此构造器方法的另外一个重要作用是设定定时器信号变化行为：前进到下一个屏幕对象。
     * @param experiment Experiment类型的实验对象，在其中应设置好Trials和Screens，用于在Form类中依次呈现。*/
    public Main(Experiment experiment){
        this.experiment = experiment;
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

    private void nextScreen() { experiment.release(); setScreen(); }

    private void setScreen() {
        //绘制GUI，添加时间监听器
        currentScreen = experiment.getScreen();
        logger.debug("Current Screen is " + currentScreen);
        if (currentScreen == null) drawEndContent();
        else {
            Platform.runLater(() -> scene.setRoot(currentScreen.layout));
            Platform.runLater(()-> setTimer(currentScreen.duration));
        }
    }

    private void setTimer(Integer duration) {
        /*if (!executor.getQueue().isEmpty()) {
            executor.remove(changeTask);
            logger.debug("Removing old timer first");
        }*/
        executor.getQueue().clear();
        logger.debug("RunTimer with max " + duration + "ms to act");
        executor.schedule(changeTask, duration, TimeUnit.MILLISECONDS);
    }

    private Parent drawWelcomeContent() {
        BorderPane root = new BorderPane();
        Button start = new Button("Start Experiment");
        start.setEffect(new DropShadow(10, Color.GREY));
        start.setFont(Font.font(20));
        root.setCenter(start);
        Text text = new Text(log.getLog());
        root.setBottom(text);
        Text copy = new Text(log.getCopyRight());
        root.setTop(copy);
        start.setOnAction((event -> setScreen()));
        return root;
    }

    private void drawEndContent() {
        executor.shutdownNow(); //关闭定时器
        FlowPane root = new FlowPane(); root.setAlignment(Pos.CENTER);
        Label label = new Label("End of Experiment");
        label.setFont(Font.font(30));
        root.getChildren().add(label);
        scene.setRoot(root);
    }

    private void addEventHandler(Event event) {
        if (currentScreen != null) currentScreen.eventHandler(event,experiment,scene);
    }

    @Override @SuppressWarnings("unchecked")
    public void start(Stage primaryStage) {
        primaryStage.setScene(scene);
        scene.addEventHandler(MouseEvent.MOUSE_CLICKED, (EventHandler) this::addEventHandler);
        scene.addEventHandler(KeyEvent.KEY_PRESSED,(EventHandler) this::addEventHandler);
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, (EventHandler) this::addEventHandler);
        if (SET.FULL_SCREEN.getValue() == 1) {
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setFullScreenExitKeyCombination(KeyCombination.valueOf("F11"));
        }
        primaryStage.setWidth(900.0);
        primaryStage.setHeight(600.0);
        primaryStage.setTitle(String.format("Psychology ToolKit - %s - Powered by PSY4J",log.getCurrentVersion()));

        //添加样式表文件
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

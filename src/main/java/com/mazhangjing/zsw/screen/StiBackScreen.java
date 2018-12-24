package com.mazhangjing.zsw.screen;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Screen;
import com.mazhangjing.zsw.SET;
import com.mazhangjing.zsw.sti.Array;
import com.mazhangjing.zsw.sti.Stimulate;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;


public class StiBackScreen extends Screen {

    private Boolean checkedMouseMoveEvent;

    protected Array array;

    public StiBackScreen(Array array) { this.array = array; }

    @Override
    public Screen initScreen() {
        BorderPane box = new BorderPane();
        //如果是异步的情况，则重新显示
        Label left = new Label((array.getBack().getLeft() != 999) ? String.valueOf(array.getBack().getLeft()) : String.valueOf(array.getHead().getLeft()));
        Label right = new Label((array.getBack().getRight() != 999) ? String.valueOf(array.getBack().getRight()) : String.valueOf(array.getHead().getRight()));
        left.setFont(Font.font(array.getBack().getLeft() != 999 ? array.getBack().getLeftSize() : array.getHead().getLeftSize()));
        right.setFont(Font.font(array.getBack().getRight() != 999 ? array.getBack().getRightSize() : array.getHead().getRightSize()));
        box.setLeft(left);
        box.setRight(right);
        box.setTop(null);
        //box.setBottom(new Text("Tail" + array.getBack().toString()));

        right.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            logger.debug("Get Right clicked" + "::" + System.nanoTime());
            makeJudge(false);
        });

        left.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            logger.debug("Get Left clicked" + "::" + System.nanoTime());
            makeJudge(true);
        });

        infomation = "第二次刺激呈现屏幕";
        layout = box;
        duration = SET.STI_SHOW_BACK_MS.getValue();
        return this;
    }

    protected void makeJudge(boolean isLeftClicked) {
        //如果是同步的情况，则判断
        if (array.getBack().getLeft() != 999 && array.getBack().getRight() != 999) {
            if ((array.getHead().getLeft() > array.getHead().getRight() && !isLeftClicked)
                    || (array.getHead().getLeft() < array.getHead().getRight() && isLeftClicked)) {
                logger.debug(String.format("1 left is %s and right is %s, the answer is wrong", array.getBack().getLeft(), array.getBack().getRight()));
                logger.info(String.format("WRONG LEFT:%s,%s RIGHT:%s,%s", array.getBack().getLeft(), array.getBack().getLeftSize(), array.getBack().getRight(), array.getBack().getRightSize()));
                showWrongScene();
            } else {
                logger.info(String.format("RIGHT LEFT:%s,%s RIGHT:%s,%s",array.getBack().getLeft(),array.getBack().getLeftSize(),array.getBack().getRight(),array.getBack().getRightSize()));
                goBackNormalScene();
            }
        }

        //如果是异步的情况，获取正确的值后再判断
        else if (array.getBack().getLeft() == 999) {
            int left = array.getHead().getLeft();
            if (((left < array.getBack().getRight()) && isLeftClicked) ||
                    ((left > array.getBack().getRight()) && !isLeftClicked)) {
                logger.debug(String.format("2 left is %s and right is %s, the ans is wrong", left, array.getBack().getRight()));
                logger.info(String.format("WRONG LEFT:%s,%s RIGHT:%s,%s", left, array.getHead().getLeftSize(), array.getBack().getRight(), array.getBack().getRightSize()));
                showWrongScene();
            } else {
                logger.info(String.format("RIGHT LEFT:%s,%s RIGHT:%s,%s", left, array.getHead().getLeftSize(), array.getBack().getRight(), array.getBack().getRightSize()));
                goBackNormalScene();
            }
        } else if (array.getBack().getRight() == 999) {
            int right = array.getHead().getRight();
            if (((right > array.getBack().getLeft()) && isLeftClicked) ||
                    (right < array.getBack().getLeft()) && !isLeftClicked) {
                logger.debug(String.format("3 left is %s and right is %s, the ans is wrong", array.getBack().getLeft(), right));
                logger.info(String.format("WRONG LEFT:%s,%s RIGHT:%s,%s", array.getBack().getLeft(), array.getBack().getLeftSize(), right, array.getHead().getRightSize()));
                showWrongScene();
            } else {
                logger.info(String.format("RIGHT LEFT:%s,%s RIGHT:%s,%s", array.getBack().getLeft(), array.getBack().getLeftSize(), right, array.getHead().getRightSize()));
                goBackNormalScene();
            }
        }
    }

    protected void goBackNormalScene() {
        logger.debug("[BACK] Go to next normal scene now...");
        getExperiment().terminal.set(0);
        getExperiment().terminal.set(1);
    }

    protected void showWrongScene() {
        //停止计时器
        logger.debug("[BACK] Stopping timer and show wrong scene now...");
        getExperiment().terminal.set(-999);
        //静态资源呈现
        FlowPane pane = new FlowPane();
        pane.setAlignment(Pos.CENTER);
        Label label = new Label("×");
        label.setFont(Font.font(SET.ERROR_SIZE.getValue()));
        label.setTextFill(Color.RED);
        pane.getChildren().addAll(label);

        getScene().setRoot(pane);
        //设置展示时间
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                goBackNormalScene();
            }
        }, SET.ERROR_ANS_MS.getValue());
    }

    @Override
    public void eventHandler(Event event, Experiment experiment, Scene scene) {
        Optional.ofNullable(event)
                .filter(event1 -> event1 instanceof MouseEvent && ((MouseEvent) event1).getButton() == MouseButton.NONE)
                .ifPresent(event1 -> {
                    if (checkedMouseMoveEvent == null) {
                        //保证只触发一次
                        checkedMouseMoveEvent = true;
                        getExperiment().terminal.set(-999);
                        //如果移动鼠标，但没有回答，则自动退出本试次
                        //new Timer().schedule(new TimerTask() { @Override public void run() { goBackNormalScene(); }}, SET.MOVE_BUT_NOT_DONE.getValue());
                        logger.debug("Mouse Moved Detected.");
                    }
                });
    }
}

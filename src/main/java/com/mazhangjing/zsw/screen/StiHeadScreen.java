package com.mazhangjing.zsw.screen;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Screen;
import com.mazhangjing.zsw.SET;
import com.mazhangjing.zsw.sti.Array;
import com.mazhangjing.zsw.sti.Stimulate;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class StiHeadScreen extends Screen {

    protected Array array;

    public StiHeadScreen(Array array) {
        this.array = array;
    }

    @Override
    public Screen initScreen() {
        BorderPane box = new BorderPane();
        Label left = new Label((array.getHead().getLeft() != 999) ? String.valueOf(array.getHead().getLeft()) : "  ");
        Label right = new Label((array.getHead().getRight() != 999) ? String.valueOf(array.getHead().getRight()) : "  ");
        left.setFont(Font.font(array.getHead().getLeft() != 999 ? array.getHead().getLeftSize() : SET.BIGGER_SIZE.getValue()));
        right.setFont(Font.font(array.getHead().getRight() != 999 ? array.getHead().getRightSize() : SET.BIGGER_SIZE.getValue()));
        box.setLeft(left);
        box.setRight(right);
        box.setTop(null);
        //box.setBottom(new Text("Head" + array.getHead().toString()));

        right.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            logger.debug("Get Right clicked" + "::" + System.nanoTime());
            makeJudge(false);
        });

        left.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            logger.debug("Get Left clicked" + "::" + System.nanoTime());
            makeJudge(true);
        });

        infomation = "首次刺激呈现屏幕";
        layout = box;
        duration = SET.STI_SHOW_HEAD_MS.getValue();
        return this;
    }

    @Override
    public void eventHandler(Event event, Experiment experiment, Scene scene) {
    }

    protected void makeJudge(boolean isLeftClicked) {
        //这里不能判断异步，如果异步并且反应，则无反应
        if (array.getHead().getLeft() != 999 || array.getHead().getRight() != 999) {
            if ((array.getHead().getLeft() > array.getHead().getRight() && !isLeftClicked) ||
                    (array.getHead().getLeft() < array.getHead().getRight() && isLeftClicked)) {
                logger.info(String.format("WRONG LEFT:%s,%s RIGHT:%s,%s", array.getHead().getLeft(), array.getHead().getLeftSize(), array.getHead().getRight(), array.getHead().getRightSize()));
                showWrongScene();
            } else {
                logger.info(String.format("RIGHT LEFT:%s,%s RIGHT:%s,%s", array.getHead().getLeft(), array.getHead().getLeftSize(), array.getHead().getRight(), array.getHead().getRightSize()));
            }
        } else {
            logger.info(String.format("ERROR LEFT:%s,%s RIGHT:%s,%s LEFT2:%s,%s RIGHT2:%s,%s",
                    array.getHead().getLeft(), array.getHead().getLeftSize(),
                    array.getHead().getRight(), array.getHead().getRightSize(),
                    array.getBack().getLeft(), array.getBack().getLeftSize(),
                    array.getBack().getRight(), array.getBack().getRightSize()));
        }
    }


    protected void showWrongScene() {
        //停止计时器
        logger.debug("[HEAD] Stopping timer and show wrong scene now...");
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
                logger.debug("[HEAD] Go to next normal scene now...");
                getExperiment().terminal.set(0);
                getExperiment().terminal.set(1);
            }
        }, SET.ERROR_ANS_MS.getValue());
    }
}

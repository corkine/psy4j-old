package com.mazhangjing.zsw.screen.test;

import com.mazhangjing.zsw.SET;
import com.mazhangjing.zsw.screen.StiHeadScreen;
import com.mazhangjing.zsw.sti.Array;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Timer;
import java.util.TimerTask;

public class TestStiHeadScreen extends StiHeadScreen {
    public TestStiHeadScreen(Array array) {
        super(array);
    }

    private void showRightScene() {
        //停止计时器
        logger.debug("[HEAD] Stopping timer and show right scene now...");
        getExperiment().terminal.set(-999);
        //静态资源呈现
        FlowPane pane = new FlowPane();
        pane.setAlignment(Pos.CENTER);
        Label label = new Label("✔");
        label.setFont(Font.font(SET.ERROR_SIZE.getValue()));
        label.setTextFill(Color.GREEN);
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

    @Override protected void makeJudge(boolean isLeftClicked) {
        //这里不能判断异步，如果异步并且反应，则无反应
        if (array.getHead().getLeft() != 999 || array.getHead().getRight() != 999) {
            if ((array.getHead().getLeft() > array.getHead().getRight() && !isLeftClicked) ||
                    (array.getHead().getLeft() < array.getHead().getRight() && isLeftClicked)) {
                logger.info(String.format("WRONG LEFT:%s,%s RIGHT:%s,%s", array.getHead().getLeft(), array.getHead().getLeftSize(), array.getHead().getRight(), array.getHead().getRightSize()));
                showWrongScene();
            } else {
                logger.info(String.format("CURRENT LEFT:%s,%s RIGHT:%s,%s", array.getHead().getLeft(), array.getHead().getLeftSize(), array.getHead().getRight(), array.getHead().getRightSize()));
                showRightScene();
            }
        } else {
            logger.info(String.format("ERROR LEFT:%s,%s RIGHT:%s,%s LEFT2:%s,%s RIGHT2:%s,%s",
            array.getHead().getLeft(), array.getHead().getLeftSize(),
            array.getHead().getRight(), array.getHead().getRightSize(),
            array.getBack().getLeft(), array.getBack().getLeftSize(),
            array.getBack().getRight(), array.getBack().getRightSize()));
        }
    }
}

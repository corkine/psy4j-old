package com.mazhangjing.zsw.screen.test;

import com.mazhangjing.zsw.SET;
import com.mazhangjing.zsw.screen.StiBackScreen;
import com.mazhangjing.zsw.sti.Array;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Timer;
import java.util.TimerTask;

public class TestStiBackScreen extends StiBackScreen {
    public TestStiBackScreen(Array array) {
        super(array);
    }

    private void showRightScene() {
        //停止计时器
        logger.debug("[BACK] Stopping timer and show right scene now...");
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
                goBackNormalScene();
            }
        }, SET.ERROR_ANS_MS.getValue());
    }

    @Override
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
                showRightScene();
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
                showRightScene();
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
                showRightScene();
            }
        }
    }
}

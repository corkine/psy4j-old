package com.mazhangjing.zsw.screen;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Screen;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ReadyScreen extends Screen {
    private static String INTRO;

    public ReadyScreen(boolean isNormal) {
        if (!isNormal) {
            INTRO = "欢迎参加本次实验! 本次实验需要您完成数字数值大小判断任务。首先屏幕下方中央会呈现一个“START”按钮，当你准备好后，点击此按钮开始练习。" +
                    "此时屏幕左上角和右上角会出现一对数字，您需要又快又准的移动鼠标去点击鼠数字对中数值更大的数字，即可完成一个试次。";
        } else {
            INTRO = "即将开始正式实验。本次实验需要您完成数字数值大小判断任务。首先屏幕下方中央会呈现一个“START”按钮，当你准备好后，点击此按钮就正式开始实验。" +
                    "此时屏幕左上角和右上角会出现一对数字，您需要又快又准的移动鼠标去点击鼠数字对中数值更大的数字，即可完成一个试次。";
        }
    }

    @Override
    public Screen initScreen() {
        this.duration = 100000000;

        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-alignment: center");
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        Button btn = new Button("START");
        box.getChildren().add(btn);
        pane.setBottom(box);

        Text text = new Text();
        text.setText(INTRO);
        text.setFont(Font.font("楷体",30));
        text.setWrappingWidth(800.0);
        text.setLineSpacing(12.0);
        text.setTextAlignment(TextAlignment.LEFT);
        pane.setCenter(text);


        btn.setOnAction(event -> getExperiment().terminal.set(1));

        this.layout = pane;
        return this;
    }

    @Override
    public void eventHandler(Event event, Experiment experiment, Scene scene) {

    }
}

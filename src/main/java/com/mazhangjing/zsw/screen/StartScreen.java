package com.mazhangjing.zsw.screen;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Screen;
import com.mazhangjing.zsw.SET;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class StartScreen extends Screen {
    @Override
    public Screen initScreen() {

        duration = SET.START_CLICK_MS.getValue();
        infomation = "准备开始屏幕";
        HBox box = new HBox();
        box.setAlignment(Pos.BOTTOM_CENTER);

        Button btn = new Button("START");
        box.getChildren().addAll(btn);
        layout = box;


        btn.setOnAction(event -> getExperiment().terminal.set(1));

        return this;
    }

    @Override
    public void eventHandler(Event event, Experiment experiment, Scene scene) {
    }
}

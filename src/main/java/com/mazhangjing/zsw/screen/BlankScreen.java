package com.mazhangjing.zsw.screen;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Screen;
import com.mazhangjing.zsw.SET;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;

public class BlankScreen extends Screen {
    @Override
    public Screen initScreen() {

        infomation = "空白屏幕";
        duration = SET.ANS_BLANK_MS.getValue();
        layout = new HBox();
        return this;
    }

    @Override
    public void eventHandler(Event event, Experiment experiment, Scene scene) {
    }
}

package com.mazhangjing.zsw.screen;

import com.mazhangjing.lab.Experiment;
import com.mazhangjing.lab.Screen;
import com.mazhangjing.zsw.SET;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;


public class InfoScreen extends Screen {
    @Override
    public Screen initScreen() {
        duration = SET.INFO_SET_MS.getValue();
        VBox box = new VBox();
        box.setStyle("-fx-alignment: center;-fx-padding: 60;-fx-spacing: 10");
        TextField id = new TextField();
        id.setPromptText("编号");
        TextField name = new TextField();
        name.setPromptText("姓名");
        TextField sex = new TextField();
        sex.setPromptText("性别");
        TextField other = new TextField();
        other.setPromptText("备注");
        Button ok = new Button("确定");
        box.getChildren().addAll(id,name,sex,other,ok);

        ok.setOnAction(event -> {
            logger.debug("Get User name: " + name.getText());
            logger.info("Starting Experiment now >>");
            logger.info(String.format("METADATA::%s::%s::%s::%s",id.getText(),name.getText(),sex.getText(),other.getText()));
            getExperiment().terminal.set(1);
            getExperiment().setGlobalData(String.format("%s_%s_%s",id.getText(),name.getText(),sex.getText()));
        });
        layout = box;
        return this;
    }

    @Override
    public void eventHandler(Event event, Experiment experiment, Scene scene) {

    }
}

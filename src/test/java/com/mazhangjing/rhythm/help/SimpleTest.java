package com.mazhangjing.rhythm.help;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.api.FxService;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.matcher.control.TextMatchers;

import static org.junit.Assert.*;
import static org.testfx.api.FxAssert.*;

public class SimpleTest extends ApplicationTest {

    static class iPane extends StackPane {
        iPane() {
            super();
            Button click = new Button("click");
            click.setOnAction(e -> click.setText("clicked"));
            getChildren().add(click);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = new iPane();
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    @Test public void showHaveSomething() {
        verifyThat(".button", LabeledMatchers.hasText("click"));
    }

    @Test public void testClick() {
        clickOn(".button");
        verifyThat(".button", LabeledMatchers.hasText("clicked"));
    }
}
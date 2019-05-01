package com.mazhangjing.lab.mouse;

import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.util.Random;

import static org.junit.Assert.*;

public class FrequencyPaneTest extends ApplicationTest {

    private FrequencyPane pane;

    @Override
    public void start(Stage stage) throws Exception {
        pane = new FrequencyPane();
        pane.decorateStage(stage).show();
    }

    @Test
    public void click() {
        clickOn("#light");
        type(KeyCode.SPACE).sleep(100);
        assertEquals(pane.angle().getFill(), Color.GREEN);
        for (int i = 0; i < 5; i++) {
            sleep(new Random().nextInt(300) + 100).clickOn("#light").sleep(10);
            if (i > 0) assertTrue("鼠标点击应该造成了数值的变化",pane.freq().get() != 0);
        }
    }
}
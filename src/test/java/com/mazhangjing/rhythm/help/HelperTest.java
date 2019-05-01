package com.mazhangjing.rhythm.help;

import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class HelperTest extends ApplicationTest {

    private Helper helper;

    @Override
    public void start(Stage stage) {
        helper = new Helper();
        helper.start(stage);
    }

    @Test public void testRecorder() throws IOException {
        String fileName = String.valueOf((new Random().nextInt(10000) + 10000));
        clickOn(helper.newRecordBtn()).write(fileName).type(KeyCode.ENTER);
        sleep(1000).write("3").sleep(200);
        sleep(1000).type(KeyCode.NUMPAD3);
        sleep(5200).type(KeyCode.NUMPAD4);
        sleep(2000).type(KeyCode.ENTER);
        sleep(4000);
        push(KeyCode.COMMAND, KeyCode.Q).sleep(100);
        File file = new File(fileName + "_record.csv");
        assertTrue("文件应该存在",file.exists());
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        boolean contains3 = false;
        boolean contains4 = false;
        boolean containsEnter = false;
        List<String> collect = bufferedReader.lines().collect(Collectors.toList());
        for (String line : collect) {
            if (line.toUpperCase().contains("NUMPAD3")) contains3 = true;
            if (line.toUpperCase().contains("NUMPAD4")) contains4 = true;
            if (line.toUpperCase().contains("ENTER")) containsEnter = true;
        }
        bufferedReader.close();
        fileReader.close();
        assertTrue("应该包含 3 和 Enter", contains3 && containsEnter);
        assertFalse("不应该包含 4",contains4);
        assertTrue("文件应该可以删除",file.delete());
    }

    @Test public void testProcessor() {
        clickOn(helper.processBtn()).sleep(2000);
    }

    @Test public void testTimer() {
        clickOn(helper.timeBtn()).sleep(70,TimeUnit.SECONDS);
    }

    @Test public void freqPaneTest() {
        clickOn(helper.freqBtn()).sleep(100);
        type(KeyCode.SPACE).sleep(120);
        type(KeyCode.SPACE).sleep(1000);
    }

}
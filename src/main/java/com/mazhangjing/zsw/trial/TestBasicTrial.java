package com.mazhangjing.zsw.trial;

import com.mazhangjing.lab.Trial;
import com.mazhangjing.zsw.screen.BlankScreen;
import com.mazhangjing.zsw.screen.StartScreen;
import com.mazhangjing.zsw.screen.StiBackScreen;
import com.mazhangjing.zsw.screen.StiHeadScreen;
import com.mazhangjing.zsw.screen.test.TestStiBackScreen;
import com.mazhangjing.zsw.screen.test.TestStiHeadScreen;
import com.mazhangjing.zsw.sti.Array;

public class TestBasicTrial extends Trial {

    private Array array;

    public TestBasicTrial(Array stimulate) { array = stimulate; }

    @Override
    public Trial initTrial() {
        information = "测试序列";
        screens.add(new StartScreen().initScreen());
        screens.add(new TestStiHeadScreen(array).initScreen());
        screens.add(new TestStiBackScreen(array).initScreen());
        screens.add(new BlankScreen().initScreen());
        return this;
    }
}

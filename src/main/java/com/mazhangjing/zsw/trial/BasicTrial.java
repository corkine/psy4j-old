package com.mazhangjing.zsw.trial;

import com.mazhangjing.lab.Trial;
import com.mazhangjing.zsw.screen.*;
import com.mazhangjing.zsw.sti.Array;

public class BasicTrial extends Trial {

    private Array array;

    public BasicTrial(Array stimulate) { array = stimulate; }

    @Override
    public Trial initTrial() {
        information = "正式序列";
        screens.add(new StartScreen().initScreen());
        screens.add(new StiHeadScreen(array).initScreen());
        screens.add(new StiBackScreen(array).initScreen());
        screens.add(new BlankScreen().initScreen());
        return this;
    }
}

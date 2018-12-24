package com.mazhangjing.zsw.trial;

import com.mazhangjing.lab.Trial;
import com.mazhangjing.zsw.screen.InfoScreen;
import com.mazhangjing.zsw.screen.RelaxScreen;

public class InfoTrial extends Trial {
    @Override
    public Trial initTrial() {

        information = "信息收集序列";
        screens.add(new InfoScreen().initScreen());
        return this;
    }
}

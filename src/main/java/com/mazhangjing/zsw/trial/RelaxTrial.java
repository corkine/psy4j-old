package com.mazhangjing.zsw.trial;

import com.mazhangjing.lab.Trial;
import com.mazhangjing.zsw.screen.RelaxScreen;

public class RelaxTrial extends Trial {
    @Override
    public Trial initTrial() {
        screens.add(new RelaxScreen().initScreen());
        return this;
    }
}

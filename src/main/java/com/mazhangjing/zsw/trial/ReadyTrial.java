package com.mazhangjing.zsw.trial;

import com.mazhangjing.lab.Trial;
import com.mazhangjing.zsw.screen.ReadyScreen;

public class ReadyTrial extends Trial {
    private boolean isNormal;

    public ReadyTrial(boolean isNormal) {
        this.isNormal = isNormal;
    }
    @Override
    public Trial initTrial() {
        screens.add(new ReadyScreen(isNormal).initScreen());
        return this;
    }
}

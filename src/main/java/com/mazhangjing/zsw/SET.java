package com.mazhangjing.zsw;

public enum SET {

    INFO_SET_MS("设置被试信息超时",100000),
    START_CLICK_MS("开始界面点击 START 按钮超时",100000),
    STI_SHOW_HEAD_MS("前半部分呈现刺激时间",100),
    STI_SHOW_BACK_MS("后半部分呈现刺激时间",400),
    MOVE_BUT_NOT_DONE("移动鼠标，但是尚未在有效时间内点击",4000),
    ERROR_ANS_MS("错误回答展示超时",2000),
    ERROR_SIZE("错误回答字体大小",90),
    ANS_BLANK_MS("回答结束后空屏幕等待",500),

    BIGGER_SIZE("较大的字体尺寸",90),
    SMALLER_SIZE("较小的字体尺寸",70),

    FULL_SCREEN("全屏显示",1)
    ;


    private String description;
    private int value;

    SET(String description, int value) {
        this.description = description;
        this.value = value;
    }

    public String getDescription() { return description; }
    public int getValue() { return value; }
}

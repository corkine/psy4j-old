package com.mazhangjing.zsw.sti;

import com.mazhangjing.zsw.SET;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class StiFactory {

    //数字、刺激 Stimulate 和筛子 Array（刺激包含左右两个数字，筛子包含前后两个刺激）
    private Integer bigSize = SET.BIGGER_SIZE.getValue();

    private Integer smallSize = SET.SMALLER_SIZE.getValue();

    private List<Stimulate> getList(boolean isNormal) {
        if (isNormal)
        return Arrays.asList(
                Stimulate.of(1,3),
                Stimulate.of(2,4),
                Stimulate.of(6,8),
                Stimulate.of(7,9),
                Stimulate.of(1,6),
                Stimulate.of(2,7),
                Stimulate.of(3,8),
                Stimulate.of(4,9)
        );
        else return Arrays.asList(
                Stimulate.of(1,4),
                Stimulate.of(2,5),
                Stimulate.of(1,2),
                Stimulate.of(4,5)
        );
    }

    //随机每个刺激的左右尺寸，但不包括数字本身，使用 changeHand 来交换左右的数字和大小
    private List<Stimulate> shuffleSize(List<Stimulate> list) {
        Random random = new Random();
        return list.stream().peek(stimulate -> {
            if (random.nextBoolean()) {
                stimulate.setLeftSize(bigSize);
                stimulate.setRightSize(smallSize);
            } else {
                stimulate.setLeftSize(smallSize);
                stimulate.setRightSize(bigSize);
            }
        }).collect(Collectors.toList());
    }

    //交换左右的数字和其大小
    private List<Stimulate> changeHand(List<Stimulate> list) {
        list.forEach(stimulate -> {
            int privLeft = stimulate.getLeft();
            int privLeftSize = stimulate.getLeftSize();
            stimulate.setLeft(stimulate.getRight());
            stimulate.setLeftSize(stimulate.getRightSize());
            stimulate.setRight(privLeft);
            stimulate.setRightSize(privLeftSize);
        });
        return list;
    }

    //将分别包含左右数字的前后刺激装载为前后左右均有的供一次 Trial 使用的筛子
    private List<Array> convertToArray(List<Stimulate> head, List<Stimulate> back) {
        List<Array> list = new ArrayList<>();
        for (int i = 0; i < head.size(); i++) {
            list.add(Array.of(head.get(i),back.get(i)));
        }
        return list;
    }

    private List<Array> getDifferentArrays(boolean isNormal) {
        ArrayList<Array> result = new ArrayList<>();

        //同步
        List<Array> syncArray = convertToArray(shuffleSize(getList(isNormal)), shuffleSize(getList(isNormal))); //左大右小 左小右大 12 * 2
        List<Array> arraysChangedHand = convertToArray(changeHand(shuffleSize(getList(isNormal))), changeHand(shuffleSize(getList(isNormal)))); //左右数字平衡 12 * 2
        syncArray.addAll(arraysChangedHand); // 8/4 * 2 * 2
        //对于左右数字相同，大小相反的数据进行修正
        Random random = new Random();
        syncArray.forEach(array -> {
            if(array.getHead().getLeftSize() != array.getBack().getLeftSize()) {
                if (random.nextBoolean()) {
                    array.getHead().setLeftSize(array.getBack().getLeftSize());
                    array.getHead().setRightSize(array.getBack().getRightSize());
                } else {
                    array.getBack().setLeftSize(array.getHead().getLeftSize());
                    array.getBack().setRightSize(array.getHead().getRightSize());
                }
            }
        });

        // 8 * 2 * 2 * 3 左右大小平衡、数字平衡、先后平衡
        // 4 * 2 * 2
        result.addAll(syncArray);

        if (isNormal) {
            //后刺激仅呈现右边，前刺激仅呈现左边（延时呈现，总是左边先呈现）
            List<Array> leftBehind = convertToArray(
                    shuffleSize(getList(true)).stream()
                            .peek(stimulate -> stimulate.setLeft(999)).collect(Collectors.toList()), //前排左边空置
                    shuffleSize(getList(true)).stream()
                            .peek(stimulate -> stimulate.setRight(999)).collect(Collectors.toList()) //后排右边空置
            );
            List<Array> leftBehindChangedHand = convertToArray(
                    changeHand(
                            shuffleSize(getList(true)).stream()
                                    .peek(stimulate -> stimulate.setLeft(999)).collect(Collectors.toList())), //前排左边空置，交换左右数字
                    changeHand(
                            shuffleSize(getList(true)).stream()
                                    .peek(stimulate -> stimulate.setRight(999)).collect(Collectors.toList())) //后排右边空置，交换左右数字
            );
            leftBehind.addAll(leftBehindChangedHand);

            //左先
            List<Array> leftFirst = convertToArray(
                    shuffleSize(getList(true)).stream()
                            .peek(stimulate -> stimulate.setRight(999)).collect(Collectors.toList()),
                    shuffleSize(getList(true)).stream()
                            .peek(stimulate -> stimulate.setLeft(999)).collect(Collectors.toList()));
            List<Array> leftFirstChangeHand = convertToArray(
                    changeHand(
                            shuffleSize(getList(true)).stream()
                                    .peek(stimulate -> stimulate.setRight(999)).collect(Collectors.toList())),
                    changeHand(
                            shuffleSize(getList(true)).stream()
                                    .peek(stimulate -> stimulate.setLeft(999)).collect(Collectors.toList())));
            leftFirst.addAll(leftFirstChangeHand);

            result.addAll(leftBehind);
            result.addAll(leftFirst);
        }

        return result;
    }

    private List<Array> getAllShuffleArrays() {
        ArrayList<Array> result = new ArrayList<>();
        //重复四次，得到 384 个试次
        for (int i = 0; i < 8; i++) {
            result.addAll(getDifferentArrays(true));
        }
        Collections.shuffle(result);
        Collections.shuffle(result);
        Collections.shuffle(result);
        fixSameSize(result);
        return result;
    }

    private List<Array> getAllTestShuffleArrays() {
        ArrayList<Array> result = new ArrayList<>();
        //重复 得到 16 个试次
        for (int i = 0; i < 2; i++) {
            result.addAll(getDifferentArrays(false));
        }
        Collections.shuffle(result);
        Collections.shuffle(result);
        Collections.shuffle(result);
        return result;
    }

    //外部统一接口
    public static List<Array> getArrays() {
        return new StiFactory().getAllShuffleArrays();
    }

    public static List<Array> getTestArrays() {
        return new StiFactory().getAllTestShuffleArrays();
    }

    @Test
    public void test() {
        int size =getAllTestShuffleArrays().size();
        System.out.println("size = " + size);
        /*List<Array> arrays = StiFactory.getArrays();
        System.out.println("arrays = " + arrays.size());*/
        /*arrays.forEach(array -> {
            Stimulate head = array.getHead();
            Stimulate back = array.getBack();
            if (head.getLeft() == 999 || head.getRight() == 999 || back.getLeft() == 999 || back.getRight() == 999) {
                if (head.getLeftSize() == back.getRightSize() && head.getLeft() != 999 && back.getRight() != 999) {
                    System.out.println(head + " :: " + back + " :: hl e br");

                }
                if (head.getRightSize() == back.getLeftSize() && head.getRight() != 999 && back.getLeft() != 999) {
                    System.out.println(head + " :: " + back + " :: hr e bl");

                }
            }
        });*/
    }

    //修复异步状态下的字体大小相等问题
    private void fixSameSize(List<Array> arrays) {
        Random random = new Random();
        arrays.forEach(array -> {
            Stimulate head = array.getHead();
            Stimulate back = array.getBack();
            if (head.getLeft() == 999 || head.getRight() == 999 || back.getLeft() == 999 || back.getRight() == 999) { //对于异步情况
                if (head.getLeftSize() == back.getRightSize() && head.getLeft() != 999 && back.getRight() != 999) { //如果前面左边等于右面后边，且其均不为空
                    if (random.nextBoolean()) { //随机为前面或者后面分配相反的字体大小
                        if (head.getLeftSize() == SET.BIGGER_SIZE.getValue()) head.setLeftSize(SET.SMALLER_SIZE.getValue());
                        else head.setLeftSize(SET.BIGGER_SIZE.getValue());
                    } else {
                        if (back.getRightSize() == SET.BIGGER_SIZE.getValue()) back.setRightSize(SET.SMALLER_SIZE.getValue());
                        else back.setRightSize(SET.BIGGER_SIZE.getValue());
                    }
                }
                if (head.getRightSize() == back.getLeftSize() && head.getRight() != 999 && back.getLeft() != 999) { //如果前面右边等于后面左边，且其均不为空
                    if (random.nextBoolean()) { //随机为前面或者后面分配相反的字体大小
                        if (back.getLeftSize() == SET.BIGGER_SIZE.getValue()) back.setLeftSize(SET.SMALLER_SIZE.getValue());
                        else back.setLeftSize(SET.BIGGER_SIZE.getValue());
                    } else {
                        if (head.getRightSize() == SET.BIGGER_SIZE.getValue()) head.setRightSize(SET.SMALLER_SIZE.getValue());
                        else head.setRightSize(SET.BIGGER_SIZE.getValue());
                    }
                }
            }
        });
    }

}





package com.mazhangjing.zsw.sti;

import java.util.Objects;

public class Stimulate {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stimulate stimulate = (Stimulate) o;
        return left == stimulate.left &&
                right == stimulate.right &&
                leftSize == stimulate.leftSize &&
                rightSize == stimulate.rightSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, leftSize, rightSize);
    }

    private int left;
    private int right;
    private int leftSize;
    private int rightSize;

    public static Stimulate of(int left, int right) {
        return new Stimulate(left,right);
    }

    private Stimulate(int left, int right) {
        this.left = left;
        this.right = right;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getLeftSize() {
        return leftSize;
    }

    public void setLeftSize(int leftSize) {
        this.leftSize = leftSize;
    }

    public int getRightSize() {
        return rightSize;
    }

    public void setRightSize(int rightSize) {
        this.rightSize = rightSize;
    }

    @Override
    public String toString() {
        return "Stimulate{" +
                "left=" + left +
                ", right=" + right +
                ", leftSize=" + leftSize +
                ", rightSize=" + rightSize +
                '}';
    }
}
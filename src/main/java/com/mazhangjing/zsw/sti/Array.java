package com.mazhangjing.zsw.sti;

import java.util.Objects;

public class Array {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Array array = (Array) o;
        return Objects.equals(head, array.head) &&
                Objects.equals(back, array.back);
    }

    @Override
    public int hashCode() {
        return Objects.hash(head, back);
    }

    private Stimulate head;
    private Stimulate back;

    public Array(Stimulate head, Stimulate back) {
        this.head = head;
        this.back = back;
    }

    public static Array of(Stimulate head, Stimulate back) {
        return new Array(head,back);
    }

    public Stimulate getHead() {
        return head;
    }

    public void setHead(Stimulate head) {
        this.head = head;
    }

    public Stimulate getBack() {
        return back;
    }

    public void setBack(Stimulate back) {
        this.back = back;
    }

    @Override
    public String toString() {
        return "Array{" +
                "\nhead=" + head +
                ", \nback=" + back +
                "}";
    }
}
package com.github.zxh.classpy.classfile.testclasses;

@FunctionalInterface
public interface MyInterface {
    
    void foo(Object x, String y, int z);
    
    default void bar() {
        
    }
    
    static int sm(int x) {
        return x;
    }
    
}

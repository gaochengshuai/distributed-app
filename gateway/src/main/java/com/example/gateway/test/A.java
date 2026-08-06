package com.example.gateway.test;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class A {
    interface Test{
        void send (String s);
    }
    public static void main(String[] args) {
//        String[] a = {"a","b","c"};
//        String[] b = {"d","e","f"};
//        List<String> aList = Arrays.stream(a).filter(s -> s.contains("a")).collect(Collectors.toList());
//        Test test = (s) -> System.out.println(s);
//        test.send("不懂");
//        System.out.println(aList);
//
        String str = "my name is gao";
//        Stream.of(str.split(" ")).map(s->s.length()).forEach(System.out::println);
//        Stream.of(str.split( " ")).filter(s -> s.length()>2).map(s->s.length()).forEach(System.out::println);
        List<String> list = Stream.of(str.split(" ")).collect(Collectors.toList());
        System.out.println(list);
    }
}
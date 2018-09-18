package com.github.lxbr.calc;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Deque;
import java.util.ArrayDeque;

public class PostfixCalc {

    public static long add(long a, long b) {
	return a + b;
    }

    public static long subtract(long a, long b) {
	return a - b;
    }

    public static long multiply(long a, long b) {
	return a * b;
    }

    public static long divide(long a, long b) {
	return a / b;
    }

    public static List<String> tokenize(String input) {
	return Arrays.asList(input.split("\\s+"));
    }

    private static Map map(Object... args) {
	Map<Object, Object> result = new HashMap<>();
	for (int i = 0; i < args.length; i+=2) {
	    result.put(args[i], args[i+1]);
	}
	return result;
    }

    public static List<Map> parse(List<String> tokens) {
	List<Map> result = new ArrayList<>();
	for (String token: tokens) {
	    String kind;
	    switch (token) {
	    case "+": kind = "add"; break;
	    case "-": kind = "sub"; break;
	    case "*": kind = "mul"; break;
	    case "/": kind = "div"; break;
	    default:  kind = "num";
	    }
	    result.add(map("token", token, "kind", kind));
	}
	return result;
    }

    public static long interpret(List<Map> data) {
	Deque<Long> stack = new ArrayDeque<>();
	long a;
	long b;
	for (Map datum: data) {
	    String token = (String) datum.get("token");
	    switch ((String) datum.get("kind")) {
	    case "add":
		b = stack.pop();
		a = stack.pop();
		stack.push(add(a, b));
		break;
	    case "sub":
		b = stack.pop();
		a = stack.pop();
		stack.push(subtract(a, b));
		break;
	    case "mul":
		b = stack.pop();
		a = stack.pop();
		stack.push(multiply(a, b));
		break;
	    case "div":
		b = stack.pop();
		a = stack.pop();
		stack.push(divide(a, b));
		break;
	    default: stack.push(Long.parseLong(token, 10));
	    }
	}
	return stack.peek();
    }

    public static long eval(String input) {
	return interpret(parse(tokenize(input)));
    }
}

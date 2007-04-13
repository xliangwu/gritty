package com.wittams.gritty;

import java.lang.reflect.Array;

public class Util {
	public static <T> T[] copyOf(T[] original, int newLength){
		Class<T> type = (Class<T>) original.getClass().getComponentType();
		T[] newArr = (T[]) Array.newInstance(type, newLength);
		
		System.arraycopy(original, 0, newArr, 0, Math.min(original.length, newLength));
		
		return newArr;	
	}
	
	public static int[] copyOf(int[] original, int newLength){
		int[] newArr = new int[newLength];
		
		System.arraycopy(original, 0, newArr, 0, Math.min(original.length, newLength));
		
		return newArr;	
	}
	
	public static char[] copyOf(char[] original, int newLength){
		char[] newArr = new char[newLength];
		
		System.arraycopy(original, 0, newArr, 0, Math.min(original.length, newLength));
		
		return newArr;	
	}
	
}

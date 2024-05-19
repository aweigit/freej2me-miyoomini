package android.util;

public  class Log {
	
	public static void  e(String a,String b, Exception e)
	{
		System.out.println(a+":"+b);
		e.getMessage();
	}
	public static void  e(String a,String b)
	{
		System.out.println(a+":"+b);
	}
	
	public static void  d(String a,String b)
	{
		System.out.println(a+":"+b);
	}
	
	public static void  w(String a,String b)
	{
		System.out.println(a+":"+b);
	}
}
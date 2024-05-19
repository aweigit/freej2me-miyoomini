package javax.microedition.shell;

public class MidletRuntime
{
	private static final String TAG = MidletRuntime.class.getName();
	
	private static final MidletRuntime MIDLET_RUNTIME = new MidletRuntime();

	private Runtime mRuntime;

	private MidletRuntime() { mRuntime = Runtime.getRuntime();}

	public static MidletRuntime getRuntime() { return MIDLET_RUNTIME; }

	public void exit(int status) { throw new SecurityException("MIDP lifecycle does not support system exit."); }

	public long totalMemory() {

		long memoryLimit = 2<<20;

		return memoryLimit;
	}

	public long freeMemory() {

		//long memoryLimit = MidletSystem.memoryLimit();
		long memoryLimit = 2<<20;

		return memoryLimit;
	}

	public long maxMemory() {

		long memoryLimit = 2<<20;
		return memoryLimit;

	}

	public void gc()
	{ 
		mRuntime.gc();
	}

}
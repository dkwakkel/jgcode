import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HelloWorldGcode {

	public static void main(String[] args) throws Throwable {
		try(InputStream gcodeStream = HelloWorldGcode.class.getResourceAsStream("helloworld.gcode")) {
			// Machine machine = wrapWithProxy(null);
			Machine machine = new MotorMachine();
			MotorMachine.execute(machine, gcodeStream);
		}
	}

	private static Machine wrapWithProxy(Machine machine)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		InvocationHandler handler = new MachineInvocationHandler(System.out, machine);
		Class<?> proxyClass = Proxy.getProxyClass(Machine.class.getClassLoader(), new Class[] { Machine.class });
		return (Machine) proxyClass.getConstructor(new Class[] { InvocationHandler.class }).newInstance(new Object[] { handler });
	}

	private static class MachineInvocationHandler implements InvocationHandler {
		private final Machine implementation;
		private final PrintStream out;

		public MachineInvocationHandler(PrintStream out, Machine implementation) {
			this.implementation = implementation;
			this.out = out;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			print(method.getName(), args);
			return implementation == null ? null : method.invoke(implementation, args);
		}

		void print(String methodName, Object ... args) {
			out.print(methodName);
			out.print('(');
			for(Object arg : args) {
				if(arg !=args[0]) {
					out.print(", ");
				}
				out.print(arg);
			}
			out.println(")");
		}
	}

}

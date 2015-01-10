import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dkwakkel.jgcode.GCodeParser;
import dkwakkel.jgcode.GCodeParser.Machine;


public class HelloWorldGcode {
	
	public static void main(String[] args) throws Throwable {
		InputStream gcodeStream = HelloWorldGcode.class.getResourceAsStream("helloworld.gcode");

		Machine machine = new EV3Machine();
		GCodeParser.execute(wrapWithProxy(machine), gcodeStream);
	}

	private static Machine wrapWithProxy(Machine machine)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		InvocationHandler handler = new InvocationHandlerImplementation(System.out, machine);
		Class<?> proxyClass = Proxy.getProxyClass(Machine.class.getClassLoader(), new Class[] { Machine.class });
		return (Machine) proxyClass.getConstructor(new Class[] { InvocationHandler.class }).newInstance(new Object[] { handler });
	}
	
	private static class InvocationHandlerImplementation implements InvocationHandler {
		private final Machine implementation;
		private final PrintStream out;

		public InvocationHandlerImplementation(PrintStream out, Machine implementation) {
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

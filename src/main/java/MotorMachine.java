import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.ceil;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.pow;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.sqrt;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import dkwakkel.jgcode.GCodeLexer;
import dkwakkel.jgcode.GCodeParser;

public class MotorMachine implements Machine
{
	// See Table 2. Default Parameter File
	// TODO: Persistence
	double[] parameters = new double[5400 + 1];
	{
		parameters[5220] = 1.0;
	}

	public static void execute(Machine machine, java.io.InputStream in) throws Exception {
		ANTLRInputStream input = new ANTLRInputStream(in == null ? System.in : in);

		Lexer lexer = new GCodeLexer(input);
		TokenStream tokens = new CommonTokenStream(lexer);
		GCodeParser parser = new GCodeParser(tokens);

		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(new Executor(machine), parser.program());
	}

	private static double INCH_IN_MM = 25.4;

	public static double convertUnit(CANON_UNITS from, CANON_UNITS to, double input) {
		return from == to ? input : (to == CANON_UNITS.CANON_UNITS_MM ? input * INCH_IN_MM : input / INCH_IN_MM);
	}

	private CANON_PLANE						canonPlane	= CANON_PLANE.CANON_PLANE_XY;
	private CANON_FEED_REFERENCE	feedReference;
	private CANON_MOTION_MODE			motionControlMode;

	private double								feedRateInUnitPerMinute;

	static class Motor
	{
		private final String	name;

		public Motor(String name) {
			this.name = name;
		}

		public void setSpeed(double speed) {
			System.out.println(name + " speed: " + speed);
		}

		public void moveTo(double valueInMM) {
			System.out.println(name + " move to: " + valueInMM);
		}
	}

	public enum Axis
	{
		X, Y, Z, A, B, C;

		Motor						motor;
		CANON_UNITS			units					= CANON_UNITS.CANON_UNITS_MM;

		double					origin;
		double					currentValue;
		private double	currentFeedRate;

		Axis() {
			this.motor = new Motor(name());
		}

		public void setMotor(Motor motor) {
			this.motor = motor;
			origin = currentValue = currentFeedRate = 0;
		}

		void setSpeed(double feedRateInUnitPerMinute) {
			if (currentFeedRate != feedRateInUnitPerMinute) {
				currentFeedRate = feedRateInUnitPerMinute;
				double feedRateInMmPerMinute = convertUnit(units, CANON_UNITS.CANON_UNITS_MM, feedRateInUnitPerMinute);
				motor.setSpeed(feedRateInMmPerMinute);
			}
		}

		public void moveTo(double value) {
			if (value != currentValue) {
				double valueInMM = convertUnit(units, CANON_UNITS.CANON_UNITS_MM, value);
				motor.moveTo(valueInMM);
				currentValue = value;
			}
		}

		public void setUnit(CANON_UNITS units) {
			origin = convertUnit(this.units, units, origin);
			currentValue = convertUnit(this.units, units, currentValue);
			this.units = units;
		}
	}

	enum DIMENSION
	{
		D, F // feed rate
		,
		H // tool length offset index
		,
		P // dwell time in canned cycles | dwell time with G4 | key used with G10
		,
		S // spindle speed
		,
		T // tool selection
		;
		double	value;
	}

	@Override
	public void SET_ORIGIN_OFFSETS(double x, double y, double z, double a, double b, double c) {
		Axis.X.origin = x;
		Axis.Y.origin = y;
		Axis.Z.origin = z;
		Axis.A.origin = a;
		Axis.B.origin = b;
		Axis.C.origin = c;

		// TODO: move to correct place
		COMMENT("Move the mill to its startpoint and press <OK>");
	}

	@Override
	public void USE_LENGTH_UNITS(CANON_UNITS units) {
		for (Axis axis : Axis.values()) {
			axis.setUnit(units);
		}
		// DIMENSION.H.value = convertUnit(this.units, units, DIMENSION.H.value);
	}

	@Override
	public void STRAIGHT_TRAVERSE(double x, double y, double z, double a, double b, double c) {
		double currentFeedRate = feedRateInUnitPerMinute;
		try {
			feedRateInUnitPerMinute = Double.MAX_VALUE;
			moveTo(Axis.X, x, Axis.Y, y, Axis.Z, z, a, b, c);
		}
		finally {
			feedRateInUnitPerMinute = currentFeedRate;
		}
	}

	@Override
	public void SELECT_PLANE(CANON_PLANE plane) {
		this.canonPlane = plane;
	}

	@Override
	public void SET_FEED_RATE(double feedRateInUnitPerMinute) {
		this.feedRateInUnitPerMinute = feedRateInUnitPerMinute;
	}

	@Override
	public void SET_FEED_REFERENCE(CANON_FEED_REFERENCE reference) {
		this.feedReference = reference;
	}

	@Override
	public void SET_MOTION_CONTROL_MODE(CANON_MOTION_MODE mode) {
		this.motionControlMode = mode;
	}

	@Override
	public void START_SPEED_FEED_SYNCH() {
		// TODO Auto-generated method stub
	}

	@Override
	public void STOP_SPEED_FEED_SYNCH() {
		// TODO Auto-generated method stub
	}

	@Override
	public void ARC_FEED(	double firstEnd,
												double secondEnd,
												double firstCenter,
												double secondCenter,
												int rotation,
												double axisEndPoint,
												double a,
												double b,
												double c) {
		Axis first, second, axis;
		switch (canonPlane) {
			case CANON_PLANE_XY:
				first = Axis.X;
				second = Axis.Y;
				axis = Axis.Z;
				break;
			case CANON_PLANE_XZ:
				first = Axis.X;
				second = Axis.Z;
				axis = Axis.Y;
				break;
			case CANON_PLANE_YZ:
				first = Axis.Y;
				second = Axis.Z;
				axis = Axis.X;
				break;
			default:
				throw new IllegalStateException("plane=" + canonPlane);
		}

		double firstDifference = firstEnd - firstCenter;
		double secondDifference = secondEnd - secondCenter;

		// Clockwise
		double angleA, angleB;
		boolean clockwise = rotation >= 0;
		if (clockwise) {
			angleA = atan2(secondDifference, firstDifference);
			angleB = atan2(second.currentValue, first.currentValue);
		}
		// Counterclockwise
		else {
			angleA = atan2(second.currentValue, first.currentValue);
			angleB = atan2(secondDifference, firstDifference);
		}

		// Make sure angleB is always greater than angleA and if not add 2PI
		// so that it is (this also takes care of the special case of angleA == angleB,
		// i.e. we want a complete circle)
		if (angleB <= angleA) {
			angleB += 2 * PI;
		}
		double angle = angleB - angleA;
		double radius = sqrt(sqr(first.currentValue - firstCenter) + sqr(second.currentValue - secondCenter));
		double length = radius * angle;

		// TODO: Good way to calculate steps
		double curveSection = 1.0; // convertUnit(CANON_UNITS_MM, units, 1.0);
		// Maximum of either 2.4 times the angle in radians
		// or the length of the curve divided by the curve section constant
		int steps = (int) ceil(max(angle * 2.4, length / curveSection));

		for (int s = 1; s <= steps; s++) {
			int step = clockwise ? steps - s : s;

			double stepValue = ((double) step) / steps;
			double firstValue = firstCenter + radius * cos(angleA + angle * stepValue);
			double secondValue = secondCenter + radius * sin(angleA + angle * stepValue);

			double axisValue = calculateStepValue(axis.currentValue, axisEndPoint, steps, s);
			double aValue = calculateStepValue(Axis.A.currentValue, a, steps, s);
			double bValue = calculateStepValue(Axis.B.currentValue, b, steps, s);
			double cValue = calculateStepValue(Axis.C.currentValue, c, steps, s);

			moveTo(first, firstValue, second, secondValue, axis, axisValue, aValue, bValue, cValue);
		}

		// Move to end point
		moveTo(first, firstEnd, second, secondEnd, axis, axisEndPoint, a, b, c);
	}

	private double calculateStepValue(double startValue, double endValue, int steps, int s) {
		return startValue + (endValue - startValue) * (s) / steps;
	}

	private static double sqr(double value) {
		return pow(value, 2);
	}

	@Override
	public void DWELL(double seconds) {
		DIMENSION.P.value = seconds;
	}

	@Override
	public void STRAIGHT_FEED(double x, double y, double z, double a, double b, double c) {
		moveTo(Axis.X, x, Axis.Y, y, Axis.Z, z, a, b, c);
	}

	@Override
	public void STRAIGHT_PROBE(double x, double y, double z, double a, double b, double c) {
		// TODO Auto-generated method stub
	}

	@Override
	public void ORIENT_SPINDLE(double orientation, CANON_DIRECTION direction) {
		// TODO Auto-generated method stub
	}

	@Override
	public void SET_SPINDLE_SPEED(double r) {
		DIMENSION.S.value = r;
	}

	@Override
	public void START_SPINDLE_CLOCKWISE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void START_SPINDLE_COUNTERCLOCKWISE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void STOP_SPINDLE_TURNING() {
		// TODO Auto-generated method stub
	}

	@Override
	public void CHANGE_TOOL(int slot) {
		// TODO Auto-generated method stub
	}

	@Override
	public void SELECT_TOOL(int i) {
		DIMENSION.T.value = i;
	}

	@Override
	public void USE_TOOL_LENGTH_OFFSET(double offset) {
		DIMENSION.H.value = offset;
	}

	@Override
	public void COMMENT(String s) {
		System.err.println(s);
	}

	@Override
	public void DISABLE_FEED_OVERRIDE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void DISABLE_SPEED_OVERRIDE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void ENABLE_FEED_OVERRIDE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void ENABLE_SPEED_OVERRIDE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void FLOOD_OFF() {
		// TODO Auto-generated method stub
	}

	@Override
	public void FLOOD_ON() {
		// TODO Auto-generated method stub
	}

	@Override
	public void INIT_CANON() {
		// TODO Auto-generated method stub
	}

	@Override
	public void MESSAGE(String s) {
		// TODO Auto-generated method stub
	}

	@Override
	public void MIST_OFF() {
		// TODO Auto-generated method stub
	}

	@Override
	public void MIST_ON() {
		// TODO Auto-generated method stub
	}

	@Override
	public void PALLET_SHUTTLE() {
		// TODO Auto-generated method stub
	}

	@Override
	public void OPTIONAL_PROGRAM_STOP() {
		// TODO Auto-generated method stub
	}

	@Override
	public void PROGRAM_END() {
		// TODO Auto-generated method stub
	}

	@Override
	public void PROGRAM_STOP() {
		// TODO Auto-generated method stub
	}

	@Override
	public void SET_CUTTER_RADIUS_COMPENSATION(double radius) {
		DIMENSION.D.value = radius;
	}

	@Override
	public void START_CUTTER_RADIUS_COMPENSATION(CANON_COMP_SIDE side) {
		// TODO Auto-generated method stub
	}

	@Override
	public void STOP_CUTTER_RADIUS_COMPENSATION() {
		// TODO Auto-generated method stub
	}

	private void moveTo(Axis firstAxis,
											double firstValue,
											Axis secondAxis,
											double secondValue,
											Axis thirdAxis,
											double thirdValue,
											double a,
											double b,
											double c) {

		firstAxis.setSpeed(feedRateInUnitPerMinute);
		secondAxis.setSpeed(feedRateInUnitPerMinute);
		thirdAxis.setSpeed(feedRateInUnitPerMinute);

		firstAxis.moveTo(firstValue);
		secondAxis.moveTo(secondValue);
		thirdAxis.moveTo(thirdValue);

		Axis.A.moveTo(a);
		Axis.B.moveTo(b);
		Axis.C.moveTo(c);
	}

	public void close() {
		// Nothing
	}

}

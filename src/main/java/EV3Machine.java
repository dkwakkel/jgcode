import static dkwakkel.jgcode.GCodeParser.convertUnit;
import static dkwakkel.jgcode.GCodeParser.CANON_UNITS.CANON_UNITS_MM;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.ceil;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.sqrt;
import lejos.hardware.Button;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.Motor;
import lejos.hardware.motor.NXTRegulatedMotor;
import dkwakkel.jgcode.GCodeParser;
import dkwakkel.jgcode.GCodeParser.CANON_COMP_SIDE;
import dkwakkel.jgcode.GCodeParser.CANON_DIRECTION;
import dkwakkel.jgcode.GCodeParser.CANON_FEED_REFERENCE;
import dkwakkel.jgcode.GCodeParser.CANON_MOTION_MODE;
import dkwakkel.jgcode.GCodeParser.CANON_PLANE;
import dkwakkel.jgcode.GCodeParser.CANON_UNITS;

public class EV3Machine implements GCodeParser.Machine {
	private CANON_PLANE plane = CANON_PLANE.CANON_PLANE_XY;
	private CANON_FEED_REFERENCE reference;
	private CANON_MOTION_MODE mode;
	
	private double feedRateInUnitPerMinute;

	enum Axis { X(Motor.A), Y(Motor.B), Z(Motor.C), A, B, C;
		final NXTRegulatedMotor motor;
		double mmPerRotation = 25;
		CANON_UNITS units = CANON_UNITS.CANON_UNITS_MM;
		
		double origin;
		double value;

		Axis() {
			this(null);
		}
		
		Axis(NXTRegulatedMotor motor) {
			this.motor = motor;
		}
		
		void setSpeed(double feedRateInUnitPerMinute) {
			if(motor != null) {
				motor.setSpeed(calculateSpeed(feedRateInUnitPerMinute));
			}
		}

		public void moveTo(double value) {
			if(motor != null) {
				motor.rotateTo((int)convertUnit(units, CANON_UNITS_MM, value));
			}			
		}

		private float calculateSpeed(double feedRateInUnitPerMinute) {
			return getRotationSpeedInDegreesPerSecond(units, feedRateInUnitPerMinute);
		}
		
		private float getRotationSpeedInDegreesPerSecond(CANON_UNITS units, double feedRateInUnitPerMinute) {
			double feedRateInMMPerMinute = convertUnit(units, CANON_UNITS_MM, feedRateInUnitPerMinute);
			double feedRateInMMPerSecond = feedRateInMMPerMinute / 60;

			double mmPerDegree = mmPerRotation / 360;
			
			return (float)(feedRateInMMPerSecond / mmPerDegree);
		}

		public void setUnit(CANON_UNITS units) {
			origin = convertUnit(this.units, units, origin);
			value = convertUnit(this.units, units, value);
		}
	};
	
	static { 
		Axis.Z.mmPerRotation = 0.5; // TODO: calculate correct value
	}

	enum DIMENSION { 
		D
		, F // feed rate
		, H // tool length offset index
		, P // dwell time in canned cycles | dwell time with G4 | key used with G10
		, S // spindle speed
		, T // tool selection
		;
		double value;
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
		KeyListener listener = new KeyListener() {
			
			@Override
			public void keyReleased(Key k) {
				System.exit(0);
			}
			
			public void keyPressed(Key k) {
				// Nothing
			}
		};
		Button.ESCAPE.addKeyListener(listener);
		Button.ENTER.waitForPressAndRelease();
		LCD.clear();
	}

	@Override
	public void USE_LENGTH_UNITS(CANON_UNITS units) {
		for(Axis axis : Axis.values()) {
			axis.setUnit(units);
		}
//		DIMENSION.H.value = convertUnit(this.units, units, DIMENSION.H.value);
	}

	@Override
	public void STRAIGHT_TRAVERSE(double x, double y, double z, double a, double b, double c) {
		double currentFeedRate = feedRateInUnitPerMinute;
		try {
			feedRateInUnitPerMinute = Double.MAX_VALUE;
			moveTo(Axis.X, x, Axis.Y, y, Axis.Z, z, a, b, c);
		} finally {
			feedRateInUnitPerMinute =currentFeedRate;
		}
	}
		
	@Override
	public void SELECT_PLANE(CANON_PLANE plane) {
		this.plane = plane;
	}

	@Override
	public void SET_FEED_RATE(double feedRateInUnitPerMinute) {
		this.feedRateInUnitPerMinute = feedRateInUnitPerMinute;
	}

	@Override
	public void SET_FEED_REFERENCE(CANON_FEED_REFERENCE reference) {
		this.reference = reference;
	}

	@Override
	public void SET_MOTION_CONTROL_MODE(CANON_MOTION_MODE mode) {
		this.mode = mode;
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
	public void ARC_FEED(double firstEnd, double secondEnd,
			double firstCenter, double secondCenter, int rotation,
			double axisEndPoint, double a, double b, double c) {
		Axis first, second, axis;
		switch(plane) {
			case CANON_PLANE_XY: first = Axis.X; second = Axis.Y; axis = Axis.Z; break;
			case CANON_PLANE_XZ: first = Axis.X; second = Axis.Z; axis = Axis.Y; break;
			case CANON_PLANE_YZ: first = Axis.Y; second = Axis.Z; axis = Axis.X; break;
			default: throw new IllegalStateException("plane=" + plane);
		}
		
		double firstDifference = firstEnd - firstCenter;
		double secondDifference = secondEnd - secondCenter;
		
		// Clockwise
		double angleA, angleB;
		boolean clockwise = rotation >= 0;
		if (clockwise) {
			angleA = atan2(secondDifference, firstDifference);
			angleB = atan2(second.value, first.value);
		}
		// Counterclockwise
		else {
			angleA = atan2(second.value, first.value);
			angleB = atan2(secondDifference, firstDifference);
		}

		// Make sure angleB is always greater than angleA and if not add 2PI
		// so that it is (this also takes care of the special case of angleA == angleB,
		// ie we want a complete circle)
		if (angleB <= angleA) {
			angleB += 2 * PI;
		}
		double angle = angleB - angleA;
		double radius = sqrt(sqr(first.value - firstCenter) + sqr(second.value - secondCenter));
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
			
			double axisValue = calculateStepValue(axis.value, axisEndPoint, steps, s);
			double aValue = calculateStepValue(Axis.A.value, a, steps, s);
			double bValue = calculateStepValue(Axis.B.value, b, steps, s);
			double cValue = calculateStepValue(Axis.C.value, c, steps, s);

			moveTo(first, firstValue, second, secondValue, axis, axisValue, aValue, bValue, cValue);
		}
		
		// Move to end point
		moveTo(first, firstEnd, second, secondEnd, axis, axisEndPoint, a, b, c);
	}

	private double calculateStepValue(double startValue, double endValue, int steps, int s) {
		return startValue + (endValue - startValue) * ((double)s) / steps;
	}
	
	private static double sqr(double value) {
		return value + value;
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
		LCD.drawString(s, 0, 5);
		LCD.scroll();
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

	private void moveTo(Axis firstAxis, double firstValue, Axis secondAxis,
			double secondValue, Axis thirdAxis, double thirdValue, double a, double b, 	double c) {
		
		// TODO: calculate motor speeds, including max speed (double max)
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

}

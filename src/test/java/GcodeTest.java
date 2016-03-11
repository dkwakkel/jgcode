import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import dkwakkel.jgcode.GCodeParser;

@RunWith(Parameterized.class)
public class GcodeTest
{
	private final TestMotor			xMotor	= new TestMotor(MotorMachine.Axis.X.name());
	private final TestMotor			yMotor	= new TestMotor(MotorMachine.Axis.Y.name());
	private final TestMotor			zMotor	= new TestMotor(MotorMachine.Axis.Z.name());
	private final TestMotor			aMotor	= new TestMotor(MotorMachine.Axis.A.name());
	private final TestMotor			bMotor	= new TestMotor(MotorMachine.Axis.B.name());
	private final TestMotor			cMotor	= new TestMotor(MotorMachine.Axis.C.name());

	private final MotorMachine	machine	= new MotorMachine();
	{
		MotorMachine.Axis.X.setMotor(xMotor);
		MotorMachine.Axis.Y.setMotor(yMotor);
		MotorMachine.Axis.Z.setMotor(zMotor);
		MotorMachine.Axis.A.setMotor(aMotor);
		MotorMachine.Axis.B.setMotor(bMotor);
		MotorMachine.Axis.C.setMotor(cMotor);
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> testAllHandlers() {
		Collection<Object[]> result = new ArrayList<>();
		for (String unitCode : new String[] { "g20", "g21" }) {
			for (MotorMachine.Axis axis : MotorMachine.Axis.values()) {
				for (String motionCode : new String[] { "g0", "g1" }) {
					addResult(result, unitCode, axis, motionCode);
				}
				for (String motionCode : new String[] { "g2", "g3" }) {
					addResult(result, unitCode, axis, motionCode);
				}
			}
		}
		return result;
	}

	private static void addResult(Collection<Object[]> result, String unitCode, MotorMachine.Axis axis, String motionCode) {
		String line = "n0 " + unitCode + " " + getAxisCode(motionCode, axis) + " " + motionCode;
		result.add(new Object[] { line, unitCode, axis, motionCode });
	}

	private static String getAxisCode(String motionCode, MotorMachine.Axis axis) {
		String axisCode = axis.toString().toLowerCase() + AXIS_END_VALUE;
		if (isArcMotionCode(motionCode)) {
			switch (axis) {
				case X:
					return axisCode + " i" + ARC_END_VALUE;
				case Y:
					return axisCode + " j" + ARC_END_VALUE;
				case Z:
					return axisCode + " k" + ARC_END_VALUE;
				default:
					return axisCode + " r" + ARC_END_VALUE;
			}
		}
		return axisCode;
	}

	private static boolean isArcMotionCode(String motionCode) {
		return motionCode.equals("g2") || motionCode.equals("g3");
	}

	@Parameter(0)
	public String							line;

	@Parameter(1)
	public String							unitCode;

	@Parameter(2)
	public MotorMachine.Axis	axis;

	@Parameter(3)
	public String							motionCode;
	private static int				AXIS_END_VALUE	= 10;
	private static int				ARC_END_VALUE		= 5;

	@Test
	public void test() throws Exception {
		double expected = calculateExpectedValue();
		System.err.println(line);
		evaluateCode();

		for (MotorMachine.Axis axis : MotorMachine.Axis.values()) {
			System.err.println(((TestMotor) axis.motor).value);
		}
		assertEquals(expected, ((TestMotor) (axis.motor)).value, 0);
	}

	private double calculateExpectedValue() {
		double expectedValue = AXIS_END_VALUE;
		return expectedValue * (isInInches() ? 25.4 : 1);
	}

	private static boolean isRotationalAxis(MotorMachine.Axis axis) {
		String n = axis.toString().toLowerCase();
		return n.equals("a") || n.equals("b") || n.equals("c");
	}

	private boolean isInInches() {
		return unitCode.equals("g20");
	}

	private void evaluateCode() throws Exception, IOException {
		try (InputStream gcodeStream = new ByteArrayInputStream((line + "\n").getBytes(UTF_8))) {
			GCodeParser.execute(machine, gcodeStream);
		}
	}

	class TestMotor extends MotorMachine.Motor
	{
		double	value;
		double	speed;

		public TestMotor(String name) {
			super(name);
		}

		@Override
		public void moveTo(double valueInMM) {
			super.moveTo(valueInMM);
			this.value = valueInMM;
		}

		@Override
		public void setSpeed(double speed) {
			super.setSpeed(speed);
			this.speed = speed;
		}
	}

}

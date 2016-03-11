import static org.junit.Assert.assertEquals;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class MotorMachineTest
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

	@DataPoints
	public static Double[] values() {
		return new Double[] { 0.0, 1.0, -1.0, 101.0, -101.0, -1000.1, 1000.1 };
	}

	@Theory
	public void straight_feed(Double x, Double y, Double z, Double a, Double b, Double c) {
		machine.STRAIGHT_FEED(x, y, z, a, b, c);

		double delta = 0;
		assertEquals(x, xMotor.value, delta);
		assertEquals(y, yMotor.value, delta);
		assertEquals(z, zMotor.value, delta);
		assertEquals(a, aMotor.value, delta);
		assertEquals(b, bMotor.value, delta);
		assertEquals(c, cMotor.value, delta);
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
			this.value = valueInMM;
		}

		@Override
		public void setSpeed(double speed) {
			this.speed = speed;
		}
	}

}

import static org.junit.Assert.assertEquals;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class MotorMachineTest
{
	TestMotor			xMotor	= new TestMotor(MotorMachine.Axis.X.name());
	TestMotor			yMotor	= new TestMotor(MotorMachine.Axis.Y.name());
	TestMotor			zMotor	= new TestMotor(MotorMachine.Axis.Z.name());

	MotorMachine	machine	= new MotorMachine();
	{
		MotorMachine.Axis.X.setMotor(xMotor);
		MotorMachine.Axis.Y.setMotor(yMotor);
		MotorMachine.Axis.Z.setMotor(zMotor);
	}

	@DataPoints
	public static Double[] status() {
		return new Double[] { 0.0, 1.0, -1.0, 101.0, -101.0, -1000.1, 1000.1 };
	}

	@Theory
	public void straight_feed(Double x, Double y, Double z) {
		assertStraightFeed(x, y, z);
	}

	private void assertStraightFeed(double x, double y, double z) {
		machine.STRAIGHT_FEED(x, y, z, 0, 0, 0);

		double delta = 0;
		assertEquals(calculateExpected(x, MotorMachine.Axis.X), xMotor.degrees, delta);
		assertEquals(calculateExpected(y, MotorMachine.Axis.Y), yMotor.degrees, delta);
		assertEquals(calculateExpected(z, MotorMachine.Axis.Z), zMotor.degrees, delta);
	}

	private double calculateExpected(double x, MotorMachine.Axis axis) {
		return (int) ((x / axis.mmPerRotation) * 360);
	}

	class TestMotor extends MotorMachine.Motor
	{
		int		degrees;
		float	speed;

		public TestMotor(String name) {
			super(name);
		}

		@Override
		public void moveTo(int degrees) {
			this.degrees += degrees;
		}

		@Override
		public void setSpeed(float speed) {
			this.speed = speed;
		}
	}

}

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

public class ProgramTest
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

	@Test
	public void test() throws Exception {
		evaluateCode();

		for (MotorMachine.Axis axis : MotorMachine.Axis.values()) {
			System.err.println(((TestMotor) axis.motor).value);
		}
	}

	private void evaluateCode() throws Exception, IOException {
		try (InputStream gcodeStream = getClass().getResourceAsStream("Program1.gcode")) {
			MotorMachine.execute(machine, gcodeStream);
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

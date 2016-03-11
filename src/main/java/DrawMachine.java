import java.util.ArrayList;
import java.util.Collection;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;

public class DrawMachine extends MotorMachine implements AutoCloseable
{
	Collection<EV3LargeRegulatedMotor>	motors	= new ArrayList<>();

	public static void printToLCD(String txt) {
		LCD.clear();
		LCD.drawString(txt, 0, 0);
	}

	public DrawMachine() {
		setMotor(Axis.X, "A");
		// setMotor(Axis.Y, MotorPort.B);
		// setMotor(Axis.Z, MotorPort.C);
	}

	@Override
	public void close() {
	}

	private void setMotor(Axis axis, String port) {
		Brick brick = BrickFinder.getDefault();
		@SuppressWarnings("resource")
		EV3LargeRegulatedMotor regulatedMotor = new EV3LargeRegulatedMotor(brick.getPort(port));
		motors.add(regulatedMotor);
		axis.setMotor(new EV3Motor(axis.name(), regulatedMotor));
	}

	@Override
	public void COMMENT(String s) {
		LCD.drawString(s, 0, 5);
		LCD.scroll();
	}

	static class EV3Motor extends MotorMachine.Motor
	{
		private final RegulatedMotor	motor;
		private final double					mmPerRotation	= 25;

		public EV3Motor(String name, RegulatedMotor motor) {
			super(name);
			this.motor = motor;
		}

		@Override
		public void moveTo(double valueInMM) {
			int degrees = (int) ((valueInMM / mmPerRotation) * 360);
			motor.rotateTo(degrees, true);
		}

		@Override
		public void setSpeed(double feedRateInMMPerMinute) {
			double speed = getRotationSpeedInDegreesPerSecond(feedRateInMMPerMinute);
			motor.setSpeed((int) speed);
		}

		private double getRotationSpeedInDegreesPerSecond(double feedRateInMMPerMinute) {
			double feedRateInMMPerSecond = feedRateInMMPerMinute / 60;
			double mmPerDegree = mmPerRotation / 360;
			return (feedRateInMMPerSecond / mmPerDegree);
		}
	}

	public static void main(String[] args) {
		LCD.drawString("Program 2", 0, 0);
		Button.waitForAnyPress();
		Brick brick = BrickFinder.getDefault();

		EV3LargeRegulatedMotor A = new EV3LargeRegulatedMotor(brick.getPort("A"));
		try {
			A.setSpeed(720);
			A.forward();
			LCD.clear();
			Delay.msDelay(2000);
			LCD.drawInt(A.getTachoCount(), 0, 0);
			A.stop();
			LCD.drawInt(A.getTachoCount(), 0, 1);
			A.backward();
			while (A.getTachoCount() > 0) {
				;
			}
			LCD.drawInt(A.getTachoCount(), 0, 2);
			A.stop();
			LCD.drawInt(A.getTachoCount(), 0, 3);
			Button.waitForAnyPress();
		}
		finally {
			A.close();
		}

		try (DrawMachine ev3Machine = new DrawMachine()) {
			ev3Machine.STRAIGHT_FEED(100.0, 100.0, 100.0, 0.0, 0.0, 0.0);
			ev3Machine.COMMENT("READY!");
		}
	}

}

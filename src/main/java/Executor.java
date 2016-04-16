import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.atan;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.floor;
import static java.lang.StrictMath.pow;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.toDegrees;

import org.antlr.v4.runtime.ParserRuleContext;

import dkwakkel.jgcode.GCodeBaseListener;
import dkwakkel.jgcode.GCodeParser.AContext;
import dkwakkel.jgcode.GCodeParser.AxisWordContext;
import dkwakkel.jgcode.GCodeParser.BContext;
import dkwakkel.jgcode.GCodeParser.CContext;
import dkwakkel.jgcode.GCodeParser.EndOfLineContext;
import dkwakkel.jgcode.GCodeParser.FContext;
import dkwakkel.jgcode.GCodeParser.G0Context;
import dkwakkel.jgcode.GCodeParser.G1Context;
import dkwakkel.jgcode.GCodeParser.G20Context;
import dkwakkel.jgcode.GCodeParser.G21Context;
import dkwakkel.jgcode.GCodeParser.G2Context;
import dkwakkel.jgcode.GCodeParser.G3Context;
import dkwakkel.jgcode.GCodeParser.IContext;
import dkwakkel.jgcode.GCodeParser.JContext;
import dkwakkel.jgcode.GCodeParser.KContext;
import dkwakkel.jgcode.GCodeParser.ProgramContext;
import dkwakkel.jgcode.GCodeParser.RContext;
import dkwakkel.jgcode.GCodeParser.XContext;
import dkwakkel.jgcode.GCodeParser.YContext;
import dkwakkel.jgcode.GCodeParser.ZContext;

class Executor extends GCodeBaseListener
{
	// https://github.com/nraynaud/webgcode/blob/gh-pages/webapp/cnc/gcode/parser.js

	private final Machine	machine;

	String								messageComment;

	double								aValue;
	double								bValue;
	double								cValue;
	double								iValue;
	double								jValue;
	double								kValue;
	double								rValue;
	double								xValue;
	double								yValue;
	double								zValue;

	double								xCurrent;
	double								yCurrent;
	double								zCurrent;

	int										group1Value;
	boolean								radiusFormat;

	Machine.CANON_PLANE		plane	= Machine.CANON_PLANE.CANON_PLANE_XY;	// Default

	public Executor(Machine machine) {
		this.machine = machine;
	}

	@Override
	public void exitA(AContext ctx) {
		aValue = getExpressionValue(ctx);
	}

	@Override
	public void exitB(BContext ctx) {
		bValue = getExpressionValue(ctx);
	}

	@Override
	public void exitC(CContext ctx) {
		cValue = getExpressionValue(ctx);
	}

	@Override
	public void exitI(IContext ctx) {
		iValue = getExpressionValue(ctx);
	}

	@Override
	public void exitJ(JContext ctx) {
		jValue = getExpressionValue(ctx);
	}

	@Override
	public void exitK(KContext ctx) {
		kValue = getExpressionValue(ctx);
	}

	@Override
	public void exitR(RContext ctx) {
		rValue = getExpressionValue(ctx);
	}

	@Override
	public void exitX(XContext ctx) {
		xValue = getExpressionValue(ctx);
	}

	@Override
	public void exitY(YContext ctx) {
		yValue = getExpressionValue(ctx);
	}

	@Override
	public void exitZ(ZContext ctx) {
		zValue = getExpressionValue(ctx);
	}

	@Override
	public void exitF(FContext ctx) {
		machine.SET_FEED_RATE(getExpressionValue(ctx));
	}

	private double getExpressionValue(ParserRuleContext ctx) {
		return Double.parseDouble(ctx.getChild(1).getText());
	}

	@Override
	public void exitAxisWord(AxisWordContext ctx) {
		radiusFormat = ctx.r() != null;
	}

	@Override
	public void exitG0(G0Context ctx) {
		group1Value = 0;
	}

	@Override
	public void exitG1(G1Context ctx) {
		group1Value = 1;
	}

	@Override
	public void exitG2(G2Context ctx) {
		group1Value = 2;
	}

	@Override
	public void exitG3(G3Context ctx) {
		group1Value = 3;
	}

	@Override
	public void exitG20(G20Context ctx) {
		machine.USE_LENGTH_UNITS(MotorMachine.CANON_UNITS.CANON_UNITS_INCHES);
	}

	@Override
	public void exitG21(G21Context ctx) {
		machine.USE_LENGTH_UNITS(MotorMachine.CANON_UNITS.CANON_UNITS_MM);
	}

	@Override
	public void exitEndOfLine(EndOfLineContext ctx) {
		if (messageComment != null) {
			machine.COMMENT(messageComment);
			messageComment = null;
		}
		switch (group1Value) {
			case 0: {
				machine.STRAIGHT_TRAVERSE(xValue, yValue, zValue, aValue, bValue, cValue);
				break;
			}
			case 1: {
				machine.STRAIGHT_FEED(xValue, yValue, zValue, aValue, bValue, cValue);
				break;
			}
			case 2:
			case 3: {
				double firstEnd, secondEnd, axisEnd, firstCurrent, secondCurrent, firstDelta, secondDelta;
				switch (plane) {
					case CANON_PLANE_XY:
						firstEnd = xValue;
						secondEnd = yValue;
						axisEnd = zValue;
						firstCurrent = xCurrent;
						firstDelta = iValue;
						secondCurrent = yCurrent;
						secondDelta = jValue;
						break;
					case CANON_PLANE_XZ:
						firstEnd = xValue;
						secondEnd = zValue;
						axisEnd = yValue;
						firstCurrent = xCurrent;
						firstDelta = iValue;
						secondCurrent = zCurrent;
						secondDelta = kValue;
						break;
					case CANON_PLANE_YZ:
						firstEnd = yValue;
						secondEnd = zValue;
						axisEnd = xValue;
						firstCurrent = yCurrent;
						firstDelta = jValue;
						secondCurrent = zCurrent;
						secondDelta = kValue;
						break;
					default:
						throw new IllegalStateException("plane=" + plane);
				}

				int rotation;
				double firstCenter, secondCenter;
				double adjacent = sqrt(pow(firstEnd - firstCurrent, 2) + pow(secondEnd - secondCurrent, 2)) / 2;

				if (radiusFormat) {
					double r = abs(rValue);
					double alpha = firstCurrent == firstEnd ? 0 : atan((secondEnd - secondCurrent) / (firstEnd - firstCurrent));
					double beta = acos(adjacent / r);
					double firstMid = firstCurrent + (firstEnd - firstCurrent) / 2;
					double secondMid = secondCurrent + (secondEnd - secondCurrent) / 2;
					firstCenter = firstCurrent + r * cos(alpha + beta) * (rValue >= 0 ^ firstEnd >= firstCurrent ? -1 : 1);
					secondCenter = secondCurrent + r * sin(alpha + beta) * (rValue >= 0 ^ secondEnd >= secondCurrent ? 1 : -1);
					rotation = (int) floor(toDegrees((Math.PI / 2.0) - beta));
				} else {
					firstCenter = firstCurrent + firstDelta;
					secondCenter = secondCurrent + secondDelta;

					double hypotenuse = sqrt(pow(firstEnd - firstCenter, 2) + pow(secondEnd - secondCenter, 2));
					rotation = (int) (2 * toDegrees(acos(adjacent / hypotenuse)));
					if (rotation == 0 && !(firstCurrent == firstEnd && secondCurrent == secondEnd)) { // 0 can mean 180 degrees sin if start != end
						rotation = 180;
					}
					// check if rotation is more then 180 degrees
					double a = (secondEnd - secondCurrent) / (firstEnd - firstCurrent);
					double b = secondCurrent - (firstCurrent * a);
					if (firstEnd > firstCenter ^ (firstEnd == firstCurrent || secondCurrent < (a * firstCurrent + b))) {
						rotation += 180;
					}
				}
				if (group1Value == 3) { // counterclockwise
					rotation = rotation - 360;
					if (rotation == 0) { // in case of full circle
						rotation = -360;
					}
				}

				machine.ARC_FEED(firstEnd, secondEnd, firstCenter, secondCenter, rotation, axisEnd, aValue, bValue, cValue);
				break;
			}
			default:
				throw new IllegalStateException("group1: " + group1Value);
		}
		xCurrent = xValue;
		yCurrent = yValue;
		zCurrent = zValue;
		iValue = jValue = kValue = 0;
	}

	@Override
	public void exitProgram(ProgramContext ctx) {
		super.exitProgram(ctx);
		System.err.println("EXIT");
	}
}
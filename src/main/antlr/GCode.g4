// Implementation of rs274ngc grammar. 
// See http://www.nist.gov/customcf/get_pdf.cfm?pub_id=823374.
grammar GCode; 

options {
	language=Java;
}

@header {
package dkwakkel.jgcode;
import static java.lang.StrictMath.*;
}

@parser::members {

public enum CANON_DIRECTION { CANON_STOPPED, CANON_CLOCKWISE, CANON_COUNTERCLOCKWISE }
public enum CANON_FEED_REFERENCE { CANON_WORKPIECE, CANON_XYZ }
public enum CANON_MOTION_MODE { CANON_EXACT_STOP, CANON_EXACT_PATH, CANON_CONTINUOUS }
public enum CANON_PLANE { CANON_PLANE_XY, CANON_PLANE_YZ, CANON_PLANE_XZ }
public enum CANON_UNITS { CANON_UNITS_INCHES, CANON_UNITS_MM /*, CANON_UNITS_CM */ }
public enum CANON_COMP_SIDE { CANON_COMP_RIGHT, CANON_COMP_LEFT, CANON_COMP_OFF }

private static double INCH_IN_MM = 25.4;
public static double convertUnit(CANON_UNITS from, CANON_UNITS to, double input)   {
	return from == to ? input : (to == CANON_UNITS.CANON_UNITS_MM ? input / INCH_IN_MM : input * INCH_IN_MM);
}

// Table 9. Canonical Machining Functions Called By Interpreter
public interface Machine {

	// Representation
	void SET_ORIGIN_OFFSETS(double x, double y, double z, double a, double b, double c);
	void USE_LENGTH_UNITS(CANON_UNITS units);

	// Free Space Motion
	void STRAIGHT_TRAVERSE(double x, double y, double z, double a, double b, double c);
	
	// Machining Attributes
	void SELECT_PLANE(CANON_PLANE plane);
	void SET_FEED_RATE(double rate);
	void SET_FEED_REFERENCE(CANON_FEED_REFERENCE reference);
	void SET_MOTION_CONTROL_MODE(CANON_MOTION_MODE mode);
	void START_SPEED_FEED_SYNCH();
	void STOP_SPEED_FEED_SYNCH();

	// Machining Functions
	void ARC_FEED(double first_end, double second_end, double first_axis, double second_axis, int rotation, double axis_end_point, double a, double b, double c);
	void DWELL(double seconds);
	void STRAIGHT_FEED(double x, double y, double z, double a, double b, double c);

	// Probe Functions
	void STRAIGHT_PROBE(double x, double y, double z, double a, double b, double c);

	// Spindle Functions
	void ORIENT_SPINDLE(double orientation, CANON_DIRECTION direction);
	void SET_SPINDLE_SPEED(double r);
	void START_SPINDLE_CLOCKWISE();
	void START_SPINDLE_COUNTERCLOCKWISE();
	void STOP_SPINDLE_TURNING();

	// Tool Functions
	void CHANGE_TOOL(int slot);
	void SELECT_TOOL(int i);
	void USE_TOOL_LENGTH_OFFSET(double offset);

	// Miscellaneous Functions
	void COMMENT(String s);
	void DISABLE_FEED_OVERRIDE();
	void DISABLE_SPEED_OVERRIDE();
	void ENABLE_FEED_OVERRIDE();
	void ENABLE_SPEED_OVERRIDE();
	void FLOOD_OFF();
	void FLOOD_ON();
	void INIT_CANON();
	void MESSAGE(String s);
	void MIST_OFF();
	void MIST_ON();
	void PALLET_SHUTTLE();

	// Program Functions
	void OPTIONAL_PROGRAM_STOP();
	void PROGRAM_END();
	void PROGRAM_STOP();
	
	void SET_CUTTER_RADIUS_COMPENSATION (double radius);
	void START_CUTTER_RADIUS_COMPENSATION (CANON_COMP_SIDE side);
	void STOP_CUTTER_RADIUS_COMPENSATION ();
};

	// See Table 2. Default Parameter File
	// TODO: Persistence
	double[] parameters = new double[5400 + 1];
	{ parameters[5220] = 1.0; }
	
	Machine machine;

	double aValue;
	double bValue;
	double cValue;
	double iValue;
	double jValue;
	double kValue;
	double rValue;
	double xValue;
	double yValue;
	double zValue;

	double xCurrent;
	double yCurrent;
	double zCurrent;

	int group1Value;
	boolean radiusFormat;
	CANON_PLANE plane = CANON_PLANE.CANON_PLANE_XY;
	
	public static void execute(Machine machine, java.io.InputStream in) throws Exception {
		ANTLRInputStream input = new ANTLRInputStream(in == null ? System.in : in);

		GCodeLexer lexer = new GCodeLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		GCodeParser parser = new GCodeParser(tokens);
		parser.machine = machine;

		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk( new GCodeBaseListener(), parser.program());
	}
}

program		: PERCENT END_OF_LINE ( line )* PERCENT END_OF_LINE | ( line )* ;

line		: ( BLOCK_DELETE )? ( LINE_NUMBER )? ( segment )* endOfLine ;

segment		: word | parameterSetting | comment { machine.COMMENT($comment.text); } | oword_label oword_statement ;

comment		: BRACKET_COMMENT | LINE_COMMENT; // TODO: return message without brackets

parameterSetting : parameter EQUALS e ;

parameter	: HASH designator ;

designator	: NUMBER |NAME | parameter | LBRACKET e RBRACKET ;

oword_label	: 'o' NUMBER | 'o' NAME | 'o' parameter ;

oword_statement
	:  SUB
	|  RETURN 	( bracketExpression )?
	|  ENDSUB 	( bracketExpression )?
	|  CALL 	( bracketExpression )*
	|  DO
	|  WHILE 	bracketExpression
	|  IF 		bracketExpression
	|  ELSEIF 	bracketExpression
	|  ELSE
	|  ENDIF
	|  BREAK
	|  CONTINUE
	|  ENDWHILE
	|  REPEAT	bracketExpression
	|  ENDREPEAT
	;

parameterList	: bracketExpression*;

optReturnValue	: bracketExpression | ;

word	: axisWord | dimensionWord | gWord | WordLetter e;
gWord	: group1 | group6;

axisWord : a | b | c | i | j | k | r | x | y | z;
a : 'a' e { aValue = Double.valueOf($e.text); }; // A-axis of machine
b : 'b' e { bValue = Double.valueOf($e.text); }; // B-axis of machine
c : 'c' e { cValue = Double.valueOf($e.text); }; // C-axis of machine
i : 'i' e { iValue = Double.valueOf($e.text); }; // X-axis offset for arcs | 'x' offset in G87 canned cycle
j : 'j' e { jValue = Double.valueOf($e.text); }; // Y-axis offset for arcs | 'y' offset in G87 canned cycle
k : 'k' e { kValue = Double.valueOf($e.text); }; // Z-axis offset for arcs | 'z' offset in G87 canned cycle
r : 'r' e { rValue = Double.valueOf($e.text); }; // arc radius | canned cycle plane
x : 'x' e { xValue = Double.valueOf($e.text); }; // X-axis of machine
y : 'y' e { yValue = Double.valueOf($e.text); }; // Y-axis of machine
z : 'z' e { zValue = Double.valueOf($e.text); }; // Z-axis of machine

dimensionWord : f;
f : 'f' e { machine.SET_FEED_RATE(Double.valueOf($e.text)); }; // feedrate

WordLetter:
	'd' | // tool radius compensation NUMBER
//	'g' | // general function (see Table 5)
	'h' | // tool length offset index
	'l' | // NUMBER of repetitions in canned cycles | key used with G10
	'm' | // miscellaneous function (see Table 7)
	'p' | // dwell time in canned cycles | dwell time with G4 | key used with G10
	'q' | // feed increment in G83 canned cycle
	's' | // spindle speed
	't' | // tool selection
	ATSIGN |
	CARET;
	
// The modal groups for G codes are:
group1 : g0 | g1 | g2 | g3; // | G38.2 | G80 | G81 | G82 | G83 | G84 | G85 | G86 | G87 | G88 | G89; // motion
//group 2 = {G17, G18, G19} plane selection
//group 3 = {G90, G91} distance mode
//group 5 = {G93, G94} feed rate mode
group6 : g20 | g21; //  units
//group 7 = {G40, G41, G42} cutter radius compensation
//group 8 = {G43, G49} tool length offset
//group 10 = {G98, G99} return mode in canned cycles
//group 12 = {G54, G55, G56, G57, G58, G59, G59.1, G59.2, G59.3} coordinate system selection
//group 13 = {G61, G61.1, G64} path control mode
//The modal groups for M codes are:
//group 4 = {M0, M1, M2, M30, M60} stopping
//group 6 = {M6} tool change
//group 7 = {M3, M4, M5} spindle turning
//group 8 = {M7, M8, M9} coolant (special case: M7 and M8 ma'y'be active at the same time)
//group 9 = {M48, M49} enable/disable feed and speed override switches
//
//// In addition to the above modal groups, there is a group for non-modal G codes:
//group0 = G4 | G10 | G28 | G30 | G53 | G92 | G92.1 | G92.2 | G92.3;

// Table 5: G codes
g0 : 'g0' x? y? z? a? b? c? { group1Value = 0; } ; // rapid positioning
g1 : 'g1' x? y? z? a? b? c? { group1Value = 1; };  // linear interpolation
g2 : 'g2' x? y? z? a? b? c? (r | i? j? k?) { radiusFormat = $r.ctx!=null; group1Value = 2; }; // circular/helical interpolation (clockwise)
g3 : 'g3' x? y? z? a? b? c? (r | i? j? k?) { radiusFormat = $r.ctx!=null; group1Value = 3; }; // circular/helical interpolation (counterclockwise)

//G4 dwell
//G10 coordinate system origin setting
//G17 XY-plane selection
//G18 XZ-plane selection
//G19 YZ-plane selection
g20 : 'g20' { machine.USE_LENGTH_UNITS(CANON_UNITS.CANON_UNITS_INCHES); };
g21 : 'g21' { machine.USE_LENGTH_UNITS(CANON_UNITS.CANON_UNITS_MM); };
//G28 return to home
//G30 return to secondar'y'home
//G38.2 straight probe
//G40 cancel cutter radius compensation
//G41 start cutter radius compensation left
//G42 start cutter radius compensation right
//G43 tool length offset (plus)
//G49 cancel tool length offset
//G53 motion in machine coordinate system
//G54 use preset work coordinate system 1
//G55 use preset work coordinate system 2
//G56 use preset work coordinate system 3
//G57 use preset work coordinate system 4
//G58 use preset work coordinate system 5
//G59 use preset work coordinate system 6
//G59.1 use preset work coordinate system 7
//G59.2 use preset work coordinate system 8
//G59.3 use preset work coordinate system 9
//G61 set path control mode: exact path
//G61.1 set path control mode: exact stop
//G64 set path control mode: continuous
//G80 cancel motion mode (including an'y'canned cycle)
//G81 canned cycle: drilling
//G82 canned cycle: drilling with dwell
//G83 canned cycle: peck drilling
//G84 canned cycle: right hand tapping
//G85 canned cycle: boring, no dwell, feed out
//G86 canned cycle: boring, spindle stop, rapid out
//G87 canned cycle: back boring
//G88 canned cycle: boring, spindle stop, manual out
//G89 canned cycle: boring, dwell, feed out
//G90 absolute distance mode
//G91 incremental distance mode
//G92 offset coordinate systems and set parameters
//G92.1 cancel offset coordinate systems and set parameters to zero
//G92.2 cancel offset coordinate systems but do not reset parameters
//G92.3 appl'y'parameters to offset coordinate systems
//G93 inverse time feed rate mode
//G94 units per minute feed rate mode
//G98 initial level return in canned cycles
//G99 R-point level return in canned cycles

e		: comparisonExpression (
					( OR comparisonExpression ) |
					( XOR comparisonExpression ) |
					( AND comparisonExpression )
				)*
				;

comparisonExpression
				: plusMinExpression (
					( EQ plusMinExpression ) |
					( NE plusMinExpression ) |
					( GT plusMinExpression ) |
					( GE plusMinExpression ) |
					( LT plusMinExpression ) |
					( LE plusMinExpression )
				)* ;

plusMinExpression
				: aggregateExpression (
					( PLUS aggregateExpression ) |
					( MINUS aggregateExpression )
				)* ;

aggregateExpression
				: powerExpression (
					( TIMES powerExpression ) |
					( SLASH powerExpression ) |
					( MOD powerExpression )
				)* ;

// If operations are strung together (for example in the expression [2.0 / 3 * 1.5 - 5.5 / 11.0]),
// operations in the first group are to be performed before operations in the second group
// and operations in the second group before operations in the third group.
// If an expression contains more than one operation from the same group
// (such as the first / and * in the example), the operation on the left is performed first.

powerExpression	returns [double v]:
	unaryExpression ( POWER powerExpression )?
	{ if($powerExpression.ctx==null) { $v = $unaryExpression.v; } else { $v = pow($unaryExpression.v, $powerExpression.v); }};

unaryExpression returns [double v]:
// TODO: fix and are these all mathematical functions?
	ACOS bracketExpression		{ $v = cos($bracketExpression.v);}
	| ASIN bracketExpression	{ $v = asin($bracketExpression.v);}
	| COS bracketExpression		{ $v = cos($bracketExpression.v);}
	| SIN bracketExpression		{ $v = sin($bracketExpression.v);}
	| TAN bracketExpression		{ $v = tan($bracketExpression.v);}
	| LN bracketExpression		
	| EXP bracketExpression		{ $v = exp($bracketExpression.v);}
	| SQRT bracketExpression	{ $v = pow($bracketExpression.v,0.5);}
	| FIX bracketExpression		{ $v = floor($bracketExpression.v);}
	| FUP bracketExpression		{ $v = ceil($bracketExpression.v);}
	| ROUND bracketExpression	{ $v = cos($bracketExpression.v);}
	| ABS bracketExpression		{ $v = abs($bracketExpression.v);}
	| EXISTS bracketExpression	
	| ATAN bracketExpression SLASH bracketExpression
	| parameter
	| primitiveExpression
;

bracketExpression returns [double v]:	LBRACKET e RBRACKET;

primitiveExpression returns [double v]:
	bracketExpression { $v = $bracketExpression.v; }
	| parameter
	| NUMBER { $v = Double.valueOf($NUMBER.text); }
	| '0' // TODO: why NUMBER does not work for '0'?
; 

endOfLine : END_OF_LINE {
	switch(group1Value) {
		case 0: { machine.STRAIGHT_TRAVERSE(xValue, yValue, zValue, aValue, bValue, cValue); break; }
		case 1: { machine.STRAIGHT_FEED(xValue, yValue, zValue, aValue, bValue, cValue); break; }
		case 2: 
		case 3: {
			double firstEnd, secondEnd, axisEnd, firstCurrent, secondCurrent, firstDelta, secondDelta;
			switch(plane) {
				case CANON_PLANE_XY:
					firstEnd = xValue; secondEnd = yValue; axisEnd = zValue;
					firstCurrent = xCurrent; firstDelta = iValue;
					secondCurrent = yCurrent; secondDelta = jValue;
					break;
				case CANON_PLANE_XZ:
					firstEnd = xValue; secondEnd = zValue; axisEnd = yValue;
					firstCurrent = xCurrent; firstDelta = iValue;
					secondCurrent = zCurrent; secondDelta = kValue;
					break;
				case CANON_PLANE_YZ:
					firstEnd = yValue; secondEnd = zValue; axisEnd = xValue;
					firstCurrent = yCurrent; firstDelta = jValue;
					secondCurrent = zCurrent; secondDelta = kValue;
					break;
				default: throw new IllegalStateException("plane=" + plane);
			}

			int rotation;
			double firstCenter, secondCenter;			
			double adjacent = sqrt(pow(firstEnd-firstCurrent,2) + pow(secondEnd-secondCurrent, 2)) / 2;
			
			if(radiusFormat) {
				double r = abs(rValue);
				double alpha = firstCurrent == firstEnd ? 0 : atan((secondEnd-secondCurrent)/(firstEnd-firstCurrent));
				double beta = acos(adjacent / r);
				double firstMid = firstCurrent + (firstEnd - firstCurrent) / 2;
				double secondMid = secondCurrent + (secondEnd - secondCurrent) / 2;
				firstCenter = firstCurrent + r * cos(alpha + beta) * (rValue >= 0 ^ firstEnd >= firstCurrent ? -1 : 1);
				secondCenter = secondCurrent + r * sin(alpha + beta) * (rValue >= 0 ^ secondEnd >= secondCurrent ? 1 : -1);
				rotation = (int)floor(toDegrees((Math.PI / 2.0) - beta));
			} else {
				firstCenter = firstCurrent + firstDelta;
				secondCenter = secondCurrent + secondDelta;

				double hypotenuse = sqrt(pow(firstEnd-firstCenter,2) + pow(secondEnd-secondCenter, 2));
				rotation = (int)(2 * toDegrees(acos(adjacent / hypotenuse)));
				if(rotation == 0 && !(firstCurrent == firstEnd && secondCurrent == secondEnd)) { // 0 can mean 180 degrees sin if start != end
					rotation = 180;
				} 
				double a = (secondEnd - secondCurrent) / (firstEnd - firstCurrent);
				double b = secondCurrent - (firstCurrent * a);
				if(firstEnd > firstCenter ^ (firstEnd == firstCurrent || secondCurrent < (a * firstCurrent + b))) {
					rotation += 180;
				}
			}
			if(group1Value == 3) { // counterclockwise
				rotation = rotation - 360;
			}

			machine.ARC_FEED(firstEnd, secondEnd, firstCenter, secondCenter, rotation, axisEnd, aValue, bValue, cValue);
			break;
		}
	}
	xCurrent = xValue;
	yCurrent = yValue;
	zCurrent = zValue;
	iValue = jValue = kValue = 0;
};

LINE_NUMBER	: 'n' Digit Digit? Digit? Digit? Digit?;

WHITESPACE	: ( ' ' | '\t' )+ -> skip ;

END_OF_LINE	: ( '\r' | '\n' | '\r' '\n' );

NUMBER 		: ('+'|'-')? ( Digit+ | Digit* ('.' Digit+) );

fragment Digit		: '0'..'9' ;

BRACKET_COMMENT		: '(' .*? ')' ;

LINE_COMMENT : ';' .*? END_OF_LINE;

NAME		: '<' ~('>')+ '>' ;

fragment Name_Char :
    | Letter
    | Digit
    | '-'
    | '_'
    | ' '
	;

fragment Letter	:  ( 'a'..'z' | 'A'..'Z') ;

SUB		: 'sub' ;
ENDSUB	: 'endsub' ;
CALL	: 'call' ('sub')? ;
DO		: 'do' ;
WHILE	: 'while' ;
ELSEIF	: 'elseif' ;
ELSE	: 'else' ;
ENDIF	: 'endif' ;
IF		: 'if' ;
BREAK	: 'break' ;
CONTINUE: 'continue' ;
ENDWHILE: 'endwhile' ;
RETURN	: 'return' ;
REPEAT	: 'repeat' ;
ENDREPEAT: 'endrepeat' ;


ABS	: 'abs' ;
ACOS: 'acos' ;
ASIN: 'asin' ;
ATAN: 'atan' ;
SIN	: 'sin' ;
COS	: 'cos' ;
TAN	: 'tan' ;
AND : 'and' ;
OR	: 'or' ;
XOR	: 'xor' ;
EXP	: 'exp' ;
FIX	: 'fix';
FUP	: 'fup' ;
MOD	: 'mod' ;
ROUND: 'round' ;
SQRT: 'sqrt' ;
LN	: 'ln' ;
EXISTS	: 'exists' ;

EQ	: 'eq' ;
NE	: 'ne' ;
GT	: 'gt' ;
GE	: 'ge' ;
LT	: 'lt' ;
LE 	: 'le' ;

POWER			: '**' ;
PLUS			: '+' ;
MINUS			: '-' ;
TIMES			: '*' ;
SLASH			: '/' ;
HASH			: '#' ;
EQUALS			: '=' ;
LBRACKET		: '[' ;
RBRACKET		: ']' ;
PERCENT			: '%' ;
LESS            : '<' ;
GREATER         : '>' ;
DOT             : '.' ;
ATSIGN          : '@' ;
CARET           : '^' ;

BLOCK_DELETE	: '/';




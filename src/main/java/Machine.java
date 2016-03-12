// Table 9. Canonical Machining Functions Called By Interpreter
public interface Machine {

	enum CANON_DIRECTION { CANON_STOPPED, CANON_CLOCKWISE, CANON_COUNTERCLOCKWISE }
	enum CANON_FEED_REFERENCE { CANON_WORKPIECE, CANON_XYZ }
	enum CANON_MOTION_MODE { CANON_EXACT_STOP, CANON_EXACT_PATH, CANON_CONTINUOUS }
	enum CANON_PLANE { CANON_PLANE_XY, CANON_PLANE_YZ, CANON_PLANE_XZ }
  enum CANON_UNITS { CANON_UNITS_INCHES, CANON_UNITS_MM /*, CANON_UNITS_CM */ }
	enum CANON_COMP_SIDE { CANON_COMP_RIGHT, CANON_COMP_LEFT, CANON_COMP_OFF }

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

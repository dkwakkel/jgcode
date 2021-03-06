%
(O2201);
(PROGRAM 1);
(TOOL #1 0.5 DIA FLAT END MILL 3 FLUTE);
N001 G[20 OR abs[20]];
N002 G00 G17 G40 G49 G80 G90;
N003 G00 G91 G28 Z0.0;
N004 G00 G91 G28 X0.0 Y0.0;
;
N005 T01 M06;
N006 G00 G90 G54 (P1) X0.5 Y-2.25 S2292 M03;
N007 G00 G90 G43 H01 Z2.0 M08;
N008 G00 G90 Z0.1;
(START OF MACHINE CODE);
(MILLING THE PROFILE);
N009 G01 G90 Z-0.25 F10.31 (1/2 IPM);
N010 G01 G90 (P2) X0.5 Y-1.75 F20.62 (FULL IPM);
N011 G03 G90 (P3) X0.0 Y-1.25 I-0.5 J0.0;
N012 G01 G90 (P4) X-1.0 Y-1.25;
N013 G02 G90 (P5) X-1.25 Y-1.0 I0.0 J0.25;
N014 G01 G90 (P6) X-1.25 Y1.0;
N015 G02 G90 (P7) X-1.0 Y1.25 I0.25 J0.0;
N016 G01 G90 (P8) X1.0 Y1.25;
N017 G02 G90 (P9) X1.25 Y1.0 I0.0 J-0.25;
N018 G01 G90 (P10) X1.25 Y-1.0;
N019 G02 G90 (P11) X1.0 Y-1.25 I-0.25 J0.0;
N020 G01 G90 (P3) X0.0 Y-1.25;
N021 G03 G90 (P12) X-0.5 Y-1.75 I0.0 J-0.5;
N022 G01 G90 (P13) X-0.5 Y-2.25;
(END OF MACHINE CODE)
N023 G01 G90 Z0.25 M05 F10.31 (1/2 IPM);
N024 G00 G90 Z2.0 M09;
N025 G00 G91 G28 Z0.0;
N026 G00 G91 G28 X0.0 Y0.0;
N027 G49;
N028 M30;
%
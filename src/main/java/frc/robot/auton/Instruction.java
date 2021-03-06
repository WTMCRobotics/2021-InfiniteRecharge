package frc.robot.auton;

import frc.robot.Robot;

public abstract class Instruction {

	/**
	 * does the instruction
	 * 
	 * @param robot the current instance of the robot
	 * 
	 * @return whether the instruction has been completed
	 */
	public abstract boolean doit(Robot robot);

}

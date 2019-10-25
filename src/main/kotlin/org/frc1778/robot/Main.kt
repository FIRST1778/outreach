package org.frc1778.robot

import edu.wpi.first.wpilibj.RobotBase
import edu.wpi.first.wpilibj.TimedRobot

object Robot : TimedRobot()

fun main() {
    RobotBase.startRobot { Robot }
}

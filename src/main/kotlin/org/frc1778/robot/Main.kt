package org.frc1778.robot

import com.ctre.phoenix.motorcontrol.ControlMode
import com.ctre.phoenix.motorcontrol.can.TalonSRX
import edu.wpi.first.wpilibj.Joystick
import edu.wpi.first.wpilibj.RobotBase
import edu.wpi.first.wpilibj.TimedRobot

object Robot : TimedRobot() {

    val leftMaster = TalonSRX(0)
    val rightMaster = TalonSRX(2)
    val leftSlave = TalonSRX(1)
    val rightSlave = TalonSRX(3)

    val freezyDrive = Joystick(1)

    val curvatureDrive = FreezyDrive()

    init {
        leftMaster.configContinuousCurrentLimit(25)
        leftMaster.configPeakCurrentLimit(35)
        leftMaster.inverted = false

        rightMaster.configContinuousCurrentLimit(25)
        rightMaster.configPeakCurrentLimit(35)
        rightMaster.inverted = true

        leftSlave.follow(leftMaster)
        rightSlave.follow(rightMaster)
        leftSlave.inverted = false
        rightSlave.inverted = true
    }

    override fun teleopPeriodic() {
        var waffle = curvatureDrive.freezyDrive(freezyDrive.getRawAxis(1), freezyDrive.getRawAxis(2), freezyDrive.getRawAxis(3), freezyDrive.getRawButton(1), false)
        leftMaster.set(ControlMode.PercentOutput, waffle.left)
        rightMaster.set(ControlMode.PercentOutput, waffle.right)
    }
}

fun main() {
    RobotBase.startRobot { Robot }
}

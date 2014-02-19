/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.oastem.frc.assist;


import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.SimpleRobot;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.camera.AxisCamera;
import edu.wpi.first.wpilibj.image.*;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.oastem.frc.Debug;
import org.oastem.frc.control.DriveSystem;
import org.oastem.frc.control.VexSpike;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the SimpleRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class RobotMain extends SimpleRobot {
    public static final int WINCH_PORT = 1;
    public static final int RIGHT_DRIVE_FRONT = 4;
    public static final int RIGHT_DRIVE_REAR = 5;
    public static final int LEFT_DRIVE_REAR = 7;
    public static final int LEFT_DRIVE_FRONT = 6;
    public static final int INTAKE_SPIKE = 7;
    public static final int WINCH_SPIKE = 8;
    public static final int TRIGGER_PORT = 2;
    
    // Buttons:
    public static final int INTAKE_BUTTON = 6;
    public static final int TRIGGER_BUTTON = 1;
    public static final int OUTTAKE_BUTTON = 7;
    public static final int TRIGGER_BUTTON_UP = 3;
    public static final int TRIGGER_BUTTON_DOWN = 2;
    public static final int WINCH_BUTTON_UP = 11;
    public static final int WINCH_BUTTON_DOWN = 10;
    public static final int WINCH_BUTTON_SPIKE = 8;
    public static final int EMERGENCY_STOP_BUTTON = 4;
    public static final int EVERYTHING_BUTTON = 5;
    
    private static final double TRIGGER_SPEED_UP = -0.75;
    private static final double TRIGGER_SPEED_DOWN = 0.25;
    
    private DriveSystem drive = DriveSystem.getInstance();
    
    private VexSpike intake;
    private VexSpike winch;
    
    /**private Victor rightDrive1 = new Victor(4);
    private Victor rightDrive2 = new Victor(5);
    private Victor leftDrive1 = new Victor(6);
    private Victor leftDrive2 = new Victor(7);
    //*/
    private DigitalInput fireLim = new DigitalInput(1);
    private DigitalInput winchLin = new DigitalInput(2);
    private long lastUpdate;
    private String[] debug = new String[6];
    private Joystick left = new Joystick(1);
    private Joystick right = new Joystick(2);
    //private Victor trigger;
    private double joyScale = 1.0;
    private double joyScale2 = 1.0;
    private long ticks = 0;
    final int XMAXSIZE = 24;
    final int XMINSIZE = 24;
    final int YMAXSIZE = 24;
    final int YMINSIZE = 48;
    final double xMax[] = {1, 1, 1, 1, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, 1, 1, 1, 1};
    final double xMin[] = {.4, .6, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, .1, 0.6, 0};
    final double yMax[] = {1, 1, 1, 1, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, .5, 1, 1, 1, 1};
    final double yMin[] = {.4, .6, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05,
								.05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05, .05,
								.05, .05, .6, 0};
    
    final int RECTANGULARITY_LIMIT = 60;
    final int ASPECT_RATIO_LIMIT = 75;
    final int X_EDGE_LIMIT = 40;
    final int Y_EDGE_LIMIT = 60;
    
    final int X_IMAGE_RES = 320;          //X Image resolution in pixels, should be 160, 320 or 640
    final double VIEW_ANGLE = 43.5;       //Axis 206 camera
//    final double VIEW_ANGLE = 48;       //Axis M1011 camera
    
    AxisCamera camera;          // the axis camera object (connected to the switch)
    CriteriaCollection cc;      // the criteria for doing the particle filter operation
    
    // we are storing the centers of masses
    private double horzCenterMassX, horzCenterMassY, vertCenterMassX, vertCenterMassY;
    
    public class Scores {
        double rectangularity;
        double aspectRatioInner;
        double aspectRatioOuter;
        double xEdge;
        double yEdge;
    }
    
    protected void robotInit(){
        Debug.clear();
        Debug.log(1, 1, "Robot Initalized");
        
        lastUpdate = System.currentTimeMillis();
        
        drive.initializeDrive(LEFT_DRIVE_FRONT, LEFT_DRIVE_REAR, RIGHT_DRIVE_FRONT, RIGHT_DRIVE_REAR);
        drive.setSafety(false);
        intake = new VexSpike(INTAKE_SPIKE);
        winch = new VexSpike(WINCH_SPIKE);
        drive.addVictor(TRIGGER_PORT);
        drive.addVictor(WINCH_PORT);
        //trigger = new Victor(TRIGGER_VICTOR);
        
        cc = new CriteriaCollection();      // create the criteria for the particle filter
        cc.addCriteria(NIVision.MeasurementType.IMAQ_MT_AREA, 400, 65535, false);
        //camera = AxisCamera.getInstance("10.40.79.11");
        horzCenterMassX = 0.0;
        horzCenterMassY = 0.0;
        vertCenterMassX = 0.0;
        vertCenterMassY = 0.0;
        
        System.out.println("End of Robot Init");
    }

    
    /**
     * This function is called once each time the robot enters autonomous mode.
     */
    public void autonomous() {
        debug[0] = "Autonomous mode";
                while (isAutonomous() && isEnabled()) {
            try {
                /**
                 * Do the image capture with the camera and apply the algorithm described above. This
                 * sample will either get images from the camera or from an image file stored in the top
                 * level directory in the flash memory on the cRIO. The file name in this case is "testImage.jpg"
                 * 
                 */
                // 43:32
                //ColorImage image = camera.getImage();     // comment if using stored images
                ColorImage image;                           // next 2 lines read image from flash on cRIO
                //image = camera.getImage();
                image = new RGBImage("/testImage.jpg");		// get the sample image from the cRIO flash
                BinaryImage thresholdImage = image.thresholdRGB(0, 50, 150, 255, 100, 200);
                //BinaryImage thresholdImage = image.thresholdHSV(60, 100, 90, 255, 20, 255);   // keep only red objects
                //thresholdImage.write("/threshold.bmp");
                BinaryImage convexHullImage = thresholdImage.convexHull(false);          // fill in occluded rectangles
                //convexHullImage.write("/convexHull.bmp");
                BinaryImage filteredImage = convexHullImage.particleFilter(cc);           // filter out small particles
                //filteredImage.write("/filteredImage.bmp");
                //SmartDashboard.
                
                //iterate through each particle and score to see if it is a target
                Scores scores[] = new Scores[filteredImage.getNumberParticles()];
                for (int i = 0; i < scores.length; i++) {
                    ParticleAnalysisReport report = filteredImage.getParticleAnalysisReport(i);
                    scores[i] = new Scores();
                    
                    scores[i].rectangularity = scoreRectangularity(report);
                    scores[i].aspectRatioOuter = scoreAspectRatio(filteredImage,report, i, true);
                    scores[i].aspectRatioInner = scoreAspectRatio(filteredImage, report, i, false);
                    scores[i].xEdge = scoreXEdge(thresholdImage, report);
                    scores[i].yEdge = scoreYEdge(thresholdImage, report);
                    
                   
                    if (scores[i].aspectRatioOuter > 1.0) {
                        // Width > height, it's the horizontal goal
                        horzCenterMassX = report.center_mass_x_normalized;
                        horzCenterMassY = report.center_mass_y_normalized;
                        System.out.println(i + ": HorizGoal cx: " + report.center_mass_x_normalized + " cy: "
                                + report.center_mass_y_normalized);
                        
                    } else {
                        // Height > width, it's the vertical goal
                        vertCenterMassX = report.center_mass_x_normalized;
                        vertCenterMassY = report.center_mass_y_normalized;
                        System.out.println(i + ": VertGoal cx: " + report.center_mass_x_normalized + " cy: "
                                + report.center_mass_y_normalized 
                                + " h: " + (report.boundingRectHeight/(double)report.imageHeight));
                        System.out.println(report.boundingRectHeight);
                        //System.out.println( (347.5 * report.boundingRectHeight) / 92.0 );
                    }
                    
                    // in discovering distance. ...
                    // y = distance to target (to find)
                    // x = sample distance (i.e. 10 meters)
                    // h = sample height of target (corresponding to sample distance)
                    // z = height of target
                    // y = hz / x
                    
                    // h = 92 px
                    // x = 347.5 cm
                    // ----------------------------------
                    // DISTANCE til full view of vision targets: 78.9 inches == 200 cm!!! 2 m
                    // x = FOV/140 //credits to Spring
                    // x = distance from wall to robot
                    // FOV = bounding rect width --> width from edge of horzgoal to other edge 
                    // TEST THIS
                    // Put robot 2 meters from the vision targets and measure the pixel width of the image
                    // (or boundingRectWidth) (from the edges of the goals
                    
                    

                    /*if(scoreCompare(scores[i], false))
                    {
                        System.out.println("particle: " + i + "is a High Goal  centerX: " + report.center_mass_x_normalized + "centerY: " + report.center_mass_y_normalized);
			System.out.println("Distance: " + computeDistance(thresholdImage, report, i, false));
                    } else if (scoreCompare(scores[i], true)) {
			System.out.println("particle: " + i + "is a Middle Goal  centerX: " + report.center_mass_x_normalized + "centerY: " + report.center_mass_y_normalized);
			System.out.println("Distance: " + computeDistance(thresholdImage, report, i, true));
                    } else {
                        System.out.println("particle: " + i + "is not a goal  centerX: " + report.center_mass_x_normalized + "centerY: " + report.center_mass_y_normalized);
                    }*/
			//System.out.println("rect: " + scores[i].rectangularity + "ARinner: " + scores[i].aspectRatioInner);
			//System.out.println("ARouter: " + scores[i].aspectRatioOuter + "xEdge: " + scores[i].xEdge + "yEdge: " + scores[i].yEdge);	
                    }
                System.out.println(isRightOrLeft(vertCenterMassX, vertCenterMassY, horzCenterMassX, horzCenterMassY)+"");

                /**
                 * all images in Java must be freed after they are used since they are allocated out
                 * of C data structures. Not calling free() will cause the memory to accumulate over
                 * each pass of this loop.
                 */
                filteredImage.free();
                convexHullImage.free();
                thresholdImage.free();
                image.free();
                System.out.println("-------");
//            } catch (AxisCameraException ex) {        // this is needed if the camera.getImage() is called
//                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            lastUpdate = System.currentTimeMillis();
        }
        Debug.log(debug);
    }

    /**
     * This function is called once each time the robot enters operator control.
     */
    public void operatorControl() {
        debug[0] = "User Control Mode";
        boolean triggerHasFired = false;
        boolean intakePressed = false;
        boolean outtakePressed = false;
        boolean winchPressed = false;
        boolean winchMovePressed = false;
        boolean winchWiggled = false;
        boolean afterFired = false;
        int winchWiggleCount = 0;
        long triggerStart = 0L;
         while(isOperatorControl() && isEnabled()) {
            double speed = left.getY() * joyScale;
            long currentTime = System.currentTimeMillis();
            joyScale = scaleZ(left.getZ());
            joyScale2 = scaleZ(right.getZ());
            debug[1] = "Speed: " + speed;                                                          
            
            
            if(left.getRawButton(WINCH_BUTTON_UP)){
                drive.set(WINCH_PORT, 1.0);
                winchMovePressed = true;
            } else if(left.getRawButton(WINCH_BUTTON_DOWN)){
                drive.set(WINCH_PORT, -1.0);
                winchMovePressed = true;
            }
            
            if (winchMovePressed && (!left.getRawButton(WINCH_BUTTON_UP) || 
                    !left.getRawButton(WINCH_BUTTON_DOWN))) {
                drive.set(WINCH_PORT, 0);
                winchMovePressed = false;
            }
            
            if (left.getRawButton(EMERGENCY_STOP_BUTTON)) {
                // HOLY CRAP STOP
                drive.set(TRIGGER_PORT, 0);
                drive.set(WINCH_PORT, 0);
                winch.deactivate();
                winchMovePressed = false;
                triggerHasFired = false;
                triggerStart = 0L;
                debug[0] = "Soft Emergency Stop";
                Debug.log(debug);
                return;
            }
            
            if(left.getRawButton(WINCH_BUTTON_SPIKE)){
                winch.goForward(); // hopefully yes
                winchPressed = true;
            } else if( !(left.getRawButton(WINCH_BUTTON_SPIKE)) && winchPressed){
                winch.deactivate();
                winchPressed = false;
            }
            
            this.doingWinchStuff(debug);
            
            this.doArcadeDrive(debug);
            
            // Intake
            if (left.getRawButton(INTAKE_BUTTON)){
                intake.goForward();
                intakePressed = true;
            } else if (!(left.getRawButton(INTAKE_BUTTON)) && intakePressed){
                intake.deactivate();
                intakePressed = false;
            }
            
            // Outtake
            if (left.getRawButton(OUTTAKE_BUTTON)){
                intake.goBackward();
                outtakePressed = true;
            }
            
            if (!(left.getRawButton(OUTTAKE_BUTTON)) && outtakePressed){
                intake.deactivate();
                outtakePressed = false;
            }
            
            if (left.getRawButton(TRIGGER_BUTTON) || triggerStart > 0L ){
                
                if (triggerStart == 0L){
                    triggerStart = currentTime;
                    winch.goForward();
                }
                else if (currentTime - triggerStart > 300L && !winchWiggled && !triggerHasFired && !afterFired){
                    if(winchWiggleCount < 1){
                        winchWiggleCount++;
                        wiggleWinch(0.10);
                    }
                    else {
                        winchWiggled = true;
                        wiggleWinch(0);
                    }
                }
                else {
                    if(currentTime - triggerStart > 500L && afterFired && !winchWiggled){
                        if(winchWiggleCount < 1){
                            winchWiggleCount++;
                            wiggleWinch(-0.15);
                        }
                        else {
                            wiggleWinch(0);
                            afterFired = false;
                            winchWiggleCount = 0;
                            triggerStart = 0L;
                        }
                    }
                    else if(currentTime - triggerStart > 100L && afterFired){
                        winch.deactivate();
                        winchWiggled = false;
                        winchWiggleCount = 0;
                    }
                    if (!triggerHasFired && !afterFired) drive.set(TRIGGER_PORT, TRIGGER_SPEED_UP);
                    if (currentTime - triggerStart > 600L && !triggerHasFired) {
                        drive.set(TRIGGER_PORT, 0);
                        triggerHasFired = true;
                        triggerStart = currentTime;
                    }
                    if (triggerHasFired && currentTime - triggerStart > 1000L) {
                        drive.set(TRIGGER_PORT, TRIGGER_SPEED_DOWN);
                        for (; fireLim.get();) {
                            long theTime = System.currentTimeMillis();
                            if (theTime - triggerStart > 4000L) {
                                debug[2] = "WTFBBQ";
                                break;
                            }
                        }
                        drive.set(TRIGGER_PORT, 0);
                        triggerHasFired = false;
                        
                        //triggerStart = 0L;
                       //winch.deactivate();
                       //winchWiggled = false;
                       afterFired = true;
                       //winchWiggled = false;
                       //winchWiggleCount = 0;
                    }
                    
                    //sleepBro(100);
                    //wiggleWinch();
                }
            }
            
            /**
            if (left.getRawButton(TRIGGER_BUTTON) || triggerStart > 0L){
                // Trigger the trigger
                if (triggerStart == 0L) triggerStart = currentTime;
                if (!triggerHasFired) drive.set(TRIGGER_PORT, TRIGGER_SPEED_UP);
                if (currentTime - triggerStart > 600L && !triggerHasFired) {
                    drive.set(TRIGGER_PORT, 0);
                    triggerHasFired = true;
                    triggerStart = currentTime;
                }
                if (triggerHasFired && currentTime - triggerStart > 1000L) {
                    drive.set(TRIGGER_PORT, TRIGGER_SPEED_DOWN);
                    for (; fireLim.get();) {
                        long theTime = System.currentTimeMillis();
                        if (theTime - triggerStart > 4000L) {
                            debug[2] = "WTFBBQ";
                            break;
                        }
                    }
                    drive.set(TRIGGER_PORT, 0);
                    triggerHasFired = false;
                    triggerStart = 0L;
                }
            }
            //*/
            
            
            if (left.getRawButton(TRIGGER_BUTTON_UP)) {
                // Window motor positive
                drive.set(TRIGGER_PORT, TRIGGER_SPEED_DOWN);
            } else if (left.getRawButton(TRIGGER_BUTTON_DOWN)) {
                // Window motor negative
                drive.set(TRIGGER_PORT, TRIGGER_SPEED_UP);
            } else if (triggerStart == 0L) {
                //trigger.set(0);
                drive.set(TRIGGER_PORT, 0);
            }
            
            
            long timeDelta = currentTime - lastUpdate;
            if (timeDelta > 250) {
                Debug.clear();
                ticks = currentTime;
            }
            
            Debug.log(debug);
            lastUpdate = System.currentTimeMillis();
         }
    }
         
    private double scaleZ(double rawZ) {
        return Math.min(1.0, 0.5 - 0.5 * rawZ);
    }
    
    private void doingWinchStuff(String[] debug){
        double y = right.getY();
        double winchMove = 0.0;
        double zone = 0.04;
        if(Math.abs(y) > zone){
            winchMove = y;
        }
        winchMove *= joyScale2 * -1;
        
        debug[5] = "rScale: "+joyScale2+" Winch: "+winchMove;
        drive.set(WINCH_PORT, winchMove);
    }
    
    
    private void wiggleWinch(double pow){
        drive.set(WINCH_PORT,pow);
    }
    
    /**private void sleepBro(int time){
        try{
            Thread.sleep(time);
        }
        catch(Exception e){
            // we should be going here
        }
    }//*/
    
     private void doArcadeDrive(String[] debug) {
        double leftMove = 0.0;
        double rightMove = 0.0;
        double zone = 0.04;

        joyScale = scaleZ(left.getZ());

        double x = left.getX() * -1;
        double y = left.getY();

        if (Math.abs(y) > zone) {
            leftMove = y;
            rightMove = y;
        }

        if (Math.abs(x) > zone) {
            leftMove = correct(leftMove + x);
            rightMove = correct(rightMove - x);
        }

        leftMove *= joyScale * -1;
        rightMove *= joyScale * -1;
        debug[3] = "Scale: " + joyScale;
        debug[4] = "Left: " + leftMove +" Right: "+rightMove;

        drive.tankDrive(leftMove, rightMove);
    }
     
     private double correct(double val) {
        if (val > 1.0) {
            return 1.0;
        }
        if (val < -1.0) {
            return -1.0;
        }
        return val;
    }
    /**
     * This function is called once each time the robot enters test mode.
     */
    public void test() {
    
    }
    
    /**
     * Computes the estimated distance to a target using the height of the particle in the image. For more information and graphics
     * showing the math behind this approach see the Vision Processing section of the ScreenStepsLive documentation.
     * 
     * @param image The image to use for measuring the particle estimated rectangle
     * @param report The Particle Analysis Report for the particle
     * @param outer True if the particle should be treated as an outer target, false to treat it as a center target
     * @return The estimated distance to the target in Inches.
     */
    double computeDistance (BinaryImage image, ParticleAnalysisReport report, int particleNumber, boolean outer) throws NIVisionException {
            double rectShort, height;
            int targetHeight;

            rectShort = NIVision.MeasureParticle(image.image, particleNumber, false, NIVision.MeasurementType.IMAQ_MT_EQUIVALENT_RECT_SHORT_SIDE);
            //using the smaller of the estimated rectangle short side and the bounding rectangle height results in better performance
            //on skewed rectangles
            height = Math.min(report.boundingRectHeight, rectShort);
            targetHeight = outer ? 29 : 21;

            return X_IMAGE_RES * targetHeight / (height * 12 * 2 * Math.tan(VIEW_ANGLE*Math.PI/(180*2)));
    }
    
    /**
     * Computes a score (0-100) comparing the aspect ratio to the ideal aspect ratio for the target. This method uses
     * the equivalent rectangle sides to determine aspect ratio as it performs better as the target gets skewed by moving
     * to the left or right. The equivalent rectangle is the rectangle with sides x and y where particle area= x*y
     * and particle perimeter= 2x+2y
     * 
     * @param image The image containing the particle to score, needed to performa additional measurements
     * @param report The Particle Analysis Report for the particle, used for the width, height, and particle number
     * @param outer	Indicates whether the particle aspect ratio should be compared to the ratio for the inner target or the outer
     * @return The aspect ratio score (0-100)
     */
    public double scoreAspectRatio(BinaryImage image, ParticleAnalysisReport report, int particleNumber, boolean outer) throws NIVisionException
    {
        double rectLong, rectShort, aspectRatio, idealAspectRatio;

        rectLong = NIVision.MeasureParticle(image.image, particleNumber, false, NIVision.MeasurementType.IMAQ_MT_EQUIVALENT_RECT_LONG_SIDE);
        rectShort = NIVision.MeasureParticle(image.image, particleNumber, false, NIVision.MeasurementType.IMAQ_MT_EQUIVALENT_RECT_SHORT_SIDE);
        //idealAspectRatio = outer ? (62/29) : (62/20);	//Dimensions of goal opening + 4 inches on all 4 sides for reflective tape
	idealAspectRatio = outer ? (43/32) : (39/28);
        //Divide width by height to measure aspect ratio
        aspectRatio = report.boundingRectWidth / (double)report.boundingRectHeight;
        /*if(report.boundingRectWidth > report.boundingRectHeight){
            //particle is wider than it is tall, divide long by short
            aspectRatio = 100*(1-Math.abs((1-((rectLong/rectShort)/idealAspectRatio))));
        } else {
            //particle is taller than it is wide, divide short by long
                aspectRatio = 100*(1-Math.abs((1-((rectShort/rectLong)/idealAspectRatio))));
        }*/
        return aspectRatio;
	//return (Math.max(0, Math.min(aspectRatio, 100.0)));		//force to be in range 0-100
    }
    
    /**
     * Compares scores to defined limits and returns true if the particle appears to be a target
     * 
     * @param scores The structure containing the scores to compare
     * @param outer True if the particle should be treated as an outer target, false to treat it as a center target
     * 
     * @return True if the particle meets all limits, false otherwise
     */
    boolean scoreCompare(Scores scores, boolean outer){
            boolean isTarget = true;

            isTarget &= scores.rectangularity > RECTANGULARITY_LIMIT;
            if(outer){
                    isTarget &= scores.aspectRatioOuter > ASPECT_RATIO_LIMIT;
            } else {
                    isTarget &= scores.aspectRatioInner > ASPECT_RATIO_LIMIT;
            }
            isTarget &= scores.xEdge > X_EDGE_LIMIT;
            isTarget &= scores.yEdge > Y_EDGE_LIMIT;

            return isTarget;
    }
    
    /**
     * Computes a score (0-100) estimating how rectangular the particle is by comparing the area of the particle
     * to the area of the bounding box surrounding it. A perfect rectangle would cover the entire bounding box.
     * 
     * @param report The Particle Analysis Report for the particle to score
     * @return The rectangularity score (0-100)
     */
    double scoreRectangularity(ParticleAnalysisReport report){
            if(report.boundingRectWidth*report.boundingRectHeight !=0){
                    return 100*report.particleArea/(report.boundingRectWidth*report.boundingRectHeight);
            } else {
                    return 0;
            }	
    }
    
    /**
     * Computes a score based on the match between a template profile and the particle profile in the X direction. This method uses the
     * the column averages and the profile defined at the top of the sample to look for the solid vertical edges with
     * a hollow center.
     * 
     * @param image The image to use, should be the image before the convex hull is performed
     * @param report The Particle Analysis Report for the particle
     * 
     * @return The X Edge Score (0-100)
     */
    public double scoreXEdge(BinaryImage image, ParticleAnalysisReport report) throws NIVisionException
    {
        double total = 0;
        LinearAverages averages;
        
        NIVision.Rect rect = new NIVision.Rect(report.boundingRectTop, report.boundingRectLeft, report.boundingRectHeight, report.boundingRectWidth);
        averages = NIVision.getLinearAverages(image.image, LinearAverages.LinearAveragesMode.IMAQ_COLUMN_AVERAGES, rect);
        float columnAverages[] = averages.getColumnAverages();
        for(int i=0; i < (columnAverages.length); i++){
                if(xMin[(i*(XMINSIZE-1)/columnAverages.length)] < columnAverages[i] 
                   && columnAverages[i] < xMax[i*(XMAXSIZE-1)/columnAverages.length]){
                        total++;
                }
        }
        total = 100*total/(columnAverages.length);
        return total;
    }
    
    /**
	 * Computes a score based on the match between a template profile and the particle profile in the Y direction. This method uses the
	 * the row averages and the profile defined at the top of the sample to look for the solid horizontal edges with
	 * a hollow center
	 * 
	 * @param image The image to use, should be the image before the convex hull is performed
	 * @param report The Particle Analysis Report for the particle
	 * 
	 * @return The Y Edge score (0-100)
	 *
    */
    public double scoreYEdge(BinaryImage image, ParticleAnalysisReport report) throws NIVisionException
    {
        double total = 0;
        LinearAverages averages;
        
        NIVision.Rect rect = new NIVision.Rect(report.boundingRectTop, report.boundingRectLeft, report.boundingRectHeight, report.boundingRectWidth);
        averages = NIVision.getLinearAverages(image.image, LinearAverages.LinearAveragesMode.IMAQ_ROW_AVERAGES, rect);
        float rowAverages[] = averages.getRowAverages();
        for(int i=0; i < (rowAverages.length); i++){
                if(yMin[(i*(YMINSIZE-1)/rowAverages.length)] < rowAverages[i] 
                   && rowAverages[i] < yMax[i*(YMAXSIZE-1)/rowAverages.length]){
                        total++;
                }
        }
        total = 100*total/(rowAverages.length);
        return total;
    }
    
    /**
     * will discover if we are right or left of the field
     * 
     * MODIFY METHOD WHEN WE HAVE MULTIPLE VERTICALS GOALS
     * (we can only see one set of vertical and horizontal goals)
     * 
     * @param vertGoalX center of mass x normalized of vertical goal
     * @param vertGoalY center of mass y normalized of vertical goal
     * @param horzGoalX center of mass x noramlized of horizontal goal
     * @param horzGoalY center of mass y normalized of horizongtal goal
     * @return true means we are on Right side of field, false means we are on left side
     */
    private boolean isRightOrLeft(double vertGoalX, double vertGoalY, double horzGoalX, double horzGoalY ){
        vertGoalY = -1 * vertGoalY;
        horzGoalY = -1 * horzGoalY;
        if(horzGoalX > vertGoalX){
            return true;
        }
        else if(horzGoalX < vertGoalX) {
            return false;
        }
        else {
            System.out.println("lol");
            return false;
        }
    }
}

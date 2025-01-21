package client.controllers;

import com.google.inject.Inject;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.awt.*;
import java.io.File;

public class NotificationsCtrl {

    private HBox notificationBar;
    private Label notificationText;

    private Timeline activeTimeline;
    private PauseTransition activePauseTransition;

    @Inject
    public NotificationsCtrl() {

    }

    public void setReferences(HBox notificationBox, Label notificationText) {
        this.notificationBar = notificationBox;
        this.notificationText = notificationText;
    }

    public void pushNotification(String message) {
        Toolkit.getDefaultToolkit().beep(); // play THAT windows sound

        if (activeTimeline != null && activeTimeline.getStatus() == Timeline.Status.RUNNING) {
            activeTimeline.stop();
        }
        if (activePauseTransition != null && activePauseTransition.getStatus() == PauseTransition.Status.RUNNING) {
            activePauseTransition.stop();
        }

        notificationText.setText(message);

        // Animation duration
        double animationDuration = 6.0; // seconds
        int frameRate = 60; // frames per second
        int totalFrames = (int) (animationDuration * frameRate);

        activeTimeline = new Timeline();

        for (int i = 0; i <= totalFrames; i++) {
            double progress = i / (double) totalFrames;

            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(i / (double) frameRate),
                    e -> notificationBar.setStyle(generateGradient(progress))
            );

            activeTimeline.getKeyFrames().add(keyFrame);
        }

        activeTimeline.setCycleCount(1); // Play the animation once
        activeTimeline.play(); // Start the animation

        // Set text
        activePauseTransition = new PauseTransition(Duration.seconds(8));
        activePauseTransition.setOnFinished(event -> notificationText.setText(""));
        activePauseTransition.play();
    }

    /**
     * Generates a gradient based on the progress.
     * @param progress Progress between 0.0 (start) and 1.0 (end).
     * @return CSS style for the gradient.
     */
    private String generateGradient(double progress) {
        // Start color
        String startColor = "#854b4a"; // Dark red
        // End color
        String endColor = "#2B2D30";  // Main foreground color

        // Interpolate the RGB values for the first color
        int startRed = Integer.parseInt(startColor.substring(1, 3), 16);
        int startGreen = Integer.parseInt(startColor.substring(3, 5), 16);
        int startBlue = Integer.parseInt(startColor.substring(5, 7), 16);

        int endRed = Integer.parseInt(endColor.substring(1, 3), 16);
        int endGreen = Integer.parseInt(endColor.substring(3, 5), 16);
        int endBlue = Integer.parseInt(endColor.substring(5, 7), 16);

        // Calculate interpolated RGB values
        int red = (int) (startRed + progress * (endRed - startRed));
        int green = (int) (startGreen + progress * (endGreen - startGreen));
        int blue = (int) (startBlue + progress * (endBlue - startBlue));

        // Format the interpolated color
        String interpolatedColor = String.format("#%02X%02X%02X", red, green, blue);

        // Return the full gradient style
        return String.format(
                "-fx-background-color: linear-gradient(to right, %s 0%%, -main-foreground 10%%, -main-foreground 100%%);",
                interpolatedColor
        );
    }
}



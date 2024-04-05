package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RaycasterEngine extends JPanel implements ActionListener {
    private final int WINDOW_WIDTH = 1024;
    private final int WINDOW_HEIGHT = 512;

    int mapX = 8, mapY = 8, mapS = 64, mp, mx, my;
    int map[] = {
            1, 1, 1, 1, 1, 1, 1, 1,
            1, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 1,
            1, 0, 0, 0, 0, 0, 0, 1,
            1, 1, 1, 1, 1, 1, 1, 1,
    };

    private final float PI = 3.1415926525F;
    private boolean[] keysPressed = new boolean[256]; // Array to track keys being pressed


    private float PLAYER_SIZE = 15;
    private float playerX = WINDOW_WIDTH / 4 - PLAYER_SIZE / 2;
    private float playerDeltaX;
    private float playerY = WINDOW_HEIGHT / 2 - PLAYER_SIZE / 2;
    private float playerDeltaY;
    private float playerAngle;

    private int rays;
    private float raysAngle;
    private float raysY;
    private float raysX;
    private float dof; // depth of field
    private float yOffset;
    private float xOffset;

    public RaycasterEngine() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.GRAY);
        setFocusable(true);
        requestFocus();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed[e.getKeyCode()] = true; // Set the corresponding key to true when pressed
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed[e.getKeyCode()] = false; // Set the corresponding key to false when released
            }
        });

        // Create and start the movement thread
        Thread movementThread = new Thread(() -> {
            while (true) {
                movePlayer();
                try {
                    Thread.sleep(10); // Adjust the sleep time as needed
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        movementThread.start();
    }

    private void movePlayer() {
        // Check for pressed keys and update player position accordingly
        if (keysPressed['A']) { // 'A' key for moving left
            playerAngle -= 0.1;
            if (playerAngle < 0) {
                playerAngle += 2 * PI;
            }
            playerDeltaX = (float) (Math.cos(playerAngle) * 5);
            playerDeltaY = (float) (Math.sin(playerAngle) * 5);
        }
        if (keysPressed['D']) { // 'D' key for moving right
            playerAngle += 0.1;
            playerAngle %= 2 * PI; // Ensure playerAngle stays within [0, 2*PI)
            playerDeltaX = (float) (Math.cos(playerAngle) * 5);
            playerDeltaY = (float) (Math.sin(playerAngle) * 5);
        }
        if (keysPressed['W']) { // 'W' key for moving up
            playerX += playerDeltaX;
            playerY += playerDeltaY;
        }
        if (keysPressed['S']) { // 'S' key for moving down
            playerX -= playerDeltaX;
            playerY -= playerDeltaY;
        }
        repaint(); // Repaint the panel to reflect the updated player position
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw the map
        for (int y = 0; y < mapY; y++) {
            for (int x = 0; x < mapX; x++) {
                int wall = map[y * mapX + x];
                if (wall == 1) {
                    // Draw white cube (black outline)
                    g.setColor(Color.WHITE);
                    g.fillRect((int) (x * mapS), (int) (y * mapS), (int) mapS, (int) mapS);
                    g.setColor(Color.BLACK);
                    g.drawRect((int) (x * mapS), (int) (y * mapS), (int) mapS, (int) mapS);
                } else {
                    // Draw black cube (white outline)
                    g.setColor(Color.BLACK);
                    g.fillRect((int) (x * mapS), (int) (y * mapS), (int) mapS, (int) mapS);
                    g.setColor(Color.WHITE);
                    g.drawRect((int) (x * mapS), (int) (y * mapS), (int) mapS, (int) mapS);
                }
            }
        }

        // Draw the player
        g.setColor(Color.YELLOW);
        g.fillRect((int) playerX, (int) playerY, (int) PLAYER_SIZE, (int) PLAYER_SIZE);

        // Draw line indicating player's angle
        int lineLength = 50; // Adjust line length as needed
        int x2 = (int) (playerX + 0.5 * PLAYER_SIZE + lineLength * Math.cos(playerAngle));
        int y2 = (int) (playerY + 0.5 * PLAYER_SIZE + lineLength * Math.sin(playerAngle));
        g2d.setStroke(new BasicStroke(4)); // Adjust line thickness as needed
        g.setColor(Color.YELLOW);
        g.drawLine((int) (playerX + 0.5 * PLAYER_SIZE), (int) (playerY + 0.5 * PLAYER_SIZE), x2, y2);

        // Draw rays
        drawRays3D(g);
    }

    public void drawRays3D(Graphics g) {
        raysAngle = playerAngle;
        float startX = playerX + 0.5f * PLAYER_SIZE; // Startposition X der Linie
        float startY = playerY + 0.5f * PLAYER_SIZE; // Startposition Y der Linie

        for (rays = 0; rays < 1; rays++) {
            dof = 0;
            float aTan = (float) (-1 / Math.tan(raysAngle));
            if (raysAngle > PI) {
                raysY = (float) ((((int) playerY >> 6) << 6) - 0.0001);
                raysX = (playerY - raysY) * aTan + playerX;
                yOffset = -64;
                xOffset = -yOffset * aTan;
            }
            if (raysAngle < PI) {
                raysY = (float) ((((int) playerY >> 6) << 6) + 64);
                raysX = (playerY - raysY) * aTan + playerX;
                yOffset = 64;
                xOffset = -yOffset * aTan;
            }
            if (raysAngle == 0 || raysAngle == PI) {
                raysX = playerX;
                raysY = playerY;
                dof = 8;
            }
            while (dof < 8) {
                // Check if raysX and raysY are within bounds
                if (raysX >= 0 && raysX < WINDOW_WIDTH && raysY >= 0 && raysY < WINDOW_HEIGHT) {
                    mx = (int) (raysX) >> 6;
                    my = (int) (raysY) >> 6;
                    mp = my * mapX + mx;
                    if (mp < mapX * mapY && map[mp] == 1) {
                        dof = 8;
                    } // hits wall
                    else {
                        raysX += xOffset;
                        raysY += yOffset;
                        dof += 1;
                    }
                } else {
                    // Rays are out of bounds, stop the loop
                    dof = 8;
                }
            }
        }

        // Draw the green line from the middle of the player
        g.setColor(Color.GREEN);
        g.drawLine((int) startX, (int) startY, (int) raysX, (int) raysY);
    }




    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Raycaster Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new RaycasterEngine());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class RaycasterEngine extends JPanel implements KeyListener {
    //-----------------------------MAP----------------------------------------------
    private final int mapX = 8;    // map width
    private final int mapY = 8;    // map height
    private final int mapS = 64;   // map cube size
    private final int[] map =      // the map array. Edit to change level but keep the outer walls
            {
                    1, 1, 1, 1, 1, 1, 1, 1,
                    1, 0, 1, 0, 1, 0, 0, 1,
                    1, 0, 1, 0, 1, 0, 0, 1,
                    1, 0, 0, 0, 1, 0, 0, 1,
                    1, 0, 0, 0, 0, 0, 0, 1,
                    1, 0, 0, 0, 0, 0, 0, 1,
                    1, 0, 0, 0, 0, 0, 0, 1,
                    1, 1, 1, 1, 1, 1, 1, 1,
            };

    //------------------------PLAYER------------------------------------------------
    private float px, py, pdx, pdy, pa;

    public RaycasterEngine() {
        addKeyListener(this);
        setFocusable(true);
        setPreferredSize(new Dimension(1024, 510));
        init();
    }

    private void init() {
        px = 150;
        py = 400;
        pa = 90;
        pdx = (float) Math.cos(Math.toRadians(pa));
        pdy = (float) -Math.sin(Math.toRadians(pa));

        new Thread(this::playerMovement).start();
    }

    private void drawMap2D(Graphics2D g2d) {
        int xo, yo;
        for (int y = 0; y < mapY; y++) {
            for (int x = 0; x < mapX; x++) {
                if (map[y * mapX + x] == 1) {
                    g2d.setColor(Color.WHITE);
                } else {
                    g2d.setColor(Color.BLACK);
                }
                xo = x * mapS;
                yo = y * mapS;
                g2d.fillRect(xo + 1, yo + 1, mapS - 2, mapS - 2);
            }
        }
    }

    private void drawPlayer2D(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawOval((int) px - 4, (int) py - 4, 8, 8);
        g2d.drawLine((int) px, (int) py, (int) (px + pdx * 20), (int) (py + pdy * 20));

        // Draw player direction
        g2d.setColor(Color.GREEN);
        int coneWidth = 90; // Width of the cone in degrees
        int coneHalfWidth = coneWidth / 2;
        int coneLength = 50; // Length of the cone
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];

        // Calculate the points of the cone
        xPoints[0] = (int) px;
        yPoints[0] = (int) py;
        xPoints[1] = (int) (px + Math.cos(Math.toRadians(pa - coneHalfWidth)) * coneLength);
        yPoints[1] = (int) (py - Math.sin(Math.toRadians(pa - coneHalfWidth)) * coneLength);
        xPoints[2] = (int) (px + Math.cos(Math.toRadians(pa + coneHalfWidth)) * coneLength);
        yPoints[2] = (int) (py - Math.sin(Math.toRadians(pa + coneHalfWidth)) * coneLength);

        g2d.fillPolygon(xPoints, yPoints, 3);
    }




    //---------------------------Draw Rays and Walls--------------------------------
    private float distance(float ax, float ay, float bx, float by, float ang) {
        return (float) (Math.cos(Math.toRadians(ang)) * (bx - ax) - Math.sin(Math.toRadians(ang)) * (by - ay));
    }

    private void drawMap3D(Graphics2D g2d) {
        int mx, my, mp, dof, side;
        float vx, vy, rx, ry, ra, xo = 0, yo = 0, disV, disH;

        ra = FixAng((int) (pa + 30));  // ray set back 30 degrees

        for (int r = 0; r < 60; r++) {
            // Vertical
            dof = 0;
            side = 0;
            disV = 100000;
            float Tan = (float) Math.tan(Math.toRadians(ra));
            if (Math.cos(Math.toRadians(ra)) > 0.001) {
                rx = (((int) px >> 6) << 6) + 64;
                ry = (px - rx) * Tan + py;
                xo = 64;
                yo = -xo * Tan;
            } else if (Math.cos(Math.toRadians(ra)) < -0.001) {
                rx = (((int) px >> 6) << 6) - 0.0001f;
                ry = (px - rx) * Tan + py;
                xo = -64;
                yo = -xo * Tan;
            } else {
                rx = px;
                ry = py;
                dof = 8;
            }

            while (dof < 8) {
                mx = (int) (rx) >> 6;
                my = (int) (ry) >> 6;
                mp = my * mapX + mx;
                if (mp > 0 && mp < mapX * mapY && map[mp] == 1) {
                    dof = 8;
                    disV = (float) (Math.cos(Math.toRadians(ra)) * (rx - px) - Math.sin(Math.toRadians(ra)) * (ry - py));
                } else {
                    rx += xo;
                    ry += yo;
                    dof += 1;
                }
            }
            vx = rx;
            vy = ry;

            // Horizontal
            dof = 0;
            disH = 100000;
            Tan = 1.0f / Tan;
            if (Math.sin(Math.toRadians(ra)) > 0.001) {
                ry = (((int) py >> 6) << 6) - 0.0001f;
                rx = (py - ry) * Tan + px;
                yo = -64;
                xo = -yo * Tan;
            } else if (Math.sin(Math.toRadians(ra)) < -0.001) {
                ry = (((int) py >> 6) << 6) + 64;
                rx = (py - ry) * Tan + px;
                yo = 64;
                xo = -yo * Tan;
            } else {
                rx = px;
                ry = py;
                dof = 8;
            }

            while (dof < 8) {
                mx = (int) (rx) >> 6;
                my = (int) (ry) >> 6;
                mp = my * mapX + mx;
                if (mp > 0 && mp < mapX * mapY && map[mp] == 1) {
                    dof = 8;
                    disH = (float) (Math.cos(Math.toRadians(ra)) * (rx - px) - Math.sin(Math.toRadians(ra)) * (ry - py));
                } else {
                    rx += xo;
                    ry += yo;
                    dof += 1;
                }
            }

            // Adjust color based on direction of the ray
            Color wallColor = new Color(255, 0, 0); // Default color set to red (RGB: 255, 0, 0)
            if (disV < disH) {
                rx = vx;
                ry = vy;
                disH = disV;

                // Adjust brightness of the color for horizontal walls (make it darker)
                float[] hsb = Color.RGBtoHSB(wallColor.getRed(), wallColor.getGreen(), wallColor.getBlue(), null);
                wallColor = Color.getHSBColor(hsb[0], hsb[1], hsb[2] * 0.8f); // Reduce brightness by 30%
            }

            g2d.setColor(wallColor);

            // Draw a single pixel at the intersection point
            g2d.fillRect((int) rx, (int) ry, 1, 1);

            int ca = FixAng((int) (pa - ra));
            disH = disH * (float) Math.cos(Math.toRadians(ca));
            int lineH = (mapS * 320) / ((int) disH);
            if (lineH > 320) {
                lineH = 320;
            }
            int lineOff = 160 - (lineH >> 1);

            g2d.setStroke(new BasicStroke(8));
            g2d.drawLine(r * 8 + 530, lineOff, r * 8 + 530, lineOff + lineH);

            ra = FixAng((int) (ra - 1));
        }
    }




    private int FixAng(int a) {
        if (a > 359) {
            a -= 360;
        }
        if (a < 0) {
            a += 360;
        }
        return a;
    }

    private float degToRad(int a) {
        return (float) (a * Math.PI / 180.0);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Set background color to grey
        setBackground(Color.GRAY);

        // Apply simple lighting effect
        float lightIntensity = 0.5f; // Adjust the light intensity as needed
        g2d.setColor(new Color(lightIntensity, lightIntensity, lightIntensity));

        drawMap2D(g2d);
        drawPlayer2D(g2d);
        drawMap3D(g2d);
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("RaycasterEngine");
        RaycasterEngine engine = new RaycasterEngine();
        frame.add(engine);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private void playerMovement() {
        while (true) {
            // Spielerbewegung
            if (moveForward) {
                float newPx = px + pdx * 10;
                float newPy = py + pdy * 10;
                if (checkCollision(newPx, newPy)) {
                    px = newPx;
                    py = newPy;
                }
            }
            if (moveBackward) {
                float newPx = px - pdx * 10;
                float newPy = py - pdy * 10;
                if (checkCollision(newPx, newPy)) {
                    px = newPx;
                    py = newPy;
                }
            }

            // Spielerrotation
            if (rotateLeft) {
                pa += 10;
                pa = FixAng((int) pa);
                pdx = (float) Math.cos(Math.toRadians(pa));
                pdy = (float) -Math.sin(Math.toRadians(pa));
            }
            if (rotateRight) {
                pa -= 10;
                pa = FixAng((int) pa);
                pdx = (float) Math.cos(Math.toRadians(pa));
                pdy = (float) -Math.sin(Math.toRadians(pa));
            }

            repaint();

            try {
                Thread.sleep(50); // Wartezeit zwischen den Aktualisierungen
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean rotateLeft = false;
    private boolean rotateRight = false;

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                moveForward = true;
                break;
            case KeyEvent.VK_S:
                moveBackward = true;
                break;
            case KeyEvent.VK_A:
                rotateLeft = true;
                break;
            case KeyEvent.VK_D:
                rotateRight = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                moveForward = false;
                break;
            case KeyEvent.VK_S:
                moveBackward = false;
                break;
            case KeyEvent.VK_A:
                rotateLeft = false;
                break;
            case KeyEvent.VK_D:
                rotateRight = false;
                break;
        }
    }

    private boolean checkCollision(float x, float y) {
        int mapXIndex = (int) (x / mapS);
        int mapYIndex = (int) (y / mapS);
        return map[mapYIndex * mapX + mapXIndex] == 0;
    }



}
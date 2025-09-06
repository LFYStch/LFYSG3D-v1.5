import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;

public class Main {
    public static void main(String[] args) {
        JFrame w = new JFrame();
        dP d = new dP();
        w.setTitle("3D Test");
        w.setSize(500, 500);
        w.setResizable(false);
        w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        w.add(d);
        w.setVisible(true);
        new javax.swing.Timer(50, e -> d.update()).start();
    }
}

class dP extends JPanel {
    vec3 cam;
    double camYaw, camPitch;
    vec3 light_source1;
    BufferedImage texture1;
    spawner sp;
    float alpha = 0.5f;
    double i = 0;

    public dP() {
        setDoubleBuffered(true);
        cam = new vec3(0, -10, 0, 0, 0);
        light_source1 = new vec3(cam.x, cam.y, cam.z - 2, 0, 0);
        camYaw = 0;
        camPitch = 0;
        loadTextures();
        sp = new spawner();
    }

    public void loadTextures() {
        try {
            texture1 = ImageIO.read(new File("dir.png"));
        } catch (IOException e) {
            System.err.println("Texture load failed.");
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        mesh LFYS = sp.LFYS(0, 0, 20, 0, i);
        drawMesh(LFYS, g2d, texture1);
        

    }

    public void drawMesh(mesh ts, Graphics2D g2d, BufferedImage texture) {
        java.util.List<tri> sortedTris = new java.util.ArrayList<>();
        for (tri[] strip : ts.tris) {
            Collections.addAll(sortedTris, strip);
        }

        sortedTris.sort((a, b) -> {
            double za = (a.v1.z + a.v2.z + a.v3.z) / 3.0;
            double zb = (b.v1.z + b.v2.z + b.v3.z) / 3.0;
            return Double.compare(zb, za);
        });

        for (tri t : sortedTris) {
            vec2 v1 = t.v1.project(cam, camYaw, camPitch);
            vec2 v2 = t.v2.project(cam, camYaw, camPitch);
            vec2 v3 = t.v3.project(cam, camYaw, camPitch);

            if (Double.isNaN(v1.x) || Double.isNaN(v2.x) || Double.isNaN(v3.x)) continue;

            int[] xPoints = { (int) v1.x, (int) v2.x, (int) v3.x };
            int[] yPoints = { (int) v1.y, (int) v2.y, (int) v3.y };

            int minX = Math.max(0, Math.min(xPoints[0], Math.min(xPoints[1], xPoints[2])));
            int maxX = Math.min(getWidth() - 1, Math.max(xPoints[0], Math.max(xPoints[1], xPoints[2])));
            int minY = Math.max(0, Math.min(yPoints[0], Math.min(yPoints[1], yPoints[2])));
            int maxY = Math.min(getHeight() - 1, Math.max(yPoints[0], Math.max(yPoints[1], yPoints[2])));

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    double[] bary = computeBarycentric(xPoints[0], yPoints[0], xPoints[1], yPoints[1], xPoints[2], yPoints[2], x, y);
                    double l1 = bary[0], l2 = bary[1], l3 = bary[2];

                    if (l1 >= 0 && l2 >= 0 && l3 >= 0) {
                        double u = l1 * t.v1.u + l2 * t.v2.u + l3 * t.v3.u;
                        double v = l1 * t.v1.v + l2 * t.v2.v + l3 * t.v3.v;

                        int texX = (int)(u * texture.getWidth());
                        int texY = (int)(v * texture.getHeight());

                        if (texX >= 0 && texX < texture.getWidth() && texY >= 0 && texY < texture.getHeight()) {
                            g2d.setColor(new Color(texture.getRGB(texX, texY)));
                            g2d.drawLine(x, y, x, y);
                        }
                    }
                }
            }

            if (t.v1.z < light_source1.z) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(xPoints, yPoints, 3);
            }

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
    public void update(){
        repaint();
        i+=0.05;
    }
    double[] computeBarycentric(double x1, double y1, double x2, double y2, double x3, double y3, int px, int py) {
        double det = (y2 - y3)*(x1 - x3) + (x3 - x2)*(y1 - y3);
        double l1 = ((y2 - y3)*(px - x3) + (x3 - x2)*(py - y3)) / det;
        double l2 = ((y3 - y1)*(px - x3) + (x1 - x3)*(py - y3)) / det;
        double l3 = 1 - l1 - l2;
        return new double[]{l1, l2, l3};
    }
}

class vec3 {
    double x, y, z;
    double u, v;
    

    public vec3(double x, double y, double z, double u, double v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }

    public vec2 project(vec3 cam, double yaw, double pitch) {
        double nX = this.x - cam.x;
        double nY = this.y - cam.y;
        double nZ = this.z - cam.z;
        if (nZ >= cam.z) {
            double rotX = nX * Math.cos(yaw) - nZ * Math.sin(yaw);
            double rotZ = nX * Math.sin(yaw) + nZ * Math.cos(yaw);
            double finalY = nY * Math.cos(pitch) - rotZ * Math.sin(pitch);
            double finalZ = nY * Math.sin(pitch) + rotZ * Math.cos(pitch);
            double scale = 200 / Math.max(finalZ, 0.1);
            return new vec2(rotX * scale + 250, finalY * scale + 250);
        } else {
            return new vec2(Double.NaN, Double.NaN);
        }
    }
}

class vec2 {
    double x, y;
    public vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

class tri {
    vec3 v1, v2, v3;
    public tri(vec3 v1, vec3 v2, vec3 v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }
}

class mesh {
    tri[][] tris;
    public mesh(tri[][] tris) {
        this.tris = tris;
    }
}

class tester {
    mesh m;
    public tester(double x, double y, double z) {
        m = new mesh(new tri[][] {
            {
                new tri(
                    new vec3(x - 5, y - 5, z + 5, 0, 0),
                    new vec3(x - 5, y + 5, z + 5, 0, 1),
                    new vec3(x + 5, y + 5, z + 5, 1, 1)
                )
            }
        });
    }
}

class spawner {
    public mesh test(double x, double y, double z) {
        return new tester(x, y, z).m;
    }

    
    public mesh LFYS(double x, double y, double z, int aI, double theta) {
        
        GameObject LFYS = new GameObject(new mesh[]{new mesh(new tri[][] {
            {
                new tri(
                new vec3(-10+x,  10+y, 10+z, 0, 0),  // Top-left
                new vec3(-10+x, -10+y, 10+z, 0, 1),  // Bottom-left
                new vec3( 10+x, -10+y, 10+z, 1, 1)   // Bottom-right
            ),
            new tri(
                new vec3(-10+x,  10+y, 10+z, 0, 0),  // Top-left
                new vec3( 10+x, -10+y, 10+z, 1, 1),  // Bottom-right
                new vec3( 10+x,  10+y, 10+z, 1, 0)
            )
            }
        })}, new AABB(new vec3(0, 0, 0, 0, 0), new vec3(0, 0, 0, 0, 0)),theta,x,z);

        mesh lfys = LFYS.getMesh(aI);

        




        return lfys;
    }
}


class AABB {
    vec3 min, max;

    public AABB(vec3 min, vec3 max) {
        this.min = min;
        this.max = max;
    }

    public boolean onColide(AABB aabbO, AABB aabbT) {
        return aabbO.min.x > aabbT.max.x && aabbO.min.x < aabbT.max.x &&
               aabbO.min.y > aabbT.max.y && aabbO.min.y < aabbT.max.y &&
               aabbO.min.z > aabbT.max.z && aabbO.min.z < aabbT.max.z;
    }
}
class GameObject {
    mesh[] anims;
    AABB hitbox;
    double theta,cx,cz;
    public GameObject(mesh[] anims, AABB hitbox, double theta,double cx,double cz) {
        this.anims = anims;
        this.hitbox = hitbox;
        this.theta = theta;
        this.cx = cx;
        this.cz = cz;
    }

    public mesh getMesh(int AnimIndex) {
        mesh lfys = anims[AnimIndex];
        for (int row = 0; row < lfys.tris.length; row++) {
    for (int col = 0; col < lfys.tris[row].length; col++) {
        tri t = lfys.tris[row][col];

        for (vec3 v : new vec3[]{t.v1, t.v2, t.v3}) {
            double relX = v.x - cx;
            double relZ = v.z - cz;

            
            double rotX = relX * Math.cos(theta) - relZ * Math.sin(theta);
            double rotZ = relX * Math.sin(theta) + relZ * Math.cos(theta);

           
            v.x = rotX + cx;
            v.z = rotZ + cz;
        }
    }
}
return lfys;
    }
}
public class objloader {
    public static tri[][] load(String objData, double x, double y, double z) {
        java.util.List<vec3> positions = new java.util.ArrayList<>();
        java.util.List<vec2> uvs = new java.util.ArrayList<>();
        java.util.List<java.util.List<tri>> meshGroups = new java.util.ArrayList<>();
        java.util.List<tri> currentGroup = new java.util.ArrayList<>();

        String[] lines = objData.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] tokens = line.split("\\s+");
            switch (tokens[0]) {
                case "v":
                    positions.add(new vec3(
                        Float.parseFloat(tokens[1]) + (float)x,
                        Float.parseFloat(tokens[2]) + (float)y,
                        Float.parseFloat(tokens[3]) + (float)z
                    ));
                    break;

                case "vt":
                    uvs.add(new vec2(
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2])
                    ));
                    break;

                case "f":
                    if (tokens.length < 4) break;

                    vec3[] verts = new vec3[3];
                    vec2[] texs = new vec2[3];

                    for (int i = 0; i < 3; i++) {
                        String[] parts = tokens[i + 1].split("/");
                        int vi = Integer.parseInt(parts[0]) - 1;
                        verts[i] = positions.get(vi);

                        if (parts.length > 1 && !parts[1].isEmpty()) {
                            int ti = Integer.parseInt(parts[1]) - 1;
                            texs[i] = uvs.get(ti);
                        } else {
                            texs[i] = new vec2(0, 0);
                        }
                    }

                    currentGroup.add(new tri(
                        verts[0], verts[1], verts[2],
                        texs[0], texs[1], texs[2]
                    ));
                    break;

                case "o": case "g":
                    if (!currentGroup.isEmpty()) {
                        meshGroups.add(currentGroup);
                        currentGroup = new java.util.ArrayList<>();
                    }
                    break;
            }
        }

        if (!currentGroup.isEmpty()) {
            meshGroups.add(currentGroup);
        }

        tri[][] result = new tri[meshGroups.size()][];
        for (int i = 0; i < meshGroups.size(); i++) {
            result[i] = meshGroups.get(i).toArray(new tri[0]);
        }

        return result;
    }
}


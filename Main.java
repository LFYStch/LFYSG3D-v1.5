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
        cam = new vec3(0, 0, -10, 0, 0);
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
        drawMesh(sp.LFYS(0,0,1,0,i,i),g2d,texture1);
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

            double avgZ = (t.v1.z + t.v2.z + t.v3.z) / 3.0;
if (avgZ < light_source1.z)
 {
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
    public vec3 copy() {
    return new vec3(this.x, this.y, this.z, this.u, this.v);
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



class spawner {
    Objloader loader = new Objloader();

    
    public mesh LFYS(double x, double y, double z, int aI, double theta, double psi) {
    
    GameObject LFYS = new GameObject(new mesh[]{
        loader.load("Cube.obj",x,y,z)
    }, new AABB(new vec3(0, 0, 0, 0, 0), new vec3(0, 0, 0, 0, 0)), theta, psi, x, y, z);
    return LFYS.getMesh(aI);
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
    double theta,cx,cz,cy,psi;
  class GameObject {
    mesh[] anims;
    AABB hitbox;
    double theta, cx, cy, cz, psi;

    public GameObject(mesh[] anims, AABB hitbox, double theta, double psi, double cx, double cy, double cz) {
        this.anims = anims;
        this.hitbox = hitbox;
        this.theta = theta;
        this.psi = psi;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
    }

    public mesh getMesh(int AnimIndex) {
        mesh lfys = anims[AnimIndex];

        for (tri[] row : lfys.tris) {
            for (tri t : row) {
                for (vec3 v : new vec3[]{t.v1, t.v2, t.v3}) {
                    // Translate to origin
                    double x = v.x - cx;
                    double y = v.y - cy;
                    double z = v.z - cz;

                    // Y-axis rotation (theta)
                    double x1 = x * Math.cos(theta) - z * Math.sin(theta);
                    double z1 = x * Math.sin(theta) + z * Math.cos(theta);

                    // X-axis rotation (psi)
                    double y2 = y * Math.cos(psi) - z1 * Math.sin(psi);
                    double z2 = y * Math.sin(psi) + z1 * Math.cos(psi);

                    // Translate back
                    v.x = x1 + cx;
                    v.y = y2 + cy;
                    v.z = z2 + cz;
                }
            }
        }

        return lfys;
    }
}

class Objloader {
    public mesh load(String path, double offsetX, double offsetY, double offsetZ) {
        java.util.List<vec3> vertices = new java.util.ArrayList<>();
        java.util.List<vec2> uvs = new java.util.ArrayList<>();
        java.util.List<tri> triangles = new java.util.ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "v": {
                        double x = Double.parseDouble(parts[1]) + offsetX;
                        double y = Double.parseDouble(parts[2]) + offsetY;
                        double z = Double.parseDouble(parts[3]) + offsetZ;
                        vertices.add(new vec3(x, y, z, 0, 0));
                        break;
                    }

                    case "vt": {
                        double u = Double.parseDouble(parts[1]);
                        double v = 1.0 - Double.parseDouble(parts[2]); // Flip V if needed
                        uvs.add(new vec2(u, v));
                        break;
                    }

                    case "f": {
                        vec3[] faceVerts = new vec3[3];
                        for (int i = 0; i < 3; i++) {
                            String[] tokens = parts[i + 1].split("/");
                            int vIdx = Integer.parseInt(tokens[0]) - 1;
                            int uvIdx = tokens.length > 1 ? Integer.parseInt(tokens[1]) - 1 : 0;

                            vec3 base = vertices.get(vIdx);
                            vec3 copy = base.copy();

                            if (!uvs.isEmpty()) {
                                vec2 uv = uvs.get(uvIdx);
                                copy.u = uv.x;
                                copy.v = uv.y;
                            }

                            faceVerts[i] = copy;
                        }

                        triangles.add(new tri(faceVerts[0], faceVerts[1], faceVerts[2]));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("OBJ load failed: " + e.getMessage());
        }

        return new mesh(new tri[][] { triangles.toArray(new tri[0]) });
    }
}

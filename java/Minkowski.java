/**
 * @(#) Minkowski.java
 */

import java.util.*;
import java.awt.*;
import java.awt.image.*;

/*

    Class that computes a minkowski sum from game map
    and a 2D circle.

 */

public class Minkowski {

    static boolean    SHOW_MAP = false; // change this to draw the pixels

    // the pixel map
    private int        m_sx = 0, m_sy = 0; // map size width, height
    BufferedImage      m_img = null;

    public Minkowski(){}

    // build from map
    public void build(World world, float radius)
    {
        // save bounds
        int sz[] = world.getBounds();
        m_sx = sz[0]; m_sy = sz[1];
        System.out.println("Building Minkowski sums for: "+m_sx+"x"+m_sy);

        // generate map
        m_img = new BufferedImage(m_sx, m_sy, BufferedImage.TYPE_INT_ARGB);
        int transp[] = new int[m_sy*m_sx];
        int val = (new Color(0,0,0,0)).getRGB(); // fill with transparent pixels
        for(int j=0; j<m_sy*m_sx; j++) transp[j] = val;
        m_img.setRGB(0, 0, m_sx, m_sy, transp, 0, m_sx);

        Graphics g = m_img.getGraphics();
        g.setColor(new Color(0xFF,0,0,0xFF));

        // build 
        for(int i=0; i<world.getNumWalls(); i++)
        {
            Wall w = world.getWall(i);

            // 1st
            // saving vectors start/end coords for easy writting
            float xs = w.getStart().x;
            float ys = w.getStart().y;
            float xe = w.getEnd().x;
            float ye = w.getEnd().y;

            float dx = xs - xe;
            float dy = ys - ye;

            // first point
            float x11 = xs - dy;
            float y11 = ys - dx;
            
            // vector normalization & lengthening to radius
            float dist = (float)Math.sqrt((xs-x11)*(xs-x11)+(ys-y11)*(ys-y11));
            float m = radius / dist;
            dx *= m;
            dy *= m;

            x11 = xs - dy;
            y11 = ys + dx;
            float x12 = xs + dy;
            float y12 = ys - dx;

            // 2nd
            dx = xe - xs;
            dy = ye - ys;

            float x21 = xe + dy;
            float y21 = ye + dx;
            dist = (float)Math.sqrt((xe-x21)*(xe-x21)+(ye-y21)*(ye-y21));
            m = radius / dist;

            dx *= m;
            dy *= m;


            x21 = xe - dy;
            y21 = ye + dx;

            float x22 = xe + dy;
            float y22 = ye - dx;

            int xa[] = { (int)x11, (int)x12, (int)x21, (int)x22 };
            int ya[] = { (int)y11, (int)y12, (int)y21, (int)y22 };
            g.fillPolygon(xa, ya, 4);
            //g.fillRect((int)x1, (int)x1, (int)x2, (int)y2);
            g.fillOval((int)(w.getStart().x-radius), (int)(w.getStart().y-radius),
                    (int)radius*2, (int)radius*2);
            

            
        }
    }
    
    // gets the current state of the pixel
    private boolean get(int x, int y){
        if(m_img == null) return false;
        
        // outside pixels are always in the "bad" area
        if(x <= 0 || y <= 0) return true;
        if(x >= m_sx || y >= m_sy) return true;

        Color c = new Color(m_img.getRGB(x, y));
        return (c.getRed() == 0xFF && c.getAlpha() == 0xFF);
    }

    // returns true if a given point is inside the minkowski area
    public boolean isInside(vector2f pos)
    {
        if(m_img == null) return false;
        int px = (int)pos.x;
        int py = (int)pos.y;

        // outside of map - always is in the bad area
        if(px <= 0 || py <= 0) return true;
        if(px >= m_sx || py >= m_sy) return true;

        return get(px, py);
    }

    // call this to draw the map
    public void visualize(Graphics2D painter)
    {
        if(SHOW_MAP == false) return;
        
        painter.drawImage(m_img, 0, 0, null);
    }
}

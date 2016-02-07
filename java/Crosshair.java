/**
 * @(#) Crosshair.java
 */

import java.awt.*;


public class Crosshair {

    static int RADIUS = 5;
    static int HAIR_SZ = 8; // cross size, must be >= RADIUS

    private vector2f    m_pos = new vector2f(0,0);

    public Crosshair(){
    }

    public void update(int mouseX, int mouseY)
    {
        // updates pos only
        m_pos = new vector2f(mouseX, mouseY);
    }

    public void draw(Graphics2D painter)
    {
        Color oldC = painter.getColor();
        painter.setColor(Color.RED);
        painter.setStroke(new BasicStroke(1.0f));
        painter.drawOval((int)(m_pos.x-RADIUS), (int)(m_pos.y-RADIUS), 2*RADIUS, 2*RADIUS);
        painter.drawLine((int)m_pos.x, (int)(m_pos.y-HAIR_SZ), (int)m_pos.x, (int)(m_pos.y+HAIR_SZ));
        painter.drawLine((int)(m_pos.x-HAIR_SZ), (int)m_pos.y, (int)(m_pos.x+HAIR_SZ), (int)m_pos.y);
        painter.setColor(oldC);
    }


}

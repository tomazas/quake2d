/**
 * @(#) Bullet.java
 */

import java.util.*;
import java.awt.*;

public class Bullet {

    static int   SIZE = 2;        // draw size in pixels
    static float SPEED = 80.0f;   // in pixels/second
    static int   DAMAGE = 20;     // damage done to players
    static float LIFETIME = 5.0f; // in seconds

    private Player      m_owner = null;
    private vector2f    m_pos = new vector2f();
    private vector2f    m_vel = new vector2f();
    private float       m_time = 0.0f;

    public Bullet(Player owner, vector2f pos, vector2f vel){
        m_owner = owner;
        m_pos = pos;
        m_vel = vel;
    }

    public vector2f getPos(){
        return m_pos;
    }

    public vector2f getVel(){
        return m_vel;
    }

    public Player getOwner(){
        return m_owner;
    }

    public boolean isDead(){
        return (m_time >= LIFETIME);
    }

    public void draw(Graphics2D painter)
    {
        painter.setColor(Color.BLACK);
        painter.fillOval((int)(m_pos.x-SIZE), (int)(m_pos.y-SIZE),
                SIZE*2, SIZE*2);
    }

    public void update(float delta)
    {
        m_time += delta;
        m_pos.self_add(m_vel.mul(delta));
    }

}

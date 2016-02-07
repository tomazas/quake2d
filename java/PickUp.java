/**
 * @(#) PickUp.java
 */

import java.awt.*;

public class PickUp
{
    static String   TYPE_AMMO = "Ammo";
    static String   TYPE_HEALTH = "Health";
    
    static int      DRAW_SIZE = 12; // in pixels
    static float    REGEN_RATE = 20.0f; // appear time in seconds after pick

    private String   m_type;
    private int      m_amount = 0;
    private vector2f m_pos = new vector2f();
    private float    m_time = 0.0f;

    private float m_pickTime = -1.0f; // -1 = not set

    public PickUp(vector2f pos, String type, int amount)
    {
        m_type = type;
        m_pos = pos;
        m_amount = amount;
    }

    public PickUp() {}

    public int getAmount( )
    {
        return m_amount;
    }

    public void setAmount( int num )
    {
        m_amount = num;
    }

    public String getType( )
    {
        return m_type;
    }

    public vector2f getPos( )
    {
        return m_pos;
    }

    // needs to be updated to regenerate this item if picked
    public void update( float delta_time )
    {
        m_time += delta_time;
        if( m_pickTime < 0.0f ) return; // not picked
       
        if( m_time - m_pickTime >= REGEN_RATE)
            m_pickTime = -1.0f; // not picked anymore
    }

    // repaint
    public void draw(Graphics2D painter)
    {
        if(this.isPicked()) return; // dont draw if is picked

        int x = (int)m_pos.x;
        int y = (int)m_pos.y;
        painter.drawRect(x-DRAW_SIZE/2, y-DRAW_SIZE/2,
                DRAW_SIZE, DRAW_SIZE);

        if ( m_type.compareTo(TYPE_HEALTH) == 0 ){
            painter.setColor(Color.RED);
            painter.drawLine(x, y-4, x, y+4 );
            painter.drawLine(x-4, y, x+4, y );
            painter.drawString( Integer.toString(m_amount),
                    x+DRAW_SIZE/2+2, y+DRAW_SIZE/2+2);
            painter.setColor(Color.BLACK);
        }else if( m_type.compareTo(TYPE_AMMO) == 0 ){
            painter.drawOval(x-DRAW_SIZE/4, y-DRAW_SIZE/4, DRAW_SIZE/2, DRAW_SIZE/2);
            painter.setColor(Color.RED);
            painter.drawString( Integer.toString(m_amount),
                    x+DRAW_SIZE/2+2, y+DRAW_SIZE/2+2);
            painter.setColor(Color.BLACK);
        }
    }

    // return true if this item is picked
    public boolean isPicked( )
    {
        return (m_pickTime > 0.0f);
    }

    // picks up the item, starts regeneration sequence
    public void pick( )
    {
        m_pickTime = m_time;//System.currentTimeMillis();
    }
}

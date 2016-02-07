
import java.awt.*;

public class InfoGfx {

    static float     LIFETIME = 3.0f;
    static float     RISE_SPEED = 5.0f;

    private float    m_time = 0.0f;
    private float    m_alpha = 1.0f;
    private String   m_text = "";
    private Color    m_color = Color.BLACK;
    private vector2f m_pos;

    public InfoGfx(vector2f pos, String text, Color color){
        m_text = text;
        m_color = color;
        m_pos = pos;
    }

    public void update(float delta){
        m_time += delta;
        m_pos.y -= delta * RISE_SPEED; // float up

        // recalc alpha
        m_alpha = (LIFETIME - m_time)/LIFETIME;
        if(m_alpha < 0.0f) m_alpha = 0.0f;
    }

    public boolean isDead(){
        return (m_time >= LIFETIME);
    }

    public void draw(Graphics2D painter)
    {
        Color oldColor = painter.getColor();
        m_color = new Color(m_color.getRed(), m_color.getGreen(), m_color.getBlue(), (int)(m_alpha * 0xFF));
        painter.setColor(m_color);
        painter.drawBytes(m_text.getBytes(), 0, m_text.length(), (int)m_pos.x, (int)m_pos.y);
        painter.setColor(oldColor);
    }
}

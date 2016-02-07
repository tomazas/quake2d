/**
 * @(#) Wall.java
 */
import java.awt.*;

public class Wall
{
	private vector2f m_start;
	private vector2f m_end;

        public Wall(vector2f start, vector2f end)
        {
            m_start = start;
            m_end = end;
        }
	
	public vector2f getStart( )
	{
		return m_start;
	}
	
	public vector2f getEnd( )
	{
		return m_end;
	}
	
	public void draw( Graphics2D g )
	{
            g.setStroke(new BasicStroke(3));
            g.drawLine((int)m_start.x, (int)m_start.y, (int)m_end.x, (int)m_end.y);
            g.setStroke(new BasicStroke(1));
	}
	
	
}

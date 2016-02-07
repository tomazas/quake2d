/**
 * @(#) Memory.java
 */

public class Memory
{
        static float      REMEMBER_TIME = 10.0f; // in seconds

	private vector2f  m_lastPos;
	private String    m_enemyName;
	private float     m_seenTime;
	private vector2f  m_lastVel;
	
	public Memory( vector2f pos, vector2f vel, String name, float time )
	{
            m_lastPos = pos;
            m_lastVel = vel;
            m_enemyName = name;
            m_seenTime = time;
	}
	
	public vector2f getLastPos( )
	{
            return m_lastPos;
	}
	
	public vector2f getVel( )
	{
            return m_lastVel;
	}
	
	public String getName( )
	{
            return m_enemyName;
	}
	
	public float getTime( )
	{
            return m_seenTime;
	}

        public void update( vector2f pos, vector2f vel, String name, float time )
	{
            m_lastPos = pos;
            m_lastVel = vel;
            m_enemyName = name;
            m_seenTime = time;
	}

        // returns true if this memory is old than a bot can remember
        public boolean isOld(float currentTime)
        {
            return (currentTime - m_seenTime > REMEMBER_TIME);
        }
	
	
}

/**
 * @(#) Player.java
 */

import java.awt.*;

public class Player
{
        static int    BOT_RADIUS = 6;   // graphics size
        static int    MOVE_SPEED = 30;  // pixels/second
        static float  BOT_FOV = 120;    // field of vision in degrees
        static float  MAX_VISION = 150; // from current pos to farthest target in pixels
        static float  HEAR_RADIUS = 10; // bot can hear sounds!
        static int    HEALTH_MAX = 100;
        static int    START_BULLETS = 20;
        static float  POINTER_SZ = 12.0f; // gun line size
        static String HUMAN_ID = "User";


        // *** NOTE: dont't forget to RESET variables in reset() ***
	protected int       m_health = HEALTH_MAX;
	protected Weapon    m_currWeapon = new Pistol(START_BULLETS);
	protected vector2f  m_pos = new vector2f();
	protected vector2f  m_velocity = new vector2f();
        protected vector2f  m_viewDir = new vector2f(0,-1); // y axis up, must be normalized!
        protected String    m_name = "undefined";
        protected Player    m_lastAttacker = null;
        protected float     m_time = 0.0f;
        protected float     m_speedBoost = 0.0f;

        // NOTE: human player needs to be created with name HUMAN_ID
        public Player(String name){
            m_name = name;
        }

	public Player( String name, vector2f pos )
	{
            m_name = name;
            m_pos = pos;
	}

        // resets all gameplay variables to starting state
        public void reset()
        {
            m_health = HEALTH_MAX;
            m_currWeapon = new Pistol(START_BULLETS);
            m_viewDir = new vector2f(0,-1);
            m_lastAttacker = null;
            m_time = 0.0f;
            m_speedBoost = 0.0f;
        }
	
        // inflicts damage
	public void dealDamage( Player attacker, int amount )
	{
            m_lastAttacker = attacker;
            m_health -= amount;
            if(m_health > HEALTH_MAX) m_health = HEALTH_MAX;
            else if(m_health < 0) m_health = 0;
	}

        // heals wounds
	public void heal( int amount )
	{
            m_health += amount;
            if(m_health > HEALTH_MAX) m_health = HEALTH_MAX;
            else if(m_health < 0) m_health = 0;
	}

        // sets additional speed increase
        public void setSpeedBoost(float value){
            m_speedBoost = value;
        }

        // returns the viewing direction vector - always normalized!
        public vector2f getView(){
            return m_viewDir;
        }

        // set a new viewing direction
        public void setView(vector2f dir){
            m_viewDir = dir.normalized();
        }

        public int getHealth(){
            return m_health;
        }

        public int getAmmo(){
            return m_currWeapon.getAmmo();
        }
	
	public boolean isDead( )
	{
            return (m_health == 0);
	}

        public boolean isHuman()
        {
            return (m_name.compareTo(HUMAN_ID) == 0);
        }

        public Player getLastAttacker(){
            return m_lastAttacker;
        }

        // approximate next position after time delta
        public vector2f approxNextPos(float delta)
        {
            return m_pos.add(this.m_velocity.mul(delta));
        }

        // updates state & movement
	public void update( World world , float delta)
	{
            m_time += delta;
            m_pos.self_add( m_velocity.mul(delta) );
	}

	
	public void draw( Graphics2D painter, Color body, boolean drawInfo)
	{
            // view vector
            painter.setColor(Color.GREEN);
            painter.setStroke(new BasicStroke(1.5f));
            painter.drawLine((int)m_pos.x, (int)m_pos.y,
                    (int)(m_pos.x+m_viewDir.x*POINTER_SZ),
                    (int)(m_pos.y+m_viewDir.y*POINTER_SZ));
            painter.setStroke(new BasicStroke(1.0f));

            // draw visibility cone
            if(drawInfo){
                painter.setColor(Color.ORANGE);
                float angle = (float)Math.atan2(m_viewDir.y, m_viewDir.x);
                float rads = (float)Math.toRadians(BOT_FOV/2);
                vector2f ng = new vector2f((float)Math.cos(angle-rads), (float)Math.sin(angle-rads));
                vector2f ps = new vector2f((float)Math.cos(angle+rads), (float)Math.sin(angle+rads));

                painter.drawLine((int)m_pos.x, (int)m_pos.y,
                        (int)(m_pos.x+ng.x*POINTER_SZ*2),
                        (int)(m_pos.y+ng.y*POINTER_SZ*2));
                painter.drawLine((int)m_pos.x, (int)m_pos.y,
                        (int)(m_pos.x+ps.x*POINTER_SZ*2),
                        (int)(m_pos.y+ps.y*POINTER_SZ*2));
            }


            // draw body
            painter.setColor(body);
            painter.fillOval((int)m_pos.x-BOT_RADIUS, (int)m_pos.y-BOT_RADIUS,
                        BOT_RADIUS*2, BOT_RADIUS*2);

            painter.setColor(Color.BLACK);
	}

        public void setWeapon(Weapon wp){
            m_currWeapon = wp;
        }

        public Weapon getWeapon(){
            return m_currWeapon;
        }
	
	public vector2f getPos( )
	{
            return m_pos;
	}
	
	public void setPos( vector2f pos)
	{
            m_pos = pos;
	}
	
	public vector2f getVelocity( )
	{
            return m_velocity;
	}
	
	public void setVelocity( vector2f vel )
	{
            m_velocity = vel;
	}

        public String getName(){
            return m_name;
        }

        public void move(vector2f dir, float delta){
            m_pos.self_add(dir.mul(delta));
        }
	
	
}

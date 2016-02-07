/**
 * @(#) Weapon.java
 */

/*
    Abstract class
 */

public class Weapon
{
    protected float  m_lastShotTime = -1.0f;
    protected int    m_numBullets = 0;

    // no more constructors allowed!
    public Weapon(int startingAmmo){
        setAmmo(startingAmmo);
    }

    public boolean canShoot( float time )
    {
        return false;// does nothing
    }

    public void shoot( float time )
    {
        m_lastShotTime = time;
        m_numBullets -= 1;
        if(m_numBullets < 0) m_numBullets = 0;
    }

    public int getAmmo( )
    {
        return m_numBullets;
    }

    public void setAmmo( int amount )
    {
        m_numBullets = amount;
    }

    public void addAmmo( int amount ){
        m_numBullets += amount;
    }
        
}

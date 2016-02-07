/**
 * @(#) Pistol.java
 */

/*
 Shoots one bullet at a time.
*/
public class Pistol extends Weapon
{
    static float RELOAD_TIME = 2.0f; // in seconds

    public Pistol(int bullets){
        super(bullets);
    }

    // returns true if this weapon is loaded and ready to shoot
    public boolean canShoot( float time )
    {
        if(this.m_lastShotTime < 0.0f) return true;
        
        boolean reloadDone = (time - this.m_lastShotTime) > RELOAD_TIME;
        boolean hasBullets = this.m_numBullets > 0;
        
        return reloadDone && hasBullets;
    }

}

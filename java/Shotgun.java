/**
 * @(#) Shotgun.java
 */

/*
    Shoots a burst of shells in a field - huge damage when close by.
 */
public class Shotgun extends Weapon
{
        public Shotgun(int bullets){
            super(bullets);
        }

	public boolean canShoot( )
	{
		return false;
	}
	
	public void shootTarget( Player target )
	{
		
	}
	
	
}

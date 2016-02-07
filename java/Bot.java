/**
 * @(#) Bot.java
 */
import java.util.*;
import java.awt.*;

public class Bot extends Player
{
        static int   MAX_MEMORY = 5;   // memory available to bot - const!

        // *** NOTE: dont't forget to RESET variables in reset() ***
        private int                 m_prevHealth = 0; // to be able to determine if the bot was hit
	private BotState            m_currState = null;
	//private vector2f            m_netForce = new vector2f();
	private Path                m_currPath = null; // the path the bot is now on
        private Node                m_pathNode = null; // for pathfinding
        private World               m_world = null; // the world that belongs to
        
	private ArrayList<Memory>   m_memory = new ArrayList<Memory>(MAX_MEMORY);
	
	public Bot( World world, String name, vector2f pos )
	{
            super(name, pos);
            m_name = name;
            m_pos = pos;
            m_world = world;
            this.changeState(new BS_Wander()); // default is wander state
	}

        public void reset()
        {
            super.reset();
            m_prevHealth = 0;
            this.changeState(new BS_Wander()); // default is wander state
            m_currPath = null;
            m_pathNode = null;
            m_memory = new ArrayList<Memory>(MAX_MEMORY);
        }

        // draw Bot graphics
	public void draw( Graphics2D painter, Color body, boolean drawPath , boolean drawInfo)
	{
            // draw path
            if(m_currPath != null && drawPath)
                m_currPath.draw(painter);
           
            // view vector
            painter.setColor(Color.BLUE);
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

            if(drawInfo) // draw state info character
                painter.drawString(m_currState.getType(),
                        m_pos.x-BOT_RADIUS+2, m_pos.y+BOT_RADIUS-2);
	}

        // change Bot state
	public void changeState( BotState newState )
	{
            if(m_currState != null) 
                m_currState.onExit(null, this);
            m_currState = newState;
            m_currState.onEnter(m_world, this);
	}


        // updates Bot state
	public void update( World world , float delta)
	{
            m_time += delta;
            
            // run state
            if(m_currState != null)
                m_currState.onExecute(world, this);

            this.m_velocity = new vector2f(0,0); // reset velocity
            
            if(m_currPath == null) return;
            if(this.isPathDone()) return;

            // choose next target based on previous path done
            Node target = null;
            if(m_pathNode == null){
                target = m_currPath.getNode(0);//m_currPath.getClosest(m_pos);
            }else{
                target = m_currPath.getNext(m_pathNode);
            }

            vector2f moveTo = (target != null) ? target.getPos() : m_currPath.getEndPos();

            vector2f dir = moveTo.sub(m_pos);
            float dist = dir.length();

            if(dist > BOT_RADIUS){ // move
               dir.self_normalize();
               this.m_velocity = dir;
               
               m_pos.self_add( dir.mul(delta * (MOVE_SPEED + m_speedBoost)) );

               // change view direction!
               //this.targetView(moveTo, 0.5f);
            }else{
                m_pathNode = target; // go to next node
            }

            // save health for hit testing
            this.m_prevHealth = m_health;
	}


        // rotate bots view to the specified target at a given speed
        // here 'speed' = [0.0; 1.0]
        public void targetView(vector2f target, float speed){
            vector2f dir = target.sub(m_pos);
            float mag = dir.self_normalize();
            if(mag <= vector2f.EPS) return; //target is at pos
            
            if(speed > 1.0f) speed = 1.0f;
            else if(speed < 0.0f) speed = 0.0f;

            float next_angle = (float)Math.atan2(dir.y, dir.x);
            float curr_angle = (float)Math.atan2(m_viewDir.y, m_viewDir.x);
            float angle = curr_angle + (next_angle-curr_angle)*speed;

            m_viewDir = new vector2f((float)Math.cos(angle), (float)Math.sin(angle));
        }

        // tells the bot to try and attack the enemy
        // approximates target movement and shoots
        public void attack(World world, Player enemy){

            if(m_currWeapon.canShoot( m_time ) == false) return;

            // distance to target
            float dist = enemy.getPos().sub(m_pos).length();
            float bullet_time = dist / Bullet.SPEED; //avg time for the bullet to reach target

            // approximate next target position
            vector2f nextPos = enemy.approxNextPos(bullet_time);

            // look to the target when shooting
            this.targetView(nextPos, 1.0f);

            // shoot bullet to the new direction
            vector2f dir = nextPos.sub(m_pos);
            dir.self_normalize();

            // shoot from the gun endpoint
            vector2f emitPos = m_pos.add(m_viewDir.mul(POINTER_SZ+Bullet.SIZE));
            m_currWeapon.shoot( m_time );
            world.addBullet(new Bullet(this, emitPos, dir.mul(Bullet.SPEED)));
        }

        // tells if the bot is on a path
        public boolean hasPath()
        {
            return (m_currPath != null);
        }

        // returns true if the bot was hit previous frame
        public boolean wasShot(){
            return (m_prevHealth > m_health);
        }

        // directly set bot path
        public void setPath(Path newPath){
            //System.out.println("Bot "+this.getName()+"new path"+newPath.getNumNodes()+" nodes");
            m_currPath = newPath;
            m_pathNode = null;
        }

        // returns true if the Bot has finished moving by it's current path
        public boolean isPathDone(){
            if(m_currPath == null) return true;
            vector2f d = m_currPath.getEndPos().sub(m_pos);
            return d.length() <= BOT_RADIUS;
        }

        // updates all other player memories
        public void updateMemory(ArrayList<Player> enemies)
        {
            int num = enemies.size();
            for(int i=0; i<num; i++)
            {
                Player e = enemies.get(i);
                
                // find memory & update it if available
                int id = -1;
                for(int j=0; j<m_memory.size(); j++){
                    if(m_memory.get(j).getName().compareTo(e.getName()) == 0)
                    {
                        id = j;
                        break;
                    }
                }


                if(id == -1){
                   // add new
                   m_memory.add(new Memory(e.getPos(),
                               e.getVelocity(),
                               e.getName(),
                               m_time)
                           );
                }
                else{
                    // update old
                    Memory mem = m_memory.get(id);
                    mem.update(e.getPos(),
                               e.getVelocity(),
                               e.getName(),
                               m_time);
                }
            }

            //System.out.println(this.getName()+" has "+m_memory.size()+" memories");
        }

        // removes memory associated with a player
        public void removeMemory(Player player){
            int num = m_memory.size();

            for(int i=0; i<num; i++){
                Memory m = m_memory.get(i);
                if(m.getName().compareTo(player.getName()) == 0){
                    m_memory.remove(i);
                    return;
                }
            }
        }

        // returns the number of records stored in memory
        public int getNumMemory()
        {
            return m_memory.size();
        }

        // returns the stored memory even if it's old
        public Memory getMemory(int n)
        {
            return m_memory.get(n);
        }

        // returns the most fresh memory or null if all memories are old
        public Memory getBestMemory(){

            int num = m_memory.size();
            Memory best = null;

            for(int i=0; i<num; i++){
                Memory m = m_memory.get(i);
                if(m.isOld(m_time)) continue;
                    
                if(best == null){ // first time
                    best = m;
                    continue;
                }

                // choose the most fresh one
                if(best.getTime() < m.getTime())
                    best = m;
            }
            
            return best;
        }
	
}

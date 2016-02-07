/**
 * @(#) BS_Attack.java
 */

import java.util.*;

/*
    Bot Attack state

    Technique:
        1. Bot hunts for enemies - items are lower priority
           If an enemy is closer than an item, goes for the enemy.
        2. Attacks closest enemy it can find. Tries to keep close
           distance with the enemy to maximize damage and accuracy.
        4. Goes to Flee state if:
           a) health falls down below critical
           b) runs out of ammo
        5. if cant see any enemies for MAX_IDLETIME then goes to Wander states
 */
public class BS_Attack extends BotState
{
    
    enum BotAction{
        NONE,
        CHECKING_MEM // checking last known memory of an enemy
    };

    static float  MAX_IDLETIME = 10.0f;  // time before the bot goes to Wander mode
    static int    CRITICAL_HEALTH = Player.HEALTH_MAX/4;   // health limit until fleeing
    static float  MAX_FIGHT_DISTANCE = Bot.MAX_VISION*3/4; // try to keep less than this distance between
    static float  MIN_FIGHT_DISTANCE = Bot.MAX_VISION/3;   // try to keep more than this distance between
    static float  DISTCHECK_INTERVAL = 0.5f; // in seconds
    
    private Random      m_random = new Random();
    private Player      m_target = null;        // the player the bot is chasing
    private BotAction   m_action = BotAction.NONE;
    private float       m_lastSeen = -1.0f;     // time without seeing any enemies -1.0f == visible
    private float       m_lastDistCheck = 0.0f; // last time the distance between bots were checked

    
    // invoked when this state is created
    public void onEnter( World world, Bot bot)
    {
        System.out.println("Bot "+bot.getName()+" entered Attack state");
    }

     // invoked when updating Bot
    public void onExecute( World world, Bot bot )
    {
        Graph g = world.getGraph();
        if(g == null) return;

        //----------------------------------------------------
        // option 4: flee
        //----------------------------------------------------
        if(bot.getHealth() <= CRITICAL_HEALTH || bot.getAmmo() == 0){
            bot.changeState(new BS_Flee());
            return;
        }

        // move path finished?
        boolean walkIsDone = bot.hasPath() == false || bot.isPathDone();
        vector2f moveTo = null;

        //----------------------------------------------------
        // 1-2 option: attack closest visible enemy
        //----------------------------------------------------
        boolean memoryCheck = false;

        ArrayList<Player> enemies = world.getEnemiesVisible(bot);
        if(enemies != null && enemies.size() > 0)
        {
            // some enemies visible
            
            bot.updateMemory(enemies);

            // attack the closest one
            Player closest = world.getClosestEnemy(enemies, bot);
            bot.attack(world, closest);
            m_target = closest;
            m_lastSeen = -1.0f; // not set
        }else{
            //no enemies around, check memory for some clues
            m_target = null;

            Memory m = bot.getBestMemory();
            if(m != null && m_action != BotAction.CHECKING_MEM)
            {
                // got check out the last know position of the enemy
                moveTo = m.getLastPos();
                walkIsDone = true;
                System.out.println("Retrieving memory");
                memoryCheck = true; // tell to start the check
            }

            //----------------------------------------------------
            // 5 option: goto Wander state if there's no fighting action
            //----------------------------------------------------
            if(m_lastSeen < 0.0f){
                m_lastSeen = world.getTime(); // enemies started to hide
            }else if(world.getTime() - m_lastSeen >= MAX_IDLETIME){
                bot.changeState(new BS_Wander()); // too boring -  no action ...
                return;
            }
         }
            
        boolean targetIsValid = m_target != null && m_target.isDead() == false;
        
        if(targetIsValid){
            
            // we have a target already
            bot.targetView(m_target.getPos(), 0.5f); // always face the target

            // keep a good fighting distance to the target
            if(world.getTime() - m_lastDistCheck > DISTCHECK_INTERVAL)
            {
                m_lastDistCheck = world.getTime();
                
                float dist = m_target.getPos().sub(bot.getPos()).length();
                
                if(dist > MAX_FIGHT_DISTANCE){
                    vector2f toTarget = m_target.getPos().sub(bot.getPos());
                    toTarget.self_normalize();
                    
                    // try to chase the enemy
                    float offset = dist - MAX_FIGHT_DISTANCE;
                    moveTo = bot.getPos().add( toTarget.mul(offset) );
                    walkIsDone = true;
                    
                }else if(dist < MIN_FIGHT_DISTANCE){
                    vector2f toTarget = m_target.getPos().sub(bot.getPos());
                    toTarget.self_normalize();

                    // try to get back a bit from the enemy
                    float offset = MIN_FIGHT_DISTANCE - dist;
                    moveTo = bot.getPos().add( toTarget.mul(-offset) );
                    walkIsDone = true;
                }
            }

        }
        

        //----------------------------------------------------
        // movement
        //----------------------------------------------------
        if(walkIsDone)
        {
            vector2f gotoPos = null;

            // we reached the memory location - nothing found?
            if(m_action == BotAction.CHECKING_MEM)
                m_action = BotAction.NONE;

            if(moveTo != null){
                // bot needs to move somewhere other
                Node n = g.closestToPoint(moveTo);
                if(n != null) gotoPos = n.getPos().copy();
                
                if(memoryCheck) m_action = BotAction.CHECKING_MEM;
            }

            if(gotoPos == null){
                // choose new path by choosing random graph nodes
                int gotoId = m_random.nextInt(g.getNumNodes());
                gotoPos = g.getNode(gotoId).getPos().copy();
            }

            Path path = g.findPath(bot.getPos(), gotoPos);
            if(path != null) {
                //path.smooth(this);
                bot.setPath(path);
            }
        }else{
            // DO NOTHING

            // look where you move
            if(m_target == null)
                bot.targetView(bot.getPos().add(bot.getVelocity()), 0.5f);
        }
    }

    // invoked when this state is about to be destroyed
    public void onExit( World world, Bot bot)
    {

    }

    String getType(){
        return "a";
    }
}

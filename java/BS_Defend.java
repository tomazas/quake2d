/**
 * @(#) BS_Defend.java
 */

import java.util.*;

/*
    Bot Defend state

    Technique:
        1. Bot searches for items - health items are greater priority
        2. Bot avoids stronger enemies
            a) if attacked, respond, but keep distance from attacker, try to run away
            b) if doesn't have bullets - goes to the Flee state
        3. If health regenerates to the normal amount, goes to the Wander state
 */
public class BS_Defend extends BotState
{
    enum BotAction{
        NONE,
        TO_PICKUP
    };

    static int   NORMAL_HEALTH = Player.HEALTH_MAX/2;
    static float DISTCHECK_INTERVAL = BS_Attack.DISTCHECK_INTERVAL;
    static float MIN_FIGHT_DISTANCE = BS_Attack.MIN_FIGHT_DISTANCE;

    private Player      m_target = null;
    private float       m_lastDistCheck = 0.0f;
    private Random      m_random = new Random();
    private BotAction   m_action = BotAction.NONE;
    private PickUp      m_desiredItem = null;
    

    // invoked when this state is created
    public void onEnter( World world, Bot bot)
    {
        System.out.println("Bot "+bot.getName()+" entered Defend state");
    }
    
    // invoked when updating Bot
    public void onExecute( World world, Bot bot )
    {
        Graph g = world.getGraph();
        if(g == null) return;
         
        // ------------------------------------------------
        // option 2b. doesn't have bullets - goes to the Flee state
        // ------------------------------------------------
        if(bot.getWeapon().getAmmo() == 0){
            bot.changeState(new BS_Flee());
            return;
        }

        // ------------------------------------------------
        // option 3. health regenerates to the normal amount, goes to the Wander state
        // ------------------------------------------------
        if(bot.getHealth() >= NORMAL_HEALTH){
            bot.changeState(new BS_Wander());
            return;
        }

        // ------------------------------------------------
        // option 2a. attacked, respond, but keep distance to attacker, try to run away
        // ------------------------------------------------
        ArrayList<Player> enemies = world.getEnemiesVisible(bot);
        bot.updateMemory(enemies);

        // we have lost sight of the target
        if(m_target != null && enemies.contains(m_target) == false)
            m_target = null;

        boolean enemiesVisible = enemies != null && enemies.size() > 0;

        if(bot.wasShot() && m_target == null){
            
            Player attacker = bot.getLastAttacker();
            if(attacker != null){
                bot.attack(world, attacker);
                m_target = attacker;
            }
        }
        
        if(enemiesVisible)
        {
            // attack the closest one
            Player closest = world.getClosestEnemy(enemies, bot);
            if(closest != null){
                bot.attack(world, closest);
                m_target = closest;
            }
        }

        vector2f moveTo = null;
        boolean walkIsDone = bot.isPathDone() || bot.hasPath() == false;
        boolean targetIsValid = m_target != null && m_target.isDead() == false;
        
        if(targetIsValid){

            // we have a target already
            bot.targetView(m_target.getPos(), 0.5f); // always face the target

            // keep a good fighting distance to the target or just run away
            if(world.getTime() - m_lastDistCheck > DISTCHECK_INTERVAL)
            {
                m_lastDistCheck = world.getTime();
                float dist = m_target.getPos().sub(bot.getPos()).length();

                if(dist < MIN_FIGHT_DISTANCE){
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
        // 1. Bot searches for items
        //----------------------------------------------------
        
        // try to find pickups if no enemies are around
        if(m_action != BotAction.TO_PICKUP){
            
            PickUp pick = world.getClosestVisiblePickup(bot);
            if(pick != null){
                System.out.println("going for pickup");
                m_action = BotAction.TO_PICKUP;
                moveTo = pick.getPos();
                m_desiredItem = pick;
                walkIsDone = true;
            }
        }else{
            // check if we/or someone else picked the item
            if(m_desiredItem.isPicked()){
                m_action = BotAction.NONE;
                walkIsDone = true;
            }
        }

        //----------------------------------------------------
        // movement
        //----------------------------------------------------
        if(walkIsDone)
        {
            vector2f gotoPos = null;

            if(moveTo != null){
                // bot needs to move somewhere other
                Node n = g.closestToPoint(moveTo);
                if(n != null) gotoPos = n.getPos().copy();
            }

            if(gotoPos == null){
                //System.out.println("wandering!");
                // choose new path by choosing random graph nodes
                int gotoId = m_random.nextInt(g.getNumNodes());
                gotoPos = g.getNode(gotoId).getPos().copy();
            }

            Path path = g.findPath(bot.getPos(), gotoPos);
            if(path != null) bot.setPath(path);
        }else{
            // DO NOTHING

            // look where you move
            if(m_target == null)
                bot.targetView(bot.getPos().add(bot.getVelocity()), 0.5f);
        }


    }

    // invoked when this state is about to be destroyed
    public void onExit( World world, Bot bot )
    {

    }

    String getType(){
        return "d";
    }
}

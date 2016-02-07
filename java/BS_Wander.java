/**
 * @(#) BS_Wander.java
 */

import java.util.*;

/*
    Bot Wander state

    Technique:
        1. Bot moves freely & randomly, gathers any pickups it sees.
        2. If attacked - goes to Defend state.
        3. If has enough ammo and health & sees enemy - goes to attack state.
 */
public class BS_Wander extends BotState
{
    enum BotAction{
        NONE,
        WALK,
        GET_ITEM
    };

    static int          NORMAL_HEALTH = BS_Defend.NORMAL_HEALTH;

    private Random      m_random = new Random();
    private BotAction   m_action = BotAction.NONE;
    private PickUp      m_desiredItem = null;
    
    // invoked when this state is created
    public void onEnter( World world, Bot bot)
    {
        System.out.println("Bot "+bot.getName()+" entered Wander state");
    }

    // invoked when updating Bot
    public void onExecute( World world, Bot bot )
    {
        Graph g = world.getGraph();
        if(g == null) return;

        //----------------------------------------------------
        // 2 option: someone is attacking - deal with it
        //----------------------------------------------------
        if(bot.wasShot()){
            
            // find the shooter
            Player attacker = bot.getLastAttacker();
            if(attacker != null){
                ArrayList<Player> mem = new ArrayList<Player>();
                mem.add(attacker);
                bot.updateMemory(mem);
            }
            
            bot.changeState(new BS_Defend());
            return;
        }

        // move path finished?
        boolean walkIsDone = bot.hasPath() == false || bot.isPathDone();

        //----------------------------------------------------
        // 1 option: check if pickups are visible then go and fetch closest
        //----------------------------------------------------
        PickUp getItem = null;
        
        // bot is already seeking for an item
        if(m_desiredItem != null && m_desiredItem.isPicked())
        {
            // someone got there first and stole our item or we picked it
            walkIsDone = true;
            m_desiredItem = null;
            m_action = BotAction.NONE;
        }

        // choose next item only if we're not seeking for one
        if(m_action != BotAction.GET_ITEM) 
        {
            getItem = world.getClosestVisiblePickup(bot);
            if(getItem != null) walkIsDone = true;
        }

        //----------------------------------------------------
        // 3 option: attack visible enemy!
        //----------------------------------------------------
        ArrayList<Player> enemies = world.getEnemiesVisible(bot);
        boolean canAttack = bot.getHealth() >= NORMAL_HEALTH && bot.getWeapon().getAmmo() > 0;
        
        if(enemies != null && enemies.size() > 0 && canAttack)
        {
            // update memory of the targets
            bot.updateMemory(enemies);

            // get aggressive
            bot.changeState(new BS_Attack());
            return;
        }


        //----------------------------------------------------
        // movement
        //----------------------------------------------------
        if(walkIsDone){
            
            vector2f gotoPos = null;

            if(getItem != null){ // go for an item that is nearby
                gotoPos = getItem.getPos().copy();
            }else{
                // choose new path by choosing random graph nodes
                int gotoId = m_random.nextInt(g.getNumNodes());
                gotoPos = g.getNode(gotoId).getPos().copy();
            }


            Path path = g.findPath(bot.getPos(), gotoPos);
            if(path != null) {
                //path.smooth(this);

                if(getItem != null){ // fetch item?
                    m_action = BotAction.GET_ITEM;
                    m_desiredItem = getItem;
                }else{
                    m_action = BotAction.WALK;
                }
                
                bot.setPath(path);
            }
        }else{
            // DO NOTHING
            
            // look where you move
            bot.targetView(bot.getPos().add(bot.getVelocity()), 0.5f);
        }

        
    }

    // invoked when this state is about to be destroyed
    public void onExit( World world, Bot bot)
    {

    }

    String getType(){
        return "w";
    }
}

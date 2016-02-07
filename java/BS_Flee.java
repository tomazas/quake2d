
/**
 * @(#) BS_Flee.java
 */


import java.util.*;


/*
    Bot Flee state

    Technique:
        1. Bot tries to run away & hide from attackers
        2. When bot is safe for a given time(SAFE_COOLDOWN),
           then goes to the Defend state
 */

public class BS_Flee extends BotState
{
    enum BotAction{
        NONE,
        RUN_AWAY,
        TO_PICKUP,
        RANDOM
    };

    static float        SAFE_COOLDOWN = 10.0f;  // in seconds
    static int          CRITICAL_HEALTH = BS_Attack.CRITICAL_HEALTH;
    static float        BOT_SPEED_BOOST = 20;   // add this amount to bot's speed
    static float        UPDATE_FREQ = 0.5f;     // bot path recalc time frequency in seconds
    static float        INFLUENCE_SCALE = 1000; // amount to scale bots direction when  dciding where to move
    static float        BOT_TRAPPED = 0.25f;     // this decides if the bot is surrounded by players
    static float        MEMORY_MULT = 0.5f;     // minimum distance the bot has to flee for
    
    private BotAction   m_action = BotAction.NONE;
    private float       m_stateChange = -1.0f;     // variable to hold time until SAFE_COOLDOWN
    private float       m_lastUpdate = 0.0f;       // last check on enemies & path generation
    private Random      m_random = new Random();
   
    
    // invoked when this state is created
    public void onEnter( World world, Bot bot)
    {
        System.out.println("Bot "+bot.getName()+" entered Flee state");
        m_stateChange = world.getTime();
    }

    public void onExecute( World world, Bot bot )
    {

        //----------------------------------------------------
        // 2 option: bot goes back to wander if he's safe or has good health
        //----------------------------------------------------
        boolean cooldownDone = world.getTime() - m_stateChange >= SAFE_COOLDOWN;
        boolean hasHealth = bot.getHealth() >= CRITICAL_HEALTH;
        boolean hasAmmo = bot.getAmmo() > 0;
        if ( cooldownDone && hasAmmo && hasHealth )  {
            bot.changeState(new BS_Defend());
            return;
        }

        Graph g = world.getGraph();
        if(g == null) return;

        vector2f nextPos = null; // next goto position
        
        ArrayList<Player> enemies = world.getEnemiesVisible(bot);
        bot.updateMemory(enemies);
        
        boolean pathDone = bot.hasPath() == false || bot.isPathDone();
        boolean threatExists = enemies.size() > 0 ;//|| bot.getBestMemory() != null;

        //----------------------------------------------------
        // 1 option: Bot tries to run away & hide from attackers
        //----------------------------------------------------
        if( threatExists && world.getTime()-m_lastUpdate > UPDATE_FREQ )
        {
            // we want to run away from all incoming players
            // calculate the sum of all influences of the incoming enemy directions
            vector2f moveDir = new vector2f(0,0);
            for(int i=0; i<enemies.size(); i++){
                vector2f dir = bot.getPos().sub(enemies.get(i).getPos());
                moveDir.self_add( dir.cross() );
            }

//            for(int i=0; i<bot.getNumMemory(); i++){
//                Memory m = bot.getMemory(i);
//                if(m.isOld(world.getTime())) continue;
//
//                // only use those memories, whose players are not visible at the moment
//                boolean influenceUsed = false;
//                for(int j=0; j<enemies.size(); j++){
//                    if(m.getName().compareTo(enemies.get(j).getName()) == 0){
//                        influenceUsed = true;
//                        break;
//                    }
//                }
//
//                if(influenceUsed) continue;
//                vector2f infl = bot.getPos().sub(m.getLastPos());
//                moveDir.self_add( infl.cross().mul(MEMORY_MULT) );
//            }

            if(moveDir.length() < BOT_TRAPPED){ // the bot is surrounded in all directions
                // choose a random direction to move
                //System.out.println("TRAPPED");
                int rnd = m_random.nextInt(g.getNumNodes());
                nextPos = g.getNode(rnd).getPos().copy();
            }else{
                moveDir.self_normalize();
                moveDir.self_mul(INFLUENCE_SCALE); // increase the effectiveness

                // calc new move position
                nextPos = bot.getPos().add(moveDir);
            }

            m_lastUpdate = world.getTime();
            m_action = BotAction.RUN_AWAY;
        }
        // no enemies are around, search for pickups
        else if( threatExists == false && m_action != BotAction.TO_PICKUP)
        {
            // try to find pickups
            ArrayList<PickUp> pickups = world.getPickupsVisible(bot);
            int size = pickups.size();

             // search for health items & go for the closest one if any
            PickUp best = null;
            float dist = -1.0f;
            
            for ( int i = 0; i < size; i++ ) { 
                PickUp pickup = pickups.get(i);
                if ( pickup.getType().compareTo("Health") != 0) continue;
                float d = pickup.getPos().sub(bot.getPos()).length();
                if ( d < dist || dist < 0.0f ){
                    dist = d;
                    best = pickup;
                }
            }

            if(best == null) // no health items , go for any item
                best = world.getClosestVisiblePickup(bot);

            if(best != null){
                m_action = BotAction.TO_PICKUP;
                nextPos = best.getPos();
            }
        }

        //----------------------------------------------------
        // movement
        //----------------------------------------------------
        if(nextPos != null){
            bot.setSpeedBoost(BOT_SPEED_BOOST); // set speed to normal
            // move to specified position
            Node node = g.closestToPoint(nextPos);
            Path path = g.findPath(bot.m_pos, node.getPos().copy());
            if(path != null) bot.setPath(path);
        }
        else if ( pathDone ) // there's nowhere to go, so choose movement randomly
        {
            // move randomly
            int node_id = m_random.nextInt(g.getNumNodes());
            Path path = g.findPath(bot.m_pos, g.getNode(node_id).getPos() );
            if(path != null) bot.setPath(path);
            bot.setSpeedBoost(0.0f); // set normal speed
            m_action = BotAction.RANDOM;
        }
        
        // set bot's view according to the traveling direction
        bot.targetView(bot.getPos().add(bot.getVelocity()), 0.5f);
    }
    public void onExit( World world, Bot bot )
    {
        bot.setSpeedBoost(0.0f);
    }
    
    String getType(){
        return "f";
    }
}

/**
 * @(#) World.java
 */

import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.awt.event.*;

public class World
{
        static int  GAME_VERSION = 1000;
        static int  MINKOWSKI_RADI = 5;
        static int  DEFAULT_NUM_BOTS = 3;

	private ArrayList<Wall>       m_walls = new ArrayList<Wall>();
	private ArrayList<PickUp>     m_items = new ArrayList<PickUp>();
	private ArrayList<Bot>        m_bots = new ArrayList<Bot>();
        private ArrayList<Bullet>     m_bullets = new ArrayList<Bullet>();
	private Player                m_player = null; // can be null if not playing
	private Graph                 m_graph = null;
	private JComponent            m_canvas = null;
        private Random                m_random = new Random();
        private Crosshair             m_cross = new Crosshair();
        private ArrayList<InfoGfx>    m_info = new ArrayList<InfoGfx>();
        private RecordSet             m_record = new RecordSet();
        private boolean               m_drawGraph = false;
        private boolean               m_showPaths = false;
        private boolean               m_drawInfo = false;
        private boolean               m_drawRatio = false;
        private boolean               m_keys[] = {false, false, false, false}; // player movement speed
        private boolean               m_wasReset = false;

        private Minkowski             m_pixmap = null; // 2D pixel map of occupied spaces
        private int                   m_mapWidth = 0;
        private int                   m_mapHeight = 0;
        private float                 m_globalTime = 0.0f;
        private int                   m_mouseX = 0, m_mouseY = 0; // mouse screen pos

        public World() { }

        // good timing is essence
        public float getTime(){
            return m_globalTime;
        }

        // returns map bounds in pixels
        public int[] getBounds()
        {
            int bounds[] = {m_mapWidth, m_mapHeight};
            return bounds;
        }
	
	public void update( float delta_time )
	{
            // after roundreset comes a very large delta_time, it could balance out
            // the game, so ignore it, after this - work ar usual
            if(m_wasReset){
                m_wasReset = false;
                return;
            }

            m_globalTime += delta_time;
            
            if(m_graph == null) return;

            if(m_record.isRoundDone()){

                System.out.println("*** Round done! ***");
                Player gold = m_record.getMostKills(RecordSet.WinPlace.FIRST);
                Player silver = m_record.getMostKills(RecordSet.WinPlace.SECOND);
                Player bronze = m_record.getMostKills(RecordSet.WinPlace.THIRD);

                String msg = "        Round done!\n"+
                        "Best players:\n"+
                        "      1st place: "+((gold!=null)?gold.getName():"none")+"\n"+
                        "      2nd place: "+((silver!=null)?silver.getName():"none")+"\n"+
                        "      3rd place: "+((bronze!=null)?bronze.getName():"none")+"\n";


                JOptionPane.showMessageDialog(null, msg);

                m_wasReset = true;
                m_record.reset();
            }

            updateBullets(delta_time);
            updateBots(delta_time);
            updatePickUps(delta_time);
            updatePlayer(delta_time);
            updateGraphics(delta_time);
	}
	
	public void create( JComponent canvas )
	{
            // save painter for further use
            m_canvas = canvas;
	}
	
	public void destroy( )
	{
            //do cleanup here
	}
	
	public void draw( Graphics2D painter, boolean drawDebug )
	{
            painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            if(m_pixmap != null) m_pixmap.visualize(painter);

            // draw all lines
            for(int i=0; i<m_walls.size(); i++)
                m_walls.get(i).draw(painter);

            // draw graph
            if ( m_graph != null && m_drawGraph )
                m_graph.draw(painter);

            // draw Pickup's
            for ( int l = 0; l < m_items.size(); l++ )
                 m_items.get(l).draw(painter);

            painter.setColor(Color.BLACK);

            // draw bullets
            for( int i=0; i<m_bullets.size(); i++)
                m_bullets.get(i).draw(painter);

            // draw bots
            Player gold = m_record.getMostKills(RecordSet.WinPlace.FIRST);
            Player silver = m_record.getMostKills(RecordSet.WinPlace.SECOND);
            Player bronze = m_record.getMostKills(RecordSet.WinPlace.THIRD);

            for(int i=0; i<m_bots.size(); i++){
                Bot b = m_bots.get(i);

                // show winners by color
                Color bodyColor = RecordSet.DEFAULT_COLOR;
                if(b == gold)        bodyColor = RecordSet.PLACE_1st_COLOR;
                else if(b == silver) bodyColor = RecordSet.PLACE_2nd_COLOR;
                else if(b == bronze) bodyColor = RecordSet.PLACE_3rd_COLOR;

                m_bots.get(i).draw(painter, bodyColor, m_showPaths, m_drawInfo);
            }

            // draw player
            if(m_player != null) {
                Color bodyColor = RecordSet.DEFAULT_USER_COLOR;
                if(m_player == gold)        bodyColor = RecordSet.PLACE_1st_COLOR;
                else if(m_player == silver) bodyColor = RecordSet.PLACE_2nd_COLOR;
                else if(m_player == bronze) bodyColor = RecordSet.PLACE_3rd_COLOR;
                m_player.draw(painter, bodyColor, m_drawInfo);
                m_cross.draw(painter);
            }

            // draw info
            for(int i=0; i<m_info.size(); i++)
                m_info.get(i).draw(painter);
            
            // draw records
            if(m_drawRatio){
                ArrayList<Player> all_players = new ArrayList<Player>(m_bots);
                if(m_player != null) all_players.add(m_player);

                for(int i=0; i<all_players.size(); i++)
                {
                    Player p = all_players.get(i);
                    RecordSet.Record r = m_record.getRecord(p);
                    if(r != null){
                        String val = Integer.toString(r.m_kills)+'/'+Integer.toString(r.m_deaths);
                        painter.drawBytes(val.getBytes(), 0, val.length(),
                                (int)p.getPos().x, (int)p.getPos().y);
                    }
                }
            }
	}


        // reset game world to the beggining
        private void reset()
        {
            m_walls.clear();
            m_items.clear();
            m_bots.clear();
            m_bullets.clear();
            m_record.reset();
        }

        // reset gameplay
        public void resetGame()
        {
            m_bullets.clear();
            if(m_bots.size()>0)
                this.setBotCount(m_bots.size());
        }

        private void updateBots(float delta_time)
        {
            // update bots
            for(int i=0; i<m_bots.size(); i++){
                Bot b = m_bots.get(i);
                b.update(this, delta_time);

                // revive dead bots at random location
                if(b.isDead()){
                    int rnd = m_random.nextInt(m_graph.getNumNodes());
                    Node n = m_graph.getNode(rnd);
                    if(n != null){
                        b.reset();
                        b.setPos(n.getPos().copy());
                    }

                    // remove memory about this dead bot from other bots' heads
                    for(int j=0; j<m_bots.size(); j++)
                        m_bots.get(j).removeMemory(b);

                    m_record.addDeath(b); // record that this bot was killed
                }
            }


            ////////////////////////////////////////////////////////////////
            // Check bot-2-bot collision (including player!)
            // Could be improved, by sending bot player in opposite directions
            //       when a collision happens(using the setPath..)
            ////////////////////////////////////////////////////////////////
            ArrayList<Player> collist = new ArrayList<Player>(m_bots);
            if(m_player != null) collist.add(m_player);

            for(int i=0; i<collist.size(); i++)
            {
                Player b = collist.get(i);
                
                for(int j=0; j<collist.size(); j++){
                    if(i==j) continue; // no self intersect
                    Player col = collist.get(j);

                    // players' circles are intersecting
                    if(utils2d.intersect_spheres(b.getPos(), Bot.BOT_RADIUS, col.getPos(), Bot.BOT_RADIUS))
                    {
                        vector2f c_dir = b.getPos().sub(col.getPos()); // vector to player centers
                        float dist = c_dir.self_normalize();

                        // force the players to safe position by shifting them in opposite sides
                        // by value 'offset/2' so that the circles won't intersect
                        float offset = Bot.BOT_RADIUS*2 - dist;
                        b.setPos( b.getPos().add(c_dir.mul(offset*0.5f)) );
                        col.setPos( col.getPos().add(c_dir.mul(-offset*0.5f)) );
                        
                    }
                }
            }

        }

        // updates pickup state & checks if any of the players pick something
        private void updatePickUps(float delta_time)
        {
            // update pickups
            ArrayList<Player> players = new ArrayList<Player>(m_bots);
            if(m_player != null) players.add(m_player);

            for(int i=0; i<m_items.size(); i++){
                PickUp item = m_items.get(i);

                // update & regenerate pickups
                item.update(delta_time);
                
                if(item.isPicked()) continue; // dont test fetched items

                // apply pickup to the player
                for(int j=0; j<players.size(); j++){
                    Player b = players.get(j);
                    float dist = b.getPos().sub(item.getPos()).length();

                    if(dist <= Bot.BOT_RADIUS+PickUp.DRAW_SIZE){
                        if(item.getType().compareTo(PickUp.TYPE_HEALTH) == 0 )
                            b.heal(item.getAmount());
                        else if(item.getType().compareTo(PickUp.TYPE_AMMO) == 0)
                            b.getWeapon().addAmmo(item.getAmount());
                        popInfo(b.getPos().copy(), "+"+item.getAmount(), Color.GREEN);
                        item.pick();
                        break;
                    }
                }

                
            }
        }

         // updates bullets state & checks if any of the players are hit
        private void updateBullets(float delta_time)
        {
            ArrayList<Player> players = new ArrayList<Player>(m_bots);
            if(m_player != null) players.add(m_player);

            ArrayList<Bullet> kill_list = new ArrayList<Bullet>();


            // injure a player if a bullets hits it
            for(int i=0; i<m_bullets.size(); i++){
                Bullet bullet = m_bullets.get(i);
                bullet.update(delta_time); // update

                // wall intersect check
                if(m_pixmap.isInside(bullet.getPos())){
                    kill_list.add(bullet);
                    continue;
                }

                // apply damage if anyone is hit
                for(int j=0; j<players.size(); j++){
                    Player b = players.get(j);

                    // don't damage bullet owner (just in case)
                    if(bullet.getOwner() == b) continue; 
                    
                    float dist = b.getPos().sub(bullet.getPos()).length();
                    if(dist <= Bot.BOT_RADIUS+Bullet.SIZE){
                        b.dealDamage(bullet.getOwner(), Bullet.DAMAGE);
                        kill_list.add(bullet); // remove later

                        if(b.isDead()) m_record.addKill(bullet.getOwner()); // one frag for a player!
                        m_record.addHit(bullet.getOwner()); // record a successful hit

                        // add some graphix ;)
                        popInfo(b.getPos().copy(), "-"+Bullet.DAMAGE, Color.RED);
                        break;
                    }
                }

                // add to be removed later
                if(bullet.isDead()) kill_list.add(bullet);
            }

            m_bullets.removeAll(kill_list); // remove used
        }

        private void updateGraphics(float delta_time)
        {
            // update graphical info
            ArrayList<InfoGfx> eraselist = new ArrayList<InfoGfx>();
            for(int i=0; i<m_info.size(); i++){
                InfoGfx info =  m_info.get(i);
                info.update(delta_time);
                if(info.isDead()) eraselist.add(info);
            }
            m_info.removeAll(eraselist);
        }

        private void updatePlayer(float delta_time)
        {
            // update the player - if he's playing only!
            if(m_player == null) return;

            m_cross.update(m_mouseX, m_mouseY); // new crosshair pos

            // compute view direction from mouse pos
            vector2f mouse = new vector2f(m_mouseX, m_mouseY);
            vector2f viewDir = mouse.sub(m_player.getPos());
            viewDir.self_normalize();
            m_player.setView(viewDir);

            // handle keys
            {
                vector2f velSum = new vector2f(0,0);
                vector2f perp = m_player.getView().cross(); // for player strafing
                
                vector2f move_dirs[] = {
                    m_player.getView().mul(Player.MOVE_SPEED), // move front
                    m_player.getView().mul(-Player.MOVE_SPEED), // move back
                    perp.mul(Player.MOVE_SPEED), // strafe left
                    perp.mul(-Player.MOVE_SPEED) // strafe right
                };
                
                for(int i=0; i<4; i++)
                    if(m_keys[i]) velSum.self_add(move_dirs[i]);

                if(velSum.equals(0,0) == false){
                    velSum.self_normalize();
                    velSum.self_mul(Player.MOVE_SPEED);
                }

                m_player.setVelocity(velSum);
            }

            // check collision against walls
            for(int i=0; i<m_walls.size(); i++)
            {
                Wall w = m_walls.get(i);
 
                // test player distance to a wall
                vector2f pt = utils2d.point_on_line(w.getStart(), w.getEnd(), m_player.getPos());
                vector2f dir = m_player.getPos().sub(pt);
                float dist = dir.self_normalize() - this.MINKOWSKI_RADI;

                // player is intersecting the line!
                if(dist < Player.BOT_RADIUS && dist > -Player.BOT_RADIUS){
                    // shift the player off the line so that he won't intersect
                    float offset = Player.BOT_RADIUS - dist;
                    vector2f newPos = m_player.getPos().add(dir.mul(offset));
                    m_player.setPos(newPos);
                }
            }

            // time for update
            m_player.update(this, delta_time);

            // revive player if he's dead
            if(m_player.isDead() && m_graph != null){

                m_record.addDeath(m_player); // record that player was killed

                // remove memory about this player from other bots' heads
                for(int j=0; j<m_bots.size(); j++){
                    m_bots.get(j).removeMemory(m_player);
                }

                // place new player on a random graph node
                int id = m_random.nextInt(m_graph.getNumNodes());
                vector2f startPos = m_graph.getNode(id).getPos().copy();
                m_player.reset();
                m_player.setPos(startPos);
            }
            
            
        }

        // handles keyboard input
        public void keyEvent(int event, int code)
        {
            if(m_player == null) return;

            if(event == KeyEvent.KEY_PRESSED){
                switch(code){
                    case KeyEvent.VK_UP:    m_keys[0] = true; break;
                    case KeyEvent.VK_DOWN:  m_keys[1] = true; break;
                    case KeyEvent.VK_LEFT:  m_keys[2] = true; break;
                    case KeyEvent.VK_RIGHT: m_keys[3] = true; break;
                    default: break;
                }
            }else if(event == KeyEvent.KEY_RELEASED){
                switch(code){
                    case KeyEvent.VK_UP:    m_keys[0] = false; break;
                    case KeyEvent.VK_DOWN:  m_keys[1] = false; break;
                    case KeyEvent.VK_LEFT:  m_keys[2] = false; break;
                    case KeyEvent.VK_RIGHT: m_keys[3] = false; break;
                    default: break;
                }
            }
        }

        // fetches new mouse coords
        public void updateMouse(int x, int y)
        {
            m_mouseX = x;
            m_mouseY = y;
        }

        // mouse was clicked event
        public void mouseClick()
        {
            //shoot from the gun when a human player is playing
            if(m_player != null && m_player.getWeapon().canShoot(m_globalTime)){
                
                vector2f dir = m_player.getView(); // shoot only from the gun endpoint
                vector2f viewShift = dir.mul(Player.POINTER_SZ+Bullet.SIZE);
                vector2f emitPos = m_player.getPos().add(viewShift);
                m_player.getWeapon().shoot( m_globalTime );

                this.addBullet(new Bullet(m_player, emitPos, dir.mul(Bullet.SPEED)));
            }
        }

        // loads the map made in the editor and creates items and walls
	public void loadMap( String fileName )
	{
            System.out.println("Opening file "+fileName);
            int node_x = 0;
            int node_y = 0;
            try {
                RandomAccessFile raf = new RandomAccessFile(fileName, "rw");

                int magic = raf.readInt();
                int ver = raf.readInt();

                if ( magic == 0xC0FFEE && ver >= GAME_VERSION)  { // check magic

                    this.reset();

                    m_mapWidth = raf.readInt();
                    m_mapHeight = raf.readInt();

                    node_x = raf.readInt();
                    node_y = raf.readInt();
                    int lines = raf.readInt();
                    int items = raf.readInt();

                    for (int i = 0; i <  lines ; i++ )  { /* reading lines */
                        vector2f p0 = new vector2f(raf.readInt(), raf.readInt());
                        vector2f p1 = new vector2f(raf.readInt(), raf.readInt());
                        m_walls.add(new Wall(p0, p1));
                    }
                    
                    for ( int i = 0; i< items; i++ )  { /* reading items */
                        int x = raf.readInt();
                        int y = raf.readInt();
                        int n_t = raf.readInt();

                        // deal with item type
                        char[] typebuf = new char[n_t];
                        for ( int j = 0; j < n_t; j++ )
                            typebuf[j] = raf.readChar();
                     
                        String typeStr = "";
                        for ( int h = 0; h<typebuf.length; h++ )
                            typeStr += typebuf[h];

                        int amount = raf.readInt();

                        m_items.add(new PickUp(new vector2f(x,y), typeStr, amount));

                    }

                    //this.SetMapDims(n_width, n_height);
                }else{
                    throw new Exception("Invalid map file or unsupported version.");
                }

                raf.close();
                System.out.println("Map loaded.");
                m_graph = new Graph(this);
                m_graph.build((float)node_x, (float)node_y);
                
                m_pixmap = new Minkowski();
                m_pixmap.build(this, MINKOWSKI_RADI);

                setBotCount(DEFAULT_NUM_BOTS);
                
            } catch ( FileNotFoundException e ) {
                ShowMessage("File not found error.");
                e.printStackTrace();
            }
            catch ( IOException e ) {
                ShowMessage("File I/O exception.");
                e.printStackTrace();
            }
            catch( Exception e){
                ShowMessage("Unknown error when loading map. "+ e.toString());
                e.printStackTrace();
            }
	}

        public void setBotCount(int num)
        {
            if(num <= 0) return;
            if(m_graph == null){
                ShowMessage("No map loaded!");
                return;
            }
            
            m_bots.clear();
            int num_nodes = m_graph.getNumNodes();
            for(int i=0; i<num; i++){
                int n_id = m_random.nextInt(num_nodes);
                Node node = m_graph.getNode(n_id);
                if(node == null) continue;
                this.addBot("Bot"+i, node.getPos().copy());
            }

            m_record.reset(); // bots have changed - so the results!
        }

        // adds a bot to the game
	public void addBot( String name, vector2f pos )
	{
            m_bots.add(new Bot(this, name, pos));
	}

        // show a graphical message that floats up & vanishes after some time
        public void popInfo(vector2f pos, String text, Color color)
        {
            m_info.add(new InfoGfx(pos.copy(), text, color));
        }
	
	public PickUp getItem( int index )
	{
            return m_items.get(index);
	}
	
	public int getNumBots( )
	{
            return m_bots.size();
	}
	
	public int getNumItems( )
	{
            return m_items.size();
	}

        public void addBullet(Bullet b){
            m_record.addShot(b.getOwner()); // record
            m_bullets.add(b);
        }
	
	public Graph getGraph( )
	{
            return m_graph;
	}
	
	public int getNumWalls( )
	{
            return m_walls.size();
	}
	
	public Wall getWall( int index )
	{
            return m_walls.get(index);
	}
	
        // Quick ShowMessage message implementation
        private void ShowMessage(String msg)
        {
            JOptionPane.showMessageDialog(null, msg);
        }
        
        public void setMapWidth( int width )  {
            m_mapWidth = width;
        }

        public void setMapHeight( int height )
        {
            m_mapHeight = height;
        }

        public int getMapWidth( int width )
        {
            return m_mapWidth;
        }

        public int getMapHeight( int height )
        {
            return m_mapHeight;
        }

        public void toggleGraphDraw()
        {
            this.m_drawGraph = !this.m_drawGraph;
            if(m_canvas != null) m_canvas.repaint();
        }

        public void togglePaths()
        {
            this.m_showPaths = !this.m_showPaths;
            if(m_canvas != null) m_canvas.repaint();
        }

        // inserts/removes player in game
        public void togglePlayer()
        {
            if(m_graph == null) return;

            // place player on a random graph node
            int id = m_random.nextInt(m_graph.getNumNodes());
            vector2f startPos = m_graph.getNode(id).getPos().copy();
            m_player = (m_player==null)? new Player(Player.HUMAN_ID, startPos) : null;

            m_record.reset(); // player left/showd up, start the counting from zero
        }

        public void toggleRatio()
        {
            this.m_drawRatio = !this.m_drawRatio;
            if(m_canvas != null) m_canvas.repaint();
        }

        // show/hide FOV cone, state info
        public void toggleInfo()
        {
            this.m_drawInfo = !this.m_drawInfo;
            if(m_canvas != null) m_canvas.repaint();
        }

        // -----------------------------------------------------------
        //                * auxiliary functions *
        // -----------------------------------------------------------
        
        // returns true if given line collides/intersects with any wall
        public boolean isColliding( vector2f p1, vector2f p2 )  {
            for ( int i = 0; i < m_walls.size(); i++ )  {
                Wall wall = m_walls.get(i);
                if( utils2d.intersect_lines(p1, p2, wall.getStart(), wall.getEnd()) != null)
                   return true;
            }
            return false;
        }

        // returns an array with all players that are near a specified position
        // not farther than radius
	public ArrayList<Player> getPlayersNearPos( vector2f pos, float radius )
	{
            ArrayList<Player> list = new ArrayList<Player>();

            // compose a list of all players to check
            ArrayList<Player> checklist = new ArrayList<Player>(m_bots);
            if(m_player != null) checklist.add(m_player);
            
            // check
            for(int i=0; i<checklist.size(); i++){
                Player b = checklist.get(i);
                float d = b.getPos().sub(pos).length();
                if(d <= radius)
                    list.add(b);
            }

            return list;
	}

        // returns visible players(enemies) seen from the bots point of view
        // including bot field of vision cone and max visible distance
        // Note: check Bot for FOV and  distance constants!
	public ArrayList<Player> getEnemiesVisible( Bot bot )
	{
            // bot info
            float rads = (float)Math.atan2(bot.getView().y, bot.getView().x);
            float angle = (float)Math.toDegrees(rads);

            ArrayList<Player> list = new ArrayList<Player>();

            // compose a list of all players to check
            ArrayList<Player> checklist = new ArrayList<Player>(m_bots);
            if(m_player != null) checklist.add(m_player);

            int num = checklist.size();
            for(int i=0; i<num; i++)
            {
                Player p = checklist.get(i);
                if(p == bot) continue; // dont test self

                vector2f dir = p.getPos().sub(bot.getPos());
                float deg = (float)Math.toDegrees( Math.atan2(dir.y, dir.x) );

                // check by FOV
                if( Math.abs(deg-angle) > Bot.BOT_FOV/2 ) continue; // outside

                // check by distance
                if( dir.length() > Bot.MAX_VISION ) continue; // can't see
                
                // vis check
                if(isColliding(p.getPos(), bot.getPos())) continue;

                list.add(p);
            }

            return list;
	}

         // returns closest visible enemy for the bot or null if none
        public Player getClosestEnemy( ArrayList<Player> list, Bot bot )
	{
            if(list != null && list.size() > 0)
            {
                Player closest = null;
                float dist = -1.0f;

                for(int i=0; i<list.size(); i++){
                    Player p = list.get(i);
                    float d = p.getPos().sub(bot.getPos()).length();
                    if(d < dist || dist < 0.0f){
                        closest = p;
                        dist = d;
                    }
                }

                return closest;
            }

            return null;
	}

        // returns visible pickups seen from the bots point of view by FOV & viewing distance
	public ArrayList<PickUp> getPickupsVisible( Bot bot )
	{
            // bot info
            float rads = (float)Math.atan2(bot.getView().y, bot.getView().x);
            float angle = (float)Math.toDegrees(rads);

            ArrayList<PickUp> list = new ArrayList<PickUp>();
            
            int num = m_items.size();
            for(int i=0; i<num; i++)
            {
                PickUp p = m_items.get(i);
                if(p.isPicked()) continue; // already picked by someone

                vector2f dir = p.getPos().sub(bot.getPos());
                float deg = (float)Math.toDegrees( Math.atan2(dir.y, dir.x) );

                // check by FOV
                if( Math.abs(deg-angle) > Bot.BOT_FOV/2 ) continue; // outside

                // check by distance
                if( dir.length() > Bot.MAX_VISION ) continue; // can't see

                // vis check
                if(isColliding(p.getPos(), bot.getPos())) continue;
                
                list.add(p);
            }

            return list;
	}

        // returns closest visible pickup for the bot or null if none
        public PickUp getClosestVisiblePickup( Bot bot )
	{
            ArrayList<PickUp> list = this.getPickupsVisible(bot);

            if(list != null && list.size() > 0)
            {
                PickUp closest = null;
                float dist = -1.0f;

                for(int i=0; i<list.size(); i++){
                    PickUp p = list.get(i);
                    float d = p.getPos().sub(bot.getPos()).length();
                    if(d < dist || dist < 0.0f){
                        closest = p;
                        dist = d;
                    }
                }

                return closest;
            }

            return null;
	}
       
}

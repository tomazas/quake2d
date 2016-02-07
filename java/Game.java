/**
 * @(#) Game.java - Main game class
 */

import java.awt.*;
import javax.swing.*;

public class Game implements Runnable
{
        public enum Message{
            LOAD_MAP,     // send this with filename to load a new map
            SIMULATE,     // send this with no parameters to start game sim
            TOGGLE_GRAPH, // change graph drawing mode
            TOGGLE_PLAYER,// insert/remove player
            TOGGLE_PATHS, // show/hide path drawing
            TOGGLE_INFO,  // show/hide aditional info (state info, FOV)
            TOGGLE_RATIO, // show/hide players' kills/deaths ratio
            BOT_COUNT,    // change bot count, pass a number through params
            KEY_EVENT,    // signals that a key was pressed, event type & keycode in params
            MOUSE_MOVE,   // send a new pair of x,y coords of the mouse pos on the window
            MOUSE_CLICK,  // mouse was clicked event, no params
            RESET,        // resets the starting game position
            BAKE_CAKE     // not used ;)
        };

        static int updateInterval = 30; // thread sleep time in ms

	private boolean     m_done = false;
        private boolean     m_running = false;
	private Thread      m_thread = null;
	private GameState   m_currState = null;
        private long        m_prevTime = 0;
        private JComponent  m_canvas = null;
        private long        m_stepSize = -1; // not stepping
        private long        m_launchTime = 0;

        public Game(){ }
	
	// initializes game
	public void initialize(JComponent canvas)
	{
            m_canvas = canvas;

            // we dont have more states (like: meniu state)
            // so just go to gameplay directly
            this.changeState( new PlayState() );
	}

        //  starts the game (pass stepSize = -1 to run normally, or >0 to step by this size)
        public void launch(long stepSize)
        {
            if(m_running) return; // already launched, need to stop first

            // reset simulation start offset
            m_launchTime = System.currentTimeMillis();
            m_prevTime = m_launchTime;
            m_stepSize = stepSize;
           
            m_done = false;
            m_thread = new Thread(this);
            m_thread.start();

            // start the game simulation
            m_currState.onMessage(Message.SIMULATE, null);
        }

        // signal state to repaint graphics this is called from a panel component
        public void repaint(Graphics2D painter)
        {
            if(m_currState != null)
                m_currState.onRepaint(painter);
        }
	
	// this method is run in a thread so that it wont block the main app
        // DONT CALL DIRECTLY - by using a thread it paints the screen in fixed fps
	public void run( )
	{
            m_running = true;
            System.out.println("Game runing...");

            while(!m_done)
            {
                try{
                    Thread.sleep(updateInterval);  
                }catch(InterruptedException e ){ }

                long t = System.currentTimeMillis();
                if(m_prevTime == 0) m_prevTime = t; // first time init
                float delta = (t - m_prevTime) * 0.001f;

                m_currState.onExecute(delta);
                m_prevTime = t;

                // stop the game if this is a step!
                if(m_stepSize > 0 && t - m_launchTime >= m_stepSize){
                    m_running = false;
                    m_done = true;
                }
            }

            System.out.println("Game done.");
	}

        // returns the game state
        boolean isRunning(){
            return m_running;
        }
	
	// stops the thread from running, ends everything
	public void finalize( )
	{
            m_running = false;
            m_done = true; // stop the thread
	}

        // send a message to the game state
        public void postMessage(Message message, String params[])
        {
            if(m_currState != null) m_currState.onMessage(message, params);
        }
	
	// let's you change the game state like: meniu state, playing state, etc.
	public void changeState( GameState newState )
	{
            System.out.println("[!] ChangeState to: "+newState.toString());
            if(m_currState != null) m_currState.onExit();
            newState.onEnter( m_canvas );
            m_currState = newState;
	}
}
